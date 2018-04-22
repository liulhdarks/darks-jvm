#include "stdafx.h"
#include "jni.h"

#include "jni_object.h"
#include "jni_array.h"

struct JNINativeInterface_ jni_native_interface = 
{
	NULL,
	NULL,
	NULL,
	NULL,
	DarksVM_GetObjectClass,
	DarksVM_GetFieldID,
	DarksVM_GetObjectField,
	DarksVM_GetIntField,
	DarksVM_SetObjectField,
	DarksVM_GetArrayType,
	DarksVM_GetArrayDirectAddress,
	DarksVM_GetArrayTypeSize,
	DarksVM_NewByteArray,
	DarksVM_SetByteArrayRegion
};