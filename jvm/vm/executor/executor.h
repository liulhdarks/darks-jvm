#pragma once

#include "threadstack.h"
#include "jclass.h"
#include "vm/exceptions.h"

class JVM_API VMExecutor
{

private:
	lpThreadStack stack;

public:
	VMExecutor();

	~VMExecutor();

	void Initialize();

	void Execute(LPCSTR class_full_name);

	void Execute(lpClass clazz);

	void Execute(lpClass clazz, lpMethod method);

	void Execute(lpClass clazz, lpMethod method, lpVariant vars, int count);

};

typedef VMExecutor *lpVMExecutor;