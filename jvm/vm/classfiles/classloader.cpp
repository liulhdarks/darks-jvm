#include "stdafx.h"
#include "classloader.h"
#include "vm/exceptions.h"
#include "env/env.h"
#include "utils/CommonUtils.h"
#include "utils/FileUtils.h"
#include "parseclass.h"
#include "platform/platform.h"

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
	string clazz(classname);
	for (size_t i = 0; i < clazz.size(); i++)
	{
		if (clazz[i] == '.')
		{
			clazz[i] = '/';
		}
	}
	clazz.append(".class");

	vector<string> classpath;
	Environment::GetClassPath(&classpath);
	// A missing classpath is not fatal by itself; the class simply is not found
	// and the caller reports a class-not-found error (exit code 4).
	for (size_t i = 0; i < classpath.size(); i++)
	{
		string path = dk::JoinPath(classpath[i], clazz);
		if (!dk::FileExists(path))
		{
			continue;
		}
		vector<uint8_t> data;
		if (!dk::ReadFileBytes(path, data))
		{
			continue;
		}
		length = static_cast<int>(data.size());
		// parse_class_bytes takes ownership of the buffer (BytesInputStream
		// deletes it), so hand it a heap copy.
		bytes = new BYTE[data.empty() ? 1 : data.size()];
		if (!data.empty())
		{
			memcpy(bytes, data.data(), data.size());
		}
		return;
	}
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
	classMap[string(jclass->thisClass->GetName())] = jclass;
	return true;
}

void ClassLoader::StoreClass(lpClass &jclass)
{
	classMap[string(jclass->thisClass->GetName())] = jclass;
}
