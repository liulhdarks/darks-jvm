#pragma once

#include "vm/types.h"
#include "vm/executor/jclass.h"

#include <string>
#include <vector>

// Locates class bytes on the configured class path. Directory entries are
// searched directly; file entries (jars/zips) are opened as archives. The
// archive handles are cached so repeated lookups do not re-read the file.
// "source" receives a human readable location such as "dir/Foo.class" or
// "/path/lib.jar!/pkg/Foo.class".
JVM_API bool DarksVM_FindClassBytes(const std::string &class_name,
                                    std::vector<uint8_t> &out,
                                    std::string *source);

class JVM_API ClassLoader
{

public:
	// Loaded classes are permanent GC roots: their constant pools may cache
	// java/lang/String objects created for string literals.
	const std::map<string, lpClass> &GetClasses() const
	{
		return classMap;
	}

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
