#include "stdafx.h"
#include "nativelibs.h"
#include "utils/DebugUtils.h"
#include "jni/jni_impl.h"

#include <vector>

NativeLibrary::NativeLibrary()
{
	handle = NULL;
}

NativeLibrary::NativeLibrary(string &libs_name)
{
	handle = NULL;
	this->libs_name = libs_name;
}

NativeLibrary::~NativeLibrary()
{
	if (handle)
	{
		DarksVM_UnloadJniLibrary(handle);
		// The OS handle is intentionally left mapped: dlclose() at process exit
		// buys nothing, and unmapping a library while its static destructors /
		// exit handlers may still run is unsafe.  JNI_OnUnload above performs
		// the library's own cleanup.
	}
}

NativeLibraryManager::NativeLibraryManager()
{

}

NativeLibraryManager::~NativeLibraryManager()
{
	map<string, lpNativeLibrary>::iterator it = libs_map.begin();
	for (;it != libs_map.end(); it++)
	{
		lpNativeLibrary lib = it->second;
		if (lib)
		{
			delete lib;
			lib = NULL;
			it->second = NULL;
		}
	}
	libs_map.clear();
	func_map.clear();
}


bool NativeLibraryManager::LoadNativeLibrary(LPCSTR libs_name)
{
	if (!libs_name || !*libs_name)
	{
		return false;
	}
	string str_libs = string(libs_name);
	if (libs_map.find(str_libs) != libs_map.end())
	{
		return true;
	}

	// The canonical name is platform specific: lib<name>.so on Linux,
	// lib<name>.dylib on macOS and <name>.dll on Windows.  The remaining
	// spellings are tried as well so that a library shipped with another
	// platform suffix (a jar bundling libfoo.so, for instance) can still be
	// opened when the OS loader accepts it.
	std::vector<string> candidates;
	char *mapped_name = MapLibraryName(libs_name);
	if (mapped_name)
	{
		candidates.push_back(string(mapped_name));
		delete[] mapped_name;
	}
	static const char *kSuffixes[] = { ".so", ".dylib", ".dll" };
	for (size_t i = 0; i < sizeof(kSuffixes) / sizeof(kSuffixes[0]); i++)
	{
		string candidate = string("lib") + libs_name + kSuffixes[i];
		bool seen = false;
		for (size_t j = 0; j < candidates.size(); j++)
		{
			if (candidates[j] == candidate)
			{
				seen = true;
				break;
			}
		}
		if (!seen)
		{
			candidates.push_back(candidate);
		}
	}

	for (size_t i = 0; i < candidates.size(); i++)
	{
		void *handle = OS::LoadNativeLibrary(candidates[i].c_str());
		if (!handle)
		{
			continue;
		}
		lpNativeLibrary native_lib = new NativeLibrary(str_libs);
		native_lib->handle = handle;
		DebugUtils::print(2, "Load native library ", candidates[i].c_str());
		libs_map[str_libs] = native_lib;
		return true;
	}
	return false;
}

bool NativeLibraryManager::LoadNativeLibraryPath(LPCSTR path)
{
	if (!path || !*path)
	{
		return false;
	}
	string str_libs = string(path);
	if (libs_map.find(str_libs) != libs_map.end())
	{
		return true;
	}
	lpNativeLibrary native_lib = new NativeLibrary(str_libs);
	native_lib->handle = OS::LoadNativeLibrary(path);
	if (native_lib->handle)
	{
		DebugUtils::print(2, "Load native library ", path);
		libs_map[str_libs] = native_lib;
		return true;
	}
	delete native_lib;
	return false;
}

void *NativeLibraryManager::GetLibraryHandle(LPCSTR libs_name)
{
	if (!libs_name)
	{
		return NULL;
	}
	map<string, lpNativeLibrary>::iterator it = libs_map.find(string(libs_name));
	if (it == libs_map.end() || !it->second)
	{
		return NULL;
	}
	return it->second->handle;
}

LPSTR NativeLibraryManager::MapLibraryName(LPCSTR libs_name)
{
	return OS::MapLibraryName(libs_name);
}

bool NativeLibraryManager::GetNativeFunction(LPCSTR func_name, NATIVE_FUNC& native_func)
{
	string str_func = string(func_name);
	if (func_map.find(str_func) != func_map.end())
	{
		native_func = func_map[str_func];
		return true;
	}
	map<string, lpNativeLibrary>::iterator it = libs_map.begin();
	for (;it != libs_map.end(); it++)
	{
		lpNativeLibrary lib = it->second;
		void* lpfunc = OS::GetNativeFunction(lib->handle, func_name);
		if (lpfunc)
		{
			native_func = (NATIVE_FUNC)lpfunc;
			func_map[str_func] = native_func;
			return true;
		}
	}
	return false;
}

