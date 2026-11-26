#pragma once

#include "gccollector.h"

class ObjectHeap;

// Placeholder for the concurrent mark-sweep collector.  Until the concurrent
// phases land it delegates to mark-sweep so DARKS_JVM_GC_IMPL=cms stays
// selectable in tests.
class CmsCollector : public GCCollector
{
public:
	explicit CmsCollector(ObjectHeap *heap);

	virtual const char *Name() const;
	virtual bool Initialize(ObjectHeap *heap);
	virtual void Collect(const char *reason);

private:
	ObjectHeap *heap;
};
