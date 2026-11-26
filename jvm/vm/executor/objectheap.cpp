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

#include <string>
#include <chrono>
#include <cstdlib>

namespace {

bool GcStatsEnabled()
{
	static const bool enabled = getenv("DARKS_GCSTATS") != NULL;
	return enabled;
}

typedef std::chrono::steady_clock GcClock;

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

bool ObjectHeap::initialize(int init_size)
{
	if (init_size < 1)
	{
		init_size = 1;
	}
	objs.assign(static_cast<size_t>(init_size), NULL);
	sequence = 1;
	ConfigureFromEnvironment();
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
	if (threshold_env && *threshold_env)
	{
		unsigned long long value = strtoull(threshold_env, NULL, 10);
		if (value >= 1024)
		{
			base_threshold = static_cast<size_t>(value);
		}
	}
	threshold = base_threshold;
}

size_t ObjectHeap::ObjectBytes(lpObject obj) const
{
	if (!obj)
	{
		return 0;
	}
	// Approximate footprint: the object header, the per-field value boxes and
	// the array payload. Field map overhead is not counted exactly, which only
	// makes the threshold slightly pessimistic.
	size_t bytes = sizeof(JObject) + obj->varants.size() * (sizeof(FieldValue) + 32);
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

void ObjectHeap::MaybeGC()
{
	// Read the accounting under the heap mutex (allocations update it there),
	// then collect without it: the collector stops the world, and the
	// safepoint handshake must not run with the heap mutex already held.
	bool needed = false;
	{
		VMLock lk;
		if (!gc_enabled || in_gc || no_gc_depth > 0)
		{
			return;
		}
		needed = (live_bytes >= threshold);
	}
	if (needed)
	{
		gc("threshold");
	}
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
	std::map<const OBJECT_REF *, OBJECT_REF>::iterator it = ref_index.find(ref);
	if (it == ref_index.end())
	{
		return;
	}
	OBJECT_REF idx = it->second;
	if (idx <= 0 || static_cast<size_t>(idx) >= objs.size())
	{
		return;
	}
	if (marked[static_cast<size_t>(idx)])
	{
		return;
	}
	marked[static_cast<size_t>(idx)] = true;
	mark_stack.push_back(idx);
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
		std::map<const OBJECT_REF *, OBJECT_REF>::iterator target_it =
			ref_index.find(target);
		if (target_it != ref_index.end())
		{
			OBJECT_REF target_index = target_it->second;
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
	// Only one collection at a time.  A native call may reach allocation from
	// another thread; in that case the collection is skipped rather than
	// waiting while holding the heap lock.
	const GcClock::time_point gc_start = GcClock::now();
	if (!reason)
	{
		reason = "unknown";
	}
	std::unique_lock<std::mutex> gc_lock(gc_mutex, std::try_to_lock);
	if (!gc_lock.owns_lock())
	{
		return;
	}
	if (in_gc)
	{
		return;
	}
	// Hold the heap mutex before stopping the world.  A worker that is blocked
	// on this mutex is then genuinely stopped for the whole collection, which
	// makes the scheduler's "waiting for the heap lock" safepoint state stable.
	// Taking the mutex afterwards would leave a window where such a worker
	// resumes and runs Java while the heap is being traced.
	VMLock heap_lock;
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
	ref_index.clear();
	for (size_t i = 1; i < objs.size(); i++)
	{
		if (objs[i])
		{
			ref_index[&objs[i]->index] = static_cast<OBJECT_REF>(i);
		}
	}

	marked.assign(objs.size(), false);
	mark_stack.clear();
	weak_reference_objects.clear();
	const GcClock::time_point setup_done = GcClock::now();

	MarkRoots();
	const GcClock::time_point roots_done = GcClock::now();

	// Iterative trace: scan every marked object for outgoing references.
	while (!mark_stack.empty())
	{
		OBJECT_REF idx = mark_stack.back();
		mark_stack.pop_back();
		lpObject obj = objs[static_cast<size_t>(idx)];
		if (!obj)
		{
			continue;
		}
		bool reference = IsReferenceObject(obj);
		if (reference)
		{
			weak_reference_objects.push_back(idx);
		}
		// Instance fields. Primitive values are tolerated: MarkObjectRef only
		// accepts pointers that match a live object's index address.  Slots
		// are scanned first because a subclass field can hide a superclass
		// field with the same name and descriptor; the name-keyed map below
		// keeps only one of them, while every field has its own slot.
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
			// Reference.referent is a weak edge; it is handled after the
			// strong trace once reachability is known.
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

	const GcClock::time_point trace_done = GcClock::now();
	ProcessWeakReferences();
	const GcClock::time_point weak_done = GcClock::now();

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
			size_t bytes = ObjectBytes(obj);
			freed_bytes += bytes;
			freed_objects++;
			delete obj;
			objs[i] = NULL;
			// Reuse the index on the next allocation. An address-validated
			// reference can never alias the recycled slot because the new
			// object lives at a different address.
			free_indices.push_back(static_cast<OBJECT_REF>(i));
		}
		else
		{
			live += ObjectBytes(obj);
			live_count++;
		}
	}

	const GcClock::time_point sweep_done = GcClock::now();
	live_bytes = live;
	live_objects = live_count;
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
		        "ref_entries=%zu rthread=%.2f rstatic=%.2f "
		        "rcp=%.2f rtemp=%.2f rglobal=%.2f rjni=%.2f slots=%zu "
		        "free_before=%zu live_before=%zu objs_before=%zu "
		        "thresh_before=%zu freed_objs=%zu freed_bytes=%zu "
		        "live_after=%zu objs_after=%zu stacks=%zu static_fields=%zu "
		        "classes=%zu cp_entries=%zu cp_strings=%zu temp_slots=%zu "
		        "globals=%zu jni_envs=%zu frame=%s\n",
		        gc_runs, reason, total_ms, preworld_ms, stw_ms, root_ms,
		        mark_ms, weak_ms, sweep_ms, post_ms, setup_ms, roots_ms,
		        ref_index.size(), root_thread_ms,
		        root_static_ms, root_cp_ms, root_temp_ms, root_global_ms,
		        root_jni_ms, slots_before, free_before, live_bytes_before,
		        live_objects_before, threshold_before, freed_objects,
		        freed_bytes, live_bytes, live_objects, root_stacks,
		        root_static_fields, root_classes, root_cp_entries,
		        root_cp_strings, root_temp_slots, root_global_count,
		        root_jni_envs, trigger_frame.c_str());
	}
}

bool ObjectHeap::Register(lpObject obj, LPOBJECT_REF &object_ref)
{
	VMLock lk;
	if (!obj)
	{
		return false;
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
	lpObject obj = new JObject(clazz);
	LPOBJECT_REF ref = NULL;
	if (!Register(obj, ref) || !obj->newArrayInstance(size))
	{
		if (obj->index > 0 && static_cast<size_t>(obj->index) < objs.size())
		{
			objs[static_cast<size_t>(obj->index)] = NULL;
			free_indices.push_back(obj->index);
		}
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
	lpObject obj = new JObject(clazz);
	LPOBJECT_REF ref = NULL;
	// An array whose component is itself an array stores references, so the
	// element representation follows the descriptor depth: only a
	// one-dimensional array has primitive slots.
	DATA_TYPE element_type = DT_REF;
	const char *class_name = (clazz && clazz->thisClass)
		? clazz->thisClass->GetName() : NULL;
	if (class_name)
	{
		TypeDesc type_desc;
		parse_desc_type(class_name, type_desc, static_cast<int>(strlen(class_name)));
		if (type_desc.array_level == 1 && type_desc.data_type != DT_ARRAY)
		{
			element_type = type_desc.data_type;
		}
	}
	if (!Register(obj, ref) || !obj->newArrayInstance(element_type, size))
	{
		if (obj->index > 0 && static_cast<size_t>(obj->index) < objs.size())
		{
			objs[static_cast<size_t>(obj->index)] = NULL;
			free_indices.push_back(obj->index);
		}
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
		// descriptor such as "[I"; JObject infers the element type from it.
		if (!obj->newArrayInstance(length))
		{
			objs[static_cast<size_t>(obj->index)] = NULL;
			free_indices.push_back(obj->index);
			object_ref = NULL;
			delete obj;
			return false;
		}
		AccountAllocation(obj);
		return true;
	}

	if (!obj->newArrayInstance(DT_REF, length))
	{
		objs[static_cast<size_t>(obj->index)] = NULL;
		free_indices.push_back(obj->index);
		object_ref = NULL;
		delete obj;
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
