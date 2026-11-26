#pragma once

#include "vm/types.h"
#include "jobject.h"
#include "jclass.h"
#include "vmmutex.h"
#include <map>
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

	// ---- statistics -------------------------------------------------------
	size_t LiveBytes() const { return live_bytes; }
	size_t LiveObjectCount() const { return live_objects; }
	int GcRuns() const { return gc_runs; }
	size_t TotalFreedBytes() const { return gc_freed_total; }
	size_t LastFreedBytes() const { return gc_freed_last; }
	bool GcEnabled() const { return gc_enabled; }

private:

	bool Register(lpObject obj, LPOBJECT_REF &object_ref);

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
	size_t ref_index_repairs;
	size_t ref_index_shrinks;

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
