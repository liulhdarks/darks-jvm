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
	// Pending Java exception (jthrowable as LPOBJECT_REF).  Thread-local: each
	// coroutine runs on exactly one OS thread at a time, so concurrent
	// exceptions cannot overwrite each other.  Each worker registers its own
	// TLS slot as a GC root when it starts.
	static thread_local LPOBJECT_REF pending_exception;
	// Every live JNIEnv (one per ThreadStack). ObjectHeap asks each of them
	// to mark its local-reference frames during a collection.
	static std::vector<void*> jni_envs;
public:
	static bool initialize();

	static bool destroy();

private:
	VMContext(){}


};