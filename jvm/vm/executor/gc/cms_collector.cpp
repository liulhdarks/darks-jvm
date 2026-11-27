#include "stdafx.h"
#include "cms_collector.h"
#include "vm/executor/objectheap.h"
#include "vm/executor/vmmutex.h"

#include <cstdio>

CmsCollector::CmsCollector(ObjectHeap *heap)
    : heap(heap),
      started(false),
      marker_run(false),
      marker_finished(true),
      sweep_run(false),
      sweep_finished(true),
      stopping(false)
{
}

CmsCollector::~CmsCollector()
{
	StopThreads();
}

const char *CmsCollector::Name() const
{
	return "cms";
}

bool CmsCollector::Initialize(ObjectHeap *heap)
{
	this->heap = heap;
	if (!heap)
	{
		return false;
	}
	if (!started)
	{
		started = true;
		marker = std::thread(&CmsCollector::MarkerMain, this);
		sweeper = std::thread(&CmsCollector::SweeperMain, this);
	}
	return true;
}

void CmsCollector::StopThreads()
{
	if (!started)
	{
		return;
	}
	{
		std::lock_guard<std::mutex> lk(state_mutex);
		stopping = true;
	}
	marker_wake.notify_all();
	sweep_wake.notify_all();
	if (marker.joinable())
	{
		marker.join();
	}
	if (sweeper.joinable())
	{
		sweeper.join();
	}
	started = false;
}

// Background trace.  Between two steps the thread holds no lock, so the
// mutator runs; VMLock is taken only for the bounded step itself.
void CmsCollector::MarkerMain()
{
	heap->CmsSetMarkerThread();
	std::unique_lock<std::mutex> lk(state_mutex);
	for (;;)
	{
		marker_wake.wait(lk, [this] { return stopping || marker_run; });
		if (stopping)
		{
			return;
		}
		lk.unlock();
		while (!heap->CmsAbortRequested() && !heap->CmsMarkComplete())
		{
			if (!heap->CmsMarkStep(2048, 1.0))
			{
				break;
			}
			// Give the mutator threads the CPU between two trace steps.  On a
			// single pinned core this is what makes the phases interleave.
			std::this_thread::yield();
		}
		lk.lock();
		marker_run = false;
		marker_finished = true;
		marker_done.notify_all();
	}
}

// Background sweep.  The dead set was fixed by the final mark, so destroying
// it needs only VMLock for one bounded chunk at a time.
void CmsCollector::SweeperMain()
{
	std::unique_lock<std::mutex> lk(state_mutex);
	for (;;)
	{
		sweep_wake.wait(lk, [this] { return stopping || sweep_run; });
		if (stopping)
		{
			return;
		}
		lk.unlock();
		while (heap->PendingFreeCount() > 0)
		{
			heap->CmsSweepStep(256);
			std::this_thread::yield();
		}
		lk.lock();
		sweep_run = false;
		sweep_finished = true;
		sweep_done.notify_all();
	}
}

void CmsCollector::Collect(const char *reason)
{
	if (!heap)
	{
		return;
	}
	const char *why = reason ? reason : "unknown";
	// Serialise whole collections.  A collection already in progress (from
	// another allocating thread) makes this request a no-op, like the
	// mark-sweep collector.
	if (!heap->CmsCycleEnter())
	{
		return;
	}

	if (!heap->IncrementalMarkEnabled())
	{
		// DARKS_JVM_MARK=stw: keep the single-stop-the-world diagnostic mode.
		heap->CollectMarkSweepSTW(why);
		heap->CmsCycleLeave();
		return;
	}

	if (VMLockHeldByCurrentThread())
	{
		// Collection was requested from code that already owns VMLock, for
		// example a nested class initializer running inside a GetStatic.  The
		// marker would block on that lock while this thread waits for it, so
		// the cycle degenerates to one stop-the-world mark-sweep (the caller
		// already excludes every other mutator anyway).
		heap->CollectMarkSweepSTW(why);
		heap->CmsCycleLeave();
		return;
	}

	heap->CmsInitMark(why);

	{
		std::lock_guard<std::mutex> lk(state_mutex);
		marker_finished = false;
		marker_run = true;
	}
	marker_wake.notify_one();
	{
		std::unique_lock<std::mutex> lk(state_mutex);
		marker_done.wait(lk, [this] { return marker_finished || !started; });
	}

	const bool remark_ok = heap->CmsFinalMark(why);
	if (!remark_ok)
	{
		// Concurrent mode failure: one full stop-the-world mark-sweep.
		heap->CollectMarkSweepSTW(why);
	}

	// Both paths can leave part of the dead set pending (the fallback sweep
	// stops after a bounded chunk).  An explicit gc() and every test must
	// observe a settled heap, so the collector waits for its sweeper.
	if (heap->PendingFreeCount() > 0)
	{
		{
			std::lock_guard<std::mutex> lk(state_mutex);
			sweep_finished = false;
			sweep_run = true;
		}
		sweep_wake.notify_one();
		std::unique_lock<std::mutex> lk(state_mutex);
		sweep_done.wait(lk, [this] { return sweep_finished || !started; });
	}
	heap->CmsCycleLeave();
}
