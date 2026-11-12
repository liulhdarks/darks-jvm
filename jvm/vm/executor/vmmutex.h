#pragma once

#include <mutex>

// Global VM lock.  Protects the object heap, class metadata and object field
// storage so that the interpreter can run on several OS threads.
std::recursive_mutex &VMMutex();

// RAII guard for VMMutex().
//
// While a thread is blocked acquiring the lock it cannot touch the heap, so it
// is reported to the scheduler as a safe point.  Without this, a stop-the-world
// collection triggered from inside an allocation (the collector already owns
// VMMutex) would wait forever for a worker that is blocked on the same lock.
class VMLock
{
public:
	VMLock();
	~VMLock();

private:
	VMLock(const VMLock &);
	VMLock &operator=(const VMLock &);
};
