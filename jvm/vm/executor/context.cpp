#include "stdafx.h"
#include "context.h"
#include "methodarea.h"
#include "scheduler.h"

lpObjectHeap VMContext::object_heap = NULL;
lpClassLoader VMContext::bootstrap_classloader = NULL;
lpNativeLibraryManager VMContext::native_libs = NULL;
thread_local LPOBJECT_REF VMContext::pending_exception = NULL;
std::vector<void*> VMContext::jni_envs;

bool VMContext::initialize()
{
	object_heap = new ObjectHeap();
	if (!object_heap)
	{
		return false;
	}
	object_heap->initialize(1024);
	// The pending exception must stay alive while handlers run and while the
	// uncaught handler prints it.  Each OS thread registers its own TLS slot
	// when it attaches to the scheduler.
	VMContext::pending_exception = NULL;
	Scheduler::Instance().Initialize();
	bootstrap_classloader = new ClassLoader();
	if (!bootstrap_classloader)
	{
		return false;
	}
	native_libs = new NativeLibraryManager();
	if (!native_libs)
	{
		return false;
	}
	native_libs->LoadNativeLibrary("native");
	return true;
}

bool VMContext::destroy()
{
	// Stop coroutine workers before tearing down the heap they reference.
	Scheduler::Instance().Shutdown();
	// Class statics belong to this VM instance; drop them before the heap
	// so the field values do not outlive the objects they point to.
	MethodArea::ClearStaticFields();
	VMContext::pending_exception = NULL;
	if (object_heap)
	{
		delete object_heap;
		object_heap = NULL;
	}
	if (bootstrap_classloader)
	{
		delete bootstrap_classloader;
		bootstrap_classloader = NULL;
	}
	if (native_libs)
	{
		delete native_libs;
		native_libs = NULL;
	}
	return true;
}