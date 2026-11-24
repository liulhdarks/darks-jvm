#if defined(_WIN32)

#include "stdafx.h"
#include "os_win32.h"
#include "../platform/platform.h"

#include <string>
#include <vector>

namespace {

std::wstring WidePath(const std::string &path)
{
	std::vector<uint16_t> units = dk::Utf8ToUtf16(path);
	return std::wstring(units.begin(), units.end());
}

void *TryLoad(const std::string &path)
{
	HMODULE handle = LoadLibraryW(WidePath(path).c_str());
	return reinterpret_cast<void *>(handle);
}

} // namespace

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
	if (!libs_name || !*libs_name)
	{
		return NULL;
	}
	// Same search order as the POSIX implementation: DARKS_JVM_NATIVE_PATH,
	// next to the executable, <exe dir>/../lib, then the loader's own search
	// path.  Absolute-path candidates are tried first so loading does not
	// depend on the current working directory.
	std::string mapped(libs_name);
	std::vector<std::string> candidates;
	const char *env_path = getenv("DARKS_JVM_NATIVE_PATH");
	if (env_path && *env_path)
	{
		candidates.push_back(dk::JoinPath(env_path, mapped));
	}
	std::string exe_dir = dk::DirectoryName(dk::ExecutablePath());
	if (!exe_dir.empty())
	{
		candidates.push_back(dk::JoinPath(exe_dir, mapped));
		candidates.push_back(dk::JoinPath(
			dk::JoinPath(exe_dir, ".."), std::string("lib\\") + mapped));
	}
	candidates.push_back(mapped);
	for (size_t i = 0; i < candidates.size(); i++)
	{
		void *handle = TryLoad(candidates[i]);
		if (handle)
		{
			return handle;
		}
	}
	dk::DebugPrint(("LoadLibrary failed for " + mapped + "\n").c_str());
	return NULL;
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
