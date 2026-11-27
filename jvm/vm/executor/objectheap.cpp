#include "stdafx.h"
#include "objectheap.h"
#include "context.h"
#include "utils/CommonUtils.h"
#include "methodarea.h"
#include "executor.h"
#include "threadstack.h"
#include "stackframe.h"
#include "descriptor.h"
#include "vm/classfiles/classloader.h"
#include "vm/classfiles/constant.h"
#include "vm/exceptions.h"
#include "jni/jni.h"
#include "scheduler.h"
#include "vmmutex.h"
#include "vm/executor/gc/gccollector.h"
#include "vm/executor/gc/gcbarrier.h"
#include "javaexception.h"

#include <string>
#include <chrono>
#include <cstdlib>
#include <cstring>
#include <thread>

#if defined(_WIN32)
#include <windows.h>
#else
#include <unistd.h>
#endif

namespace {

bool GcStatsEnabled()
{
	static const bool enabled = getenv("DARKS_GCSTATS") != NULL;
	return enabled;
}

typedef std::chrono::steady_clock GcClock;

// Lazy sweep chunk sizes.  A dead object costs roughly a microsecond to tear
// down (destructor, free, index erase), so these bound the extra latency of
// one allocation and of the post-pause drain to a few microseconds each.
const size_t kAllocSweepChunk = 32;
const size_t kPostPauseSweepChunk = 2048;

// Sliced marking budget.  A bounded pause scans a sixteenth of the slot table,
// clamped to this range, and never spends more than kMarkSliceMillis of trace
// work (petclinic traces an object in roughly 0.6 us, so the upper bound is
// about 80 ms).  Progress is paced by the allocation counter because
// live_bytes still counts the garbage of the open cycle and therefore stays
// above the collection threshold.
const size_t kMarkSliceMinObjects = 8 * 1024;
const size_t kMarkSliceMaxObjects = 128 * 1024;
const double kMarkSliceMillis = 80.0;
const size_t kMarkAllocationsPerSlice = 2048;
const size_t kMarkRecordsPerSlice = 64 * 1024;

// Physical memory used for the -Xmx/-Xms ergonomics.  Matches what the JDK
// reports through sysconf(_SC_PHYS_PAGES) (GlobalMemoryStatusEx on Windows).
size_t DetectPhysicalMemory()
{
#if defined(_WIN32)
	MEMORYSTATUSEX status;
	status.dwLength = sizeof(status);
	if (GlobalMemoryStatusEx(&status))
	{
		return static_cast<size_t>(status.ullTotalPhys);
	}
	return 0;
#else
	long pages = sysconf(_SC_PHYS_PAGES);
	long page_size = sysconf(_SC_PAGESIZE);
	if (pages > 0 && page_size > 0)
	{
		return static_cast<size_t>(pages) * static_cast<size_t>(page_size);
	}
	return 0;
#endif
}

// Round down to a whole MiB, like the JDK's heap sizing.
size_t AlignToMiB(size_t bytes)
{
	return bytes & ~static_cast<size_t>(1024 * 1024 - 1);
}

double MillisecondsSince(const GcClock::time_point &start)
{
	return std::chrono::duration<double, std::milli>(GcClock::now() - start)
		.count();
}

size_t ElementBytes(DATA_TYPE data_type)
{
	switch (data_type)
	{
	case DT_BOOL:
	case DT_BYTE:
		return 1;
	case DT_CHAR:
	case DT_SHORT:
		return 2;
	case DT_INT:
	case DT_FLOAT:
		return 4;
	case DT_LONG:
	case DT_DOUBLE:
		return 8;
	case DT_ARRAY:
	case DT_REF:
		return sizeof(jobject);
	default:
		return 0;
	}
}

} // namespace

namespace {

lpFieldValue FindFieldValue(lpObject obj, const char *name)
{
	if (!obj || !name)
	{
		return NULL;
	}
	std::string key(name);
	FIELD_MAP::iterator it = obj->varants.find(key);
	if (it != obj->varants.end())
	{
		return it->second;
	}
	key += ":";
	for (it = obj->varants.begin(); it != obj->varants.end(); ++it)
	{
		if (it->first.compare(0, key.size(), key) == 0)
		{
			return it->second;
		}
	}
	return NULL;
}

bool IsReferenceObject(lpObject obj)
{
	if (!obj || !obj->obj_class)
	{
		return false;
	}
	for (lpClass clazz = obj->obj_class; clazz; clazz = clazz->super_class)
	{
		if (clazz->thisClass && clazz->thisClass->GetName() &&
		    strcmp(clazz->thisClass->GetName(), "java/lang/ref/Reference") == 0)
		{
			return true;
		}
	}
	return false;
}

} // namespace

ObjectHeap::~ObjectHeap()
{
	delete collector;
	collector = NULL;
	if (gc_log && gc_runs > 0)
	{
		fprintf(stderr, "gc-summary runs=%d freed_total=%zu bytes\n",
			gc_runs, gc_freed_total);
	}
	for (size_t i = 0; i < objs.size(); i++)
	{
		if (objs[i])
		{
			delete objs[i];
			objs[i] = NULL;
		}
	}
	objs.clear();
	sequence = 0;
}

void ObjectHeap::SetHeapLimits(size_t initial_bytes, size_t max_bytes)
{
	requested_initial_heap = initial_bytes;
	requested_max_heap = max_bytes;
}

void ObjectHeap::ResolveHeapLimits()
{
	const size_t physical = DetectPhysicalMemory();
	size_t max_bytes = requested_max_heap;
	if (max_bytes == 0)
	{
		// JDK default: MaxRAMPercentage=25 of physical memory.
		max_bytes = physical / 4;
		// -Xms alone raises the default maximum so the request stays valid.
		if (requested_initial_heap > max_bytes)
		{
			max_bytes = requested_initial_heap;
		}
	}
	if (max_bytes == 0)
	{
		// Physical memory could not be detected; fall back to 1 GiB rather
		// than clamping the heap to the 1 MiB minimum below.
		max_bytes = 1024ULL * 1024 * 1024;
	}
	max_bytes = AlignToMiB(max_bytes);
	if (max_bytes < 1024 * 1024)
	{
		max_bytes = 1024 * 1024;
	}

	size_t initial_bytes = requested_initial_heap;
	if (initial_bytes == 0)
	{
		// JDK default: InitialRAMPercentage=1.5625, i.e. -Xmx/16.
		initial_bytes = AlignToMiB(max_bytes / 16);
	}
	initial_bytes = AlignToMiB(initial_bytes);
	if (initial_bytes < 1024 * 1024)
	{
		initial_bytes = 1024 * 1024;
	}
	if (initial_bytes > max_bytes)
	{
		initial_bytes = max_bytes;
	}

	max_heap_bytes = max_bytes;
	initial_heap_bytes = initial_bytes;
}

bool ObjectHeap::WouldExceedMax(size_t extra)
{
	if (max_heap_bytes == 0 || oom_in_progress ||
	    VMContext::pending_exception)
	{
		return false;
	}
	if (live_bytes + extra <= max_heap_bytes)
	{
		return false;
	}
	// The accounting still counts the dead set of the last cycle until the
	// sweeper reaches it.  Under pressure free just enough of it to compare
	// against the real live size instead of reporting an OOM that the pending
	// reclaim would have avoided.
	if (cms_sweep_pending)
	{
		VMLock lk;
		while (cms_sweep_pending && live_bytes + extra > max_heap_bytes)
		{
			CmsSweepScanLocked(4096);
		}
	}
	return live_bytes + extra > max_heap_bytes;
}

void ObjectHeap::ThrowHeapOOM()
{
	if (VMContext::pending_exception)
	{
		return;
	}
	oom_in_progress = true;
	VMContext::pending_exception = CreateJavaException(
		"java/lang/OutOfMemoryError", "Java heap space");
	oom_in_progress = false;
}

bool ObjectHeap::initialize(int init_size)
{
	if (init_size < 1)
	{
		init_size = 1;
	}
	ResolveHeapLimits();
	// -Xms reserves object slots up front; 64 bytes is the average footprint
	// seen on the Spring Boot workload, so the slot vector is not the limit.
	size_t slots = static_cast<size_t>(init_size);
	const size_t slots_for_initial = initial_heap_bytes / 64;
	if (slots_for_initial > slots)
	{
		slots = slots_for_initial;
	}
	objs.assign(slots, NULL);
	sequence = 1;
	ConfigureFromEnvironment();
	if (ref_index_incremental)
	{
		// One entry per live object.  Reserving up front keeps the hash table
		// from rehashing while objects are registered one by one; the slot
		// vector was already sized for -Xms just above.
		ref_index.reserve(objs.size());
	}
	const char *impl_env = getenv("DARKS_JVM_GC_IMPL");
	if (collector)
	{
		delete collector;
		collector = NULL;
	}
	collector = CreateGCCollector(
		(impl_env && *impl_env) ? impl_env : "cms", this);
	if (collector)
	{
		collector->Initialize(this);
		cms_mode = strcmp(collector->Name(), "cms") == 0;
		cms_gc_deferred.store(false);
	}
	if (getenv("DARKS_GC_SATB_TEST"))
	{
		GCBarrier::Enable();
	}
	if (GcStatsEnabled())
	{
		fprintf(stderr,
		        "[gcinit] heap_min=%zu heap_max=%zu slots=%zu "
		        "base_threshold=%zu\n",
		        initial_heap_bytes, max_heap_bytes, objs.size(),
		        base_threshold);
	}
	return true;
}

void ObjectHeap::ConfigureFromEnvironment()
{
	const char *gc_env = getenv("DARKS_JVM_GC");
	if (gc_env && *gc_env && strcmp(gc_env, "0") != 0 && strcmp(gc_env, "off") != 0)
	{
		gc_enabled = true;
	}
	else if (gc_env && *gc_env)
	{
		gc_enabled = false;
	}

	const char *log_env = getenv("DARKS_JVM_GC_LOG");
	gc_log = (log_env && *log_env && strcmp(log_env, "0") != 0);

	const char *threshold_env = getenv("DARKS_JVM_GC_THRESHOLD");
	bool threshold_overridden = false;
	if (threshold_env && *threshold_env)
	{
		unsigned long long value = strtoull(threshold_env, NULL, 10);
		if (value >= 1024)
		{
			base_threshold = static_cast<size_t>(value);
			threshold_overridden = true;
		}
	}
	// -Xms raises the initial collection threshold so startup does not collect
	// every few megabytes; DARKS_JVM_GC_THRESHOLD still wins when set.
	if (!threshold_overridden && initial_heap_bytes > base_threshold)
	{
		base_threshold = initial_heap_bytes;
	}
	threshold = base_threshold;

	// The reference index is maintained incrementally by default; "full"
	// restores the M2a rebuild at the start of every collection, which is the
	// fallback if the incremental bookkeeping ever misbehaves.
	const char *ref_index_env = getenv("DARKS_JVM_REFINDEX");
	ref_index_incremental = true;
	if (ref_index_env && *ref_index_env &&
	    (strcmp(ref_index_env, "full") == 0 || strcmp(ref_index_env, "rebuild") == 0 ||
	     strcmp(ref_index_env, "0") == 0))
	{
		ref_index_incremental = false;
	}

	// Dead objects are destroyed outside the pause by default; "pause"
	// restores the stop-the-world sweep (the M2b behaviour) as the fallback.
	const char *sweep_env = getenv("DARKS_JVM_SWEEP");
	lazy_sweep = true;
	if (sweep_env && *sweep_env &&
	    (strcmp(sweep_env, "pause") == 0 || strcmp(sweep_env, "inline") == 0 ||
	     strcmp(sweep_env, "0") == 0))
	{
		lazy_sweep = false;
	}

	// Marking runs in bounded slices by default; "stw" restores the single
	// stop-the-world pause as the fallback.
	const char *mark_env = getenv("DARKS_JVM_MARK");
	incremental_mark = true;
	if (mark_env && *mark_env &&
	    (strcmp(mark_env, "stw") == 0 || strcmp(mark_env, "pause") == 0 ||
	     strcmp(mark_env, "0") == 0))
	{
		incremental_mark = false;
	}
	// The full index is only rebuilt while the world is stopped, so it cannot
	// describe objects allocated during an open cycle; that fallback keeps the
	// single-pause collection.
	if (!ref_index_incremental)
	{
		incremental_mark = false;
	}
}

size_t ObjectHeap::ObjectBytes(lpObject obj) const
{
	if (!obj)
	{
		return 0;
	}
	if (cms_world_stopped.load())
	{
		// Bookkeeping for tests: the concurrent collector must not measure
		// objects inside a pause.
		cms_object_bytes_in_pause.fetch_add(1);
	}
	// Approximate footprint: the object header, the per-field value boxes and
	// the array payload. Field map overhead is not counted exactly, which only
	// makes the threshold slightly pessimistic.
	size_t bytes = sizeof(JObject) + obj->varants.size() * FieldEntryBytes();
	if (obj->array_var && obj->array_length > 0)
	{
		bytes += static_cast<size_t>(obj->array_length) * ElementBytes(obj->array_data_type);
	}
	return bytes;
}

void ObjectHeap::AccountAllocation(lpObject obj)
{
	if (!obj)
	{
		return;
	}
	live_bytes += ObjectBytes(obj);
	live_objects++;
}

void ObjectHeap::AccountFieldGrowth(size_t bytes)
{
	if (bytes == 0)
	{
		return;
	}
	VMLock lk;
	live_bytes += bytes;
}

void ObjectHeap::MaybeGC()
{
	// Read the accounting under the heap mutex (allocations update it there),
	// then collect without it: the collector stops the world, and the
	// safepoint handshake must not run with the heap mutex already held.
	bool needed = false;
	bool near_max = false;
	bool abort_cms = false;
	{
		VMLock lk;
		// The OutOfMemoryError itself is allocated while the heap is over
		// budget: collecting there would either collect the partially built
		// exception or re-enter the failing allocation.
		if (!gc_enabled || no_gc_depth > 0 || oom_in_progress)
		{
			return;
		}
		needed = (live_bytes >= threshold);
		near_max = max_heap_bytes != 0 &&
			live_bytes > max_heap_bytes - (max_heap_bytes >> 3);
		if (in_gc)
		{
			// A concurrent cycle is open (it owns gc_mutex for its whole
			// duration).  The trace cannot be paced by live_bytes, which still
			// counts this cycle's garbage; close to -Xmx the cycle is aborted
			// so the collector retries with one stop-the-world pass instead of
			// risking an allocation failure mid-mark.
			abort_cms = cms_mode && near_max && mark_cycle_active.load();
		}
	}
	if (abort_cms)
	{
		CmsRequestAbort();
		return;
	}
	if (in_gc)
	{
		return;
	}
	if (cms_mode)
	{
		// The collector owns the cycle and blocks until it completes; the
		// stop-the-world parts are only the initial and final marks.
		if (needed && incremental_mark && !near_max &&
		    VMLockHeldByCurrentThread())
		{
			// Requested from code that already owns VMLock (a class
			// initializer reached from GetStatic, or a native body holding the
			// lock): the marker would block on this lock while the thread
			// waits for the cycle, so it cannot start here.  Remember the
			// request and start the cycle from the next allocation outside
			// the lock; a heap close to -Xmx still takes the immediate
			// stop-the-world fallback through the branch below.
			cms_gc_deferred.store(true);
			return;
		}
		const bool deferred = cms_gc_deferred.exchange(false);
		if (needed || deferred)
		{
			gc("threshold");
		}
		return;
	}
	if (incremental_mark && mark_cycle_active.load())
	{
		// The cycle is already open; live_bytes still counts its garbage, so
		// the threshold cannot pace the trace.  Near -Xmx the cycle is
		// finished on the spot (a collect-before-OOM) instead of waiting for
		// the allocation-paced slices; otherwise ContinueMarkCycle does.
		if (!near_max)
		{
			ContinueMarkCycle();
			return;
		}
		std::unique_lock<std::recursive_mutex> gc_lock(gc_mutex, std::try_to_lock);
		if (gc_lock.owns_lock() && !in_gc)
		{
			do
			{
				RunMarkPause("near-max");
			} while (mark_cycle_active.load());
		}
		return;
	}
	if (!needed)
	{
		return;
	}
	if (!incremental_mark)
	{
		gc("threshold");
		return;
	}
	std::unique_lock<std::recursive_mutex> gc_lock(gc_mutex, std::try_to_lock);
	if (!gc_lock.owns_lock() || in_gc)
	{
		return;
	}
	RunMarkPause("threshold");
}

void ObjectHeap::AddRootSlot(LPOBJECT_REF *slot)
{
	VMLock lk;
	if (slot)
	{
		root_slots.push_back(slot);
	}
}

void ObjectHeap::RemoveRootSlot(LPOBJECT_REF *slot)
{
	VMLock lk;
	for (std::vector<LPOBJECT_REF *>::iterator it = root_slots.begin();
	     it != root_slots.end(); ++it)
	{
		if (*it == slot)
		{
			root_slots.erase(it);
			return;
		}
	}
}

void ObjectHeap::RegisterThreadStack(ThreadStack *stack)
{
	VMLock lk;
	if (!stack)
	{
		return;
	}
	for (size_t i = 0; i < thread_stacks.size(); i++)
	{
		if (thread_stacks[i] == stack)
		{
			return;
		}
	}
	thread_stacks.push_back(stack);
}

void ObjectHeap::UnregisterThreadStack(ThreadStack *stack)
{
	VMLock lk;
	for (std::vector<ThreadStack *>::iterator it = thread_stacks.begin();
	     it != thread_stacks.end(); ++it)
	{
		if (*it == stack)
		{
			thread_stacks.erase(it);
			return;
		}
	}
}

void ObjectHeap::SnapshotThreadStacks(std::vector<ThreadStack *> &out)
{
	VMLock lk;
	out = thread_stacks;
}

void ObjectHeap::PushNoGc()
{
	VMLock lk;
	no_gc_depth++;
}

void ObjectHeap::PopNoGc()
{
	VMLock lk;
	if (no_gc_depth > 0)
	{
		no_gc_depth--;
	}
}

void ObjectHeap::MarkObjectRef(LPOBJECT_REF ref)
{
	if (!ref)
	{
		return;
	}
	// The candidate pointer is never dereferenced: random integer/float slot
	// contents must not fault. Only pointers that are exactly the address of a
	// live object's index field are accepted, so the scan is conservative.
	OBJECT_REF idx = 0;
	if (!LookupRefIndex(ref, idx))
	{
		return;
	}
	if (idx <= 0 || static_cast<size_t>(idx) >= objs.size())
	{
		return;
	}
	if (static_cast<size_t>(idx) >= marked.size())
	{
		// Objects registered while a cycle is open grow objs[]; keep the mark
		// bitmap aligned with the slot table.
		marked.resize(objs.size(), false);
	}
	if (marked[static_cast<size_t>(idx)])
	{
		return;
	}
	marked[static_cast<size_t>(idx)] = true;
	mark_stack.push_back(idx);
}

bool ObjectHeap::LookupRefIndex(const OBJECT_REF *candidate, OBJECT_REF &index) const
{
	if (!candidate)
	{
		return false;
	}
	std::unordered_map<const OBJECT_REF *, OBJECT_REF>::const_iterator it =
		ref_index.find(candidate);
	if (it == ref_index.end())
	{
		return false;
	}
	index = it->second;
	return true;
}

void ObjectHeap::IndexObject(lpObject obj)
{
	if (!ref_index_incremental || !obj)
	{
		return;
	}
	if (ref_index_shrink_pending)
	{
		MaybeShrinkRefIndex();
	}
	ref_index[&obj->index] = obj->index;
}

void ObjectHeap::UnindexObject(lpObject obj)
{
	if (!obj)
	{
		return;
	}
	// Erasing happens in both modes: "full" rebuilds the index at the start
	// of a collection, so removing the swept entries keeps the post-collection
	// index equal to the live set there as well.
	ref_index.erase(&obj->index);
}

void ObjectHeap::ReleaseSlot(lpObject obj)
{
	if (!obj)
	{
		return;
	}
	OBJECT_REF index = obj->index;
	if (index > 0 && static_cast<size_t>(index) < objs.size() &&
	    objs[static_cast<size_t>(index)] == obj)
	{
		objs[static_cast<size_t>(index)] = NULL;
		if (mark_cycle_active.load() && static_cast<size_t>(index) < marked.size())
		{
			// The slot is recycled immediately; drop the allocate-black mark
			// so a later occupant cannot inherit it.
			marked[static_cast<size_t>(index)] = false;
		}
	}
	UnindexObject(obj);
	free_indices.push_back(index);
}

void ObjectHeap::RebuildRefIndex()
{
	ref_index.clear();
	for (size_t i = 1; i < objs.size(); i++)
	{
		if (objs[i])
		{
			ref_index[&objs[i]->index] = static_cast<OBJECT_REF>(i);
		}
	}
}

void ObjectHeap::RepairRefIndexIfNeeded()
{
	// The incremental index holds exactly one entry per registered object, and
	// live_objects is maintained independently (allocation increments, sweep
	// recounts), so a size mismatch means the bookkeeping diverged from the
	// heap.  Rebuild from the slot vector and let the next run proceed
	// incrementally again; a healthy run never takes this path.
	if (ref_index.size() == live_objects)
	{
		return;
	}
	ref_index_repairs++;
	RebuildRefIndex();
}

void ObjectHeap::MaybeShrinkRefIndex()
{
	ref_index_shrink_pending = false;
	const size_t buckets = ref_index.bucket_count();
	if (buckets <= 4096 || ref_index.size() * 8 >= buckets)
	{
		return;
	}
	// Most entries were freed; the sweep only set the flag, the rehash itself
	// happens here on the mutator thread instead of inside the pause.
	ref_index.rehash(ref_index.size() * 2 + 1);
	ref_index_shrinks++;
}

size_t ObjectHeap::MeasureLiveBytesForTest() const
{
	size_t bytes = 0;
	for (size_t i = 0; i < objs.size(); i++)
	{
		if (objs[i])
		{
			bytes += ObjectBytes(objs[i]);
		}
	}
	return bytes;
}

bool ObjectHeap::RefIndexMatchesHeap() const
{
	size_t live = 0;
	for (size_t i = 1; i < objs.size(); i++)
	{
		lpObject obj = objs[i];
		if (!obj)
		{
			continue;
		}
		live++;
		std::unordered_map<const OBJECT_REF *, OBJECT_REF>::const_iterator it =
			ref_index.find(&obj->index);
		if (it == ref_index.end() || it->second != static_cast<OBJECT_REF>(i))
		{
			return false;
		}
	}
	return live == ref_index.size();
}

void ObjectHeap::MarkThreadStackRoots()
{
	const GcClock::time_point start = GcClock::now();
	VMLock lk;
	root_stacks = thread_stacks.size();
	for (size_t i = 0; i < thread_stacks.size(); i++)
	{
		thread_stacks[i]->MarkRoots(this);
	}
	root_thread_ms = MillisecondsSince(start);
}

void ObjectHeap::MarkStaticRoots()
{
	const GcClock::time_point start = GcClock::now();
	root_static_fields = 0;
	std::map<string, lpFieldValue>::iterator it = MethodArea::static_field_map.begin();
	for (; it != MethodArea::static_field_map.end(); ++it)
	{
		lpFieldValue fv = it->second;
		if (fv)
		{
			root_static_fields++;
			MarkObjectRef(fv->field_value.heapPtr);
		}
	}
	root_static_ms = MillisecondsSince(start);
}

void ObjectHeap::MarkConstantPoolRoots()
{
	const GcClock::time_point start = GcClock::now();
	root_classes = 0;
	root_cp_entries = 0;
	root_cp_strings = 0;
	const std::vector<ClassLoader *> &loaders = DarksVM_AllClassLoaders();
	for (size_t li = 0; li < loaders.size(); li++)
	{
		ClassLoader *loader = loaders[li];
		if (!loader)
		{
			continue;
		}
		const std::map<string, lpClass> &classes = loader->GetClasses();
		for (std::map<string, lpClass>::const_iterator it = classes.begin();
		     it != classes.end(); ++it)
		{
			lpClass clazz = it->second;
			if (!clazz || !clazz->const_pool)
			{
				continue;
			}
			root_classes++;
			int size = clazz->const_pool->size();
			root_cp_entries += static_cast<size_t>(size);
			for (int i = 1; i <= size; i++)
			{
				lpConstant constant = clazz->const_pool->GetConstant(i);
				if (constant && constant->tag == STRING_CONSTANT)
				{
					StringConstant *string_constant = static_cast<StringConstant *>(constant);
					if (string_constant->string_ref)
					{
						root_cp_strings++;
					}
					MarkObjectRef(string_constant->string_ref);
				}
			}
		}
	}
	root_cp_ms = MillisecondsSince(start);
}

void ObjectHeap::MarkTemporaryRoots()
{
	const GcClock::time_point start = GcClock::now();
	root_temp_slots = root_slots.size();
	for (size_t i = 0; i < root_slots.size(); i++)
	{
		LPOBJECT_REF *slot = root_slots[i];
		if (slot)
		{
			MarkObjectRef(*slot);
		}
	}
	root_temp_ms = MillisecondsSince(start);
}

void ObjectHeap::AddGlobalRef(LPOBJECT_REF ref)
{
	VMLock lk;
	if (ref)
	{
		global_refs.push_back(ref);
	}
}

void ObjectHeap::RemoveGlobalRef(LPOBJECT_REF ref)
{
	VMLock lk;
	for (std::vector<LPOBJECT_REF>::iterator it = global_refs.begin();
	     it != global_refs.end(); ++it)
	{
		if (*it == ref)
		{
			global_refs.erase(it);
			return;
		}
	}
}

void ObjectHeap::AddWeakGlobalRef(LPOBJECT_REF ref)
{
	VMLock lk;
	if (ref)
	{
		weak_global_refs.push_back(ref);
	}
}

void ObjectHeap::RemoveWeakGlobalRef(LPOBJECT_REF ref)
{
	VMLock lk;
	for (std::vector<LPOBJECT_REF>::iterator it = weak_global_refs.begin();
	     it != weak_global_refs.end(); ++it)
	{
		if (*it == ref)
		{
			weak_global_refs.erase(it);
			return;
		}
	}
}

bool ObjectHeap::IsGlobalRef(LPOBJECT_REF ref) const
{
	VMLock lk;
	for (size_t i = 0; i < global_refs.size(); i++)
	{
		if (global_refs[i] == ref)
		{
			return true;
		}
	}
	return false;
}

bool ObjectHeap::IsWeakGlobalRef(LPOBJECT_REF ref) const
{
	VMLock lk;
	for (size_t i = 0; i < weak_global_refs.size(); i++)
	{
		if (weak_global_refs[i] == ref)
		{
			return true;
		}
	}
	return false;
}

void ObjectHeap::MarkGlobalRefs()
{
	const GcClock::time_point start = GcClock::now();
	root_global_count = global_refs.size();
	for (size_t i = 0; i < global_refs.size(); i++)
	{
		MarkObjectRef(global_refs[i]);
	}
	root_global_ms = MillisecondsSince(start);
}

void ObjectHeap::MarkJniLocalRoots()
{
	// The JNI environment list is mutated when a coroutine finishes, possibly
	// while this collection runs on another worker.
	const GcClock::time_point start = GcClock::now();
	VMLock lk;
	root_jni_envs = VMContext::jni_envs.size();
	for (size_t i = 0; i < VMContext::jni_envs.size(); i++)
	{
		DarksVM_MarkJniEnvRoots(VMContext::jni_envs[i], this);
	}
	root_jni_ms = MillisecondsSince(start);
}

void ObjectHeap::MarkRoots()
{
	MarkThreadStackRoots();
	MarkStaticRoots();
	MarkConstantPoolRoots();
	MarkTemporaryRoots();
	MarkGlobalRefs();
	MarkJniLocalRoots();
}

void ObjectHeap::ProcessWeakReferences()
{
	for (size_t i = 0; i < weak_reference_objects.size(); i++)
	{
		OBJECT_REF index = weak_reference_objects[i];
		if (index <= 0 || static_cast<size_t>(index) >= objs.size())
		{
			continue;
		}
		lpObject reference = objs[static_cast<size_t>(index)];
		if (!reference)
		{
			continue;
		}
		lpFieldValue referent = FindFieldValue(reference, "referent");
		if (!referent || !referent->field_value.heapPtr)
		{
			continue;
		}
		LPOBJECT_REF target = referent->field_value.heapPtr;
		bool reachable = false;
		OBJECT_REF target_index = 0;
		if (LookupRefIndex(target, target_index))
		{
			reachable = target_index > 0 &&
				static_cast<size_t>(target_index) < marked.size() &&
				marked[static_cast<size_t>(target_index)];
		}
		if (reachable)
		{
			continue;
		}
		referent->field_value.heapPtr = NULL;

		lpFieldValue queue_field = FindFieldValue(reference, "queue");
		if (!queue_field || !queue_field->field_value.heapPtr)
		{
			continue;
		}
		lpFieldValue enqueued = FindFieldValue(reference, "enqueued");
		if (enqueued && enqueued->field_value.blValue)
		{
			continue;
		}
		lpObject queue = NULL;
		if (!GetObject(queue_field->field_value.heapPtr, queue) || !queue)
		{
			continue;
		}
		lpFieldValue head = FindFieldValue(queue, "head");
		lpFieldValue tail = FindFieldValue(queue, "tail");
		if (!head)
		{
			continue;
		}
		LPOBJECT_REF self = &reference->index;
		lpFieldValue next = FindFieldValue(reference, "next");
		if (next)
		{
			next->field_value.heapPtr = NULL;
		}
		if (!head->field_value.heapPtr)
		{
			head->field_value.heapPtr = self;
			if (tail)
			{
				tail->field_value.heapPtr = self;
			}
		}
		else
		{
			lpObject last = NULL;
			if (!tail || !tail->field_value.heapPtr ||
			    !GetObject(tail->field_value.heapPtr, last) || !last)
			{
				if (!GetObject(head->field_value.heapPtr, last) || !last)
				{
					continue;
				}
				while (true)
				{
					lpFieldValue link = FindFieldValue(last, "next");
					lpObject following = NULL;
					if (!link || !link->field_value.heapPtr ||
					    !GetObject(link->field_value.heapPtr, following) || !following)
					{
						break;
					}
					last = following;
				}
			}
			lpFieldValue link = FindFieldValue(last, "next");
			if (link)
			{
				link->field_value.heapPtr = self;
			}
			if (tail)
			{
				tail->field_value.heapPtr = self;
			}
		}
		if (enqueued)
		{
			enqueued->field_value.blValue = 1;
		}
	}
}

void ObjectHeap::gc(const char *reason)
{
	if (collector)
	{
		collector->Collect(reason);
		return;
	}
	CollectMarkSweep(reason);
}

void ObjectHeap::CollectMarkSweep(const char *reason)
{
	if (!reason)
	{
		reason = "unknown";
	}
	// Only one collection at a time.  A native call may reach allocation from
	// another thread; in that case the collection is skipped rather than
	// waiting while holding the heap lock.
	std::unique_lock<std::recursive_mutex> gc_lock(gc_mutex, std::try_to_lock);
	if (!gc_lock.owns_lock())
	{
		return;
	}
	if (in_gc)
	{
		return;
	}
	if (!incremental_mark)
	{
		CollectMarkSweepSTW(reason);
		return;
	}
	// An explicit request must return with the collection complete: gc() and
	// the tests inspect the heap right after the call.  The cycle still runs as
	// bounded pauses, taken back to back.
	do
	{
		RunMarkPause(reason);
	} while (mark_cycle_active.load());
}

// One stop-the-world collection (DARKS_JVM_MARK=stw).  The caller holds
// gc_mutex and in_gc is false.
void ObjectHeap::CollectMarkSweepSTW(const char *reason)
{
	const GcClock::time_point gc_start = GcClock::now();
	// Hold the heap mutex before stopping the world.  A worker that is blocked
	// on this mutex is then genuinely stopped for the whole collection, which
	// makes the scheduler's "waiting for the heap lock" safepoint state stable.
	// Taking the mutex afterwards would leave a window where such a worker
	// resumes and runs Java while the heap is being traced.
	VMLock heap_lock;
	// Finish any lazy sweep before the trace starts.  Besides keeping the
	// dead set disjoint between two collections (the same slot must never
	// appear twice in pending_free) this restores the invariant that the
	// live object table, the index and live_bytes describe the same set.  It
	// runs before the world is stopped, so it is counted as pre-world work.
	SweepPending(static_cast<size_t>(-1));
	// Stop every interpreter worker at a safepoint before tracing.  Native
	// code is treated as stopped: its Java frames are at the call boundary and
	// its JNI local references are registered roots.
	Scheduler::Instance().SafepointBegin();
	const GcClock::time_point world_stopped = GcClock::now();
	const size_t slots_before = objs.size();
	const size_t free_before = free_indices.size();
	const size_t live_bytes_before = live_bytes;
	const size_t live_objects_before = live_objects;
	const size_t threshold_before = threshold;
	std::string trigger_frame;
	{
		ThreadStack *trigger_stack = Scheduler::CurrentStack();
		if (!trigger_stack || !trigger_stack->DescribeTopFrame(trigger_frame))
		{
			trigger_frame.assign("native");
		}
	}
	in_gc = true;

	const GcClock::time_point setup_start = GcClock::now();
	if (ref_index_incremental)
	{
		// The index was maintained by Register/ReleaseSlot/sweep, so the
		// pause no longer rebuilds it.  The size check is O(1) and only
		// detects a diverged bookkeeping; a healthy run never repairs.
		RepairRefIndexIfNeeded();
	}
	else
	{
		// DARKS_JVM_REFINDEX=full: M2a behaviour, rebuild the whole index.
		RebuildRefIndex();
	}

	marked.assign(objs.size(), false);
	mark_stack.clear();
	weak_reference_objects.clear();
	const GcClock::time_point setup_done = GcClock::now();

	MarkRoots();
	const GcClock::time_point roots_done = GcClock::now();

	// Iterative trace: scan every marked object for outgoing references.
	ScanMarkSlice(static_cast<size_t>(-1), 1.0e30);

	const GcClock::time_point trace_done = GcClock::now();
	ProcessWeakReferences();
	const GcClock::time_point weak_done = GcClock::now();

	// Only record the dead slots here: destroying the object (destructor plus
	// the free) and erasing its index entry is the expensive part of the
	// sweep, and it does not have to happen inside the pause.  The survivor
	// size still has to be measured exactly (an object can gain fields after
	// it was allocated, so the allocation-time accounting drifts).
	size_t freed_bytes = 0;
	size_t freed_objects = 0;
	size_t live = 0;
	size_t live_count = 0;
	for (size_t i = 1; i < objs.size(); i++)
	{
		lpObject obj = objs[i];
		if (!obj)
		{
			continue;
		}
		if (!marked[i])
		{
			freed_bytes += ObjectBytes(obj);
			freed_objects++;
			pending_free.push_back(static_cast<OBJECT_REF>(i));
		}
		else
		{
			live += ObjectBytes(obj);
			live_count++;
		}
	}
	// Outside the pause only a small chunk is freed so the mutator resumes
	// promptly; the rest is drained from the allocation path.  With
	// DARKS_JVM_SWEEP=pause the whole list is freed here, restoring the
	// stop-the-world sweep.
	if (lazy_sweep)
	{
		SweepPending(kPostPauseSweepChunk);
	}
	else
	{
		SweepPending(static_cast<size_t>(-1));
	}

	const GcClock::time_point sweep_done = GcClock::now();
	live_bytes = live;
	live_objects = live_count;
	// A large drop in live objects leaves the hash table oversized.  Only
	// record the intent here; the rehash runs on the next allocation so the
	// pause keeps its length.
	if (ref_index_incremental && ref_index.bucket_count() > 4096 &&
	    ref_index.size() * 8 < ref_index.bucket_count())
	{
		ref_index_shrink_pending = true;
	}
	gc_runs++;
	gc_freed_last = freed_bytes;
	gc_freed_total += freed_bytes;

	// Avoid collecting on every single allocation when almost everything is
	// still reachable: grow the threshold past the surviving heap.
	size_t next_threshold = base_threshold;
	if (live_bytes > next_threshold / 2)
	{
		next_threshold = live_bytes * 2;
	}
	if (max_heap_bytes != 0 && next_threshold > max_heap_bytes)
	{
		next_threshold = max_heap_bytes;
	}
	threshold = next_threshold;

	if (gc_log)
	{
		fprintf(stderr,
			"gc-collect run=%d objects=%zu freed=%zu bytes live=%zu bytes\n",
			gc_runs, freed_objects, freed_bytes, live_bytes);
	}

	in_gc = false;
	Scheduler::Instance().SafepointEnd();
	const GcClock::time_point world_resumed = GcClock::now();

	if (GcStatsEnabled())
	{
		const double preworld_ms =
			std::chrono::duration<double, std::milli>(world_stopped - gc_start).count();
		const double total_ms =
			std::chrono::duration<double, std::milli>(world_resumed - gc_start).count();
		const double stw_ms =
			std::chrono::duration<double, std::milli>(world_resumed - world_stopped).count();
		const double root_ms =
			std::chrono::duration<double, std::milli>(roots_done - world_stopped).count();
		const double mark_ms =
			std::chrono::duration<double, std::milli>(trace_done - roots_done).count();
		const double weak_ms =
			std::chrono::duration<double, std::milli>(weak_done - trace_done).count();
		const double sweep_ms =
			std::chrono::duration<double, std::milli>(sweep_done - weak_done).count();
		const double post_ms =
			std::chrono::duration<double, std::milli>(world_resumed - sweep_done).count();
		const double setup_ms =
			std::chrono::duration<double, std::milli>(setup_done - setup_start).count();
		const double roots_ms =
			std::chrono::duration<double, std::milli>(roots_done - setup_done).count();
		fprintf(stderr,
		        "[gcstat] run=%d reason=%s total_ms=%.2f preworld_ms=%.2f "
		        "stw_ms=%.2f root_ms=%.2f mark_ms=%.2f weak_ms=%.2f "
		        "sweep_ms=%.2f post_ms=%.2f setup_ms=%.2f roots_ms=%.2f "
		        "ref_entries=%zu ref_repairs=%zu ref_shrinks=%zu refindex=%s "
		        "pend_free=%zu sweep=%s mark=stw pauses=1 pause_max_ms=%.2f "
		        "rthread=%.2f rstatic=%.2f "
		        "rcp=%.2f rtemp=%.2f rglobal=%.2f rjni=%.2f slots=%zu "
		        "free_before=%zu live_before=%zu objs_before=%zu "
		        "thresh_before=%zu heap_max=%zu heap_used=%zu "
		        "freed_objs=%zu freed_bytes=%zu "
		        "live_after=%zu objs_after=%zu stacks=%zu static_fields=%zu "
		        "classes=%zu cp_entries=%zu cp_strings=%zu temp_slots=%zu "
		        "globals=%zu jni_envs=%zu frame=%s\n",
		        gc_runs, reason, total_ms, preworld_ms, stw_ms, root_ms,
		        mark_ms, weak_ms, sweep_ms, post_ms, setup_ms, roots_ms,
		        ref_index.size(), ref_index_repairs, ref_index_shrinks,
		        ref_index_incremental ? "incremental" : "full",
		        pending_free.size(), lazy_sweep ? "lazy" : "pause", stw_ms,
		        root_thread_ms,
		        root_static_ms, root_cp_ms, root_temp_ms, root_global_ms,
		        root_jni_ms, slots_before, free_before, live_bytes_before,
		        live_objects_before, threshold_before, max_heap_bytes,
		        live_bytes_before, freed_objects,
		        freed_bytes, live_bytes, live_objects, root_stacks,
		        root_static_fields, root_classes, root_cp_entries,
		        root_cp_strings, root_temp_slots, root_global_count,
		        root_jni_envs, trigger_frame.c_str());
	}
}

// One bounded marking pause.  The pause that opens a cycle snapshots the roots
// and drains a slice of the mark stack; later pauses drain further slices; the
// pause that empties the stack also re-reads the roots, clears weak referents
// and sweeps, which completes the collection.  The caller holds gc_mutex.
void ObjectHeap::RunMarkPause(const char *reason)
{
	const bool first = !mark_cycle_active.load();
	const GcClock::time_point pause_start = GcClock::now();
	if (first)
	{
		cycle_gc_start = pause_start;
		cycle_root_ms = 0.0;
		cycle_mark_ms = 0.0;
		cycle_setup_ms = 0.0;
		cycle_pause_sum_ms = 0.0;
		cycle_pause_max_ms = 0.0;
		cycle_pauses = 0;
		cycle_preworld_ms = 0.0;
		cycle_slice_budget = kMarkSliceMinObjects;
		cycle_trigger_frame.clear();
	}
	VMLock heap_lock;
	if (first)
	{
		// Same pre-world work as the stop-the-world path: the dead set of the
		// previous collection is gone before this trace starts, and the two
		// dead sets can never share a slot.
		SweepPending(static_cast<size_t>(-1));
	}
	Scheduler::Instance().SafepointBegin();
	const GcClock::time_point world_stopped = GcClock::now();
	if (first)
	{
		cycle_preworld_ms = std::chrono::duration<double, std::milli>(
			world_stopped - cycle_gc_start).count();
		cycle_slots_before = objs.size();
		cycle_free_before = free_indices.size();
		cycle_live_bytes_before = live_bytes;
		cycle_live_objects_before = live_objects;
		cycle_threshold_before = threshold;
		ThreadStack *trigger_stack = Scheduler::CurrentStack();
		if (!trigger_stack || !trigger_stack->DescribeTopFrame(cycle_trigger_frame))
		{
			cycle_trigger_frame.assign("native");
		}
	}
	in_gc = true;

	double setup_ms = 0.0;
	if (first)
	{
		const GcClock::time_point setup_start = GcClock::now();
		if (ref_index_incremental)
		{
			RepairRefIndexIfNeeded();
		}
		else
		{
			RebuildRefIndex();
		}
		marked.assign(objs.size(), false);
		mark_stack.clear();
		weak_reference_objects.clear();
		setup_ms = MillisecondsSince(setup_start);
	}
	double roots_ms = 0.0;
	if (first)
	{
		// Snapshot of the roots when the cycle opens.  Everything reachable
		// here stays marked: overwrites are journaled by the barrier below and
		// allocations are marked as they are registered.
		const GcClock::time_point roots_start = GcClock::now();
		MarkRoots();
		roots_ms = MillisecondsSince(roots_start);

		// Journal overwritten references until the final pause.  Only the old
		// value is needed: the snapshot carries everything reachable when the
		// cycle opened, and new objects are allocate-black.
		cycle_recorded = 0;
		cycle_dropped_before = GCBarrier::Dropped();
		if (!GCBarrier::Enabled())
		{
			GCBarrier::SetRecordNew(false);
			GCBarrier::Enable();
			gc_barrier_owned = true;
		}
	}

	// Pull in the references the mutator overwrote since the last pause and
	// mark them before this slice drains.  The world is stopped here, so the
	// journal cannot change while it is drained.
	size_t pause_records = 0;
	if (GCBarrier::Enabled())
	{
		std::vector<LPOBJECT_REF> recorded;
		GCBarrier::Drain(recorded);
		pause_records = recorded.size();
		cycle_recorded += pause_records;
		for (size_t i = 0; i < recorded.size(); i++)
		{
			MarkObjectRef(recorded[i]);
		}
	}

	mark_cycle_active.store(true);
	if (first)
	{
		// The slot table is pre-sized, so its size says nothing about the
		// trace.  Pace the cycle on the last known live count instead: about
		// sixteen pauses per collection, each bounded in time as well.
		size_t budget = live_objects / 16;
		if (budget < kMarkSliceMinObjects)
		{
			budget = kMarkSliceMinObjects;
		}
		if (budget > kMarkSliceMaxObjects)
		{
			budget = kMarkSliceMaxObjects;
		}
		cycle_slice_budget = budget;
	}
	const GcClock::time_point slice_start = GcClock::now();
	ScanMarkSlice(cycle_slice_budget, kMarkSliceMillis);
	double mark_ms = MillisecondsSince(slice_start);

	double weak_ms = 0.0;
	double sweep_ms = 0.0;
	size_t freed_bytes = 0;
	size_t freed_objects = 0;
	bool finished = false;
	bool cycle_aborted = false;
	if (mark_stack.empty())
	{
		finished = true;
		// Final pause.  Re-read every root: a reference can move into a stack
		// slot, an operand slot or a temporary root without any heap store, so
		// the reference the mutator holds right now has to be visible to the
		// trace that decides the dead set.
		const GcClock::time_point final_roots_start = GcClock::now();
		MarkRoots();
		roots_ms += MillisecondsSince(final_roots_start);
		const GcClock::time_point drain_start = GcClock::now();
		ScanMarkSlice(static_cast<size_t>(-1), 1.0e30);
		mark_ms += MillisecondsSince(drain_start);

		if (marked.size() < objs.size())
		{
			marked.resize(objs.size(), false);
		}
		if (GCBarrier::Dropped() != cycle_dropped_before)
		{
			// The journal ran out of room, so an overwritten edge may be
			// missing.  Freeing on incomplete information would corrupt the
			// heap: keep every object that exists now and let the next cycle
			// retry with a journal that the drains keep short.
			for (size_t i = 1; i < objs.size(); i++)
			{
				if (objs[i])
				{
					marked[i] = true;
				}
			}
			cycle_aborted = true;
		}
		if (gc_barrier_owned)
		{
			GCBarrier::Disable();
			GCBarrier::SetRecordNew(true);
			gc_barrier_owned = false;
		}

		const GcClock::time_point weak_start = GcClock::now();
		ProcessWeakReferences();
		weak_ms = MillisecondsSince(weak_start);

		const GcClock::time_point sweep_start = GcClock::now();
		size_t live = 0;
		size_t live_count = 0;
		for (size_t i = 1; i < objs.size(); i++)
		{
			lpObject obj = objs[i];
			if (!obj)
			{
				continue;
			}
			if (!marked[i])
			{
				freed_bytes += ObjectBytes(obj);
				freed_objects++;
				pending_free.push_back(static_cast<OBJECT_REF>(i));
			}
			else
			{
				live += ObjectBytes(obj);
				live_count++;
			}
		}
		if (lazy_sweep)
		{
			SweepPending(kPostPauseSweepChunk);
		}
		else
		{
			SweepPending(static_cast<size_t>(-1));
		}
		sweep_ms = MillisecondsSince(sweep_start);

		live_bytes = live;
		live_objects = live_count;
		// A large drop in live objects leaves the hash table oversized.  Only
		// record the intent here; the rehash runs on the next allocation so
		// the pause keeps its length.
		if (ref_index_incremental && ref_index.bucket_count() > 4096 &&
		    ref_index.size() * 8 < ref_index.bucket_count())
		{
			ref_index_shrink_pending = true;
		}
		gc_runs++;
		gc_freed_last = freed_bytes;
		gc_freed_total += freed_bytes;

		// Avoid collecting on every single allocation when almost everything
		// is still reachable: grow the threshold past the surviving heap.
		size_t next_threshold = base_threshold;
		if (live_bytes > next_threshold / 2)
		{
			next_threshold = live_bytes * 2;
		}
		if (max_heap_bytes != 0 && next_threshold > max_heap_bytes)
		{
			next_threshold = max_heap_bytes;
		}
		threshold = next_threshold;

		if (gc_log)
		{
			fprintf(stderr,
				"gc-collect run=%d objects=%zu freed=%zu bytes live=%zu bytes\n",
				gc_runs, freed_objects, freed_bytes, live_bytes);
		}
		mark_cycle_active.store(false);
	}

	const GcClock::time_point work_done = GcClock::now();
	in_gc = false;
	Scheduler::Instance().SafepointEnd();
	const GcClock::time_point world_resumed = GcClock::now();

	const double stw_ms = MillisecondsSince(pause_start);
	cycle_root_ms += roots_ms;
	cycle_mark_ms += mark_ms;
	cycle_setup_ms += setup_ms;
	cycle_pauses++;
	cycle_pause_sum_ms += stw_ms;
	if (stw_ms > cycle_pause_max_ms)
	{
		cycle_pause_max_ms = stw_ms;
	}
	mark_cycle_allocs.store(0);

	if (!GcStatsEnabled())
	{
		return;
	}
	PrintSliceStat(reason, finished ? "finish" : (first ? "begin" : "slice"),
	               stw_ms, roots_ms, mark_ms, pause_records);
	if (!finished)
	{
		return;
	}
	const double cycle_ms =
		std::chrono::duration<double, std::milli>(world_resumed - cycle_gc_start).count();
	const double post_ms =
		std::chrono::duration<double, std::milli>(world_resumed - work_done).count();
	fprintf(stderr,
	        "[gcstat] run=%d reason=%s total_ms=%.2f preworld_ms=%.2f "
	        "stw_ms=%.2f root_ms=%.2f mark_ms=%.2f weak_ms=%.2f "
	        "sweep_ms=%.2f post_ms=%.2f setup_ms=%.2f roots_ms=%.2f "
	        "ref_entries=%zu ref_repairs=%zu ref_shrinks=%zu refindex=%s "
	        "pend_free=%zu sweep=%s mark=incremental pauses=%zu cycle_ms=%.2f "
	        "pause_sum_ms=%.2f pause_max_ms=%.2f mark_total_ms=%.2f "
	        "satb_records=%zu satb_dropped=%llu satb_abort=%d "
	        "rthread=%.2f rstatic=%.2f "
	        "rcp=%.2f rtemp=%.2f rglobal=%.2f rjni=%.2f slots=%zu "
	        "free_before=%zu live_before=%zu objs_before=%zu "
	        "thresh_before=%zu heap_max=%zu heap_used=%zu "
	        "freed_objs=%zu freed_bytes=%zu "
	        "live_after=%zu objs_after=%zu stacks=%zu static_fields=%zu "
	        "classes=%zu cp_entries=%zu cp_strings=%zu temp_slots=%zu "
	        "globals=%zu jni_envs=%zu frame=%s\n",
	        gc_runs, reason, stw_ms, cycle_preworld_ms, stw_ms, cycle_root_ms,
	        mark_ms, weak_ms, sweep_ms, post_ms, cycle_setup_ms, cycle_root_ms,
	        ref_index.size(), ref_index_repairs, ref_index_shrinks,
	        ref_index_incremental ? "incremental" : "full",
	        pending_free.size(), lazy_sweep ? "lazy" : "pause", cycle_pauses,
	        cycle_ms, cycle_pause_sum_ms, cycle_pause_max_ms, cycle_mark_ms,
	        cycle_recorded,
	        static_cast<unsigned long long>(GCBarrier::Dropped()),
	        cycle_aborted ? 1 : 0, root_thread_ms,
	        root_static_ms, root_cp_ms, root_temp_ms, root_global_ms,
	        root_jni_ms, cycle_slots_before, cycle_free_before,
	        cycle_live_bytes_before, cycle_live_objects_before,
	        cycle_threshold_before, max_heap_bytes, cycle_live_bytes_before,
	        freed_objects, freed_bytes, live_bytes, live_objects, root_stacks,
	        root_static_fields, root_classes, root_cp_entries,
	        root_cp_strings, root_temp_slots, root_global_count,
	        root_jni_envs, cycle_trigger_frame.c_str());
}

size_t ObjectHeap::ScanMarkSlice(size_t max_objects, double max_ms)
{
	const GcClock::time_point start = GcClock::now();
	size_t scanned = 0;
	while (!mark_stack.empty() && scanned < max_objects)
	{
		const OBJECT_REF idx = mark_stack.back();
		mark_stack.pop_back();
		ScanMarkedObject(idx);
		scanned++;
		if ((scanned & 0xFFF) == 0 && MillisecondsSince(start) >= max_ms)
		{
			break;
		}
	}
	return scanned;
}

void ObjectHeap::ScanMarkedObject(OBJECT_REF idx)
{
	lpObject obj = objs[static_cast<size_t>(idx)];
	if (!obj)
	{
		return;
	}
	bool reference = IsReferenceObject(obj);
	if (reference)
	{
		weak_reference_objects.push_back(idx);
	}
	// Instance fields. Primitive values are tolerated: MarkObjectRef only
	// accepts pointers that match a live object's index address.  Slots are
	// scanned first because a subclass field can hide a superclass field with
	// the same name and descriptor; the name-keyed map below keeps only one of
	// them, while every field has its own slot.
	for (size_t k = 0; k < obj->instance_slots.size(); k++)
	{
		lpFieldValue fv = obj->instance_slots[k];
		if (!fv)
		{
			continue;
		}
		if (reference && fv->field && fv->field->fieldName &&
		    fv->field->fieldName->GetName() &&
		    strcmp(fv->field->fieldName->GetName(), "referent") == 0)
		{
			continue;
		}
		MarkObjectRef(fv->field_value.heapPtr);
	}
	FIELD_MAP::iterator field_it = obj->varants.begin();

	for (; field_it != obj->varants.end(); ++field_it)
	{
		// Reference.referent is a weak edge; it is handled after the strong
		// trace once reachability is known.
		if (reference && field_it->first.compare(0, 9, "referent:") == 0)
		{
			continue;
		}
		lpFieldValue fv = field_it->second;
		if (fv)
		{
			MarkObjectRef(fv->field_value.heapPtr);
		}
	}
	// Reference arrays (object arrays and nested arrays).
	if (obj->array_var &&
	    (obj->array_data_type == DT_REF || obj->array_data_type == DT_ARRAY))
	{
		jobject *refs = static_cast<jobject *>(obj->array_var);
		for (int k = 0; k < obj->array_length; k++)
		{
			MarkObjectRef(reinterpret_cast<LPOBJECT_REF>(refs[k]));
		}
	}
}

void ObjectHeap::MarkNewObjectLocked(lpObject obj)
{
	if (!obj)
	{
		return;
	}
	const size_t index = static_cast<size_t>(obj->index);
	if (objs.size() > marked.size())
	{
		marked.resize(objs.size(), false);
	}
	if (index == 0 || index >= marked.size() || marked[index])
	{
		return;
	}
	// Allocate black: an object created while a cycle is open is treated as a
	// root for that cycle.  It is scanned by the next slice, so the references
	// it receives after registration are traced too.
	marked[index] = true;
	mark_stack.push_back(obj->index);
}

void ObjectHeap::ContinueMarkCycle()
{
	if (!mark_cycle_active.load())
	{
		return;
	}
	const size_t allocs =
		mark_cycle_allocs.fetch_add(1, std::memory_order_relaxed) + 1;
	// Also break in when the journal has grown, so a write-heavy stretch
	// without allocations cannot outrun the drains.
	if (allocs < kMarkAllocationsPerSlice &&
	    GCBarrier::Pending() < kMarkRecordsPerSlice)
	{
		return;
	}
	mark_cycle_allocs.store(0, std::memory_order_relaxed);
	std::unique_lock<std::recursive_mutex> gc_lock(gc_mutex, std::try_to_lock);
	if (!gc_lock.owns_lock() || in_gc || !mark_cycle_active.load())
	{
		return;
	}
	RunMarkPause("slice");
}

void ObjectHeap::PrintSliceStat(const char *reason, const char *phase,
                                double stw_ms, double roots_ms, double mark_ms,
                                size_t records)
{
	fprintf(stderr,
	        "[gcpause] seq=%zu reason=%s phase=%s stw_ms=%.2f roots_ms=%.2f "
	        "mark_ms=%.2f stack=%zu allocs=%zu records=%zu\n",
	        cycle_pauses, reason, phase, stw_ms, roots_ms, mark_ms,
	        mark_stack.size(), mark_cycle_allocs.load(), records);
}

void ObjectHeap::SweepPending(size_t max_objects)
{
	size_t freed = 0;
	while (!pending_free.empty() && freed < max_objects)
	{
		OBJECT_REF index = pending_free.back();
		pending_free.pop_back();
		lpObject obj = objs[static_cast<size_t>(index)];
		if (!obj)
		{
			continue;
		}
		// The index entry has to go with the object: the slot is recycled and
		// a stale address->slot entry would keep a later occupant of the slot
		// alive through a dangling reference.
		UnindexObject(obj);
		delete obj;
		objs[static_cast<size_t>(index)] = NULL;
		// Reuse the slot on the next allocation.  An address-validated
		// reference can never alias the recycled slot because the new object
		// lives at a different address.
		free_indices.push_back(index);
		freed++;
	}
}

void ObjectHeap::DrainPendingFreeAll()
{
	VMLock lk;
	SweepPending(static_cast<size_t>(-1));
	while (cms_sweep_pending)
	{
		CmsSweepScanLocked(static_cast<size_t>(-1));
	}
}

// ---------------------------------------------------------------------------
// Concurrent (CMS-lite) collector
// ---------------------------------------------------------------------------
const char *ObjectHeap::CollectorName() const
{
	return collector ? collector->Name() : "none";
}

bool ObjectHeap::CmsCycleEnter()
{
	if (!gc_mutex.try_lock())
	{
		return false;
	}
	if (in_gc.load())
	{
		gc_mutex.unlock();
		return false;
	}
	in_gc.store(true);
	return true;
}

void ObjectHeap::CmsCycleLeave()
{
	in_gc.store(false);
	gc_mutex.unlock();
}

void ObjectHeap::CmsRequestAbort()
{
	cms_abort_requested.store(true);
}

void ObjectHeap::CmsSetMarkerThread()
{
	unsigned long long id = static_cast<unsigned long long>(
		std::hash<std::thread::id>()(std::this_thread::get_id()));
	cms_marker_thread.store(id);
}

void ObjectHeap::CmsAbandonCycle()
{
	mark_cycle_active.store(false);
	cms_cycle_running.store(false);
	cms_mark_complete.store(true);
	if (gc_barrier_owned)
	{
		GCBarrier::Disable();
		GCBarrier::SetRecordNew(true);
		gc_barrier_owned = false;
	}
	mark_stack.clear();
	weak_reference_objects.clear();
}

// Initial mark.  The whole phase runs inside one stop-the-world pause: the
// previous cycle's dead set is finished first so two cycles can never share a
// slot, then the roots are snapshotted and the SATB barrier is switched on.
// Everything reachable at this instant stays marked: later overwrites are
// journaled by the barrier and new objects are allocate-black.
void ObjectHeap::CmsInitMark(const char *reason)
{
	(void)reason;
	cms_abort_requested.store(false);
	cms_mark_complete.store(false);
	cms_mark_steps.store(0);
	cms_mark_allocs.store(0);
	cms_cycle_running.store(true);
	cms_mark_start = GcClock::now();

	VMLock heap_lock;
	SweepPending(static_cast<size_t>(-1));
	// The collector waits for its sweeper before it starts another cycle, so
	// this is a safety net only: marked[] must not be recomputed while an old
	// scan still reads it.
	while (cms_sweep_pending)
	{
		CmsSweepScanLocked(static_cast<size_t>(-1));
	}
	cms_world_stopped.store(true);
	Scheduler::Instance().SafepointBegin();
	const GcClock::time_point pause_start = GcClock::now();

	if (ref_index_incremental)
	{
		RepairRefIndexIfNeeded();
	}
	else
	{
		RebuildRefIndex();
	}
	marked.assign(objs.size(), 0);
	mark_stack.clear();
	weak_reference_objects.clear();
	cycle_recorded = 0;
	// Drop journal entries left by an earlier cycle that was abandoned: the
	// root snapshot below reconstructs the whole reachable set, and a stale
	// entry could name a slot that has since been recycled.
	std::vector<LPOBJECT_REF> stale;
	GCBarrier::Drain(stale);
	cycle_dropped_before = GCBarrier::Dropped();
	MarkRoots();
	if (!GCBarrier::Enabled())
	{
		// Snapshot-at-the-beginning only needs the overwritten value: the
		// snapshot already covers what was reachable, and allocations are
		// marked as they are registered.
		GCBarrier::SetRecordNew(false);
		GCBarrier::Enable();
		gc_barrier_owned = true;
	}
	mark_cycle_active.store(true);

	Scheduler::Instance().SafepointEnd();
	cms_world_stopped.store(false);
	cms_init_ms = MillisecondsSince(pause_start);

	if (GcStatsEnabled())
	{
		fprintf(stderr, "[gcpause] seq=%zu reason=%s phase=cms-init stw_ms=%.2f "
		        "stack=%zu\n",
		        cycle_pauses + 1, reason ? reason : "unknown", cms_init_ms,
		        mark_stack.size());
	}
}

// One bounded background trace step.  The marker holds VMLock only for this
// step and drops it afterwards, so mutator threads keep running between steps.
bool ObjectHeap::CmsMarkStep(size_t max_objects, double max_ms)
{
	if (!mark_cycle_active.load() || cms_mark_complete.load())
	{
		return false;
	}
	if (cms_abort_requested.load())
	{
		return false;
	}
	if (cms_world_stopped.load())
	{
		// A stop-the-world phase owns the heap; retry after it ends instead of
		// extending the pause.
		return true;
	}
	VMLock lk;
	if (cms_world_stopped.load() || !mark_cycle_active.load())
	{
		return !cms_mark_complete.load();
	}
	if (GCBarrier::Dropped() != cycle_dropped_before)
	{
		// The journal overflowed: an overwritten edge may be missing.  Falling
		// back to a full stop-the-world pass is the only safe option.
		CmsRequestAbort();
		return false;
	}
	std::vector<LPOBJECT_REF> recorded;
	GCBarrier::Drain(recorded);
	cycle_recorded += recorded.size();
	for (size_t i = 0; i < recorded.size(); i++)
	{
		MarkObjectRef(recorded[i]);
	}
	ScanMarkSlice(max_objects, max_ms);
	cms_mark_steps.fetch_add(1);
	if (mark_stack.empty())
	{
		cms_mark_complete.store(true);
		return false;
	}
	return true;
}

// Final mark.  Re-reads every root (a reference can move into a stack slot
// without any heap store), drains the last journal entries, resolves weak
// references and moves the dead set to pending_free.  On failure the cycle is
// abandoned and the caller retries with a full stop-the-world mark-sweep;
// nothing is freed from an incomplete trace.
bool ObjectHeap::CmsFinalMark(const char *reason)
{
	const char *why = reason ? reason : "unknown";
	if (cms_final_gate.load())
	{
		// Test-only gate: the world still runs here, so a test can add a root
		// after the initial snapshot before the roots are re-read below.
		cms_final_gate_waiting.store(true);
		while (!cms_final_gate_open.load())
		{
			std::this_thread::sleep_for(std::chrono::milliseconds(1));
		}
		cms_final_gate_waiting.store(false);
	}
	const GcClock::time_point pause_start = GcClock::now();
	VMLock heap_lock;
	cms_world_stopped.store(true);
	Scheduler::Instance().SafepointBegin();

	const GcClock::time_point journal_start = GcClock::now();
	if (GCBarrier::Enabled())
	{
		std::vector<LPOBJECT_REF> recorded;
		GCBarrier::Drain(recorded);
		cycle_recorded += recorded.size();
		for (size_t i = 0; i < recorded.size(); i++)
		{
			MarkObjectRef(recorded[i]);
		}
	}
	const double journal_ms = MillisecondsSince(journal_start);
	const GcClock::time_point roots_start = GcClock::now();
	MarkRoots();
	const double roots_ms = MillisecondsSince(roots_start);
	const GcClock::time_point scan_start = GcClock::now();
	ScanMarkSlice(static_cast<size_t>(-1), 1.0e30);
	if (marked.size() < objs.size())
	{
		marked.resize(objs.size(), 0);
	}
	const double scan_ms = MillisecondsSince(scan_start);

	if (cms_abort_requested.load() ||
	    GCBarrier::Dropped() != cycle_dropped_before)
	{
		cms_failures.fetch_add(1);
		CmsAbandonCycle();
		Scheduler::Instance().SafepointEnd();
		cms_world_stopped.store(false);
		cms_final_ms = MillisecondsSince(pause_start);
		if (GcStatsEnabled())
		{
			fprintf(stderr,
			        "[gcstat] run=%d reason=%s mode=cms concurrent_mode_failure=1 "
			        "init_stw_ms=%.2f final_stw_ms=%.2f satb_records=%zu "
			        "satb_dropped=%llu\n",
			        gc_runs, why, cms_init_ms, cms_final_ms, cycle_recorded,
			        static_cast<unsigned long long>(GCBarrier::Dropped()));
		}
		return false;
	}

	const GcClock::time_point weak_start = GcClock::now();
	ProcessWeakReferences();
	const double weak_ms = MillisecondsSince(weak_start);

	// The dead set is not classified here.  The pause only records the slot
	// range: the sweeper walks it, measures the objects it destroys and
	// shrinks live_bytes/live_objects as it goes.  Until that finishes the
	// accounting still counts the new garbage, which is why the threshold is
	// adapted in CmsFinishSweepLocked and not here; the collector waits for
	// the sweep before it leaves the cycle, so nothing allocates against the
	// stale numbers (WouldExceedMax drains on demand near -Xmx).
	cms_sweep_hi = objs.size();
	cms_sweep_cursor = 1;
	cms_sweep_freed_bytes = 0;
	cms_sweep_freed_objects = 0;
	cms_swept_slots = 0;
	cms_sweep_start = GcClock::now();
	cms_sweep_pending = true;
	gc_runs++;
	cms_mark_wall_ms = std::chrono::duration<double, std::milli>(
		GcClock::now() - cms_mark_start).count();

	mark_cycle_active.store(false);
	cms_cycle_running.store(false);
	if (gc_barrier_owned)
	{
		GCBarrier::Disable();
		GCBarrier::SetRecordNew(true);
		gc_barrier_owned = false;
	}
	Scheduler::Instance().SafepointEnd();
	cms_world_stopped.store(false);
	cms_final_ms = MillisecondsSince(pause_start);

	if (GcStatsEnabled())
	{
		const double mark_wall_ms = cms_mark_wall_ms;
		fprintf(stderr,
		        "[gcstat] run=%d reason=%s mode=cms init_stw_ms=%.2f "
		        "final_stw_ms=%.2f stw_ms=%.2f mark_wall_ms=%.2f "
		        "mark_steps=%zu allocs_during_mark=%zu weak_ms=%.2f "
		        "final_journal_ms=%.2f final_roots_ms=%.2f final_scan_ms=%.2f "
		        "rthread=%.2f rstatic=%.2f rcp=%.2f "
		        "rtemp=%.2f rglobal=%.2f rjni=%.2f "
		        "satb_records=%zu satb_dropped=%llu sweep=deferred "
		        "ref_entries=%zu ref_repairs=%zu ref_shrinks=%zu "
		        "refindex=%s\n",
		        gc_runs, why, cms_init_ms, cms_final_ms,
		        cms_init_ms + cms_final_ms, mark_wall_ms,
		        cms_mark_steps.load(), cms_mark_allocs.load(), weak_ms,
		        journal_ms, roots_ms, scan_ms,
		        root_thread_ms, root_static_ms, root_cp_ms, root_temp_ms,
		        root_global_ms, root_jni_ms,
		        cycle_recorded,
		        static_cast<unsigned long long>(GCBarrier::Dropped()),
		        ref_index.size(), ref_index_repairs,
		        ref_index_shrinks,
		        ref_index_incremental ? "incremental" : "full");
	}
	return true;
}

void ObjectHeap::CmsSweepStep(size_t max_objects)
{
	VMLock lk;
	if (cms_sweep_pending)
	{
		CmsSweepScanLocked(max_objects);
		return;
	}
	SweepPending(max_objects);
}

// Walks the slot range recorded by the final mark and destroys the slots that
// marked[] still calls dead.  The sweep owns the whole dead set: it measures
// each object before destroying it and keeps live_bytes/live_objects exact for
// the slots that are left.  max_slots bounds one call, so a chunk full of
// survivors still holds VMLock for a bounded time.  A slot recycled by an
// allocation after the final mark was marked live by Register, so it is
// skipped like any survivor.
void ObjectHeap::CmsSweepScanLocked(size_t max_slots)
{
	if (!cms_sweep_pending)
	{
		return;
	}
	const bool unlimited = max_slots == static_cast<size_t>(-1);
	size_t scanned = 0;
	while (cms_sweep_cursor < cms_sweep_hi && (unlimited || scanned < max_slots))
	{
		const size_t index = cms_sweep_cursor++;
		scanned++;
		lpObject obj = objs[index];
		if (!obj)
		{
			continue;
		}
		if (index < marked.size() && marked[index])
		{
			continue;
		}
		const size_t bytes = ObjectBytes(obj);
		UnindexObject(obj);
		delete obj;
		objs[index] = NULL;
		free_indices.push_back(static_cast<OBJECT_REF>(index));
		live_bytes -= bytes;
		live_objects--;
		cms_sweep_freed_bytes += bytes;
		cms_sweep_freed_objects++;
	}
	cms_swept_slots += scanned;
	if (cms_sweep_cursor >= cms_sweep_hi)
	{
		CmsFinishSweepLocked();
	}
}

// Publishes the exact live size of the cycle, adapts the threshold like the
// stop-the-world path does and prints the per-cycle statistics that the final
// pause could not know.  Runs once, under VMLock, at the end of the sweep.
void ObjectHeap::CmsFinishSweepLocked()
{
	cms_sweep_pending = false;
	cms_sweep_ms = MillisecondsSince(cms_sweep_start);

	size_t next_threshold = base_threshold;
	if (live_bytes > next_threshold / 2)
	{
		next_threshold = live_bytes * 2;
	}
	if (max_heap_bytes != 0 && next_threshold > max_heap_bytes)
	{
		next_threshold = max_heap_bytes;
	}
	threshold = next_threshold;

	if (ref_index_incremental && ref_index.bucket_count() > 4096 &&
	    ref_index.size() * 8 < ref_index.bucket_count())
	{
		ref_index_shrink_pending = true;
	}
	gc_freed_last = cms_sweep_freed_bytes;
	gc_freed_total += cms_sweep_freed_bytes;

	if (gc_log)
	{
		fprintf(stderr,
		        "gc-collect run=%d objects=%zu freed=%zu bytes live=%zu bytes\n",
		        gc_runs, cms_sweep_freed_objects, cms_sweep_freed_bytes,
		        live_bytes);
	}
	if (GcStatsEnabled())
	{
		fprintf(stderr,
		        "[gcstat] run=%d mode=cms phase=sweep-done sweep_ms=%.2f "
		        "swept_slots=%zu freed_objs=%zu freed_bytes=%zu "
		        "live_after=%zu objs_after=%zu threshold_after=%zu "
		        "ref_entries=%zu ref_shrinks=%zu\n",
		        gc_runs, cms_sweep_ms, cms_swept_slots,
		        cms_sweep_freed_objects, cms_sweep_freed_bytes, live_bytes,
		        live_objects, threshold, ref_index.size(), ref_index_shrinks);
	}
}

bool ObjectHeap::Register(lpObject obj, LPOBJECT_REF &object_ref)
{
	VMLock lk;
	if (!obj)
	{
		return false;
	}
	// Spread the tear-down of the previous collection's dead objects over the
	// allocations instead of paying for all of them in the pause.  A bounded
	// chunk keeps every single allocation short.  An open concurrent cycle is
	// the exception: its dead set belongs to the collector's sweeper, and
	// draining it here would race the sweeper for no benefit.  Collections
	// that fall back to the sliced mark-sweep (class initialisers holding
	// VMLock) leave their dead set behind after the cycle, so they need this
	// path.
	if (!(cms_mode && mark_cycle_active.load()) && !pending_free.empty())
	{
		SweepPending(kAllocSweepChunk);
	}
	OBJECT_REF index;
	if (!free_indices.empty())
	{
		index = free_indices.back();
		free_indices.pop_back();
	}
	else
	{
		index = sequence++;
	}
	obj->index = index;
	if (static_cast<size_t>(index) >= objs.size())
	{
		objs.resize(static_cast<size_t>(index) + 1, NULL);
	}
	objs[static_cast<size_t>(index)] = obj;
	IndexObject(obj);
	if (mark_cycle_active.load())
	{
		cms_mark_allocs.fetch_add(1);
		MarkNewObjectLocked(obj);
	}
	else if (cms_sweep_pending &&
	         static_cast<size_t>(index) < marked.size())
	{
		// The deferred sweep is still walking the slots of the last cycle.  A
		// recycled slot below its range must not be mistaken for dead garbage,
		// so mark the new occupant live until the sweep finishes.
		marked[static_cast<size_t>(index)] = static_cast<unsigned char>(1);
	}
	object_ref = &obj->index;
	return true;
}

bool ObjectHeap::NewObject(lpClass clazz, LPOBJECT_REF &object_ref)
{
	// Collect before acquiring the heap mutex: the collector stops the world
	// and must not run with the heap mutex already held (lock order).
	MaybeGC();
	VMLock lk;
	lpObject obj = new JObject(clazz);
	if (!obj)
	{
		return false;
	}
	if (!obj->newInstance())
	{
		delete obj;
		return false;
	}
	if (WouldExceedMax(ObjectBytes(obj)))
	{
		delete obj;
		ThrowHeapOOM();
		return false;
	}
	if (!Register(obj, object_ref))
	{
		delete obj;
		return false;
	}
	AccountAllocation(obj);
	return true;
}

bool ObjectHeap::NewArrayObject(int size, lpClass clazz, LPOBJECT_REF &object_ref)
{
	MaybeGC();
	VMLock lk;
	if (size < 0)
	{
		return false;
	}
	if (!clazz)
	{
		return false;
	}
	lpObject obj = new JObject(clazz);
	if (!obj)
	{
		return false;
	}
	if (!obj->newArrayInstance(clazz->ArrayElementType(), size))
	{
		delete obj;
		return false;
	}
	if (WouldExceedMax(ObjectBytes(obj)))
	{
		delete obj;
		ThrowHeapOOM();
		return false;
	}
	LPOBJECT_REF ref = NULL;
	if (!Register(obj, ref))
	{
		delete obj;
		return false;
	}
	object_ref = ref;
	AccountAllocation(obj);
	return true;
}

bool ObjectHeap::NewObjectArrayObject(int size, lpClass clazz, LPOBJECT_REF &object_ref)
{
	MaybeGC();
	VMLock lk;
	if (size < 0)
	{
		return false;
	}
	if (!clazz)
	{
		return false;
	}
	lpObject obj = new JObject(clazz);
	if (!obj)
	{
		return false;
	}
	// An array whose component is itself an array stores references, so the
	// element representation follows the descriptor depth: only a
	// one-dimensional array has primitive slots.
	if (!obj->newArrayInstance(clazz->ArrayElementType(), size))
	{
		delete obj;
		return false;
	}
	if (WouldExceedMax(ObjectBytes(obj)))
	{
		delete obj;
		ThrowHeapOOM();
		return false;
	}
	LPOBJECT_REF ref = NULL;
	if (!Register(obj, ref))
	{
		delete obj;
		return false;
	}
	object_ref = ref;
	AccountAllocation(obj);
	return true;
}

bool ObjectHeap::CreateArrayDimension(lpClass array_class, LPINT32 nums, int level, int count,
                                      LPOBJECT_REF &object_ref)
{
	if (!array_class || !array_class->thisClass || !nums || level < 0 || level >= count)
	{
		return false;
	}
	// The parent dimension must survive collections triggered while the child
	// dimensions are being allocated.
	GCRootScope root(this, &object_ref);
	MaybeGC();
	lpObject obj = new JObject(array_class);
	LPOBJECT_REF ref = NULL;
	if (!Register(obj, ref))
	{
		delete obj;
		return false;
	}
	object_ref = ref;

	int length = nums[level];
	if (level + 1 == count)
	{
		// Innermost dimension: the array class name is a one-dimensional
		// descriptor such as "[I", so its element type is primitive.
		if (!obj->newArrayInstance(array_class->ArrayElementType(), length))
		{
			ReleaseSlot(obj);
			object_ref = NULL;
			delete obj;
			return false;
		}
		if (WouldExceedMax(ObjectBytes(obj)))
		{
			ReleaseSlot(obj);
			object_ref = NULL;
			delete obj;
			ThrowHeapOOM();
			return false;
		}
		AccountAllocation(obj);
		return true;
	}

	if (!obj->newArrayInstance(DT_REF, length))
	{
		ReleaseSlot(obj);
		object_ref = NULL;
		delete obj;
		return false;
	}
	if (WouldExceedMax(ObjectBytes(obj)))
	{
		ReleaseSlot(obj);
		object_ref = NULL;
		delete obj;
		ThrowHeapOOM();
		return false;
	}
	AccountAllocation(obj);

	const char *name = array_class->thisClass->GetName();
	if (!name || name[0] != '[')
	{
		return false;
	}
	lpClass child_class = NULL;
	if (!VMContext::bootstrap_classloader->FindClass(name + 1, child_class))
	{
		return false;
	}
	jobject *refs = static_cast<jobject*>(obj->array_var);
	for (int i = 0; i < length; i++)
	{
		LPOBJECT_REF child_ref = NULL;
		if (!CreateArrayDimension(child_class, nums, level + 1, count, child_ref))
		{
			return false;
		}
		refs[i] = reinterpret_cast<jobject>(child_ref);
	}
	return true;
}

bool ObjectHeap::NewMultiArrayObject(LPINT32 nums, int count, lpClass clazz, LPOBJECT_REF &object_ref)
{
	VMLock lk;
	if (!nums || count <= 0 || !clazz)
	{
		return false;
	}
	for (int i = 0; i < count; i++)
	{
		if (nums[i] < 0)
		{
			return false;
		}
	}
	return CreateArrayDimension(clazz, nums, 0, count, object_ref);
}

// data is a UTF-8 byte string.
bool ObjectHeap::NewStringObject(LPSTR data, int length, LPOBJECT_REF &object_ref)
{
	VMLock lk;
	int unicode_len = 0;
	unsigned short* unicodes = CommonUtils::Utf8ToUnicode(data, unicode_len);
	if (!unicodes)
	{
		return false;
	}
	// The UTF-16 code unit count is authoritative; the UTF-8 byte length is
	// not a valid character count.
	(void)length;
	return NewStringObject(unicodes, unicode_len, object_ref);
}

// data is a UTF-16 code unit buffer; ownership is transferred to the char[]
// object. length is the number of code units.
bool ObjectHeap::NewStringObject(unsigned short *data, int length, LPOBJECT_REF &object_ref)
{
	VMLock lk;
	// A String is a char[] plus the String object; reject before touching the
	// caller's buffer so the failure is a Java OOME, not a C++ exception.
	const size_t estimated = length > 0
		? static_cast<size_t>(length) * 2 + 256 : 256;
	if (WouldExceedMax(estimated))
	{
		delete[] data;
		ThrowHeapOOM();
		return false;
	}
	bool transferred = false;
	{
		// The char[] and the freshly allocated String are only held by C++
		// locals until they are linked/returned, so pin them across the
		// allocations performed while running String.<init>.
		GCRootScope root_object(this, &object_ref);
		LPOBJECT_REF chars_ref = NULL;
		GCRootScope root_chars(this, &chars_ref);
		try
		{
			lpClass data_clazz = NULL;
			if (!VMContext::bootstrap_classloader->FindClass("[C", data_clazz))
			{
				throw new VMException("Fail to load char array class.");
			}
			if (!NewArrayObject(length, data_clazz, chars_ref))
			{
				throw new VMException("Fail to create char array.");
			}
			lpObject chars_obj = NULL;
			if (!GetObject(chars_ref, chars_obj))
			{
				throw new VMException("Fail to get char array direct object.");
			}
			chars_obj->SetData(DT_CHAR, (LPVOID)data, length);
			transferred = true;

			lpClass str_class = NULL;
			if (!VMContext::bootstrap_classloader->FindClass("java/lang/String", str_class))
			{
				throw new VMException("Fail to load string class.");
			}
			if (!NewObject(str_class, object_ref))
			{
				throw new VMException("Fail to new string object.");
			}
			lpMethod method = MethodArea::GetMethod(str_class, "<init>:([C)V");
			if (!method)
			{
				throw new VMException("Fail to find string init method.");
			}
			lpVariant vars = new Variant[2];
			vars[0].heapPtr = object_ref;
			vars[1].heapPtr = chars_ref;
			lpVMExecutor exec = new VMExecutor();
			exec->Initialize();
			exec->Execute(str_class, method, vars, 2);
			delete[] vars;
			delete exec;
		}
		catch (...)
		{
			if (!transferred)
			{
				delete[] data;
			}
			throw;
		}
	}
	return true;
}

bool ObjectHeap::GetObject(OBJECT_REF obj_ref, lpObject &obj)
{
	VMLock lk;
	obj = NULL;
	if (obj_ref <= 0 || static_cast<size_t>(obj_ref) >= objs.size())
	{
		return false;
	}
	obj = objs[static_cast<size_t>(obj_ref)];
	return obj != NULL;
}
