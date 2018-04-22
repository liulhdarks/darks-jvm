#include "stdafx.h"
#include "filesystem.h"
#include <io.h>
jfieldID fd = NULL;

/*
 * Class:     java_io_FileOutputStream
 * Method:    open
 * Signature: (Ljava/lang/String;Z)V
 */
void JNICALL Java_java_io_FileOutputStream_open
  (JNIEnv *, jobject, jstring)
{

}

/*
 * Class:     java_io_FileOutputStream
 * Method:    write
 * Signature: (IZ)V
 */
void JNICALL Java_java_io_FileOutputStream_write
	(JNIEnv *env, jobject obj, jint byte)
{
	if (!fd)
	{
		cout<<"fd is not inited"<<endl;
		return;
	}
	jobject fdobj = env->GetObjectField(obj, fd);
	jclass fd_class = env->GetObjectClass(fdobj);
	jfieldID fd_field = env->GetFieldID(fd_class, "fd", "I");
	jint fd_val = env->GetIntField(fdobj, fd_field);
	write(fd_val, &byte, 1);
}
/*
 * Class:     java_io_FileOutputStream
 * Method:    writeBytes
 * Signature: ([BIIZ)V
 */
void JNICALL Java_java_io_FileOutputStream_writeBytes
	(JNIEnv *env, jobject obj, jbyteArray bytesarg, jint offset, jint len)
{
	if (!fd)
	{
		cout<<"fd is not inited"<<endl;
		return;
	}
	jobject fdobj = env->GetObjectField(obj, fd);
	jclass fd_class = env->GetObjectClass(fdobj);
	jfieldID fd_field = env->GetFieldID(fd_class, "fd", "I");
	jint fd_val = env->GetIntField(fdobj, fd_field);
	char *data = (char *)env->GetArrayDirectAddress(bytesarg);
	data += offset;
	write(fd_val, (void*)data, len - 1);
}

/*
 * Class:     java_io_FileOutputStream
 * Method:    close0
 * Signature: ()V
 */
void JNICALL Java_java_io_FileOutputStream_close0
	(JNIEnv *, jobject)
{

}

/*
 * Class:     java_io_FileOutputStream
 * Method:    initIDs
 * Signature: ()V
 */
void JNICALL Java_java_io_FileOutputStream_initIDs
	(JNIEnv *env, jclass clazz)
{
	fd = env->GetFieldID(clazz, "fd", "Ljava/io/FileDescriptor;");
}