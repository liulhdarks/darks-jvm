#pragma once

class ObjectHeap;

// Collector abstraction owned by ObjectHeap.  M1 only needs the lifecycle
// hooks; the concurrent-mark work adds barrier and allocation hooks here.
class GCCollector
{
public:
	virtual ~GCCollector() {}

	virtual const char *Name() const = 0;

	// Called once after the heap is initialized, before the first collection.
	virtual bool Initialize(ObjectHeap *heap) = 0;

	// Runs one collection.  Implementations may stop the world and split the
	// work into several pauses.
	virtual void Collect(const char *reason) = 0;
};

// Maps DARKS_JVM_GC_IMPL to a collector: "marksweep" selects the sliced
// stop-the-world collector, any other name (including none, the default)
// selects CMS.  The returned collector is owned by the caller.
GCCollector *CreateGCCollector(const char *name, ObjectHeap *heap);
