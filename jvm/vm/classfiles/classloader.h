#pragma once

#include "vm/types.h"
#include "vm/executor/jclass.h"

class JVM_API ClassLoader
{

private:
	ClassLoader *parent;

	map<string, lpClass> classMap;

private:
	bool GetClass(LPCSTR classname, lpClass &jclass);

public:
	ClassLoader();

	ClassLoader(ClassLoader *parent);

	~ClassLoader();

	bool FindClass(LPCSTR classname, lpClass &jclass, bool load_super = true);

	bool FindBootClass(LPCSTR classname, lpClass &jclass);

	void LoadClass(LPCSTR classname, LPBYTE &bytes, int &length);

	bool DefineClass(LPBYTE bytes, int length, lpClass &jclass);

	bool DefineArrayClass(LPCSTR classname, lpClass &jclass);

	void StoreClass(lpClass &jclass);
};
