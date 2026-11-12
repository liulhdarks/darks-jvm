#include "stdafx.h"
#include "nativelibs.h"
#include "utils/DebugUtils.h"
#include "jni/jni_impl.h"

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
	string str_libs = string(libs_name);
	if (libs_map.find(str_libs) != libs_map.end())
	{
		return true;
	}
	char *mapped_name = MapLibraryName(libs_name);
	if (!mapped_name)
	{
		return false;
	}
	lpNativeLibrary native_lib = new NativeLibrary(str_libs);
	native_lib->handle = OS::LoadNativeLibrary(mapped_name);
	delete[] mapped_name;
	if (native_lib->handle)
	{
		DebugUtils::print(2, "Load native library ", libs_name);
		libs_map[str_libs] = native_lib;
		return true;
	}
	delete native_lib;
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

