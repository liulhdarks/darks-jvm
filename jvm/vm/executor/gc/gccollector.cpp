#include "stdafx.h"
#include "gccollector.h"
#include "cms_collector.h"
#include "marksweep_collector.h"

#include <cstdio>
#include <cstring>

GCCollector *CreateGCCollector(const char *name, ObjectHeap *heap)
{
	if (name && strcmp(name, "marksweep") == 0)
	{
		return new MarkSweepCollector(heap);
	}
	if (name && *name && strcmp(name, "cms") != 0)
	{
		fprintf(stderr, "gc: unknown DARKS_JVM_GC_IMPL=%s; using cms\n", name);
	}
	return new CmsCollector(heap);
}
