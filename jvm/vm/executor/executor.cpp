#include "stdafx.h"
#include "executor.h"
#include "context.h"

VMExecutor::VMExecutor()
{
	stack = NULL;
}

VMExecutor::~VMExecutor()
{
	if (stack)
	{
		delete stack;
		stack = NULL;
	}
	//VMContext::destroy();
}

void VMExecutor::Initialize()
{
	//VMContext::initialize();
	stack = new ThreadStack();
}

void VMExecutor::Execute(LPCSTR class_full_name)
{
	Class *jclass = NULL;
	stack->class_loader->FindClass(class_full_name, jclass);
	if (jclass)
	{
		Execute(jclass);
	}
}

void VMExecutor::Execute(lpClass clazz)
{
	if (!clazz->mainMethod)
	{
		throw new VMException("Class main method does not exists.");
	}
	stack->execute(clazz, clazz->mainMethod);
}

void VMExecutor::Execute(lpClass clazz, lpMethod method)
{
	if (!method)
	{
		throw new VMException("Class method is invalid.");
	}
	stack->execute(clazz, method);
}

void VMExecutor::Execute(lpClass clazz, lpMethod method, lpVariant vars, int count)
{
	if (!method)
	{
		throw new VMException("Class method is invalid.");
	}
	stack->execute(clazz, method, vars, count);
}