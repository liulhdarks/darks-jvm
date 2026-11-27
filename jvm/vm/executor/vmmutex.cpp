#include "stdafx.h"
#include "vmmutex.h"
#include "scheduler.h"

namespace {

thread_local int t_vm_lock_depth = 0;

} // namespace

bool VMLockHeldByCurrentThread()
{
	return t_vm_lock_depth > 0;
}

std::recursive_mutex &VMMutex()
{
	// Heap allocated: field access happens during VM start-up and shut-down,
	// including from static destructors.
	static std::recursive_mutex *mutex = new std::recursive_mutex();
	return *mutex;
}

VMLock::VMLock()
{
	Scheduler::EnterVMLockWait();
	VMMutex().lock();
	t_vm_lock_depth++;
	Scheduler::ExitVMLockWait();
}

VMLock::~VMLock()
{
	t_vm_lock_depth--;
	VMMutex().unlock();
}
