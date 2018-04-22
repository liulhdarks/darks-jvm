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

public:
	ThreadStack()
	{
		class_loader = VMContext::bootstrap_classloader;
		jni_env = new JNIEnv();
		jni_env->functions = &jni_native_interface;
	}

	~ThreadStack();

	EventNotify execute(lpClass clazz, lpMethod method);

	EventNotify execute(lpClass clazz, lpMethod method, lpVariant vars, int count);

	void AddFrame(lpClass clazz, lpMethod method, lpStackFrame &frame);

	EventNotify start();
};
