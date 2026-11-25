#pragma once

#include "common.h"

#if defined(_WIN32)
#define OS_API __declspec(dllexport)
#else
#define OS_API __attribute__((visibility("default")))
#endif

// Thin OS portability facade used by the VM and the native library.
class OS_API OS
{

public:

	static LONG64 GetCurrentTimeMillis();

	static void *LoadNativeLibrary(const char *libs_name);

	// OS loader message for the most recent failed LoadNativeLibrary call
	// (empty when the last call succeeded or the loader gave no reason).
	static const std::string &LastLoadError();

	static void ReleaseNativeLibrary(void *handle);

	static char *MapLibraryName(const char *libs_name);

	static void *GetNativeFunction(void *handle, const char *func_name);
};
