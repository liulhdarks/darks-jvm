// java.lang.Thread / Object monitor natives.
//
// The interpreter intercepts the blocking forms (Thread.sleep/yield/join and
// Object.wait/notify) before libffi dispatch so a coroutine can suspend.  The
// functions below are the fallback used when the same natives are invoked from
// JNI (Call*Method) or reflection, where the calling C stack cannot be
// suspended; they fall back to OS-level behaviour or report an explicit
// UnsupportedOperationException instead of pretending to work.

#include "stdafx.h"
#include "jni_impl.h"

#include "vm/executor/scheduler.h"
#include "vm/executor/context.h"
#include "vm/executor/objectheap.h"
#include "vm/executor/jobject.h"
#include "vm/executor/javaexception.h"
#include "vm/executor/vmmutex.h"
#include "vm/classfiles/classloader.h"

#include <chrono>
#include <cstring>
#include <string>
#include <thread>

namespace {

void SetRawField(LPOBJECT_REF ref, const char *name, const Variant &value)
{
	VMLock lk;
	lpObject obj = NULL;
	if (!ref || !VMContext::object_heap || !VMContext::object_heap->GetObject(ref, obj) || !obj)
	{
		return;
	}
	// Layout keys are "<name>:<descriptor>"; accept the bare name.
	std::string key(name);
	FIELD_MAP::iterator it = obj->varants.find(key);
	if (it == obj->varants.end())
	{
		key += ':';
		size_t key_len = key.size();
		for (it = obj->varants.begin(); it != obj->varants.end(); ++it)
		{
			if (it->first.size() >= key_len && it->first.compare(0, key_len, key) == 0)
			{
				break;
			}
		}
	}
	if (it != obj->varants.end() && it->second)
	{
		it->second->field_value = value;
	}
}

void ThrowUnsupported(const char *what)
{
	VMContext::pending_exception = CreateJavaException(
		"java/lang/UnsupportedOperationException", what);
}

LPOBJECT_REF EnsureCurrentThreadObject()
{
	lpCoroutine co = Scheduler::Current();
	if (!co)
	{
		return NULL;
	}
	if (co->thread_ref)
	{
		return co->thread_ref;
	}
	if (!VMContext::bootstrap_classloader || !VMContext::object_heap)
	{
		return NULL;
	}
	lpClass thread_class = NULL;
	if (!VMContext::bootstrap_classloader->FindClass("java/lang/Thread", thread_class) ||
	    !thread_class)
	{
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewObject(thread_class, ref) || !ref)
	{
		return NULL;
	}
	// Register as the coroutine's Thread object immediately: the slot is a GC
	// root, so the object survives the string allocation below.
	co->thread_ref = ref;

	LPOBJECT_REF name_ref = NULL;
	VMContext::object_heap->NewStringObject(const_cast<LPSTR>("main"), 4, name_ref);
	if (name_ref)
	{
		Variant v;
		v.heapPtr = name_ref;
		SetRawField(ref, "name", v);
	}
	Variant flag;
	flag.iValue = 1;
	SetRawField(ref, "alive", flag);
	SetRawField(ref, "started", flag);
	flag.iValue = 0;
	SetRawField(ref, "daemon", flag);
	SetRawField(ref, "priority", flag);
	Variant tid;
	tid.lValue = 0;
	SetRawField(ref, "tid", tid);
	return ref;
}

} // namespace

extern "C" {

JNIEXPORT void JNICALL Java_java_lang_Thread_start0(JNIEnv *env, jobject thiz)
{
	(void)env;
	if (!Scheduler::Instance().StartThread(JniRef(thiz)))
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/IllegalThreadStateException", "cannot start thread");
	}
}

JNIEXPORT void JNICALL Java_java_lang_Thread_sleep0(JNIEnv *env, jclass clazz, jlong millis)
{
	(void)env;
	(void)clazz;
	if (millis > 0)
	{
		std::this_thread::sleep_for(std::chrono::milliseconds(static_cast<long long>(millis)));
	}
}

JNIEXPORT void JNICALL Java_java_lang_Thread_yield0(JNIEnv *env, jclass clazz)
{
	(void)env;
	(void)clazz;
	std::this_thread::yield();
}

JNIEXPORT void JNICALL Java_java_lang_Thread_join0(JNIEnv *env, jobject thiz, jlong millis)
{
	(void)env;
	// Fallback path only (reflection/JNI invocation): poll the alive flag.
	LPOBJECT_REF target = JniRef(thiz);
	long long waited = 0;
	while (Scheduler::Instance().ThreadAlive(target))
	{
		std::this_thread::sleep_for(std::chrono::milliseconds(1));
		waited += 1;
		if (millis > 0 && waited >= millis)
		{
			break;
		}
	}
}

JNIEXPORT void JNICALL Java_java_lang_Thread_interrupt0(JNIEnv *env, jobject thiz)
{
	(void)env;
	Scheduler::Instance().InterruptThread(JniRef(thiz));
}

JNIEXPORT jboolean JNICALL Java_java_lang_Thread_isInterrupted0(JNIEnv *env, jobject thiz,
                                                                jboolean clear)
{
	(void)env;
	return Scheduler::Instance().IsInterrupted(JniRef(thiz), clear != 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_java_lang_Thread_interrupted0(JNIEnv *env, jclass clazz)
{
	(void)env;
	(void)clazz;
	LPOBJECT_REF ref = EnsureCurrentThreadObject();
	return Scheduler::Instance().IsInterrupted(ref, true) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobject JNICALL Java_java_lang_Thread_currentThread0(JNIEnv *env, jclass clazz)
{
	(void)clazz;
	LPOBJECT_REF ref = EnsureCurrentThreadObject();
	if (!ref)
	{
		return NULL;
	}
	jobject result = JniObject(ref);
	JniAddLocalRef(env, result);
	return result;
}

// ---- Object monitor fallbacks --------------------------------------------
JNIEXPORT void JNICALL Java_java_lang_Object_wait(JNIEnv *env, jobject thiz)
{
	(void)env;
	(void)thiz;
	ThrowUnsupported("Object.wait() is only supported from interpreted code");
}

JNIEXPORT void JNICALL Java_java_lang_Object_wait__J(JNIEnv *env, jobject thiz, jlong millis)
{
	(void)env;
	(void)thiz;
	(void)millis;
	ThrowUnsupported("Object.wait(long) is only supported from interpreted code");
}

JNIEXPORT void JNICALL Java_java_lang_Object_wait__JI(JNIEnv *env, jobject thiz,
                                                      jlong millis, jint nanos)
{
	(void)env;
	(void)thiz;
	(void)millis;
	(void)nanos;
	ThrowUnsupported("Object.wait(long,int) is only supported from interpreted code");
}

JNIEXPORT void JNICALL Java_java_lang_Object_notify(JNIEnv *env, jobject thiz)
{
	(void)env;
	(void)thiz;
	ThrowUnsupported("Object.notify() is only supported from interpreted code");
}

JNIEXPORT void JNICALL Java_java_lang_Object_notifyAll(JNIEnv *env, jobject thiz)
{
	(void)env;
	(void)thiz;
	ThrowUnsupported("Object.notifyAll() is only supported from interpreted code");
}

} // extern "C"
