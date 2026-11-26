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

	static void Record(LPOBJECT_REF old_value, LPOBJECT_REF new_value,
	                   Site site);

	// Moves the recorded references out for the remark phase.
	static void Drain(std::vector<LPOBJECT_REF> &out);

	static void PrintStats();
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
