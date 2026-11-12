#include "stdafx.h"
#include "classloader.h"
#include "vm/exceptions.h"
#include "env/env.h"
#include "utils/CommonUtils.h"
#include "utils/FileUtils.h"
#include "parseclass.h"
#include "platform/platform.h"
#include "vm/io/zipreader.h"
#include "utils/DebugUtils.h"

#include <map>

namespace {

// Cache of opened archives. Class path entries are few and archives stay open
// for the process lifetime.  The map itself is heap allocated so that the
// cached ZipFile objects remain reachable at exit; otherwise the static
// destructor would run and LeakSanitizer would report them as leaks.
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

ClassLoader::ClassLoader()
{
	parent = NULL;
}

ClassLoader::ClassLoader(ClassLoader *parent)
{
	this->parent = parent;
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
}

bool ClassLoader::GetClass(LPCSTR classname, lpClass &jclass)
{
	string s(classname);
	map<string, lpClass>::iterator it = classMap.find(s);
	if (it != classMap.end())
	{
		jclass = it->second;
		return true;
	}
	return false;
}

bool ClassLoader::FindClass(LPCSTR classname, lpClass &jclass, bool load_super)
{
	if (!classname)
	{
		throw new VMException((LPSTR)"Class name is null.");
	}
	if (GetClass(classname, jclass))
	{
		return true;
	}

	if (parent)
	{
		if (parent->FindClass(classname, jclass))
		{
			return true;
		}
	}

	if (FindBootClass(classname, jclass))
	{
		return true;
	}

	if (classname[0] == '[')
	{
		return DefineArrayClass(classname, jclass);
	}

	LPBYTE bytes = NULL;
	int len = 0;
	LoadClass(classname, bytes, len);
	if (!bytes)
	{
		DebugUtils::trace("class bytes not found for %s\n", classname);
		throw new VMException((LPSTR)"Fail to load class bytes");
	}
	if (DefineClass(bytes, len, jclass))
	{
		jclass->classloader = this;
		return jclass->initialize(load_super);
	}
	return false;
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
		classMap[string(jclass->thisClass->GetName())] = jclass;
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
	classMap[string(jclass->thisClass->GetName())] = jclass;
	return true;
}

void ClassLoader::StoreClass(lpClass &jclass)
{
	classMap[string(jclass->thisClass->GetName())] = jclass;
}
