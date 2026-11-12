#pragma once

#include "vm/types.h"
#include "jobject.h"
#include "jclass.h"
#include <map>
#include <utility>
#include <vector>

class ThreadStack;

// ObjectHeap owns every Java object. References (LPOBJECT_REF) are pointers to
// an object's "index" field, so objects must never move; the collector is a
// non-moving stop-the-world mark-sweep.
//
// Configuration (environment, DARKS_JVM_* style):
//   DARKS_JVM_GC=0            disable automatic (allocation-triggered) GC
//   DARKS_JVM_GC_THRESHOLD=N  collection threshold in heap bytes (default 4 MiB)
//   DARKS_JVM_GC_LOG=1        log every collection and a final summary to stderr
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
		gc_enabled = true;
		gc_log = false;
		in_gc = false;
		no_gc_depth = 0;
		base_threshold = 4 * 1024 * 1024;
		threshold = base_threshold;
		live_bytes = 0;
		gc_runs = 0;
		gc_freed_total = 0;
		gc_freed_last = 0;
		live_objects = 0;
	}

	~ObjectHeap();

	bool initialize(int init_size);

	// Stop-the-world mark-sweep over the registered thread stacks, static
	// fields, constant-pool strings and temporary (native/C++) roots.
	void gc();

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

	void RegisterThreadStack(ThreadStack *stack);
	void UnregisterThreadStack(ThreadStack *stack);

	// Nested no-collect regions. Native code may hold JNI references that
	// are not visible to the collector, so automatic collection is deferred
	// while a native method runs.
	void PushNoGc();
	void PopNoGc();

	// Marks the object referenced by "ref" if it still points at the index
	// field of a live object. Never dereferences unknown memory.
	void MarkObjectRef(LPOBJECT_REF ref);

	// ---- statistics -------------------------------------------------------
	size_t LiveBytes() const { return live_bytes; }
	size_t LiveObjectCount() const { return live_objects; }
	int GcRuns() const { return gc_runs; }
	size_t TotalFreedBytes() const { return gc_freed_total; }
	size_t LastFreedBytes() const { return gc_freed_last; }
	bool GcEnabled() const { return gc_enabled; }

private:

	bool Register(lpObject obj, LPOBJECT_REF &object_ref);

	// Creates one array dimension. level is the index into nums; the array
	// class name must contain (count - level) leading '[' characters.
	bool CreateArrayDimension(lpClass array_class, LPINT32 nums, int level, int count,
	                          LPOBJECT_REF &object_ref);

	bool GetObject(OBJECT_REF obj_ref, lpObject &obj);

	// Triggers a collection when the live heap size reaches the threshold.
	void MaybeGC();

	void AccountAllocation(lpObject obj);

	void ConfigureFromEnvironment();

	void MarkRoots();

	void MarkStaticRoots();
	void MarkConstantPoolRoots();
	void MarkThreadStackRoots();
	void MarkTemporaryRoots();

	size_t ObjectBytes(lpObject obj) const;

	std::vector<bool> marked;
	std::vector<OBJECT_REF> mark_stack;
	std::map<const OBJECT_REF *, OBJECT_REF> ref_index;

	std::vector<OBJECT_REF> free_indices;
	std::vector<LPOBJECT_REF *> root_slots;
	std::vector<ThreadStack *> thread_stacks;

	bool gc_enabled;
	bool gc_log;
	bool in_gc;
	int no_gc_depth;
	size_t base_threshold;
	size_t threshold;
	size_t live_bytes;
	int gc_runs;
	size_t gc_freed_total;
	size_t gc_freed_last;
	size_t live_objects;
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
