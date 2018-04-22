#pragma once

#include "vm/types.h"
#include "vm/classfiles/constant.h"
#include "vm/executor/attributes.h"
#include "jclass.h"

class JVM_API Method : public CommonAttributes
{
public:
	U2 accFlag;
	lpUtf8Constant methodName;
	lpUtf8Constant methodDesc;
	U2 attrCount;

	lpCodeAttribute code_attr;
	lpException exception_attr;
	LPSTR fullName;
	lpClass clazz;
	LPSTR jni_method_name;
public:
	Method();

	~Method();

	bool isNative()
	{
		return (accFlag & ACM_NATIVE) != 0;
	}

	bool isStatic()
	{
		return (accFlag & ACM_STATIC) != 0;
	}

	bool isSpecial()
	{
		if (strcmp(methodName->GetName(), "<init>") == 0)
		{
			return true;
		}
		return false;
	}

	bool isFinal()
	{
		return (accFlag & ACM_FINAL) != 0;
	}

	LPSTR GetJniName(int bytesize);
};

typedef Method *lpMethod;