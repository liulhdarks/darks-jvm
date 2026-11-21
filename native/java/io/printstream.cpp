#include "stdafx.h"
#include "printstream.h"
#include "platform/platform.h"

#include <cstdio>
#include <string>

namespace {

void write_jstring(JNIEnv *env, jstring str, FILE *stream)
{
	if (!str)
	{
		return;
	}
	const char *chars = env->GetStringUTFChars(str, NULL);
	if (!chars)
	{
		return;
	}
	fwrite(chars, 1, strlen(chars), stream);
	fflush(stream);
	env->ReleaseStringUTFChars(str, chars);
}

} // namespace

JNIEXPORT void JNICALL Java_java_io_PrintStream_write0
	(JNIEnv *env, jobject thiz, jstring str)
{
	(void)thiz;
	write_jstring(env, str, stdout);
}

JNIEXPORT void JNICALL Java_java_io_PrintStream_writeErr0
	(JNIEnv *env, jobject thiz, jstring str)
{
	(void)thiz;
	write_jstring(env, str, stderr);
}

namespace {

void write_jbytes(JNIEnv *env, jbyteArray bytes, jint offset, jint length, FILE *stream)
{
	if (!bytes || length <= 0)
	{
		return;
	}
	std::string buffer(static_cast<size_t>(length), '\0');
	env->GetByteArrayRegion(bytes, offset, length,
		reinterpret_cast<jbyte*>(&buffer[0]));
	fwrite(buffer.data(), 1, buffer.size(), stream);
	fflush(stream);
}

} // namespace

JNIEXPORT void JNICALL Java_java_io_PrintStream_writeBytes0
	(JNIEnv *env, jobject thiz, jbyteArray bytes, jint offset, jint length)
{
	(void)thiz;
	write_jbytes(env, bytes, offset, length, stdout);
}

JNIEXPORT void JNICALL Java_java_io_PrintStream_writeErrBytes0
	(JNIEnv *env, jobject thiz, jbyteArray bytes, jint offset, jint length)
{
	(void)thiz;
	write_jbytes(env, bytes, offset, length, stderr);
}

JNIEXPORT void JNICALL Java_java_io_PrintStream_println
	(JNIEnv *env, jobject thiz, jstring str)
{
	(void)thiz;
	write_jstring(env, str, stdout);
	fputc('\n', stdout);
	fflush(stdout);
}
