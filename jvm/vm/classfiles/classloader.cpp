#include "stdafx.h"
#include "classloader.h"
#include "vm/executor/vmmutex.h"
#include "vm/executor/methodarea.h"
#include "vm/exceptions.h"
#include "env/env.h"
#include "utils/CommonUtils.h"
#include "utils/FileUtils.h"
#include "parseclass.h"
#include "platform/platform.h"
#include "vm/io/zipreader.h"
#include "utils/DebugUtils.h"
#include "vm/executor/scheduler.h"
#include "vm/executor/threadstack.h"
#include "vm/executor/javaexception.h"
#include "jni/jni_impl.h"

#include <map>
#include <cstdio>
#if defined(__linux__)
#endif

namespace {

// Cache of opened archives. Class path entries are few and archives stay open
// for the process lifetime.  The map itself is heap allocated so that the
// cached ZipFile objects remain reachable at exit; otherwise the static
// destructor would run and LeakSanitizer would report them as leaks.
std::map<ClassLoader*, LPOBJECT_REF> &JavaLoaders()
{
	static std::map<ClassLoader*, LPOBJECT_REF> *loaders =
		new std::map<ClassLoader*, LPOBJECT_REF>();
	return *loaders;
}

std::map<std::string, ZipFile*> &ArchiveCache()
{
	static std::map<std::string, ZipFile*> *cache = new std::map<std::string, ZipFile*>();
	return *cache;
}

ZipFile *OpenArchive(const std::string &path)
{
	std::map<std::string, ZipFile*>::iterator it = ArchiveCache().find(path);
	if (it != ArchiveCache().end())
	{
		return it->second->IsOpen() ? it->second : NULL;
	}
	ZipFile *zip = new ZipFile();
	if (!zip->Open(path))
	{
		DebugUtils::trace("jar: cannot open %s: %s\n", path.c_str(), zip->LastError().c_str());
		delete zip;
		ArchiveCache()[path] = NULL;
		return NULL;
	}
	ArchiveCache()[path] = zip;
	return zip;
}

bool LooksLikeArchive(const std::string &path)
{
	size_t n = path.size();
	if (n >= 4)
	{
		std::string ext = path.substr(n - 4);
		for (size_t i = 0; i < ext.size(); i++)
		{
			if (ext[i] >= 'A' && ext[i] <= 'Z') ext[i] = static_cast<char>(ext[i] - 'A' + 'a');
		}
		if (ext == ".jar" || ext == ".zip") return true;
	}
	return false;
}

std::string ClassRelativePath(const std::string &class_name)
{
	std::string path = class_name;
	for (size_t i = 0; i < path.size(); i++)
	{
		if (path[i] == '.') path[i] = '/';
	}
	path += ".class";
	return path;
}

} // namespace

JVM_API bool DarksVM_FindClassBytes(const std::string &class_name,
                                    std::vector<uint8_t> &out,
                                    std::string *source)
{
	out.clear();
	std::vector<std::string> classpath;
	Environment::GetClassPath(&classpath);
	if (classpath.empty())
	{
		classpath.push_back(".");
	}
	std::string relative = ClassRelativePath(class_name);
	for (size_t i = 0; i < classpath.size(); i++)
	{
		const std::string &entry = classpath[i];
		if (dk::IsDirectory(entry))
		{
			std::string candidate = dk::JoinPath(entry, relative);
			if (dk::FileExists(candidate) && dk::ReadFileBytes(candidate, out) && !out.empty())
			{
				if (source) *source = candidate;
				return true;
			}
			continue;
		}
		if (!dk::FileExists(entry))
		{
			continue;
		}
		// A file class path entry is treated as an archive. Only report the
		// archive error for .jar/.zip names to keep unrelated paths quiet.
		ZipFile *zip = OpenArchive(entry);
		if (!zip)
		{
			continue;
		}
		if (zip->ReadEntry(relative, out) && !out.empty())
		{
			if (source) *source = entry + "!/" + relative;
			return true;
		}
		if (LooksLikeArchive(entry))
		{
			// Continue: the class may still live in a later entry.
			continue;
		}
	}
	return false;
}

JVM_API bool DarksVM_FindResourceBytes(const std::string &resource_name,
                                       std::vector<uint8_t> &out,
                                       std::string *source)
{
	out.clear();
	std::string name = resource_name;
	while (!name.empty() && name[0] == '/')
	{
		name.erase(name.begin());
	}
	if (name.empty())
	{
		return false;
	}
	std::vector<std::string> classpath;
	Environment::GetClassPath(&classpath);
	if (classpath.empty())
	{
		classpath.push_back(".");
	}
	for (size_t i = 0; i < classpath.size(); i++)
	{
		const std::string &entry = classpath[i];
		if (dk::IsDirectory(entry))
		{
			std::string candidate = dk::JoinPath(entry, name);
			if (dk::FileExists(candidate) && dk::ReadFileBytes(candidate, out))
			{
				if (source) *source = candidate;
				return true;
			}
			continue;
		}
		if (!dk::FileExists(entry))
		{
			continue;
		}
		ZipFile *zip = OpenArchive(entry);
		if (!zip)
		{
			continue;
		}
		if (zip->ReadEntry(name, out))
		{
			if (source) *source = entry + "!/" + name;
			return true;
		}
	}
	return false;
}

namespace {

std::string FileUrlPath(const std::string &path)
{
	std::string result = path;
	for (size_t i = 0; i < result.size(); i++)
	{
		if (result[i] == '\\')
		{
			result[i] = '/';
		}
	}
#if defined(_WIN32)
	// A drive-letter path is absolute, but a file URL keeps the leading slash
	// ("file:/C:/dir") that HotSpot also produces.  FopenUtf8 drops it again
	// before calling the CRT.
	if (result.size() >= 2 && result[1] == ':' &&
	    ((result[0] >= 'A' && result[0] <= 'Z') ||
	     (result[0] >= 'a' && result[0] <= 'z')))
	{
		result.insert(result.begin(), '/');
	}
#endif
	return result;
}

} // namespace

JVM_API bool DarksVM_FindResourceUrls(const std::string &resource_name,
                                      std::vector<std::string> &urls)
{
	urls.clear();
	std::string name = resource_name;
	while (!name.empty() && name[0] == '/')
	{
		name.erase(name.begin());
	}
	if (name.empty())
	{
		return false;
	}
	std::vector<std::string> classpath;
	Environment::GetClassPath(&classpath);
	if (classpath.empty())
	{
		classpath.push_back(".");
	}
	for (size_t i = 0; i < classpath.size(); i++)
	{
		const std::string &entry = classpath[i];
		if (dk::IsDirectory(entry))
		{
			std::string candidate = dk::JoinPath(entry, name);
			if (dk::FileExists(candidate))
			{
				urls.push_back("file:" + FileUrlPath(candidate));
			}
			continue;
		}
		if (!dk::FileExists(entry))
		{
			continue;
		}
		ZipFile *zip = OpenArchive(entry);
		if (zip && zip->HasEntry(name))
		{
			urls.push_back("jar:file:" + FileUrlPath(entry) + "!/" + name);
		}
	}
	return !urls.empty();
}

namespace {

std::vector<ClassLoader *> &AllLoaders()
{
	static std::vector<ClassLoader *> *loaders = new std::vector<ClassLoader *>();
	return *loaders;
}

}

const std::vector<ClassLoader *> &DarksVM_AllClassLoaders()
{
	return AllLoaders();
}

namespace {
bool FindClassThroughJava(ClassLoader *loader, LPCSTR classname, lpClass &out);
}

// Defines (or finds) a class through the registered Java loaders without
// running its static initializer.  Reflection uses this for lookups such as
// Class.getComponentType() that must not trigger <clinit>.
bool DarksVM_LoadClassWithoutInitializing(const std::string &name, lpClass &out)
{
	out = NULL;
	if (name.empty())
	{
		return false;
	}
	// The native bootstrap loader owns the class path.  Asking it first keeps a
	// single Class object per name: a Java loader whose parent is the bootstrap
	// (for example the system loader) would otherwise read the same bytes again
	// and define a second copy, breaking Class identity checks such as Spring's
	// `annotationType() == Annotation.class` in @AliasFor resolution.
	if (VMContext::bootstrap_classloader)
	{
		try
		{
			if (VMContext::bootstrap_classloader->FindClass(name.c_str(), out, false) && out)
			{
				return true;
			}
		}
		catch (...)
		{
			out = NULL;
		}
	}
	for (std::map<ClassLoader*, LPOBJECT_REF>::iterator it = JavaLoaders().begin();
	     it != JavaLoaders().end(); ++it)
	{
		lpClass loaded = NULL;
		if (it->first && it->first->GetLoadedClass(name.c_str(), loaded) && loaded)
		{
			out = loaded;
			return true;
		}
	}
	for (std::map<ClassLoader*, LPOBJECT_REF>::iterator it = JavaLoaders().begin();
	     it != JavaLoaders().end(); ++it)
	{
		lpClass found = NULL;
		if (FindClassThroughJava(it->first, name.c_str(), found) && found)
		{
			out = found;
			return true;
		}
	}
	return false;
}

ClassLoader::ClassLoader()
{
	parent = NULL;
	AllLoaders().push_back(this);
}

ClassLoader::ClassLoader(ClassLoader *parent)
{
	this->parent = parent;
	AllLoaders().push_back(this);
}

ClassLoader::~ClassLoader()
{
	map<string, Class*>::iterator it = classMap.begin();
	for (; it != classMap.end(); it++)
	{
		Class *clazz = it->second;
		if (clazz)
		{
			delete clazz;
			clazz = NULL;
			it->second = NULL;
		}
	}
	classMap.clear();
	if (VMContext::object_heap)
	{
		VMContext::object_heap->InvalidateClassPoolStringRoots();
	}
}

bool ClassLoader::GetClass(LPCSTR classname, lpClass &jclass)
{
	VMLock lk;
	string s(classname);
	map<string, lpClass>::iterator it = classMap.find(s);
	if (it != classMap.end())
	{
		jclass = it->second;
		return true;
	}
	return false;
}

void DarksVM_RegisterJavaClassLoader(ClassLoader *loader, LPOBJECT_REF java_loader)
{
	if (loader)
	{
		JavaLoaders()[loader] = java_loader;
	}
}

namespace {

// Resolves a class through the Java java.lang.ClassLoader associated with this
// VM loader.  Returns false when no association exists or the Java loader
// cannot find the class (a pending ClassNotFoundException is discarded so the
// caller can report the failure through the usual VM error path).
bool FindClassThroughJava(ClassLoader *loader, LPCSTR classname, lpClass &out)
{
	out = NULL;
	std::map<ClassLoader*, LPOBJECT_REF>::iterator it = JavaLoaders().find(loader);
	if (it == JavaLoaders().end() || !it->second)
	{
		return false;
	}
	ThreadStack *ts = Scheduler::CurrentStack();
	if (!ts)
	{
		ts = DarksVM_ActiveThreadStack();
	}
	if (!ts || !ts->jni_env)
	{
		return false;
	}
	JNIEnv *env = ts->jni_env;
	lpClass loader_class = JniFindClass(env, "java/lang/ClassLoader");
	if (!loader_class)
	{
		return false;
	}
	lpMethod method = JniFindMethod(loader_class, "loadClass",
		"(Ljava/lang/String;)Ljava/lang/Class;", false);
	if (!method)
	{
		return false;
	}
	std::string dotted(classname);
	for (size_t i = 0; i < dotted.size(); i++)
	{
		if (dotted[i] == '/')
		{
			dotted[i] = '.';
		}
	}
	jstring name = JniMakeString(env, dotted.c_str());
	if (!name)
	{
		return false;
	}
	jvalue arg;
	memset(&arg, 0, sizeof(arg));
	arg.l = name;
	jvalue result = JniInvokeMethod(env, JniObject(it->second),
		reinterpret_cast<jclass>(loader_class),
		reinterpret_cast<jmethodID>(method), &arg, false, false);
	if (!result.l || VMContext::pending_exception)
	{
		if (getenv("DARKS_JVM_TRACE_DEFINE"))
		{
			fprintf(stderr, "[trace java loadClass] failed for %s result=%p pending=%p\n",
				classname, (void *)result.l, (void *)VMContext::pending_exception);
			if (VMContext::pending_exception)
			{
				PrintUncaughtException(VMContext::pending_exception);
			}
			fflush(stderr);
		}
		VMContext::pending_exception = NULL;
		return false;
	}
	// The Java loadClass call returns a java.lang.Class mirror object, not a
	// raw VM Class pointer: read the vmClass field that JniClassMirror fills.
	jlong vm_class = JniGetLongFieldByName(result.l, "vmClass");
	lpClass found = reinterpret_cast<lpClass>(static_cast<intptr_t>(vm_class));
	if (!found)
	{
		return false;
	}
	out = found;
	return true;
}

} // namespace

bool ClassLoader::FindClass(LPCSTR classname, lpClass &jclass, bool initialize)
{
	// The heap mutex must not be held while <clinit> runs: class initialisation
	// executes Java code, which may suspend at a coroutine blocking point or
	// safepoint.  Holding the lock across such a suspension would deadlock the
	// scheduler, because a collection needs the same mutex to stop the world.
	{
		VMLock lk;
		if (!classname)
		{
			throw new VMException((LPSTR)"Class name is null.");
		}
		jclass = NULL;
		bool found = GetClass(classname, jclass);
		if (!found && parent)
		{
			try
			{
				found = parent->FindClass(classname, jclass, false);
			}
			catch (VMException *error)
			{
				// A parent loader that cannot find the class must not abort the
				// child lookup: custom loaders (URLClassLoader subclasses) fall
				// back to their own sources after parent delegation fails.
				delete error;
				jclass = NULL;
				found = false;
			}
			if (!found)
			{
				jclass = NULL;
			}
		}
		if (!found && FindBootClass(classname, jclass))
		{
			found = true;
		}
		if (!found && classname[0] == '[')
		{
			if (!DefineArrayClass(classname, jclass))
			{
				return false;
			}
			found = true;
		}
		if (!found)
		{
			LPBYTE bytes = NULL;
			int len = 0;
			LoadClass(classname, bytes, len);
			if (!bytes)
			{
				if (FindClassThroughJava(this, classname, jclass))
				{
					found = true;
				}
				else
				{
					DebugUtils::trace("class bytes not found for %s\n", classname);
					throw new MissingClassException(classname);
				}
			}
			else
			{
				if (!DefineClass(bytes, len, jclass))
				{
					return false;
				}
				jclass->classloader = this;
				try
				{
					LinkClass(jclass);
				}
				catch (...)
				{
					DiscardClass(jclass);
					jclass = NULL;
					throw;
				}
				found = true;
			}
		}
	}
	if (!jclass)
	{
		return false;
	}
	if (initialize)
	{
		return jclass->initialize(true);
	}
	return true;
}

void ClassLoader::LinkClass(lpClass clazz)
{
	if (!clazz)
	{
		return;
	}
	if (clazz->superClass && clazz->superClass->GetName())
	{
		const char *name = clazz->superClass->GetName();
		lpClass super = NULL;
		try
		{
			if (!FindClass(name, super, false) || !super)
			{
				throw new ClassLinkageException(name);
			}
		}
		catch (ClassLinkageException *)
		{
			throw;
		}
		catch (MissingClassException *error)
		{
			std::string missing = error->missing;
			delete error;
			throw new ClassLinkageException(missing);
		}
		catch (VMException *)
		{
			throw new ClassLinkageException(name);
		}
		catch (...)
		{
			throw new ClassLinkageException(name);
		}
		clazz->super_class = super;
	}
	// Field resolution walks superinterfaces as well as the superclass
	// chain (JVMS 5.4.3.2), so keep the resolved direct interfaces.
	clazz->interface_classes.clear();
	for (U2 i = 0; i < clazz->interfaceCount; i++)
	{
		lpClassConstant ic = clazz->interfaces ? clazz->interfaces[i] : NULL;
		const char *name = ic ? ic->GetName() : NULL;
		if (!name)
		{
			continue;
		}
		lpClass iface = NULL;
		try
		{
			if (!FindClass(name, iface, false) || !iface)
			{
				throw new ClassLinkageException(name);
			}
		}
		catch (ClassLinkageException *)
		{
			throw;
		}
		catch (MissingClassException *error)
		{
			std::string missing = error->missing;
			delete error;
			throw new ClassLinkageException(missing);
		}
		catch (VMException *)
		{
			throw new ClassLinkageException(name);
		}
		catch (...)
		{
			throw new ClassLinkageException(name);
		}
		clazz->interface_classes.push_back(iface);
	}
}

void ClassLoader::DiscardClass(lpClass clazz)
{
	if (!clazz)
	{
		return;
	}
	MethodArea::RemoveMethodsOf(clazz);
	{
		VMLock lk;
		if (clazz->thisClass && clazz->thisClass->GetName())
		{
			classMap.erase(string(clazz->thisClass->GetName()));
		}
	}
	delete clazz;
	if (VMContext::object_heap)
	{
		// The pool's cached String roots must not outlive the class.
		VMContext::object_heap->InvalidateClassPoolStringRoots();
	}
}

bool ClassLoader::FindBootClass(LPCSTR classname, lpClass &jclass)
{
	return false;
}

void ClassLoader::LoadClass(LPCSTR classname, LPBYTE &bytes, int &length)
{
	bytes = NULL;
	length = 0;
	if (!classname)
	{
		return;
	}
	std::vector<uint8_t> data;
	if (!DarksVM_FindClassBytes(std::string(classname), data, NULL) || data.empty())
	{
		return;
	}
	length = static_cast<int>(data.size());
	// parse_class_bytes takes ownership of the buffer (BytesInputStream
	// deletes it), so hand it a heap copy.
	bytes = new BYTE[data.size()];
	memcpy(bytes, data.data(), data.size());
}

bool ClassLoader::DefineClass(LPBYTE bytes, int length, lpClass &jclass)
{
	bool result = parse_class_bytes(bytes, length, jclass);
	if (result && jclass && jclass->thisClass && jclass->thisClass->GetName())
	{
		AddToClassMap(jclass);
	}
	return result;
}

bool ClassLoader::DefineArrayClass(LPCSTR classname, lpClass &jclass)
{
	jclass = new Class(CT_ARRAY);
	if (!jclass)
	{
		return false;
	}
	jclass->thisClass = new ClassConstant();
	size_t len = strlen(classname);
	jclass->thisClass->name = new CHAR[len + 1];
	memcpy(jclass->thisClass->name, classname, len + 1);
	// The constant pool owns the class-name constant (for parsed classes the
	// equivalent constant lives in the pool as well), so add it there instead
	// of leaking it when the Class is destroyed.
	jclass->const_pool->init(1);
	jclass->const_pool->AddConstant(jclass->thisClass);
	jclass->classloader = this;
	AddToClassMap(jclass);
	return true;
}

void ClassLoader::StoreClass(lpClass &jclass)
{
	VMLock lk;
	AddToClassMap(jclass);
}

void ClassLoader::AddToClassMap(lpClass clazz)
{
	std::pair<map<string, lpClass>::iterator, bool> inserted =
		classMap.insert(std::make_pair(string(clazz->thisClass->GetName()), clazz));
	if (!inserted.second)
	{
		// A redefinition replaces the entry; the replaced Class is not owned
		// here, so the inventory does not change.
		inserted.first->second = clazz;
		return;
	}
	if (VMContext::object_heap)
	{
		VMContext::object_heap->NoteClassPoolLoaded(
			clazz->const_pool ? static_cast<size_t>(clazz->const_pool->size()) : 0);
	}
}
