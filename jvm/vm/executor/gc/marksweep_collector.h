#pragma once

#include "gccollector.h"

class ObjectHeap;

// Existing stop-the-world mark-sweep, kept as the default and as the
// DARKS_JVM_GC_IMPL=marksweep fallback.
class MarkSweepCollector : public GCCollector
{
public:
	explicit MarkSweepCollector(ObjectHeap *heap);

	virtual const char *Name() const;
	virtual bool Initialize(ObjectHeap *heap);
	virtual void Collect(const char *reason);

private:
	ObjectHeap *heap;
};
