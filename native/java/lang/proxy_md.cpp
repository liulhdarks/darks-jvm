// java.lang.reflect.Proxy support: creates synthetic classes implementing the
// requested interfaces and dispatches their methods to an InvocationHandler
// (see StackFrame::InvokeProxy).

#include "stdafx.h"
#include "jni_impl.h"
#include "vm/executor/proxy.h"
#include "vm/executor/javaexception.h"

#include <cstring>
#include <string>
#include <vector>

namespace {

lpClass MirrorClass(jobject mirror)
{
	return reinterpret_cast<lpClass>(static_cast<intptr_t>(
		JniGetLongFieldByName(mirror, "vmClass")));
}

void ThrowJava(JNIEnv *env, const char *class_name, const char *message)
{
	jclass error = reinterpret_cast<jclass>(JniFindClass(env, class_name));
	if (error)
	{
		env->ThrowNew(error, message);
	}
}

} // namespace

extern "C" {

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Proxy_newProxyInstance0(
	JNIEnv *env, jclass unused, jobject loader, jobjectArray interfaces,
	jobject handler)
{
	(void)unused;
	(void)loader;
	if (!interfaces || !handler)
	{
		ThrowJava(env, "java/lang/NullPointerException", "null proxy argument");
		return NULL;
	}
	lpObject array = JniResolveObject(reinterpret_cast<jobject>(interfaces));
	if (!array || !array->array_var)
	{
		ThrowJava(env, "java/lang/IllegalArgumentException", "invalid interface list");
		return NULL;
	}
	jobject *elements = static_cast<jobject *>(array->array_var);
	std::vector<std::string> names;
	for (int i = 0; i < array->array_length; i++)
	{
		lpClass iface = MirrorClass(elements[i]);
		if (!iface || !iface->thisClass || !iface->thisClass->GetName())
		{
			ThrowJava(env, "java/lang/IllegalArgumentException",
				"null interface in proxy list");
			return NULL;
		}
		if (!iface->isInterface())
		{
			ThrowJava(env, "java/lang/IllegalArgumentException",
				"proxy type must be an interface");
			return NULL;
		}
		names.push_back(iface->thisClass->GetName());
	}
	if (names.empty())
	{
		ThrowJava(env, "java/lang/IllegalArgumentException", "no interfaces");
		return NULL;
	}
	GCNativeScope no_gc(VMContext::object_heap);
	lpClass proxy_class = DarksVM_FindProxyClass(names,
		VMContext::bootstrap_classloader);
	if (!proxy_class)
	{
		ThrowJava(env, "java/lang/IllegalArgumentException", "cannot create proxy class");
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewObject(proxy_class, ref))
	{
		return NULL;
	}
	lpObject object = NULL;
	if (!VMContext::object_heap->GetObject(ref, object) || !object)
	{
		return NULL;
	}
	DarksVM_StoreProxyHandler(object, JniRef(handler));
	jobject proxy = JniObject(ref);
	JniAddLocalRef(env, proxy);
	return proxy;
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Proxy_getInvocationHandler0(
	JNIEnv *env, jclass unused, jobject proxy)
{
	(void)unused;
	lpObject object = JniResolveObject(proxy);
	LPOBJECT_REF handler = NULL;
	if (!object || !DarksVM_LoadProxyHandler(object, handler))
	{
		ThrowJava(env, "java/lang/IllegalArgumentException", "not a proxy instance");
		return NULL;
	}
	jobject result = JniObject(handler);
	JniAddLocalRef(env, result);
	return result;
}

JNIEXPORT jboolean JNICALL Java_java_lang_reflect_Proxy_isProxyClass0(
	JNIEnv *env, jclass unused, jobject clazz)
{
	(void)env;
	(void)unused;
	lpClass target = MirrorClass(clazz);
	if (!target || !target->thisClass || !target->thisClass->GetName())
	{
		return JNI_FALSE;
	}
	return strncmp(target->thisClass->GetName(), "darks/Proxy$", 12) == 0
		? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
