#pragma once

#include "vm/types.h"
#include <vector>

// SATB/incremental-update write barrier.  While a concurrent cycle is
// marking, every heap reference write records both the overwritten value
// (snapshot-at-the-beginning) and the stored value (incremental update), so
// the remark phase can keep everything reachable that either the mutator or
// the marker could observe.
//
// The barrier is a single global flag read on every reference write: while no
// concurrent cycle is active the cost is one predictable branch.
class GCBarrier
{
public:
	enum Site
	{
		FIELD = 0,
		STATIC = 1,
		ARRAY = 2,
		JNI = 3,
		REFLECT = 4,
		OTHER = 5,
		SITE_COUNT = 6
	};

	static void Enable();
	static void Disable();
	static bool Enabled();

	// A sliced marking cycle only needs the overwritten values: the snapshot
	// keeps everything that was reachable when the cycle opened, and every
	// allocation is marked as it is registered.  Sites stay recorded either
	// way, so the default keeps both operands for the SATB test to inspect.
	static void SetRecordNew(bool enable);

	static void Record(LPOBJECT_REF old_value, LPOBJECT_REF new_value,
	                   Site site);

	// Moves the recorded references out for the remark phase.
	static void Drain(std::vector<LPOBJECT_REF> &out);

	// Overwrites the buffer refused to record because it was full.
	static unsigned long long Dropped();
	// References waiting in the buffer (cheap pacing signal for the marker).
	static size_t Pending();

	static void PrintStats();

	// Test hook: caps the journal at "limit" entries (0 = the default 4 MiB
	// cap).  The overflow path must turn the cycle into a full stop-the-world
	// mark-sweep instead of freeing from an incomplete trace.
	static void SetRecordLimit(size_t limit);
};

extern bool g_gc_barrier_active;

inline void GCBarrierWrite(LPOBJECT_REF old_value, LPOBJECT_REF new_value,
                           GCBarrier::Site site)
{
	if (g_gc_barrier_active)
	{
		GCBarrier::Record(old_value, new_value, site);
	}
}
