#include "stdafx.h"
#include "os_win32.h"

__int64 OS::GetCurrentTimeMillis()
{
	__timeb64 t;
	_ftime64(&t);
	__int64 v = t.time;
	__int64 ret = ((v & 0xFFFFFFFF) * 1000) | t.millitm;
	return ret;
}

long OS::LoadNativeLibrary(const char* libs_name)
{
	HMODULE handle = LoadLibrary(libs_name);
	return (long)handle;
}

void OS::ReleaseNativeLibrary(long handle)
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

void* OS::GetNativeFunction(long handle, const char* func_name)
{
	HMODULE mod = (HMODULE) handle;
	if (mod)
	{
		FARPROC func = GetProcAddress(mod, func_name);
		return (void*)func;
	}
	return NULL;
}