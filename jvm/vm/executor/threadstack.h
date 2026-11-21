#pragma once

#include "vm/types.h"
#include <stack>
#include <string>
#include <utility>
#include <vector>
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
	// Set while a blocking native (sleep/wait/join) suspended the coroutine.
	bool yield_pending;
	// Exception to raise at the next interpreter instruction (interrupt).
	LPOBJECT_REF injected_exception;
	// Interpreter poll counters (safepoints and cooperative preemption).
	int safepoint_countdown;
	int fairness_countdown;

public:
	ThreadStack()
	{
		class_loader = VMContext::bootstrap_classloader;
		jni_env = new JNIEnv();
		jni_env->functions = DarksVM_GetJniInterface();
		return_wide = false;
		has_return = false;
		unwind_stop_depth = 0;
		yield_pending = false;
		injected_exception = NULL;
		safepoint_countdown = 512;
		fairness_countdown = 32768;
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

	// True when any frame on this stack currently holds a Java monitor.
	// Used by virtual-thread pinning diagnostics: entering a native method
	// while holding a monitor may pin the carrier thread.
	bool HoldsMonitor() const;

	// Class loader of the innermost frame, used to resolve Class.forName and
	// other symbolic lookups performed from that frame's code.
	lpClassLoader TopFrameClassLoader() const;

	// Diagnostics: writes the innermost frame as "Class.method" (internal
	// class name).  Returns false when the stack is empty.
	bool DescribeTopFrame(std::string &out) const;

	// One entry per Java frame, innermost first.  line_number is -1 when the
	// class has no line table and -2 for native methods (JDK conventions).
	struct FrameInfo
	{
		std::string declaring_class;
		std::string method_name;
		std::string file_name;
		int line_number;
	};
	void DescribeJavaFrames(std::vector<FrameInfo> &out) const;

	// Diagnostics: appends every monitor held by a frame, outermost first.
	void CollectHeldMonitors(std::vector<std::pair<LPOBJECT_REF, lpClass> > &out) const;
};
