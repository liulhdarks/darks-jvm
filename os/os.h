#pragma once


#define OS_API __declspec(dllexport)

class OS_API OS
{

public:

	static __int64 GetCurrentTimeMillis();

	static long LoadNativeLibrary(const char* libs_name);

	static void ReleaseNativeLibrary(long handle);

	static char* MapLibraryName(const char* libs_name);

	static void* GetNativeFunction(long handle, const char* func_name);
};