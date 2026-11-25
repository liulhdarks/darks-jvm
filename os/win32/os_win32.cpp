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

std::string &LoadErrorStorage()
{
	static std::string *message = new std::string();
	return *message;
}

std::string FormatLastError()
{
	DWORD code = GetLastError();
	if (code == 0)
	{
		return std::string();
	}
	LPWSTR buffer = NULL;
	DWORD length = FormatMessageW(
		FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM |
			FORMAT_MESSAGE_IGNORE_INSERTS,
		NULL, code, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
		reinterpret_cast<LPWSTR>(&buffer), 0, NULL);
	std::string reason;
	if (length > 0 && buffer)
	{
		std::vector<uint16_t> units;
		for (DWORD i = 0; i < length; i++)
		{
			units.push_back(static_cast<uint16_t>(buffer[i]));
		}
		reason = dk::Utf16ToUtf8(units);
		while (!reason.empty() &&
		       (reason[reason.size() - 1] == '\r' || reason[reason.size() - 1] == '\n'))
		{
			reason.erase(reason.size() - 1);
		}
	}
	if (buffer)
	{
		LocalFree(buffer);
	}
	std::string prefix = "error " + std::to_string(static_cast<unsigned>(code));
	return reason.empty() ? prefix : prefix + ": " + reason;
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
	LoadErrorStorage().clear();
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
	LoadErrorStorage() = FormatLastError();
	dk::DebugPrint(("LoadLibrary failed for " + mapped + "\n").c_str());
	return NULL;
}

const std::string &OS::LastLoadError()
{
	return LoadErrorStorage();
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
