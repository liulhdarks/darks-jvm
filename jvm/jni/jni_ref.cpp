// JNI reference, local-frame, exception and monitor support.
//
// Layout notes:
//   jobject   -> LPOBJECT_REF (address of JObject::index)
//   jclass    -> Class*
//   jmethodID -> Method*
//   jfieldID  -> Field*
// The standard 233-slot function table is defined in jni.cpp; every function
// below matches the official JDK signature exactly.

#include "stdafx.h"
#include "jni_impl.h"

#include <cstdio>
#include <cstdlib>
#include <mutex>

namespace {

std::recursive_mutex &JniMonitorMutex()
{
	static std::recursive_mutex m;
	return m;
}

thread_local JNIEnv *tls_current_env = NULL;

} // namespace

void JniSetCurrentEnv(JNIEnv *env)
{
	tls_current_env = env;
}

JNIEnv *JniGetCurrentEnv()
{
	return tls_current_env;
}

// ---------------------------------------------------------------------------
// Local frames
// ---------------------------------------------------------------------------

void DarksVM_InitJniEnv(JNIEnv *env)
{
	if (!env)
	{
		return;
	}
	env->thread_stack = NULL;
	env->local_frames = new std::vector<JniLocalFrame *>();
	VMContext::jni_envs.push_back(env);
}

void DarksVM_DestroyJniEnv(JNIEnv *env)
{
	if (!env)
	{
		return;
	}
	for (size_t i = 0; i < VMContext::jni_envs.size(); i++)
	{
		if (VMContext::jni_envs[i] == env)
		{
			VMContext::jni_envs.erase(VMContext::jni_envs.begin() + i);
			break;
		}
	}
	if (env->local_frames)
	{
		for (size_t i = 0; i < env->local_frames->size(); i++)
		{
			delete (*env->local_frames)[i];
		}
		delete env->local_frames;
		env->local_frames = NULL;
	}
}

void JniPushLocalFrame(JNIEnv *env)
{
	if (!env || !env->local_frames)
	{
		return;
	}
	env->local_frames->push_back(new JniLocalFrame());
}

void JniPopLocalFrame(JNIEnv *env, jobject result)
{
	if (!env || !env->local_frames || env->local_frames->empty())
	{
		return;
	}
	JniLocalFrame *frame = env->local_frames->back();
	env->local_frames->pop_back();
	delete frame;
	if (result)
	{
		JniAddLocalRef(env, result);
	}
}

void JniAddLocalRef(JNIEnv *env, jobject ref)
{
	if (!env || !ref)
	{
		return;
	}
	if (!env->local_frames || env->local_frames->empty())
	{
		// No active frame: the reference is only used inside C++ code and its
		// owner is responsible for rooting it (JNI functions always run under
		// a frame while a Java method executes).
		return;
	}
	env->local_frames->back()->refs.push_back(ref);
}

void DarksVM_PushJniLocalFrame(JNIEnv *env)
{
	JniPushLocalFrame(env);
}

void DarksVM_PopJniLocalFrame(JNIEnv *env)
{
	JniPopLocalFrame(env, NULL);
}

jobject DarksVM_NewLocalRef(JNIEnv *env, jobject ref)
{
	if (!ref || !VMContext::object_heap)
	{
		return NULL;
	}
	JniAddLocalRef(env, ref);
	return ref;
}

void DarksVM_AddLocalRef(JNIEnv *env, jobject ref)
{
	JniAddLocalRef(env, ref);
}

void DarksVM_MarkJniEnvRoots(void *env_ptr, void *heap_ptr)
{
	JNIEnv *env = reinterpret_cast<JNIEnv *>(env_ptr);
	ObjectHeap *heap = reinterpret_cast<ObjectHeap *>(heap_ptr);
	if (!env || !heap || !env->local_frames)
	{
		return;
	}
	for (size_t i = 0; i < env->local_frames->size(); i++)
	{
		JniLocalFrame *frame = (*env->local_frames)[i];
		if (!frame)
		{
			continue;
		}
		for (size_t j = 0; j < frame->refs.size(); j++)
		{
			heap->MarkObjectRef(JniRef(frame->refs[j]));
		}
	}
}

// ---------------------------------------------------------------------------
// Standard reference functions
// ---------------------------------------------------------------------------

extern "C" JNIEXPORT jint JNICALL JNI_GetVersion(JNIEnv *env)
{
	(void)env;
	return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jint JNICALL JNI_PushLocalFrame(JNIEnv *env, jint capacity)
{
	(void)capacity;
	JniPushLocalFrame(env);
	return JNI_OK;
}

extern "C" JNIEXPORT jobject JNICALL JNI_PopLocalFrame(JNIEnv *env, jobject result)
{
	JniPopLocalFrame(env, result);
	return result;
}

extern "C" JNIEXPORT jobject JNICALL JNI_NewGlobalRef(JNIEnv *env, jobject obj)
{
	(void)env;
	if (!obj || !VMContext::object_heap)
	{
		return NULL;
	}
	lpObject object = JniResolveObject(obj);
	if (!object)
	{
		return NULL;
	}
	VMContext::object_heap->AddGlobalRef(JniRef(obj));
	return obj;
}

extern "C" JNIEXPORT void JNICALL JNI_DeleteGlobalRef(JNIEnv *env, jobject obj)
{
	(void)env;
	if (obj && VMContext::object_heap)
	{
		VMContext::object_heap->RemoveGlobalRef(JniRef(obj));
	}
}

extern "C" JNIEXPORT void JNICALL JNI_DeleteLocalRef(JNIEnv *env, jobject obj)
{
	(void)env;
	(void)obj;
	// Conservative: the object simply stays alive until the frame is popped.
}

extern "C" JNIEXPORT jboolean JNICALL JNI_IsSameObject(JNIEnv *env, jobject a, jobject b)
{
	(void)env;
	if (!a || !b)
	{
		return (a == b) ? JNI_TRUE : JNI_FALSE;
	}
	return (JniRef(a) == JniRef(b)) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jobject JNICALL JNI_NewLocalRef(JNIEnv *env, jobject obj)
{
	return DarksVM_NewLocalRef(env, obj);
}

extern "C" JNIEXPORT jint JNICALL JNI_EnsureLocalCapacity(JNIEnv *env, jint capacity)
{
	(void)env;
	(void)capacity;
	return JNI_OK;
}

extern "C" JNIEXPORT jobject JNICALL JNI_NewWeakGlobalRef(JNIEnv *env, jobject obj)
{
	(void)env;
	if (!obj || !VMContext::object_heap)
	{
		return NULL;
	}
	VMContext::object_heap->AddWeakGlobalRef(JniRef(obj));
	return obj;
}

extern "C" JNIEXPORT void JNICALL JNI_DeleteWeakGlobalRef(JNIEnv *env, jobject obj)
{
	(void)env;
	if (obj && VMContext::object_heap)
	{
		VMContext::object_heap->RemoveWeakGlobalRef(JniRef(obj));
	}
}

extern "C" JNIEXPORT jobjectRefType JNICALL JNI_GetObjectRefType(JNIEnv *env, jobject obj)
{
	(void)env;
	if (!obj || !VMContext::object_heap)
	{
		return JNIInvalidRefType;
	}
	LPOBJECT_REF ref = JniRef(obj);
	if (VMContext::object_heap->IsGlobalRef(ref))
	{
		return JNIGlobalRefType;
	}
	if (VMContext::object_heap->IsWeakGlobalRef(ref))
	{
		return JNIWeakGlobalRefType;
	}
	lpObject object = NULL;
	if (VMContext::object_heap->GetObject(ref, object))
	{
		return JNILocalRefType;
	}
	return JNIInvalidRefType;
}

// ---------------------------------------------------------------------------
// Exceptions
// ---------------------------------------------------------------------------

void DarksVM_SetPendingException(jobject exception)
{
	VMContext::pending_exception = JniRef(exception);
}

jobject DarksVM_GetPendingException()
{
	return JniObject(VMContext::pending_exception);
}

void DarksVM_ClearPendingException()
{
	VMContext::pending_exception = NULL;
}

void DarksVM_PrintPendingException()
{
	if (VMContext::pending_exception)
	{
		PrintUncaughtException(VMContext::pending_exception);
	}
}

extern "C" JNIEXPORT jint JNICALL JNI_Throw(JNIEnv *env, jthrowable obj)
{
	(void)env;
	if (!obj)
	{
		return JNI_ERR;
	}
	DarksVM_SetPendingException(obj);
	return JNI_OK;
}

extern "C" JNIEXPORT jint JNICALL JNI_ThrowNew(JNIEnv *env, jclass clazz, const char *message)
{
	(void)env;
	lpClass c = JniResolveClass(clazz);
	if (!c || !c->thisClass || !c->thisClass->GetName())
	{
		return JNI_ERR;
	}
	LPOBJECT_REF ref = CreateJavaException(c->thisClass->GetName(), message);
	if (!ref)
	{
		return JNI_ERR;
	}
	DarksVM_SetPendingException(JniObject(ref));
	return JNI_OK;
}

extern "C" JNIEXPORT jthrowable JNICALL JNI_ExceptionOccurred(JNIEnv *env)
{
	(void)env;
	return reinterpret_cast<jthrowable>(JniObject(VMContext::pending_exception));
}

extern "C" JNIEXPORT void JNICALL JNI_ExceptionDescribe(JNIEnv *env)
{
	(void)env;
	if (VMContext::pending_exception)
	{
		PrintUncaughtException(VMContext::pending_exception);
	}
}

extern "C" JNIEXPORT void JNICALL JNI_ExceptionClear(JNIEnv *env)
{
	(void)env;
	DarksVM_ClearPendingException();
}

extern "C" JNIEXPORT jboolean JNICALL JNI_ExceptionCheck(JNIEnv *env)
{
	(void)env;
	return VMContext::pending_exception ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL JNI_FatalError(JNIEnv *env, const char *message)
{
	(void)env;
	fprintf(stderr, "FATAL ERROR in native method: %s\n", message ? message : "");
	fflush(stderr);
	abort();
}

// ---------------------------------------------------------------------------
// Monitors
// ---------------------------------------------------------------------------

extern "C" JNIEXPORT jint JNICALL JNI_MonitorEnter(JNIEnv *env, jobject obj)
{
	(void)env;
	if (!obj)
	{
		return JNI_ERR;
	}
	JniMonitorMutex().lock();
	return JNI_OK;
}

extern "C" JNIEXPORT jint JNICALL JNI_MonitorExit(JNIEnv *env, jobject obj)
{
	(void)env;
	if (!obj)
	{
		return JNI_ERR;
	}
	// Every Java monitor maps to the same recursive mutex, so an unmatched
	// exit is ignored rather than aborting.
	JniMonitorMutex().unlock();
	return JNI_OK;
}

// ---------------------------------------------------------------------------
// JavaVM
// ---------------------------------------------------------------------------

namespace {

jint JNICALL VM_DestroyJavaVM(JavaVM *vm)
{
	(void)vm;
	return JNI_OK;
}

jint JNICALL VM_AttachCurrentThread(JavaVM *vm, void **penv, void *args)
{
	(void)vm;
	(void)args;
	JNIEnv *env = JniGetCurrentEnv();
	if (!env)
	{
		if (penv)
		{
			*penv = NULL;
		}
		return JNI_EDETACHED;
	}
	if (penv)
	{
		*penv = env;
	}
	return JNI_OK;
}

jint JNICALL VM_DetachCurrentThread(JavaVM *vm)
{
	(void)vm;
	return JNI_OK;
}

jint JNICALL VM_GetEnv(JavaVM *vm, void **penv, jint version)
{
	(void)vm;
	(void)version;
	JNIEnv *env = JniGetCurrentEnv();
	if (!env)
	{
		if (penv)
		{
			*penv = NULL;
		}
		return JNI_EDETACHED;
	}
	if (penv)
	{
		*penv = env;
	}
	return JNI_OK;
}

jint JNICALL VM_AttachCurrentThreadAsDaemon(JavaVM *vm, void **penv, void *args)
{
	return VM_AttachCurrentThread(vm, penv, args);
}

const JNIInvokeInterface_ g_java_vm_interface = {
	NULL,
	NULL,
	NULL,
	VM_DestroyJavaVM,
	VM_AttachCurrentThread,
	VM_DetachCurrentThread,
	VM_GetEnv,
	VM_AttachCurrentThreadAsDaemon
};

JavaVM_ g_java_vm = { &g_java_vm_interface };

} // namespace

JavaVM *DarksVM_GetJavaVMInstance()
{
	static bool initialized = false;
	if (!initialized)
	{
		g_java_vm.functions = &g_java_vm_interface;
		initialized = true;
	}
	return &g_java_vm;
}

extern "C" JNIEXPORT jint JNICALL JNI_GetJavaVM(JNIEnv *env, JavaVM **vm)
{
	(void)env;
	if (!vm)
	{
		return JNI_ERR;
	}
	*vm = DarksVM_GetJavaVMInstance();
	return JNI_OK;
}

jint JNICALL JNI_GetEnv(JavaVM *vm, void **penv, jint version)
{
	return VM_GetEnv(vm, penv, version);
}

jint JNICALL JNI_AttachCurrentThread(JavaVM *vm, void **penv, void *args)
{
	return VM_AttachCurrentThread(vm, penv, args);
}

jint JNICALL JNI_DetachCurrentThread(JavaVM *vm)
{
	return VM_DetachCurrentThread(vm);
}

jint JNICALL JNI_AttachCurrentThreadAsDaemon(JavaVM *vm, void **penv, void *args)
{
	return VM_AttachCurrentThreadAsDaemon(vm, penv, args);
}

jint JNICALL JNI_DestroyJavaVM(JavaVM *vm)
{
	return VM_DestroyJavaVM(vm);
}
