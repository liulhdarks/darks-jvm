#pragma once

#include "common.h"
#include "jni/jni.h"

typedef LPVOID (JNICALL *NATIVE_FUNC)(...);
typedef long LIB_HANDLE;

class NativeLibrary
{

public:
	LIB_HANDLE handle;
	string libs_name;

public:

	NativeLibrary();

	NativeLibrary(string &libs_name);

	~NativeLibrary();
};

typedef NativeLibrary *lpNativeLibrary;

class NativeLibraryManager
{
private:
	map<string, lpNativeLibrary> libs_map;
	map<string, NATIVE_FUNC> func_map;

public:

	NativeLibraryManager();

	~NativeLibraryManager();

	bool LoadNativeLibrary(LPCSTR libs_name);

	LPSTR MapLibraryName(LPCSTR libs_name);

	bool GetNativeFunction(LPCSTR func_name, NATIVE_FUNC& native_func);
};

typedef NativeLibraryManager *lpNativeLibraryManager;