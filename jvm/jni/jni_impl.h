#pragma once

#include "jni.h"

#include "vm/types.h"
#include "vm/executor/context.h"
#include "vm/executor/objectheap.h"
#include "vm/executor/jobject.h"
#include "vm/executor/jclass.h"
#include "vm/executor/jfield.h"
#include "vm/executor/jmethod.h"
#include "vm/executor/methodarea.h"
#include "vm/executor/descriptor.h"
#include "vm/executor/javaexception.h"
#include "vm/executor/executor.h"
#include "vm/classfiles/classloader.h"
#include "vm/exceptions.h"
#include "platform/platform.h"

#include <cstring>
#include <map>
#include <set>
#include <string>
#include <vector>

struct JniLocalFrame
{
	std::vector<jobject> refs;
};

inline LPOBJECT_REF JniRef(jobject obj)
{
	return reinterpret_cast<LPOBJECT_REF>(obj);
}

inline jobject JniObject(LPOBJECT_REF ref)
{
	return reinterpret_cast<jobject>(ref);
}

inline lpObject JniResolveObject(jobject obj)
{
	lpObject result = NULL;
	if (obj && VMContext::object_heap &&
	    VMContext::object_heap->GetObject(JniRef(obj), result))
	{
		return result;
	}
	return NULL;
}

inline lpClass JniResolveClass(jclass clazz)
{
	return reinterpret_cast<lpClass>(clazz);
}

inline lpMethod JniResolveMethod(jmethodID method)
{
	return reinterpret_cast<lpMethod>(method);
}

inline lpField JniResolveField(jfieldID field)
{
	return reinterpret_cast<lpField>(field);
}

// Adds a reference to the current local frame (implemented in jni_ref.cpp).
void JniAddLocalRef(JNIEnv *env, jobject ref);

// Current JNIEnv for the executing thread (used by JavaVM::GetEnv).
void JniSetCurrentEnv(JNIEnv *env);
JNIEnv *JniGetCurrentEnv();

// Pushes/pops an internal local-reference frame.
void JniPushLocalFrame(JNIEnv *env);
void JniPopLocalFrame(JNIEnv *env, jobject result);

// Field value access (static fields live in MethodArea).
lpFieldValue JniStaticFieldValue(lpClass clazz, lpField field, bool create);
lpFieldValue JniInstanceFieldValue(jobject obj, jfieldID field);

// Instance field setter by plain field name (used when synthesising Class
// objects and exceptions from native code).
bool JniSetObjectFieldByName(jobject obj, const char *name, jobject value);
bool JniSetLongFieldByName(jobject obj, const char *name, jlong value);
jlong JniGetLongFieldByName(jobject obj, const char *name);

jint DarksVM_EnsureInitialized(lpClass clazz);

// Creates a java.lang.Class mirror for a VM Class (cached).
jobject JniClassMirror(jclass clazz);

// Result of a Java method invocation from JNI code.
jvalue JniInvokeMethod(JNIEnv *env, jobject receiver, jclass clazz,
                       jmethodID method, const jvalue *args,
                       bool isStatic, bool nonvirtual);

// Builds an internal class name from a JNI name ("java/lang/String" or
// "[Ljava/lang/String;"); returns NULL when loading fails.
lpClass JniFindClass(JNIEnv *env, const char *name);

// Recursive method/field lookup (class chain included).
lpMethod JniFindMethod(lpClass clazz, const char *name, const char *sig, bool isStatic);
lpField JniFindField(lpClass clazz, const char *name, const char *sig, bool isStatic);

// String helpers.
bool JniStringChars(LPOBJECT_REF string_ref, jchar **chars, jsize *length);
jstring JniMakeString(JNIEnv *env, const char *utf8);
jstring JniMakeString(JNIEnv *env, const jchar *units, jsize length);

// JavaVM singleton and registered native methods.
JavaVM *DarksVM_GetJavaVMInstance();
void *DarksVM_GetRegisteredNative(lpMethod method);
bool DarksVM_UnregisterNatives(lpClass clazz);
void DarksVM_RegisterNative(lpMethod method, void *fn);

// Calls JNI_OnUnload (if exported) before the library handle is released.
void DarksVM_UnloadJniLibrary(void *handle);
