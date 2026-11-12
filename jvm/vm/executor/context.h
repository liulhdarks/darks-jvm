#pragma once


#include "vm/types.h"
#include "objectheap.h"
#include "vm/classfiles/classloader.h"
#include "nativelibs.h"
#include <vector>

class JVM_API VMContext
{
public:
	static lpObjectHeap object_heap;
	static lpClassLoader bootstrap_classloader;
	static lpNativeLibraryManager native_libs;
	// Pending Java exception (jthrowable as LPOBJECT_REF). Registered as a GC
	// root in VMContext::initialize so it survives allocations during unwinding.
	static LPOBJECT_REF pending_exception;
	// Every live JNIEnv (one per ThreadStack). ObjectHeap asks each of them
	// to mark its local-reference frames during a collection.
	static std::vector<void*> jni_envs;
public:
	static bool initialize();

	static bool destroy();

private:
	VMContext(){}


};