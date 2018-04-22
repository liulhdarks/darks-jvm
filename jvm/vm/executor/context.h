#pragma once


#include "vm/types.h"
#include "objectheap.h"
#include "vm/classfiles/classloader.h"
#include "nativelibs.h"

class JVM_API VMContext
{
public:
	static lpObjectHeap object_heap;
	static lpClassLoader bootstrap_classloader;
	static lpNativeLibraryManager native_libs;
public:
	static bool initialize();

	static bool destroy();

private:
	VMContext(){}


};