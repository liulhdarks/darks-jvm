#include "stdafx.h"
#include "methodarea.h"

#define METHOD_NAME_SIZE 1024

map<string, lpMethod> MethodArea::methods_map;
map<string, lpFieldValue> MethodArea::static_field_map;

void MethodArea::AddMethod(lpClass clazz, lpMethod &method)
{
	char method_name[METHOD_NAME_SIZE];
	sprintf(method_name, "%s.%s:%s", clazz->thisClass->GetName(), 
		method->methodName->bytes, 
		method->methodDesc->bytes);
	LPSTR namePtr = method_name;
	methods_map[string(namePtr)] = method;
}

lpMethod MethodArea::GetMethod(LPSTR method_name)
{
	return methods_map[string(method_name)];
}

void MethodArea::AddStaticField(LPSTR field_full_name, lpFieldValue fv)
{
	static_field_map[string(field_full_name)] = fv;
}

lpFieldValue MethodArea::GetStaticField(LPSTR field_full_name)
{
	return static_field_map[string(field_full_name)];
}

lpMethod MethodArea::GetMethod(lpClass clazz, LPSTR method_name)
{
	char method_full_name[METHOD_NAME_SIZE];
	sprintf(method_full_name, "%s.%s", clazz->thisClass->GetName(), method_name);
	LPSTR namePtr = method_full_name;
	return methods_map[string(namePtr)];
}