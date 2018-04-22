#include "stdafx.h"
#include "jni_object.h"
#include "jni_external.h"
#include "utils/DebugUtils.h"


jclass JNICALL DarksVM_GetObjectClass(JNIEnv *env, jobject obj)
{
	lpObject object = GetVMObject(obj);
	if (!object)
	{
		DebugUtils::print("GetObjectClass cannot find object reference.");
		return NULL;
	}
	return GetJniClass(object->obj_class);
}

jfieldID JNICALL DarksVM_GetFieldID(JNIEnv *env, jclass clazz, const char *name, const char *sig)
{
	lpClass c = GetVMClass(clazz);
	if (!c)
	{
		DebugUtils::print("GetFieldID class is invalid.");
		return NULL;
	}
	LPSTR field_name = GetFieldName(name, sig);
	if (!field_name)
	{
		DebugUtils::print("GetFieldID fail to get field name.");
		return NULL;
	}
	lpField field = c->fields_map[string(field_name)];
	if (!field_name)
	{
		DebugUtils::print(2, "GetFieldID cannot find field ", field_name);
		return NULL;
	}
	return (jfieldID)field;
}

jobject JNICALL DarksVM_GetObjectField(JNIEnv *env, jobject obj, jfieldID fieldID)
{
	lpFieldValue fv = GetVMFieldValue(obj, fieldID);
	if (!fv)
	{
		DebugUtils::print("GetObjectField fail to get field value.");
		return NULL;
	}
	return GetJniObject(fv);;
}

jint JNICALL DarksVM_GetIntField(JNIEnv *env, jobject obj, jfieldID fieldID)
{
	lpFieldValue fv = GetVMFieldValue(obj, fieldID);
	if (!fv)
	{
		DebugUtils::print("GetIntField fail to get field value.");
		return 0;
	}
	return fv->field_value.iValue;
}

void JNICALL DarksVM_SetObjectField(JNIEnv *env, jobject obj, jfieldID fieldID, jobject val)
{
	lpFieldValue fv = GetVMFieldValue(obj, fieldID);
	if (!fv)
	{
		DebugUtils::print("SetObjectField fail to get field value.");
		return;
	}
	fv->field_value.heapPtr = GetVMObjectRef(val);
}
