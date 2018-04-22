#pragma once

#include "vm/types.h"
#include <map>
#include <string>
#include "jclass.h"
#include "jmethod.h"
using namespace std;

class MethodArea
{

public:
	//class_full_name@method_name:method_params
	static map<string, lpMethod> methods_map;

	static map<string, lpFieldValue> static_field_map;
	
	static void AddMethod(lpClass clazz, lpMethod &method);

	static lpMethod GetMethod(LPSTR method_name);

	static lpMethod GetMethod(lpClass clazz, LPSTR method_name);

	static void AddStaticField(LPSTR field_full_name, lpFieldValue fv);

	static lpFieldValue GetStaticField(LPSTR field_full_name);
};