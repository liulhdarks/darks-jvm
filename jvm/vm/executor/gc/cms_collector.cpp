#include "stdafx.h"
#include "cms_collector.h"
#include "vm/executor/objectheap.h"

#include <atomic>
#include <cstdio>

CmsCollector::CmsCollector(ObjectHeap *heap) : heap(heap)
{
}

const char *CmsCollector::Name() const
{
	return "cms";
}

bool CmsCollector::Initialize(ObjectHeap *heap)
{
	this->heap = heap;
	if (heap)
	{
		static std::atomic<bool> warned(false);
		if (!warned.exchange(true))
		{
			fprintf(stderr,
			        "gc: cms collector is not implemented yet; "
			        "falling back to mark-sweep\n");
		}
	}
	return heap != NULL;
}

void CmsCollector::Collect(const char *reason)
{
	if (heap)
	{
		heap->CollectMarkSweep(reason);
	}
}
