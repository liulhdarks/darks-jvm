#include "stdafx.h"
#include "threadstack.h"
#include "exec_common.h"

#define LOOP_TRUE 1

ThreadStack::~ThreadStack()
{
	lpStackFrame top_frame = NULL;
	while (!frames.empty())
	{
		top_frame = frames.top();
		delete top_frame;
		top_frame = NULL;
		frames.pop();
	}

	if (jni_env)
	{
		delete jni_env;
		jni_env = NULL;
	}
}

void ThreadStack::AddFrame(lpClass clazz, lpMethod method, lpStackFrame &frame)
{
	frame = new StackFrame(this, clazz, method, class_loader);
	frames.push(frame);
	frame->jni_env = jni_env;
}

EventNotify ThreadStack::execute(lpClass clazz, lpMethod method)
{
	lpStackFrame frame = NULL;
	AddFrame(clazz, method, frame);
	return start();
}

EventNotify ThreadStack::execute(lpClass clazz, lpMethod method, lpVariant vars, int count)
{
	lpStackFrame frame = NULL;
	AddFrame(clazz, method, frame);
	frame->init(vars, count);
	return start();
}

EventNotify ThreadStack::start()
{
	while (LOOP_TRUE)
	{
		if (frames.empty())
		{
			return EN_RETURN;
		}
		lpStackFrame top_frame = frames.top();
		if (!top_frame)
		{
			return EN_RETURN;
		}

		EventNotify ev = top_frame->Execute();
		if (ev == EN_POP)
		{
			delete top_frame;
			frames.pop();
		}
		else if (ev == EN_KEEP)
		{
			continue;
		}
		else if (ev == EN_RETURN)
		{
			delete top_frame;
			frames.pop();
			return EN_RETURN;
		}
		else if (ev == EN_EXCEPTION)
		{
			delete top_frame;
			frames.pop();
			//------------------------------
			return EN_EXCEPTION;
		}
	}
}