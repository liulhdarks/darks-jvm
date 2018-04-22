#include "stdafx.h"
#include "jmethod.h"
#include "descriptor.h"

Method::Method()
{
	methodName = NULL;
	methodDesc = NULL;
	code_attr = NULL;
	exception_attr = NULL;
	jni_method_name = NULL;
}

Method::~Method()
{
	if (code_attr)
	{
		delete[] code_attr;
		code_attr = NULL;
	}
	if (exception_attr)
	{
		delete[] exception_attr;
		exception_attr = NULL;
	}
	if (jni_method_name)
	{
		delete[] jni_method_name;
		jni_method_name = NULL;
	}
	methodName = NULL;
	methodDesc = NULL;
}

LPSTR Method::GetJniName(int bytesize)
{
	if (!jni_method_name)
	{
		int mlen = strlen(methodName->GetName());
		int clen = strlen(clazz->thisClass->GetName());
		jni_method_name = new char[mlen + clen + 20];
		sprintf(jni_method_name, "_Java_%s_%s@%d", clazz->thisClass->GetName(), methodName->GetName(), bytesize);
		int len = strlen(jni_method_name);
		for (int i = 0; i < len; i++)
		{
			if (jni_method_name[i] == '/' || jni_method_name[i] == '.')
			{
				jni_method_name[i] = '_';
			}
		}
	}
	return jni_method_name;
}