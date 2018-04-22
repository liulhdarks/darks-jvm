#pragma once

#include "jni.h"

jclass JNICALL DarksVM_GetObjectClass(JNIEnv *env, jobject obj);

jfieldID JNICALL DarksVM_GetFieldID(JNIEnv *env, jclass clazz, const char *name, const char *sig);

jobject JNICALL DarksVM_GetObjectField(JNIEnv *env, jobject obj, jfieldID fieldID);

jint JNICALL DarksVM_GetIntField(JNIEnv *env, jobject obj, jfieldID fieldID);

void JNICALL DarksVM_SetObjectField(JNIEnv *env, jobject obj, jfieldID fieldID, jobject val);