#include "stdafx.h"
#include "string_md.h"

jbyteArray JNICALL Java_java_lang_StringCoding_encode
	(JNIEnv *env, jclass clazz, jcharArray chararg, jint offset, jint length)
{
	void* data = env->GetArrayDirectAddress(chararg);
	jchar* chars = (jchar*)data; 
	chars += offset;
	length = WideCharToMultiByte(CP_ACP, 0, (LPCWSTR)chars, -1, NULL, 0, NULL, NULL);  
	char *szGBK = new char[length + 1];  
	memset(szGBK, 0, length + 1);  
	WideCharToMultiByte(CP_ACP,0, (LPCWSTR)chars, -1, szGBK, length, NULL, NULL); 
	jbyteArray bytearg = env->NewByteArray(length);
	env->SetByteArrayRegion(bytearg, 0, length, (jbyte*)szGBK);
	return bytearg;
}

jcharArray JNICALL Java_java_lang_StringCoding_decode
	(JNIEnv *, jclass, jbyteArray, jint, jint)
{

	return NULL;
}