#pragma once

#include "vm/types.h"
#include "jobject.h"
#include "jclass.h"
#include "vmmutex.h"
#include <atomic>
#include <chrono>
#include <map>
#include <string>
#include <unordered_map>
#include <utility>
#include <vector>

class ThreadStack;
class GCCollector;

// ObjectHeap owns every Java object. References (LPOBJECT_REF) are pointers to
// an object's "index" field, so objects must never move; the collector is a
// non-moving mark-sweep.
//
// Configuration (environment, DARKS_JVM_* style):
//   DARKS_JVM_GC=0               disable automatic (allocation-triggered) GC
//   DARKS_JVM_GC_IMPL=NAME       collector: marksweep (default) or cms
//   DARKS_JVM_GC_THRESHOLD=N     collection threshold in heap bytes (default 4 MiB)
//   DARKS_JVM_REFINDEX=MODE      reference index maintenance: incremental
//                                (default) or full (rebuild on every collection)
//   DARKS_JVM_MARK=MODE          marking: incremental (default, sliced pauses)
//                                or stw (one stop-the-world pause)
//   DARKS_JVM_GC_LOG=1           log every collection and a final summary to stderr
//   DARKS_GCSTATS=1              log per-collection STW phase breakdown
class ObjectHeap
{

public:
	// Heap references are indices into this vector; slot 0 is unused so that a
	// zero reference means "null".
	std::vector<lpObject> objs;

	OBJECT_REF sequence;

public:
	ObjectHeap()
	{
		sequence = 1;
		collector = NULL;
		requested_initial_heap = 0;
		requested_max_heap = 0;
		initial_heap_bytes = 0;
		max_heap_bytes = 0;
		oom_in_progress = false;
		gc_enabled = true;
		gc_log = false;
		in_gc = false;
		no_gc_depth = 0;
		base_threshold = 4 * 1024 * 1024;
		threshold = base_threshold;
		live_bytes = 0;
		ref_index_incremental = true;
		ref_index_shrink_pending = false;
		lazy_sweep = true;
		incremental_mark = true;
		mark_cycle_active = false;
		mark_cycle_allocs = 0;
		cycle_root_ms = 0;
		cycle_mark_ms = 0;
		cycle_setup_ms = 0;
		cycle_pause_sum_ms = 0;
		cycle_pause_max_ms = 0;
		cycle_preworld_ms = 0;
		cycle_pauses = 0;
		cycle_recorded = 0;
		cycle_dropped_before = 0;
		gc_barrier_owned = false;
		ref_index_repairs = 0;
		ref_index_shrinks = 0;
		gc_runs = 0;
		gc_freed_total = 0;
		gc_freed_last = 0;
		live_objects = 0;
		root_thread_ms = 0;
		root_static_ms = 0;
		root_cp_ms = 0;
		root_temp_ms = 0;
		root_global_ms = 0;
		root_jni_ms = 0;
		root_stacks = 0;
		root_static_fields = 0;
		root_classes = 0;
		root_cp_entries = 0;
		root_cp_strings = 0;
		root_temp_slots = 0;
		root_global_count = 0;
		root_jni_envs = 0;
	}

	~ObjectHeap();

	bool initialize(int init_size);

	// -Xms/-Xmx in bytes before initialize(); 0 means ergonomics.  The
	// launcher resolves the command line, gc_tests leaves both at 0.
	void SetHeapLimits(size_t initial_bytes, size_t max_bytes);

	// Stop-the-world mark-sweep over the registered thread stacks, static
	// fields, constant-pool strings and temporary (native/C++) roots.
	// reason is diagnostic only: it labels the trigger in DARKS_GCSTATS output.
	void gc(const char *reason = "explicit");

	bool NewObject(lpClass clazz, LPOBJECT_REF &object_ref);

	bool NewArrayObject(int size, lpClass clazz, LPOBJECT_REF &object_ref);

	bool NewObjectArrayObject(int size, lpClass clazz, LPOBJECT_REF &object_ref);

	bool NewMultiArrayObject(LPINT32 nums, int count, lpClass clazz, LPOBJECT_REF &object_ref);

	bool NewStringObject(LPSTR data, int length, LPOBJECT_REF &object_ref);

	bool NewStringObject(unsigned short*data, int length, LPOBJECT_REF &object_ref);

	bool GetObject(LPOBJECT_REF obj_ref, lpObject &obj)
	{
		if (!obj_ref)
		{
			obj = NULL;
			return false;
		}
		return GetObject(*obj_ref, obj);
	}

	// ---- GC roots ---------------------------------------------------------
	// A root slot is the address of a variable holding an LPOBJECT_REF. The
	// heap re-reads the slot on every collection, so callers may register the
	// slot before the reference is assigned. Callers must remove the slot
	// before the variable goes out of scope.
	void AddRootSlot(LPOBJECT_REF *slot);
	void RemoveRootSlot(LPOBJECT_REF *slot);

	// ---- JNI reference management ----------------------------------------
	// Global references are strong roots; weak globals are tracked but do not
	// keep their referent alive.
	void AddGlobalRef(LPOBJECT_REF ref);
	void RemoveGlobalRef(LPOBJECT_REF ref);
	void AddWeakGlobalRef(LPOBJECT_REF ref);
	void RemoveWeakGlobalRef(LPOBJECT_REF ref);
	bool IsGlobalRef(LPOBJECT_REF ref) const;
	bool IsWeakGlobalRef(LPOBJECT_REF ref) const;

	void RegisterThreadStack(ThreadStack *stack);
	void UnregisterThreadStack(ThreadStack *stack);
	// Diagnostic: copies the registered stacks so a caller can walk them
	// without holding the heap lock while formatting frames.
	void SnapshotThreadStacks(std::vector<ThreadStack *> &out);

	// Nested no-collect regions. Native code may hold JNI references that
	// are not visible to the collector, so automatic collection is deferred
	// while a native method runs.
	void PushNoGc();
	void PopNoGc();

	// Marks the object referenced by "ref" if it still points at the index
	// field of a live object. Never dereferences unknown memory.
	void MarkObjectRef(LPOBJECT_REF ref);

	// ---- reference index --------------------------------------------------
	// ref_index maps the address of an object's index field (LPOBJECT_REF) to
	// the object's slot, so a candidate pointer can be validated without
	// dereferencing it.  It is bookkeeping only: the entries are never roots
	// and never keep an object alive.
	//
	// Since M2b the index is maintained incrementally when an object is
	// registered and when the sweep frees it, instead of being rebuilt from
	// the whole slot vector at every collection.  DARKS_JVM_REFINDEX=full
	// restores the per-collection rebuild (the M2a behaviour) as a fallback.
	//
	// Concurrency: every mutation (Register/ReleaseSlot/sweep) and every read
	// (MarkObjectRef, ProcessWeakReferences, the helpers below) happens under
	// VMLock while the world is either running without a collection or stopped
	// at a safepoint, so one lock serializes them.  M3's concurrent marker has
	// to keep that rule or introduce its own read protocol; nothing here is
	// safe to touch without VMLock.
	//
	// Returns the slot recorded for a candidate address, false when the
	// address is not the index field of a live object.
	bool LookupRefIndex(const OBJECT_REF *candidate, OBJECT_REF &index) const;

	size_t RefIndexEntries() const { return ref_index.size(); }
	bool RefIndexIncremental() const { return ref_index_incremental; }
	// Number of times the incremental index had to be rebuilt because it
	// disagreed with the live object table; zero in a healthy run.
	size_t RefIndexRepairs() const { return ref_index_repairs; }
	// Diagnostic/test hook: compares the index against a rebuild from the slot
	// vector.  O(slots), so it is not used on the collection path.
	bool RefIndexMatchesHeap() const;

	// ---- lazy sweep -------------------------------------------------------
	// A collection records the dead slots instead of freeing them inside the
	// pause; the objects are destroyed in bounded chunks from the allocation
	// path and right after the world resumes.  DrainPendingFreeAll finishes
	// the work immediately (tests use it before checking the heap invariants).
	// While a slot is pending its object is still in objs[] but marked dead:
	// it is unreachable, so no mutator can touch it, and the slot is not
	// reused until the object has been destroyed.
	void DrainPendingFreeAll();
	size_t PendingFreeCount() const { return pending_free.size(); }
	bool LazySweepEnabled() const { return lazy_sweep; }
	bool IncrementalMarkEnabled() const { return incremental_mark; }
	// True while a sliced marking cycle has not reached its final pause.
	bool MarkCycleActive() const { return mark_cycle_active; }
	// Overwritten references the write barrier journaled in the last cycle.
	size_t LastCycleRecords() const { return cycle_recorded; }

	// ---- statistics -------------------------------------------------------
	size_t LiveBytes() const { return live_bytes; }
	size_t LiveObjectCount() const { return live_objects; }
	int GcRuns() const { return gc_runs; }
	size_t TotalFreedBytes() const { return gc_freed_total; }
	size_t LastFreedBytes() const { return gc_freed_last; }
	bool GcEnabled() const { return gc_enabled; }

private:

	bool Register(lpObject obj, LPOBJECT_REF &object_ref);

	// Destroys up to max_objects of the dead objects recorded by the last
	// collection.  The caller holds VMLock.
	void SweepPending(size_t max_objects);

	// Index maintenance.  IndexObject is a no-op in "full" mode; UnindexObject
	// always removes the entry so the index equals the live set after a
	// collection in both modes.  ReleaseSlot unregisters an object whose
	// construction failed after Register (slot returned to free_indices,
	// index entry removed).
	void IndexObject(lpObject obj);
	void UnindexObject(lpObject obj);
	void ReleaseSlot(lpObject obj);
	void RebuildRefIndex();
	// Rebuilds the index if it disagrees with the live object table.
	void RepairRefIndexIfNeeded();
	// Rehashes the map after a collection freed most of its entries.  Runs on
	// the next allocation, never inside a pause.
	void MaybeShrinkRefIndex();

	// Creates one array dimension. level is the index into nums; the array
	// class name must contain (count - level) leading '[' characters.
	bool CreateArrayDimension(lpClass array_class, LPINT32 nums, int level, int count,
	                          LPOBJECT_REF &object_ref);

	bool GetObject(OBJECT_REF obj_ref, lpObject &obj);

	// Triggers a collection when the live heap size reaches the threshold.
	void MaybeGC();

	// Mark-sweep implementation, invoked through the selected collector.
	void CollectMarkSweep(const char *reason);
	// One stop-the-world collection (DARKS_JVM_MARK=stw).
	void CollectMarkSweepSTW(const char *reason);

	// ---- incremental (sliced) marking -------------------------------------
	// A cycle runs as a sequence of bounded pauses: the first one snapshots
	// the roots and drains a slice of the mark stack, later pauses drain more
	// slices while the mutator keeps running, and the pause that empties the
	// stack re-reads every root, clears weak referents and sweeps.  Objects
	// allocated while the cycle is open are marked as they are registered
	// ("allocate black").  Edges the mutator overwrites are journaled by the
	// SATB write barrier (overwritten value only) and drained here, so an
	// object that was reachable when the cycle opened stays reachable even if
	// its last edge is moved into an already scanned object and the source
	// edge is dropped.  The final root re-read covers plain stack/operand
	// slots, which can be filled without any heap store.
	void RunMarkPause(const char *reason);
	void ContinueMarkCycle();
	// Drains at most max_objects entries of the mark stack (and stops after
	// max_ms of trace work).  Holds VMLock.  Returns the number scanned.
	size_t ScanMarkSlice(size_t max_objects, double max_ms);
	// Scans one marked object and pushes its references.  Holds VMLock.
	void ScanMarkedObject(OBJECT_REF idx);
	// Marks an object allocated while a cycle is active (allocate black), so
	// the sweep cannot reclaim it and its children are scanned by a slice.
	void MarkNewObjectLocked(lpObject obj);
	// Prints the short per-pause [gcpause] line (DARKS_GCSTATS output).
	void PrintSliceStat(const char *reason, const char *phase, double stw_ms,
	                    double roots_ms, double mark_ms, size_t records);

	friend class MarkSweepCollector;
	friend class CmsCollector;

	void AccountAllocation(lpObject obj);

	void ConfigureFromEnvironment();

	// Resolves the heap limits from the requested -Xms/-Xmx values, falling
	// back to JDK ergonomics (25% / 1.5625% of physical memory).
	void ResolveHeapLimits();

	// True when allocating extra bytes would cross -Xmx.
	bool WouldExceedMax(size_t extra) const;

	// Installs java.lang.OutOfMemoryError("Java heap space") as the pending
	// exception.  Allocations made while the error is being built bypass the
	// limit so the error object itself can always be created.
	void ThrowHeapOOM();

	void MarkRoots();

	// Clears unreachable referents and enqueues the affected
	// java.lang.ref.Reference objects on their ReferenceQueue.
	void ProcessWeakReferences();

	void MarkStaticRoots();
	void MarkConstantPoolRoots();
	void MarkThreadStackRoots();
	void MarkTemporaryRoots();
	void MarkGlobalRefs();
	void MarkJniLocalRoots();

	size_t ObjectBytes(lpObject obj) const;

	std::vector<bool> marked;
	std::vector<OBJECT_REF> mark_stack;
	std::vector<OBJECT_REF> weak_reference_objects;
	std::unordered_map<const OBJECT_REF *, OBJECT_REF> ref_index;
	bool ref_index_incremental;
	bool ref_index_shrink_pending;
	bool lazy_sweep;
	bool incremental_mark;
	std::atomic<bool> mark_cycle_active;
	std::atomic<size_t> mark_cycle_allocs;
	// Accumulated over the pauses of one cycle, printed on the final
	// [gcstat] line.
	double cycle_root_ms;
	double cycle_mark_ms;
	double cycle_setup_ms;
	double cycle_pause_sum_ms;
	double cycle_pause_max_ms;
	double cycle_preworld_ms;
	size_t cycle_pauses;
	// Trace budget of the current cycle, fixed in its first pause.
	size_t cycle_slice_budget;
	// References drained from the write barrier over the whole cycle, and the
	// overflow counter seen when it opened.
	size_t cycle_recorded;
	unsigned long long cycle_dropped_before;
	// True when this cycle enabled the barrier itself, so it also disables it.
	bool gc_barrier_owned;
	// Cycle-start snapshots repeated on the final [gcstat] line so the pause
	// accounting still describes the whole collection.
	size_t cycle_slots_before;
	size_t cycle_free_before;
	size_t cycle_live_bytes_before;
	size_t cycle_live_objects_before;
	size_t cycle_threshold_before;
	std::string cycle_trigger_frame;
	std::chrono::steady_clock::time_point cycle_gc_start;
	size_t ref_index_repairs;
	size_t ref_index_shrinks;

	std::vector<OBJECT_REF> pending_free;
	std::vector<OBJECT_REF> free_indices;
	std::vector<LPOBJECT_REF *> root_slots;
	std::vector<LPOBJECT_REF> global_refs;
	std::vector<LPOBJECT_REF> weak_global_refs;
	std::vector<ThreadStack *> thread_stacks;

	std::mutex gc_mutex;
	bool gc_enabled;
	bool gc_log;
	bool in_gc;
	int no_gc_depth;
	GCCollector *collector;
	size_t requested_initial_heap;
	size_t requested_max_heap;
	size_t initial_heap_bytes;
	size_t max_heap_bytes;
	bool oom_in_progress;
	size_t base_threshold;
	size_t threshold;
	size_t live_bytes;
	int gc_runs;
	size_t gc_freed_total;
	size_t gc_freed_last;
	size_t live_objects;

	// Diagnostic breakdown of the last root scan (printed under DARKS_GCSTATS).
	double root_thread_ms;
	double root_static_ms;
	double root_cp_ms;
	double root_temp_ms;
	double root_global_ms;
	double root_jni_ms;
	size_t root_stacks;
	size_t root_static_fields;
	size_t root_classes;
	size_t root_cp_entries;
	size_t root_cp_strings;
	size_t root_temp_slots;
	size_t root_global_count;
	size_t root_jni_envs;
};

typedef ObjectHeap *lpObjectHeap;

// RAII helper: keeps one reference variable alive across allocations.
class GCRootScope
{
public:
	explicit GCRootScope(lpObjectHeap heap, LPOBJECT_REF *slot)
	{
		this->heap = heap;
		this->slot = slot;
		if (heap)
		{
			heap->AddRootSlot(slot);
		}
	}

	~GCRootScope()
	{
		if (heap)
		{
			heap->RemoveRootSlot(slot);
		}
	}

private:
	GCRootScope(const GCRootScope &);
	GCRootScope &operator=(const GCRootScope &);

	lpObjectHeap heap;
	LPOBJECT_REF *slot;
};

// RAII helper: keeps a set of reference variables alive across allocations.
class GCRootSet
{
public:
	explicit GCRootSet(lpObjectHeap heap)
	{
		this->heap = heap;
	}

	~GCRootSet()
	{
		for (size_t i = 0; i < slots.size(); i++)
		{
			if (heap)
			{
				heap->RemoveRootSlot(slots[i]);
			}
		}
	}

	void Add(LPOBJECT_REF *slot)
	{
		slots.push_back(slot);
		if (heap)
		{
			heap->AddRootSlot(slot);
		}
	}

private:
	GCRootSet(const GCRootSet &);
	GCRootSet &operator=(const GCRootSet &);

	lpObjectHeap heap;
	std::vector<LPOBJECT_REF *> slots;
};

// RAII helper: defers automatic collection while native code runs.
class GCNativeScope
{
public:
	explicit GCNativeScope(lpObjectHeap heap)
	{
		this->heap = heap;
		if (heap)
		{
			heap->PushNoGc();
		}
	}

	~GCNativeScope()
	{
		if (heap)
		{
			heap->PopNoGc();
		}
	}

private:
	GCNativeScope(const GCNativeScope &);
	GCNativeScope &operator=(const GCNativeScope &);

	lpObjectHeap heap;
};
