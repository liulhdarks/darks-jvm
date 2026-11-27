#include "stdafx.h"
#include "gcbarrier.h"
#include "vm/executor/vmmutex.h"

#include <atomic>
#include <cstdio>
#include <cstdlib>

bool g_gc_barrier_active = false;

namespace {

// Cap the recorded set so a long marking cycle cannot grow without bound.
// The concurrent collector turns an overflow into a full stop-the-world
// mark-sweep instead of freeing from an incomplete trace.
const size_t kMaxRecorded = 4 * 1024 * 1024;

std::atomic<size_t> g_record_limit(kMaxRecorded);

std::vector<LPOBJECT_REF> &Recorded()
{
	static std::vector<LPOBJECT_REF> recorded;
	return recorded;
}

std::atomic<unsigned long long> g_site_counts[GCBarrier::SITE_COUNT];
std::atomic<unsigned long long> g_dropped(0);
std::atomic<unsigned long long> g_pending(0);
std::atomic<bool> g_record_new(true);

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

void GCBarrier::SetRecordNew(bool enable)
{
	g_record_new.store(enable, std::memory_order_relaxed);
}

unsigned long long GCBarrier::Dropped()
{
	return g_dropped.load(std::memory_order_relaxed);
}

size_t GCBarrier::Pending()
{
	return static_cast<size_t>(g_pending.load(std::memory_order_relaxed));
}

void GCBarrier::Record(LPOBJECT_REF old_value, LPOBJECT_REF new_value,
                       Site site)
{
	g_site_counts[site].fetch_add(1, std::memory_order_relaxed);
	const bool record_new = g_record_new.load(std::memory_order_relaxed);
	if (!old_value && (!record_new || !new_value))
	{
		return;
	}
	VMLock lk;
	std::vector<LPOBJECT_REF> &recorded = Recorded();
	size_t limit = g_record_limit.load(std::memory_order_relaxed);
	if (limit == 0)
	{
		limit = kMaxRecorded;
	}
	if (recorded.size() >= limit)
	{
		g_dropped.fetch_add(1, std::memory_order_relaxed);
		return;
	}
	if (old_value)
	{
		recorded.push_back(old_value);
	}
	if (record_new && new_value)
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

void GCBarrier::SetRecordLimit(size_t limit)
{
	g_record_limit.store(limit, std::memory_order_relaxed);
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
