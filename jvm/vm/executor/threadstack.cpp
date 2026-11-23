#include "stdafx.h"
#include "threadstack.h"
#include <cstdio>
#include <cstring>
#include <execinfo.h>
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

void ThreadStack::ReleaseMonitors()
{
	std::stack<lpStackFrame> copy = frames;
	while (!copy.empty())
	{
		lpStackFrame frame = copy.top();
		copy.pop();
		if (frame)
		{
			Scheduler::Instance().ReleaseFrameMonitors(frame);
		}
	}
}

lpClassLoader ThreadStack::TopFrameClassLoader() const
{
	if (frames.empty())
	{
		return class_loader;
	}
	lpStackFrame frame = frames.top();
	if (frame && frame->GetClassLoader())
	{
		return frame->GetClassLoader();
	}
	return class_loader;
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

lpClass ThreadStack::ReflectionCallerClass() const
{
	std::stack<lpStackFrame> copy = frames;
	while (!copy.empty())
	{
		lpStackFrame frame = copy.top();
		copy.pop();
		if (!frame)
		{
			continue;
		}
		lpClass clazz = frame->GetDeclaringClass();
		const char *name = (clazz && clazz->thisClass)
			? clazz->thisClass->GetName() : NULL;
		if (!name)
		{
			continue;
		}
		// The reflection implementation and Class.newInstance() forward to
		// the members being checked; the JDK skips those frames when it asks
		// Reflection.getCallerClass() for the member's caller.
		if (strncmp(name, "java/lang/reflect/", 18) == 0 ||
		    strcmp(name, "java/lang/Class") == 0)
		{
			continue;
		}
		return clazz;
	}
	return NULL;
}

void ThreadStack::DescribeJavaFrames(std::vector<FrameInfo> &out) const
{
	out.clear();
	std::stack<lpStackFrame> copy = frames;
	std::vector<lpStackFrame> ordered;
	while (!copy.empty())
	{
		ordered.push_back(copy.top());
		copy.pop();
	}
	for (size_t i = 0; i < ordered.size(); i++)
	{
		lpStackFrame frame = ordered[i];
		if (!frame)
		{
			continue;
		}
		lpMethod method = frame->GetMethod();
		lpClass clazz = frame->GetDeclaringClass();
		if (!method || !method->methodName)
		{
			continue;
		}
		FrameInfo info;
		info.declaring_class = (clazz && clazz->thisClass && clazz->thisClass->GetName())
			? clazz->thisClass->GetName() : "";
		for (size_t c = 0; c < info.declaring_class.size(); c++)
		{
			if (info.declaring_class[c] == '/')
			{
				info.declaring_class[c] = '.';
			}
		}
		info.method_name = method->methodName->GetName();
		info.file_name = (clazz && clazz->source_file && clazz->source_file->source_file)
			? clazz->source_file->source_file->GetName() : "";
		int line = -1;
		if (method->code_attr && method->code_attr->line_number_table &&
		    method->code_attr->line_number_table->line_table_count > 0)
		{
			int pc = frame->GetInstructionPc();
			lpLineNumberTable table = method->code_attr->line_number_table;
			for (U2 n = 0; n < table->line_table_count; n++)
			{
				if (static_cast<int>(table->line_number_infos[n].start_pc) <= pc)
				{
					line = table->line_number_infos[n].line_number;
				}
			}
		}
		else if (method->isNative())
		{
			line = -2;
		}
		info.line_number = line;
		out.push_back(info);
	}
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
	// Resolve symbolic references of the executed method through the loader
	// that defined its class.  Runtime-defined classes (cglib, proxies) only
	// exist in that loader, so the default (bootstrap) loader would fail to
	// find the class itself when its own bytecode refers to it.
	lpClassLoader loader = (clazz && clazz->classloader)
		? clazz->classloader : class_loader;
	frame = new StackFrame(this, clazz, method, loader);
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
	try
	{
		frame->init(vars, count);
	}
	catch (...)
	{
		frames.pop();
		delete frame;
		throw;
	}
	size_t saved_depth = unwind_stop_depth;
	unwind_stop_depth = frames.size() - 1;
	has_return = false;
	yield_pending = false;
	EventNotify result = start();
	unwind_stop_depth = saved_depth;
	return result;
}

EventNotify ThreadStack::executeNested(lpClass clazz, lpMethod method, lpVariant vars, int count)
{
	lpStackFrame frame = NULL;
	AddFrame(clazz, method, frame);
	try
	{
		frame->init(vars, count);
	}
	catch (...)
	{
		frames.pop();
		delete frame;
		throw;
	}
	size_t saved_depth = unwind_stop_depth;
	unwind_stop_depth = frames.size() - 1;
	has_return = false;
	yield_pending = false;
	EventNotify result = start();
	// A blocking primitive suspended the coroutine while the native frame
	// (Method.invoke, JNI Call*Method) is still on the C stack.  Keep the
	// native alive: park, let other coroutines run and resume the invocation
	// so that its return value and exception semantics stay intact.
	Scheduler &sched = Scheduler::Instance();
	while (result == EN_YIELD || result == EN_SAFEPOINT)
	{
		// Other coroutines (including GC) must be able to run while this
		// invocation is parked, so the carrier thread is not "in native"
		// during the wait.
		sched.ExitNative();
		bool progressed = (result == EN_SAFEPOINT)
			? sched.CompleteSafepoint()
			: sched.AwaitCurrentResume(this);
		sched.EnterNative();
		if (!progressed)
		{
			break;
		}
		result = start();
	}
	unwind_stop_depth = saved_depth;
	return result;
}

namespace {

thread_local ThreadStack *t_active_stack = NULL;

struct ActiveStackScope
{
	ThreadStack *saved;
	explicit ActiveStackScope(ThreadStack *ts) : saved(t_active_stack)
	{
		t_active_stack = ts;
	}
	~ActiveStackScope()
	{
		t_active_stack = saved;
	}
};

} // namespace

ThreadStack *DarksVM_ActiveThreadStack()
{
	return t_active_stack;
}

EventNotify ThreadStack::start()
{
	ActiveStackScope active_scope(this);
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
