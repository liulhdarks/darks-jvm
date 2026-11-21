// java.util.concurrent.StructuredTaskScope natives.
//
// The Java class keeps the scope state and joins children with the regular
// Thread.join path (the scheduler's joiner lists); the natives below only
// bridge scope identity to the scheduler: starting a child with its parent
// scope recorded on the coroutine, interrupting the remaining children on
// shutdown, and reporting/retiring the scheduler-side registry entry.

#include "stdafx.h"
#include "jni_impl.h"

#include "vm/executor/scheduler.h"
#include "vm/executor/context.h"
#include "vm/executor/javaexception.h"

extern "C" {

JNIEXPORT void JNICALL
Java_java_util_concurrent_StructuredTaskScope_startScoped0(JNIEnv *env, jclass clazz,
                                                           jobject thread,
                                                           jobject scope)
{
	(void)env;
	(void)clazz;
	if (!Scheduler::Instance().StartScopedThread(JniRef(thread), JniRef(scope)))
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/IllegalStateException", "scope is closed");
	}
}

JNIEXPORT void JNICALL
Java_java_util_concurrent_StructuredTaskScope_shutdownScope0(JNIEnv *env, jclass clazz,
                                                             jobject scope)
{
	(void)env;
	(void)clazz;
	Scheduler::Instance().ScopeShutdown(JniRef(scope));
}

JNIEXPORT jint JNICALL
Java_java_util_concurrent_StructuredTaskScope_liveSubtasks0(JNIEnv *env, jclass clazz,
                                                            jobject scope)
{
	(void)env;
	(void)clazz;
	return static_cast<jint>(Scheduler::Instance().ScopeLiveChildren(JniRef(scope)));
}

JNIEXPORT jint JNICALL
Java_java_util_concurrent_StructuredTaskScope_closeScope0(JNIEnv *env, jclass clazz,
                                                          jobject scope)
{
	(void)env;
	(void)clazz;
	return static_cast<jint>(Scheduler::Instance().ScopeClose(JniRef(scope)));
}

JNIEXPORT jobject JNICALL
Java_java_util_concurrent_StructuredTaskScope_currentScope0(JNIEnv *env, jclass clazz)
{
	(void)clazz;
	LPOBJECT_REF ref = Scheduler::Instance().CurrentScope();
	if (!ref)
	{
		return NULL;
	}
	jobject result = JniObject(ref);
	JniAddLocalRef(env, result);
	return result;
}

} // extern "C"
