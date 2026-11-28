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
	// PE files cannot import a thread-local variable from another module, so
	// code outside the VM core (the launcher and native.dll) uses these
	// accessors instead of the member directly.
	static LPOBJECT_REF PendingException();
	static void SetPendingException(LPOBJECT_REF value);
	// Every live JNIEnv (one per ThreadStack). ObjectHeap asks each of them
	// to mark its local-reference frames during a collection.
	static std::vector<void*> jni_envs;
	// -Xms/-Xmx in bytes as parsed by the launcher; 0 keeps JDK ergonomics.
	static size_t requested_initial_heap;
	static size_t requested_max_heap;
	static void SetHeapLimits(size_t initial_bytes, size_t max_bytes);
public:
	static bool initialize();

	static bool destroy();

private:
	VMContext(){}


};