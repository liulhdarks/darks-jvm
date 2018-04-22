#include "stdafx.h"
#include "context.h"

lpObjectHeap VMContext::object_heap = NULL;
lpClassLoader VMContext::bootstrap_classloader = NULL;
lpNativeLibraryManager VMContext::native_libs = NULL;

bool VMContext::initialize()
{
	object_heap = new ObjectHeap();
	if (!object_heap)
	{
		return false;
	}
	object_heap->initialize(1024);
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