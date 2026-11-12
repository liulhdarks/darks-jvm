// JNI registration, native library loading and unsupported-feature stubs.

#include "stdafx.h"
#include "jni_impl.h"

#include "platform/platform.h"

#include <cstdio>
#include <map>
#include <vector>

namespace {

std::map<lpMethod, void *> &RegisteredNatives()
{
	static std::map<lpMethod, void *> natives;
	return natives;
}

} // namespace

void *DarksVM_GetRegisteredNative(lpMethod method)
{
	if (!method)
	{
		return NULL;
	}
	std::map<lpMethod, void *>::iterator it = RegisteredNatives().find(method);
	return it == RegisteredNatives().end() ? NULL : it->second;
}

void DarksVM_RegisterNative(lpMethod method, void *fn)
{
	if (method)
	{
		RegisteredNatives()[method] = fn;
	}
}

bool DarksVM_UnregisterNatives(lpClass clazz)
{
	if (!clazz)
	{
		return false;
	}
	for (U2 i = 0; i < clazz->methodCount; i++)
	{
		lpMethod method = clazz->methods ? clazz->methods[i] : NULL;
		if (method)
		{
			RegisteredNatives().erase(method);
		}
	}
	return true;
}

jint DarksVM_EnsureInitialized(lpClass clazz)
{
	if (!clazz)
	{
		return JNI_ERR;
	}
	try
	{
		clazz->initialize();
	}
	catch (...)
	{
		return JNI_ERR;
	}
	return JNI_OK;
}


// Called by NativeLibrary's destructor (VM teardown) before the OS handle is
// released. The VM is already partly torn down at this point, so JNI_OnUnload
// implementations must only release their own resources (no Java calls).
void DarksVM_UnloadJniLibrary(void *handle)
{
	if (!handle)
	{
		return;
	}
	void *on_unload = OS::GetNativeFunction(handle, "JNI_OnUnload");
	if (!on_unload)
	{
		return;
	}
	typedef void(JNICALL *OnUnloadFn)(JavaVM *, void *);
	reinterpret_cast<OnUnloadFn>(on_unload)(DarksVM_GetJavaVMInstance(), NULL);
}

static jint CallOnLoad(const char *library_name)
{
	if (!library_name || !VMContext::native_libs)
	{
		return JNI_ERR;
	}
	void *handle = VMContext::native_libs->GetLibraryHandle(library_name);
	if (!handle)
	{
		return JNI_ERR;
	}
	void *on_load = OS::GetNativeFunction(handle, "JNI_OnLoad");
	if (!on_load)
	{
		return JNI_OK;
	}
	typedef jint(JNICALL *OnLoadFn)(JavaVM *, void *);
	JavaVM *vm = DarksVM_GetJavaVMInstance();
	jint version = reinterpret_cast<OnLoadFn>(on_load)(vm, NULL);
	if (version == JNI_ERR)
	{
		fprintf(stderr, "JNI_OnLoad failed in %s\n", library_name);
		return JNI_ERR;
	}
	return JNI_OK;
}

jint DarksVM_LoadNativeLibrary(const char *name)
{
	if (!name || !VMContext::native_libs)
	{
		return JNI_ERR;
	}
	if (!VMContext::native_libs->LoadNativeLibrary(name))
	{
		return JNI_ERR;
	}
	return CallOnLoad(name);
}

jint DarksVM_LoadNativeLibraryPath(const char *path)
{
	if (!path || !VMContext::native_libs)
	{
		return JNI_ERR;
	}
	if (!VMContext::native_libs->LoadNativeLibraryPath(path))
	{
		return JNI_ERR;
	}
	return CallOnLoad(path);
}

extern "C" JNIEXPORT jint JNICALL JNI_RegisterNatives(JNIEnv *env, jclass clazz,
	const JNINativeMethod *methods, jint nMethods)
{
	(void)env;
	lpClass c = JniResolveClass(clazz);
	if (!c || !methods || nMethods < 0)
	{
		return JNI_ERR;
	}
	for (jint i = 0; i < nMethods; i++)
	{
		lpMethod method = JniFindMethod(c, methods[i].name, methods[i].signature, false);
		if (!method)
		{
			method = JniFindMethod(c, methods[i].name, methods[i].signature, true);
		}
		if (!method)
		{
			fprintf(stderr, "RegisterNatives: method not found %s%s\n",
				methods[i].name ? methods[i].name : "?",
				methods[i].signature ? methods[i].signature : "");
			return JNI_ERR;
		}
		RegisteredNatives()[method] = methods[i].fnPtr;
	}
	return JNI_OK;
}

extern "C" JNIEXPORT jint JNICALL JNI_UnregisterNatives(JNIEnv *env, jclass clazz)
{
	(void)env;
	return DarksVM_UnregisterNatives(JniResolveClass(clazz)) ? JNI_OK : JNI_ERR;
}

// ---------------------------------------------------------------------------
// Unsupported / optional features (documented stubs that fail cleanly).
// ---------------------------------------------------------------------------

extern "C" JNIEXPORT jobject JNICALL JNI_NewDirectByteBuffer(JNIEnv *env,
	void *address, jlong capacity)
{
	(void)env;
	(void)address;
	(void)capacity;
	return NULL;
}

extern "C" JNIEXPORT void * JNICALL JNI_GetDirectBufferAddress(JNIEnv *env, jobject buf)
{
	(void)env;
	(void)buf;
	return NULL;
}

extern "C" JNIEXPORT jlong JNICALL JNI_GetDirectBufferCapacity(JNIEnv *env, jobject buf)
{
	(void)env;
	(void)buf;
	return -1;
}

extern "C" JNIEXPORT jobject JNICALL JNI_GetModule(JNIEnv *env, jclass clazz)
{
	(void)env;
	(void)clazz;
	return NULL;
}

extern "C" JNIEXPORT jboolean JNICALL JNI_IsVirtualThread(JNIEnv *env, jobject obj)
{
	(void)env;
	(void)obj;
	return JNI_FALSE;
}
