#pragma once

#include "vm/types.h"
#include "vm/classfiles/constant.h"
#include "vm/executor/attributes.h"
#include "jclass.h"

#include <atomic>

namespace darksjit { struct Program; }

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

	// Lightweight-JIT state.  Kept per method so the invocation hot path does
	// not take a global lock: 0 = never invoked, 1 = a thread is compiling,
	// 2 = program published, 3 = compilation not possible.
	std::atomic<int> jit_state;
	std::atomic<int> jit_hit_count;
	std::atomic<const darksjit::Program *> jit_program;

	// Bounded inlining plan (see StackFrame::TryInline).  inline_kind is 0
	// until analysed; inline_cp holds the getfield/putfield constant index.
	std::atomic<int> inline_kind;
	std::atomic<int> inline_cp;
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

	LPSTR GetJniName();
};

typedef Method *lpMethod;