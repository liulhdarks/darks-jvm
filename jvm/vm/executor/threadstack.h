#pragma once

#include "vm/types.h"
#include <stack>
#include "stackframe.h"
#include "vm/classfiles/classloader.h"
#include "context.h"
#include "jni/jni.h"
using namespace std;

class JVM_API ThreadStack
{

private:
	stack<lpStackFrame> frames;

public:
	lpClassLoader class_loader;
	JNIEnv *jni_env;
	// Result of a top-level invocation (used by JNI Call*Method). Wide values
	// (long/double) use both slots: return_value holds the low 32 bits.
	Slot return_value;
	Slot return_value_hi;
	bool return_wide;
	bool has_return;
	// Exception unwinding never pops frames at or below this depth. Nested
	// invocations (JNI Call*Method) set it so a Java exception propagates back
	// to the calling native frame instead of destroying it.
	size_t unwind_stop_depth;

public:
	ThreadStack()
	{
		class_loader = VMContext::bootstrap_classloader;
		jni_env = new JNIEnv();
		jni_env->functions = DarksVM_GetJniInterface();
		return_wide = false;
		has_return = false;
		unwind_stop_depth = 0;
		return_value.iValue = 0;
		return_value_hi.iValue = 0;
		DarksVM_InitJniEnv(jni_env);
		jni_env->thread_stack = this;
		if (VMContext::object_heap)
		{
			VMContext::object_heap->RegisterThreadStack(this);
		}
	}

	~ThreadStack();

	EventNotify execute(lpClass clazz, lpMethod method);

	EventNotify execute(lpClass clazz, lpMethod method, lpVariant vars, int count);

	void AddFrame(lpClass clazz, lpMethod method, lpStackFrame &frame);

	EventNotify start();

	// Marks references held by every frame on this stack.
	void MarkRoots(ObjectHeap *heap);
};
