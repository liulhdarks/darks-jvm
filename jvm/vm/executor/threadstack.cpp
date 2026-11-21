#include "stdafx.h"
#include "threadstack.h"
#include "exec_common.h"
#include "objectheap.h"
#include "scheduler.h"

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

bool ThreadStack::HoldsMonitor() const
{
	std::stack<lpStackFrame> copy = frames;
	while (!copy.empty())
	{
		lpStackFrame frame = copy.top();
		copy.pop();
		if (frame && !frame->monitors_held.empty())
		{
			return true;
		}
	}
	return false;
}

bool ThreadStack::DescribeTopFrame(std::string &out) const
{
	out.clear();
	if (frames.empty())
	{
		return false;
	}
	lpStackFrame frame = frames.top();
	if (!frame)
	{
		return false;
	}
	lpMethod method = frame->GetMethod();
	const char *class_name = (frame->GetDeclaringClass() && frame->GetDeclaringClass()->thisClass)
		? frame->GetDeclaringClass()->thisClass->GetName() : NULL;
	const char *method_name = (method && method->methodName) ? method->methodName->GetName() : NULL;
	if (class_name)
	{
		out += class_name;
		if (method_name)
		{
			out += '.';
		}
	}
	if (method_name)
	{
		out += method_name;
		if (method->methodDesc && method->methodDesc->GetName())
		{
			out += method->methodDesc->GetName();
		}
	}
	if (out.empty())
	{
		out = "<unknown frame>";
	}
	return true;
}

void ThreadStack::CollectHeldMonitors(
	std::vector<std::pair<LPOBJECT_REF, lpClass> > &out) const
{
	out.clear();
	// std::stack iterates innermost first; walk a copy into a vector so the
	// report lists monitors in acquisition order.
	std::vector<lpStackFrame> ordered;
	std::stack<lpStackFrame> copy = frames;
	while (!copy.empty())
	{
		ordered.push_back(copy.top());
		copy.pop();
	}
	for (size_t i = ordered.size(); i > 0; i--)
	{
		lpStackFrame frame = ordered[i - 1];
		if (!frame)
		{
			continue;
		}
		for (size_t m = 0; m < frame->monitors_held.size(); m++)
		{
			out.push_back(frame->monitors_held[m]);
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
	yield_pending = false;
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
	yield_pending = false;
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
			Scheduler::Instance().ReleaseFrameMonitors(top_frame);
			delete top_frame;
			frames.pop();
		}
		else if (ev == EN_KEEP)
		{
			continue;
		}
		else if (ev == EN_YIELD || ev == EN_SAFEPOINT)
		{
			// Suspend without touching the Java frame stack: the coroutine is
			// resumed by calling start() again.
			return ev;
		}
		else if (ev == EN_RETURN)
		{
			Scheduler::Instance().ReleaseFrameMonitors(top_frame);
			delete top_frame;
			frames.pop();
			return EN_RETURN;
		}
		else if (ev == EN_EXCEPTION)
		{
			Scheduler::Instance().ReleaseFrameMonitors(top_frame);
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
				Scheduler::Instance().ReleaseFrameMonitors(caller);
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
