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

// Locates an arbitrary class-path resource (Class.getResourceAsStream and
// ClassLoader.getResourceAsStream).  Unlike class bytes an empty resource is
// a valid result, so a successful read may leave "out" empty.
JVM_API bool DarksVM_FindResourceBytes(const std::string &name,
                                       std::vector<uint8_t> &out,
                                       std::string *source);

// Enumerates every class-path occurrence of a resource as a URL string.
// Directory entries produce "file:<path>"; archive entries produce
// "jar:file:<archive>!/<name>".  Used by ClassLoader.getResources and
// ServiceLoader; the URL layer owns the jar protocol handler.
JVM_API bool DarksVM_FindResourceUrls(const std::string &name,
                                      std::vector<std::string> &urls);

// Associates a VM loader created for a Java java.lang.ClassLoader with the
// corresponding Java object (held as a global reference by the caller).  When
// a class cannot be found on the VM class path, resolution is delegated to
// that Java loader so classes defined by custom loaders (for example Spring
// Boot's nested-jar LaunchedURLClassLoader) can be linked from constant-pool
// references.
JVM_API void DarksVM_RegisterJavaClassLoader(ClassLoader *loader,
                                            LPOBJECT_REF java_loader);

// Every VM class loader created in this process.  Constant pools of classes
// defined by custom (Java) loaders also cache java/lang/String objects for
// string literals, so the collector has to trace all of them.
JVM_API const std::vector<ClassLoader *> &DarksVM_AllClassLoaders();

// Finds or defines a class through the registered Java class loaders without
// running its static initializer.  Reflection uses this for lookups such as
// Class.getComponentType() that must not trigger <clinit>.
JVM_API bool DarksVM_LoadClassWithoutInitializing(const std::string &name, lpClass &out);

class JVM_API ClassLoader
{

public:
	// Loaded classes are permanent GC roots: their constant pools may cache
	// java/lang/String objects created for string literals.
	const std::map<string, lpClass> &GetClasses() const
	{
		return classMap;
	}

	// Looks up an already loaded class without triggering a load (used by
	// type checks that must not allocate).
	bool GetLoadedClass(LPCSTR classname, lpClass &jclass)
	{
		return GetClass(classname, jclass);
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

	// Loads (and links) the named class.  initialize mirrors
	// java.lang.Class.forName: false stops after superclass and superinterface
	// resolution, true additionally runs <clinit> on first active use.
	bool FindClass(LPCSTR classname, lpClass &jclass, bool initialize = true);

	bool FindBootClass(LPCSTR classname, lpClass &jclass);

	void LoadClass(LPCSTR classname, LPBYTE &bytes, int &length);

	bool DefineClass(LPBYTE bytes, int length, lpClass &jclass);

	bool DefineArrayClass(LPCSTR classname, lpClass &jclass);

	void StoreClass(lpClass &jclass);

	// Resolves the superclass and every superinterface without running
	// <clinit>.  Throws ClassLinkageException when a referenced type is missing.
	void LinkClass(lpClass clazz);

	// Removes a freshly defined class that failed linking: its method index
	// entries are dropped and the Class object (with its methods and constant
	// pool) is destroyed so a later load starts from clean bytes.
	void DiscardClass(lpClass clazz);
};
