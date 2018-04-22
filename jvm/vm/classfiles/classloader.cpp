#include "stdafx.h"
#include "classloader.h"
#include "vm/exceptions.h"
#include "env/env.h"
#include "utils/CommonUtils.h"
#include "utils/FileUtils.h"
#include "parseclass.h"

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
	else
	{
		LPBYTE bytes = NULL;
		int len = 0;
		LoadClass(classname, bytes, len);
		if (!bytes)
		{
			throw VMException("Fail to load class bytes");
		}
		if (DefineClass(bytes, len, jclass))
		{
			jclass->classloader = this;
			return jclass->initialize(load_super);
		}
		else
		{
			return false;
		}
	}

}

bool ClassLoader::FindBootClass(LPCSTR classname, lpClass &jclass)
{
	return false;
}

void ClassLoader::LoadClass(LPCSTR classname, LPBYTE &bytes, int &length)
{
	string clazz(classname);
	if (clazz.find('.') != string::npos)
	{
		CommonUtils::ReplaceStr(clazz, ".", "/");
	}
	clazz.append(".class");
	char path[256] = {0};
	vector<string> classpath;
	Environment::GetClassPath(&classpath);
	if (classpath.empty())
	{
		throw VMException("Class path is empty.");
	}
	int len = classpath.size();
	for (int i = 0; i < len; i++)
	{
		memset(path, 0, sizeof(path));
		string cp = classpath[i];
		sprintf(path, "%s%s", cp.c_str(), clazz.c_str());
		if (access(path, 0) == 0)
		{
			FileUtils::ReadFile(path, bytes, length);
			break;
		}
	}
}

bool ClassLoader::DefineClass(LPBYTE bytes, int length, lpClass &jclass)
{
	bool result = parse_class_bytes(bytes, length, jclass);
	classMap[string(jclass->thisClass->GetName())] = jclass;
	return result;
}

bool ClassLoader::DefineArrayClass(LPCSTR classname, lpClass &jclass)
{
	jclass = new Class(CT_ARRAY);
	if (!jclass)
	{
		return false;
	}
	int len = strlen(classname);
	jclass->thisClass = new ClassConstant();
	jclass->thisClass->name = new CHAR[len + 1];
	strcpy(jclass->thisClass->name, classname);
	jclass->thisClass->name[len] = '\0';
	classMap[string(jclass->thisClass->GetName())] = jclass;
	return true;
}

void ClassLoader::StoreClass(lpClass &jclass)
{
	classMap[string(jclass->thisClass->GetName())] = jclass;
}