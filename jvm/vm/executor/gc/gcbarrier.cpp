#include "stdafx.h"
#include "gcbarrier.h"
#include "vm/executor/vmmutex.h"

#include <atomic>
#include <cstdio>
#include <cstdlib>

bool g_gc_barrier_active = false;

namespace {

// Cap the recorded set so a long marking cycle cannot grow without bound.
// M3 turns an overflow into a full remark rescan instead of relying on this
// buffer alone.
const size_t kMaxRecorded = 4 * 1024 * 1024;

std::vector<LPOBJECT_REF> &Recorded()
{
	static std::vector<LPOBJECT_REF> recorded;
	return recorded;
}

std::atomic<unsigned long long> g_site_counts[GCBarrier::SITE_COUNT];
std::atomic<unsigned long long> g_dropped(0);
std::atomic<unsigned long long> g_pending(0);

void PrintStatsAtExit()
{
	GCBarrier::PrintStats();
}

std::atomic<bool> g_atexit_registered(false);

} // namespace

void GCBarrier::Enable()
{
	if (!g_atexit_registered.exchange(true))
	{
		if (getenv("DARKS_GC_SATB_STATS") || getenv("DARKS_GC_SATB_TEST"))
		{
			atexit(PrintStatsAtExit);
		}
	}
	g_gc_barrier_active = true;
}

void GCBarrier::Disable()
{
	g_gc_barrier_active = false;
}

bool GCBarrier::Enabled()
{
	return g_gc_barrier_active;
}

void GCBarrier::Record(LPOBJECT_REF old_value, LPOBJECT_REF new_value,
                       Site site)
{
	g_site_counts[site].fetch_add(1, std::memory_order_relaxed);
	if (!old_value && !new_value)
	{
		return;
	}
	VMLock lk;
	std::vector<LPOBJECT_REF> &recorded = Recorded();
	if (recorded.size() >= kMaxRecorded)
	{
		g_dropped.fetch_add(1, std::memory_order_relaxed);
		return;
	}
	if (old_value)
	{
		recorded.push_back(old_value);
	}
	if (new_value)
	{
		recorded.push_back(new_value);
	}
	g_pending.store(recorded.size(), std::memory_order_relaxed);
}

void GCBarrier::Drain(std::vector<LPOBJECT_REF> &out)
{
	VMLock lk;
	out.swap(Recorded());
	g_pending.store(0, std::memory_order_relaxed);
}

void GCBarrier::PrintStats()
{
	unsigned long long counts[SITE_COUNT];
	for (int i = 0; i < SITE_COUNT; i++)
	{
		counts[i] = g_site_counts[i].load(std::memory_order_relaxed);
	}
	fprintf(stderr,
	        "[satb] field=%llu static=%llu array=%llu jni=%llu "
	        "reflect=%llu other=%llu pending=%llu dropped=%llu\n",
	        counts[FIELD], counts[STATIC], counts[ARRAY], counts[JNI],
	        counts[REFLECT], counts[OTHER],
	        g_pending.load(std::memory_order_relaxed),
	        g_dropped.load(std::memory_order_relaxed));
}
