#include "stdafx.h"
#include "marksweep_collector.h"
#include "vm/executor/objectheap.h"

MarkSweepCollector::MarkSweepCollector(ObjectHeap *heap) : heap(heap)
{
}

const char *MarkSweepCollector::Name() const
{
	return "marksweep";
}

bool MarkSweepCollector::Initialize(ObjectHeap *heap)
{
	this->heap = heap;
	return heap != NULL;
}

void MarkSweepCollector::Collect(const char *reason)
{
	if (heap)
	{
		heap->CollectMarkSweep(reason);
	}
}
