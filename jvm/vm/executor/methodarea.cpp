#include "stdafx.h"
#include "methodarea.h"
#include "vmmutex.h"

map<string, lpMethod> MethodArea::methods_map;
map<string, lpFieldValue> MethodArea::static_field_map;

void MethodArea::AddMethod(lpClass clazz, lpMethod &method)
{
	VMLock lk;
	// Method descriptors have no bounded length (ByteBuddy and other code
	// generators emit constructors with long parameter lists), so the key is
	// built without a fixed-size buffer.
	string full_name(clazz->thisClass->GetName());
	full_name += ".";
	full_name += method->methodName->bytes;
	full_name += ":";
	full_name += method->methodDesc->bytes;
	methods_map[full_name] = method;
}

lpMethod MethodArea::GetMethod(LPSTR method_name)
{
	VMLock lk;
	return methods_map[string(method_name)];
}

void MethodArea::AddStaticField(LPSTR field_full_name, lpFieldValue fv)
{
	VMLock lk;
	// putstatic may run many times; release the value it replaces so the
	// static area does not leak one FieldValue per assignment.
	string key(field_full_name);
	map<string, lpFieldValue>::iterator it = static_field_map.find(key);
	if (it != static_field_map.end() && it->second && it->second != fv)
	{
		delete it->second;
	}
	static_field_map[key] = fv;
}

lpFieldValue MethodArea::GetStaticField(LPSTR field_full_name)
{
	VMLock lk;
	return static_field_map[string(field_full_name)];
}

void MethodArea::ClearStaticFields()
{
	VMLock lk;
	map<string, lpFieldValue>::iterator it = static_field_map.begin();
	for (; it != static_field_map.end(); ++it)
	{
		if (it->second)
		{
			delete it->second;
			it->second = NULL;
		}
	}
	static_field_map.clear();
}

lpMethod MethodArea::GetMethod(lpClass clazz, LPSTR method_name)
{
	VMLock lk;
	if (!clazz || !clazz->thisClass || !clazz->thisClass->GetName())
	{
		return NULL;
	}
	string full_name(clazz->thisClass->GetName());
	full_name += ".";
	full_name += method_name;
	return methods_map[full_name];
}

void MethodArea::RemoveMethodsOf(lpClass clazz)
{
	if (!clazz || !clazz->thisClass || !clazz->thisClass->GetName())
	{
		return;
	}
	string prefix(clazz->thisClass->GetName());
	prefix += ".";
	VMLock lk;
	for (map<string, lpMethod>::iterator it = methods_map.begin();
	     it != methods_map.end();)
	{
		bool owned = it->second && it->second->clazz == clazz;
		bool keyed = it->first.compare(0, prefix.size(), prefix) == 0;
		if (owned || keyed)
		{
			methods_map.erase(it++);
		}
		else
		{
			++it;
		}
	}
}
