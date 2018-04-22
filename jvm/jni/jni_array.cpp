#include "stdafx.h"
#include "jni_array.h"
#include "jni_external.h"
#include "utils/DebugUtils.h"
#include "vm/executor/descriptor.h"
#include "vm/executor/context.h"

jint JNICALL DarksVM_GetArrayType(JNIEnv *env, jarray array)
{
	lpObject arg_obj = GetVMObject(array);
	if (!arg_obj)
	{
		DebugUtils::print("GetArrayType array is null.");
		return NULL;
	}
	return arg_obj->array_data_type;
}

void* JNICALL DarksVM_GetArrayDirectAddress(JNIEnv *env, jarray array)
{
	lpObject arg_obj = GetVMObject(array);
	if (!arg_obj)
	{
		DebugUtils::print("GetArrayType array is null.");
		return NULL;
	}
	return arg_obj->array_var;
}

jint JNICALL DarksVM_GetArrayTypeSize(JNIEnv *env, jarray array)
{
	lpObject arg_obj = GetVMObject(array);
	if (!arg_obj)
	{
		DebugUtils::print("GetArrayType array is null.");
		return NULL;
	}
	switch(arg_obj->array_data_type)
	{
	case DT_ARRAY:
	case DT_REF:
		return sizeof(jobject);
	case DT_BYTE:
		return sizeof(jbyte);
	case DT_CHAR:
		return sizeof(jchar);
	case DT_DOUBLE:
		return sizeof(jdouble);
	case DT_FLOAT:
		return sizeof(jfloat);
	case DT_INT:
		return sizeof(jint);
	case DT_LONG:
		return sizeof(jlong);
	case DT_SHORT:
		return sizeof(jshort);
	case DT_BOOL:
		return sizeof(jboolean);
	default:
		return 1;
	}
}

jbyteArray JNICALL DarksVM_NewByteArray(JNIEnv *env, jsize len)
{
	lpClass clazz = NULL;
	if (!VMContext::bootstrap_classloader->FindClass("[B", clazz))
	{
		return NULL;
	}
	LPOBJECT_REF obj_ref = NULL;
	if (!VMContext::object_heap->NewArrayObject(len, clazz, obj_ref))
	{
		return NULL;
	}
	return (jbyteArray)GetJniObject(obj_ref);
}

void JNICALL DarksVM_SetByteArrayRegion(JNIEnv *env, jbyteArray array, 
	jsize start, jsize len, const jbyte *buf)
{
	lpObject arg_obj = GetVMObject(array);
	if (!arg_obj)
	{
		DebugUtils::print("SetByteArrayRegion array is null.");
		return;
	}
	buf += start;
	arg_obj->SetData(DT_BYTE, (LPVOID)buf, len);
}