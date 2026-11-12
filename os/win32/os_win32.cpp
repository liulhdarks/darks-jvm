#if defined(_WIN32)

#include "stdafx.h"
#include "os_win32.h"

LONG64 OS::GetCurrentTimeMillis()
{
	__timeb64 t;
	_ftime64(&t);
	// Milliseconds since the epoch: seconds * 1000 + milliseconds.
	// The original used bitwise OR and truncated to 32 bits.
	LONG64 v = static_cast<LONG64>(t.time);
	LONG64 ret = v * 1000 + static_cast<LONG64>(t.millitm);
	return ret;
}

void* OS::LoadNativeLibrary(const char* libs_name)
{
	HMODULE handle = LoadLibraryA(libs_name);
	return (void*)handle;
}

void OS::ReleaseNativeLibrary(void* handle)
{
	HMODULE mod = (HMODULE) handle;
	if (mod)
	{
		FreeLibrary(mod);
	}
}

char* OS::MapLibraryName(const char* libs_name)
{
	int len = strlen(libs_name) + 5;
	char* real_name = new char[len];
	sprintf(real_name, "%s.dll", libs_name);
	return real_name;
}

void* OS::GetNativeFunction(void* handle, const char* func_name)
{
	HMODULE mod = (HMODULE) handle;
	if (mod)
	{
		FARPROC func = GetProcAddress(mod, func_name);
		return (void*)func;
	}
	return NULL;
}

#endif // _WIN32
