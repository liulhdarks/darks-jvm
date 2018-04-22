#pragma once

#include "jni.h"

jint JNICALL DarksVM_GetArrayType(JNIEnv *env, jarray array);

void* JNICALL DarksVM_GetArrayDirectAddress(JNIEnv *env, jarray array);

jint JNICALL DarksVM_GetArrayTypeSize(JNIEnv *env, jarray array);

jbyteArray JNICALL DarksVM_NewByteArray(JNIEnv *env, jsize len);

void JNICALL DarksVM_SetByteArrayRegion(JNIEnv *env, jbyteArray array, 
						jsize start, jsize len, const jbyte *buf);