#include "stdafx.h"
#include "jmethod.h"
#include "descriptor.h"

#include <string>

Method::Method()
{
	methodName = NULL;
	methodDesc = NULL;
	code_attr = NULL;
	exception_attr = NULL;
	jni_method_name = NULL;
	fullName = NULL;
	clazz = NULL;
	attrCount = 0;
	accFlag = 0;
}

Method::~Method()
{
	if (code_attr)
	{
		// Single object, not an array.
		delete code_attr;
		code_attr = NULL;
	}
	if (exception_attr)
	{
		delete exception_attr;
		exception_attr = NULL;
	}
	if (jni_method_name)
	{
		delete[] jni_method_name;
		jni_method_name = NULL;
	}
	if (fullName)
	{
		delete[] fullName;
		fullName = NULL;
	}
	methodName = NULL;
	methodDesc = NULL;
}

// Builds the standard JNI symbol name (JNI naming convention, no byte-size
// suffix: the original appended "@<bytes>" to support the x86 `push`-based
// calling convention).
LPSTR Method::GetJniName()
{
	if (!jni_method_name)
	{
		if (!clazz || !clazz->thisClass || !methodName)
		{
			return NULL;
		}
		std::string name = clazz->thisClass->GetName();
		for (size_t i = 0; i < name.size(); i++)
		{
			char c = name[i];
			if (c == '/')
			{
				name[i] = '_';
			}
			else if (c == '_')
			{
				name.replace(i, 1, "_1");
				i += 1;
			}
			else if (c == ';')
			{
				name.replace(i, 1, "_2");
				i += 1;
			}
			else if (c == '[')
			{
				name.replace(i, 1, "_3");
				i += 1;
			}
		}

		std::string prefix = "Java_" + name + "_";
		std::string method = methodName->GetName() ? methodName->GetName() : "";
		size_t offset = 0;
		while ((offset = method.find('_', offset)) != std::string::npos)
		{
			method.replace(offset, 1, "_1");
			offset += 2;
		}
		std::string full = prefix + method;
		jni_method_name = new char[full.size() + 1];
		memcpy(jni_method_name, full.c_str(), full.size() + 1);
	}
	return jni_method_name;
}
