#pragma once

#include "gccollector.h"

#include <condition_variable>
#include <mutex>
#include <thread>

class ObjectHeap;

// CMS-lite: a non-moving concurrent mark-sweep collector.
//
// One collection is four phases: a stop-the-world initial mark that snapshots
// the roots, a background trace that runs on a dedicated marker thread while
// the mutator keeps executing, a stop-the-world final mark that drains the
// SATB journal and moves the dead set to the sweeper, and a background sweep
// that destroys the dead objects in bounded chunks.
//
// A concurrent mode failure (SATB journal overflow, or the heap coming within
// 1/8 of -Xmx during the trace) abandons the cycle and retries the collection
// as one full stop-the-world mark-sweep; nothing is freed from a partial
// trace.
//
// The marker thread is not a scheduler worker, so SafepointBegin never waits
// for it.  It parks while a pause is in progress and takes VMLock only for a
// bounded trace step.
class CmsCollector : public GCCollector
{
public:
	explicit CmsCollector(ObjectHeap *heap);
	virtual ~CmsCollector();

	virtual const char *Name() const;
	virtual bool Initialize(ObjectHeap *heap);
	virtual void Collect(const char *reason);

private:
	void MarkerMain();
	void SweeperMain();
	void StopThreads();

	ObjectHeap *heap;
	std::thread marker;
	std::thread sweeper;
	std::mutex state_mutex;
	std::condition_variable marker_wake;
	std::condition_variable marker_done;
	std::condition_variable sweep_wake;
	std::condition_variable sweep_done;
	bool started;
	bool marker_run;
	bool marker_finished;
	bool sweep_run;
	bool sweep_finished;
	bool stopping;
};
