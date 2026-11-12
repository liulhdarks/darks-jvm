#include "stdafx.h"
#include "vmmutex.h"
#include "scheduler.h"

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
	Scheduler::ExitVMLockWait();
}

VMLock::~VMLock()
{
	VMMutex().unlock();
}
