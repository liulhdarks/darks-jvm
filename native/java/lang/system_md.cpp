#include "stdafx.h"
#include "system_md.h"
/*
 * Class:     java_lang_System
 * Method:    registerNatives
 * Signature: ()V
 */
void JNICALL Java_java_lang_System_registerNatives
  (JNIEnv *, jclass)
{
	
}

/*
 * Class:     java_lang_System
 * Method:    setIn0
 * Signature: (Ljava/io/InputStream;)V
 */
void JNICALL Java_java_lang_System_setIn0
	(JNIEnv *, jclass, jobject)
{

}

/*
 * Class:     java_lang_System
 * Method:    setOut0
 * Signature: (Ljava/io/PrintStream;)V
 */
void JNICALL Java_java_lang_System_setOut0
	(JNIEnv *, jclass, jobject)
{

}

/*
 * Class:     java_lang_System
 * Method:    setErr0
 * Signature: (Ljava/io/PrintStream;)V
 */
void JNICALL Java_java_lang_System_setErr0
	(JNIEnv *, jclass, jobject)
{

}

/*
 * Class:     java_lang_System
 * Method:    currentTimeMillis
 * Signature: ()J
 */
jlong JNICALL Java_java_lang_System_currentTimeMillis
	(JNIEnv *, jclass)
{
	return OS::GetCurrentTimeMillis();
}

/*
 * Class:     java_lang_System
 * Method:    nanoTime
 * Signature: ()J
 */
jlong JNICALL Java_java_lang_System_nanoTime
	(JNIEnv *, jclass)
{
	return 0;
}

/*
 * Class:     java_lang_System
 * Method:    arraycopy
 * Signature: (Ljava/lang/Object;ILjava/lang/Object;II)V
 */
void JNICALL Java_java_lang_System_arraycopy
	(JNIEnv *env, jclass clazz, jobject src, jint src_pos, jobject dest, jint dest_pos, jint length)
{
	jarray src_arg = (jarray) src;
	jarray dest_arg = (jarray) dest;
	int src_type_size = env->GetArrayTypeSize(src_arg);
	int dest_type_size = env->GetArrayTypeSize(dest_arg);
	src_pos *= src_type_size;
	dest_pos *= dest_type_size;
	length *= src_type_size;
	char* src_data = (char*)env->GetArrayDirectAddress(src_arg);
	char* dest_data = (char*)env->GetArrayDirectAddress(dest_arg);
	if (!src_data || !dest_data)
	{
		cout<<"Arraycopy Array direct address is invalid"<<endl;
		return;
	}
	memmove(dest_data + dest_pos, src_data + src_pos, length);
}

/*
 * Class:     java_lang_System
 * Method:    identityHashCode
 * Signature: (Ljava/lang/Object;)I
 */
jint JNICALL Java_java_lang_System_identityHashCode
	(JNIEnv *, jclass, jobject)
{
	return 0;
}

/*
 * Class:     java_lang_System
 * Method:    initProperties
 * Signature: (Ljava/util/Properties;)Ljava/util/Properties;
 */
jobject JNICALL Java_java_lang_System_initProperties
	(JNIEnv *, jclass, jobject)
{
	return NULL;
}

/*
 * Class:     java_lang_System
 * Method:    mapLibraryName
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
jstring JNICALL Java_java_lang_System_mapLibraryName
	(JNIEnv *, jclass, jstring)
{
	return NULL;
}