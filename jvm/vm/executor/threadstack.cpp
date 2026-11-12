#include "stdafx.h"
#include "threadstack.h"
#include "exec_common.h"
#include "objectheap.h"

#define LOOP_TRUE 1

ThreadStack::~ThreadStack()
{
	if (VMContext::object_heap)
	{
		VMContext::object_heap->UnregisterThreadStack(this);
	}

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
		DarksVM_DestroyJniEnv(jni_env);
		delete jni_env;
		jni_env = NULL;
	}
}

void ThreadStack::MarkRoots(ObjectHeap *heap)
{
	if (!heap)
	{
		return;
	}
	// std::stack has no iterators; copy the (small) pointer container.
	std::stack<lpStackFrame> copy = frames;
	while (!copy.empty())
	{
		lpStackFrame frame = copy.top();
		copy.pop();
		if (frame)
		{
			frame->MarkRoots(heap);
		}
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
	size_t saved_depth = unwind_stop_depth;
	unwind_stop_depth = frames.size() - 1;
	has_return = false;
	EventNotify result = start();
	unwind_stop_depth = saved_depth;
	return result;
}

EventNotify ThreadStack::execute(lpClass clazz, lpMethod method, lpVariant vars, int count)
{
	lpStackFrame frame = NULL;
	AddFrame(clazz, method, frame);
	frame->init(vars, count);
	size_t saved_depth = unwind_stop_depth;
	unwind_stop_depth = frames.size() - 1;
	has_return = false;
	EventNotify result = start();
	unwind_stop_depth = saved_depth;
	return result;
}

EventNotify ThreadStack::start()
{
	while (LOOP_TRUE)
	{
		if (frames.empty())
		{
			return EN_RETURN;
		}
		// A nested execute() (JNI Call*Method or a native->Java callback)
		// must stop as soon as its own frame has returned; otherwise this
		// loop would resume the suspended caller frame while the native
		// function is still on the C stack.
		if (frames.size() <= unwind_stop_depth)
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
			// Unwind the Java stack looking for an exception-table handler.
			// Frames at or below unwind_stop_depth belong to an outer
			// invocation (for example the native method that called us) and
			// must survive so the pending exception can be observed there.
			bool handled = false;
			while (frames.size() > unwind_stop_depth)
			{
				lpStackFrame caller = frames.top();
				if (caller && caller->HandleException())
				{
					handled = true;
					break;
				}
				delete caller;
				frames.pop();
			}
			if (!handled)
			{
				return EN_EXCEPTION;
			}
		}
	}
}
