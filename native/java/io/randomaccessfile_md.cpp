// java.io.RandomAccessFile natives.

#include "stdafx.h"
#include "jni_impl.h"
#include "platform/platform.h"
#include "vm/executor/deterministic.h"

#include <cstdio>
#include <vector>
#include <string>

namespace {

FILE *FileHandle(jlong handle)
{
	return reinterpret_cast<FILE*>(static_cast<intptr_t>(handle));
}

std::string JStringToUtf8(JNIEnv *env, jstring value)
{
	if (!value)
	{
		return std::string();
	}
	const char *chars = env->GetStringUTFChars(value, NULL);
	if (!chars)
	{
		return std::string();
	}
	std::string result(chars);
	env->ReleaseStringUTFChars(value, chars);
	return result;
}

} // namespace

extern "C" JNIEXPORT jlong JNICALL Java_java_io_RandomAccessFile_open0(
	JNIEnv *env, jclass, jstring name, jboolean writeable)
{
	DarksDeterministic::Instance().Boundary("io.RandomAccessFile.open");
	std::string path = JStringToUtf8(env, name);
	FILE *file = fopen(path.c_str(), writeable ? "r+b" : "rb");
	if (!file && writeable)
	{
		file = fopen(path.c_str(), "w+b");
	}
	return reinterpret_cast<jlong>(file);
}

extern "C" JNIEXPORT jint JNICALL Java_java_io_RandomAccessFile_read0(
	JNIEnv *, jclass, jlong handle)
{
	FILE *file = FileHandle(handle);
	if (!file)
	{
		return -1;
	}
	int value = fgetc(file);
	return value == EOF ? -1 : value;
}

extern "C" JNIEXPORT jint JNICALL Java_java_io_RandomAccessFile_readBytes0(
	JNIEnv *env, jclass, jlong handle, jbyteArray buffer, jint offset, jint length)
{
	FILE *file = FileHandle(handle);
	if (!file || !buffer || offset < 0 || length < 0)
	{
		return -1;
	}
	std::vector<char> data(static_cast<size_t>(length));
	size_t read = fread(data.empty() ? NULL : &data[0], 1,
		static_cast<size_t>(length), file);
	if (read > 0)
	{
		env->SetByteArrayRegion(buffer, offset, static_cast<jsize>(read),
			reinterpret_cast<const jbyte*>(&data[0]));
	}
	return static_cast<jint>(read);
}

extern "C" JNIEXPORT void JNICALL Java_java_io_RandomAccessFile_write0(
	JNIEnv *, jclass, jlong handle, jint value)
{
	FILE *file = FileHandle(handle);
	if (file)
	{
		fputc(value & 0xff, file);
	}
}

extern "C" JNIEXPORT void JNICALL Java_java_io_RandomAccessFile_writeBytes0(
	JNIEnv *env, jclass, jlong handle, jbyteArray buffer, jint offset, jint length)
{
	FILE *file = FileHandle(handle);
	if (!file || !buffer || offset < 0 || length < 0)
	{
		return;
	}
	std::vector<char> data(static_cast<size_t>(length));
	if (length > 0)
	{
		env->GetByteArrayRegion(buffer, offset, length,
			reinterpret_cast<jbyte*>(&data[0]));
		fwrite(&data[0], 1, static_cast<size_t>(length), file);
	}
}

extern "C" JNIEXPORT void JNICALL Java_java_io_RandomAccessFile_seek0(
	JNIEnv *, jclass, jlong handle, jlong position)
{
	FILE *file = FileHandle(handle);
	if (file)
	{
		fseek(file, static_cast<long>(position), SEEK_SET);
	}
}

extern "C" JNIEXPORT jlong JNICALL Java_java_io_RandomAccessFile_position0(
	JNIEnv *, jclass, jlong handle)
{
	FILE *file = FileHandle(handle);
	return file ? static_cast<jlong>(ftell(file)) : -1;
}

extern "C" JNIEXPORT jlong JNICALL Java_java_io_RandomAccessFile_length0(
	JNIEnv *, jclass, jlong handle)
{
	FILE *file = FileHandle(handle);
	if (!file)
	{
		return 0;
	}
	long current = ftell(file);
	fseek(file, 0, SEEK_END);
	long length = ftell(file);
	fseek(file, current, SEEK_SET);
	return static_cast<jlong>(length);
}

extern "C" JNIEXPORT void JNICALL Java_java_io_RandomAccessFile_setLength0(
	JNIEnv *, jclass, jlong handle, jlong length)
{
	(void)handle;
	(void)length;
}

extern "C" JNIEXPORT void JNICALL Java_java_io_RandomAccessFile_close0(
	JNIEnv *, jclass, jlong handle)
{
	FILE *file = FileHandle(handle);
	if (file)
	{
		fclose(file);
	}
}
