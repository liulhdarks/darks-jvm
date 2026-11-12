#include "stdafx.h"
#include "nativelibs.h"
#include "utils/DebugUtils.h"

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
		OS::ReleaseNativeLibrary(handle);
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

