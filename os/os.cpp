// Portable OS layer (Linux / macOS). The MSVC implementation lives in
// os/win32/os_win32.cpp and is selected on Windows.
#include "stdafx.h"
#include "os.h"

#include "../platform/platform.h"

#if !defined(_WIN32)
#include <dlfcn.h>

LONG64 OS::GetCurrentTimeMillis()
{
	return static_cast<LONG64>(dk::CurrentTimeMillis());
}

void *OS::LoadNativeLibrary(const char *libs_name)
{
	if (!libs_name)
	{
		return NULL;
	}
	// The caller already mapped the base name (for example "native" ->
	// "libnative.so"), so do not map it a second time here.
	std::string mapped(libs_name);

	// Search order:
	//   1. next to the executable
	//   2. <exe dir>/../lib (macOS uses an @loader_path/../lib rpath as well)
	//   3. the bare mapped name (system loader search path)
	std::string exe = dk::ExecutablePath();
	std::string candidates[3];
	candidates[0] = dk::JoinPath(dk::DirectoryName(exe), mapped);
	candidates[1] = dk::JoinPath(dk::JoinPath(dk::DirectoryName(exe), ".."), "lib/" + mapped);
	candidates[2] = mapped;

	const char *env_path = getenv("DARKS_JVM_NATIVE_PATH");
	void *handle = NULL;
	if (env_path && *env_path)
	{
		std::string full = dk::JoinPath(env_path, mapped);
		handle = dk::OpenLibrary(full);
	}
	for (int i = 0; !handle && i < 3; i++)
	{
		handle = dk::OpenLibrary(candidates[i]);
	}
	if (!handle)
	{
		const char *err = dlerror();
		if (err)
		{
			dk::DebugPrint((std::string("dlopen failed: ") + err + "\n").c_str());
		}
	}
	return handle;
}

void OS::ReleaseNativeLibrary(void *handle)
{
	dk::CloseLibrary(handle);
}

char *OS::MapLibraryName(const char *libs_name)
{
	if (!libs_name)
	{
		return NULL;
	}
	std::string mapped = dk::MapLibraryName(std::string(libs_name));
	char *real_name = new char[mapped.size() + 1];
	memcpy(real_name, mapped.c_str(), mapped.size() + 1);
	return real_name;
}

void *OS::GetNativeFunction(void *handle, const char *func_name)
{
	return dk::LookupSymbol(handle, func_name);
}

#endif // !_WIN32
