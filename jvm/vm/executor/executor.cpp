#include "stdafx.h"
#include "executor.h"
#include "context.h"
#include "scheduler.h"

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
	// Attaching the coroutine scheduler main coroutine is deliberately left to
	// the embedding entry point (java.cpp).  Nested executors created for
	// class initialisation and string construction also call Initialize(); if
	// they attached here they could become the process main coroutine and be
	// destroyed right afterwards, leaving the scheduler with a dangling stack.
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
	Scheduler::Instance().RunMain(stack, clazz, clazz->mainMethod, NULL, 0);
}

void VMExecutor::Execute(lpClass clazz, lpMethod method)
{
	if (!method)
	{
		throw new VMException("Class method is invalid.");
	}
	Scheduler::Instance().RunMain(stack, clazz, method, NULL, 0);
}

void VMExecutor::Execute(lpClass clazz, lpMethod method, lpVariant vars, int count)
{
	if (!method)
	{
		throw new VMException("Class method is invalid.");
	}
	Scheduler::Instance().RunMain(stack, clazz, method, vars, count);
}