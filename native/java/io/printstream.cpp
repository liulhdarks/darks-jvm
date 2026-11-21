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

JNIEXPORT void JNICALL Java_java_io_PrintStream_println
	(JNIEnv *env, jobject thiz, jstring str)
{
	(void)thiz;
	write_jstring(env, str, stdout);
	fputc('\n', stdout);
	fflush(stdout);
}
