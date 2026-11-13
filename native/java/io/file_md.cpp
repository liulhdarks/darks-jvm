// java.io.File / FileInputStream / FileOutputStream natives.

#include "stdafx.h"
#include "jni_impl.h"
#include "platform/platform.h"

#include <algorithm>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

#if defined(_WIN32)
#include <direct.h>
#include <io.h>
#include <windows.h>
#else
#include <dirent.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#endif

namespace {

std::string FilePath(JNIEnv *env, jobject thiz)
{
	if (!thiz)
	{
		return std::string();
	}
	jclass clazz = env->GetObjectClass(thiz);
	if (!clazz)
	{
		return std::string();
	}
	jfieldID field = env->GetFieldID(clazz, "path", "Ljava/lang/String;");
	if (!field)
	{
		return std::string();
	}
	jstring value = reinterpret_cast<jstring>(env->GetObjectField(thiz, field));
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

bool StatPath(const std::string &path, bool &is_file, bool &is_dir, long long &size)
{
	is_file = false;
	is_dir = false;
	size = 0;
#if defined(_WIN32)
	struct _stat64 info;
	if (_stat64(path.c_str(), &info) != 0)
	{
		return false;
	}
	is_dir = (info.st_mode & _S_IFDIR) != 0;
	is_file = (info.st_mode & _S_IFREG) != 0;
	size = static_cast<long long>(info.st_size);
	return true;
#else
	struct stat info;
	if (stat(path.c_str(), &info) != 0)
	{
		return false;
	}
	is_dir = S_ISDIR(info.st_mode);
	is_file = S_ISREG(info.st_mode);
	size = static_cast<long long>(info.st_size);
	return true;
#endif
}

bool MakeDirectory(const std::string &path)
{
#if defined(_WIN32)
	return _mkdir(path.c_str()) == 0;
#else
	return mkdir(path.c_str(), 0755) == 0;
#endif
}

} // namespace

extern "C" {

jboolean JNICALL Java_java_io_File_exists0(JNIEnv *env, jobject thiz)
{
	bool is_file = false;
	bool is_dir = false;
	long long size = 0;
	return StatPath(FilePath(env, thiz), is_file, is_dir, size) ? JNI_TRUE : JNI_FALSE;
}

jboolean JNICALL Java_java_io_File_isFile0(JNIEnv *env, jobject thiz)
{
	bool is_file = false;
	bool is_dir = false;
	long long size = 0;
	if (!StatPath(FilePath(env, thiz), is_file, is_dir, size))
	{
		return JNI_FALSE;
	}
	return is_file ? JNI_TRUE : JNI_FALSE;
}

jboolean JNICALL Java_java_io_File_isDirectory0(JNIEnv *env, jobject thiz)
{
	bool is_file = false;
	bool is_dir = false;
	long long size = 0;
	if (!StatPath(FilePath(env, thiz), is_file, is_dir, size))
	{
		return JNI_FALSE;
	}
	return is_dir ? JNI_TRUE : JNI_FALSE;
}

jlong JNICALL Java_java_io_File_length0(JNIEnv *env, jobject thiz)
{
	bool is_file = false;
	bool is_dir = false;
	long long size = 0;
	if (!StatPath(FilePath(env, thiz), is_file, is_dir, size) || is_dir)
	{
		return 0;
	}
	return static_cast<jlong>(size);
}

jboolean JNICALL Java_java_io_File_delete0(JNIEnv *env, jobject thiz)
{
	std::string path = FilePath(env, thiz);
#if defined(_WIN32)
	if (RemoveDirectoryA(path.c_str()))
	{
		return JNI_TRUE;
	}
	return DeleteFileA(path.c_str()) ? JNI_TRUE : JNI_FALSE;
#else
	return remove(path.c_str()) == 0 ? JNI_TRUE : JNI_FALSE;
#endif
}

jboolean JNICALL Java_java_io_File_mkdirs0(JNIEnv *env, jobject thiz)
{
	std::string path = FilePath(env, thiz);
	if (path.empty())
	{
		return JNI_FALSE;
	}
	bool is_file = false;
	bool is_dir = false;
	long long size = 0;
	if (StatPath(path, is_file, is_dir, size))
	{
		return is_dir ? JNI_TRUE : JNI_FALSE;
	}
	// Create each parent component (skipping the leading '/' of an absolute
	// path and tolerating existing directories).
	for (size_t i = 1; i < path.size(); i++)
	{
		if (path[i] == '/' || path[i] == '\\')
		{
			std::string parent = path.substr(0, i);
			if (!parent.empty())
			{
				MakeDirectory(parent);
			}
		}
	}
	if (!MakeDirectory(path))
	{
		if (StatPath(path, is_file, is_dir, size) && is_dir)
		{
			return JNI_TRUE;
		}
		return JNI_FALSE;
	}
	return JNI_TRUE;
}

jobjectArray JNICALL Java_java_io_File_list0(JNIEnv *env, jobject thiz)
{
	std::string path = FilePath(env, thiz);
	std::vector<std::string> names;
#if defined(_WIN32)
	WIN32_FIND_DATAA data;
	HANDLE handle = FindFirstFileA((path + "\\*").c_str(), &data);
	if (handle == INVALID_HANDLE_VALUE)
	{
		return NULL;
	}
	do
	{
		std::string name = data.cFileName;
		if (name != "." && name != "..")
		{
			names.push_back(name);
		}
	}
	while (FindNextFileA(handle, &data));
	FindClose(handle);
#else
	DIR *dir = opendir(path.c_str());
	if (!dir)
	{
		return NULL;
	}
	struct dirent *entry;
	while ((entry = readdir(dir)) != NULL)
	{
		std::string name = entry->d_name;
		if (name != "." && name != "..")
		{
			names.push_back(name);
		}
	}
	closedir(dir);
#endif
	std::sort(names.begin(), names.end());
	jclass string_class = reinterpret_cast<jclass>(JniFindClass(env, "java/lang/String"));
	if (!string_class)
	{
		return NULL;
	}
	jobjectArray result = env->NewObjectArray(static_cast<jsize>(names.size()),
		string_class, NULL);
	if (!result)
	{
		return NULL;
	}
	for (size_t i = 0; i < names.size(); i++)
	{
		jstring name = env->NewStringUTF(names[i].c_str());
		if (name)
		{
			env->SetObjectArrayElement(result, static_cast<jsize>(i), name);
		}
	}
	return result;
}

// ---------------------------------------------------------------------------
// FileInputStream
// ---------------------------------------------------------------------------

jlong JNICALL Java_java_io_FileInputStream_open0(JNIEnv *env, jclass, jstring name)
{
	std::string path;
	const char *chars = name ? env->GetStringUTFChars(name, NULL) : NULL;
	if (chars)
	{
		path.assign(chars);
		env->ReleaseStringUTFChars(name, chars);
	}
	FILE *file = fopen(path.c_str(), "rb");
	return reinterpret_cast<jlong>(file);
}

jint JNICALL Java_java_io_FileInputStream_read0(JNIEnv *, jclass, jlong handle)
{
	FILE *file = reinterpret_cast<FILE*>(static_cast<intptr_t>(handle));
	if (!file)
	{
		return -1;
	}
	int value = fgetc(file);
	return value == EOF ? -1 : value;
}

jint JNICALL Java_java_io_FileInputStream_readBytes0(JNIEnv *env, jclass, jlong handle,
	jbyteArray buffer, jint offset, jint length)
{
	FILE *file = reinterpret_cast<FILE*>(static_cast<intptr_t>(handle));
	if (!file || !buffer || offset < 0 || length < 0)
	{
		return -1;
	}
	char *data = static_cast<char*>(env->GetArrayDirectAddress(buffer));
	if (!data)
	{
		return -1;
	}
	size_t read = fread(data + offset, 1, static_cast<size_t>(length), file);
	return static_cast<jint>(read);
}

void JNICALL Java_java_io_FileInputStream_close0(JNIEnv *, jclass, jlong handle)
{
	FILE *file = reinterpret_cast<FILE*>(static_cast<intptr_t>(handle));
	if (file)
	{
		fclose(file);
	}
}

// ---------------------------------------------------------------------------
// FileOutputStream
// ---------------------------------------------------------------------------

jlong JNICALL Java_java_io_FileOutputStream_open0(JNIEnv *env, jclass, jstring name,
	jboolean append)
{
	std::string path;
	const char *chars = name ? env->GetStringUTFChars(name, NULL) : NULL;
	if (chars)
	{
		path.assign(chars);
		env->ReleaseStringUTFChars(name, chars);
	}
	FILE *file = fopen(path.c_str(), append ? "ab" : "wb");
	return reinterpret_cast<jlong>(file);
}

void JNICALL Java_java_io_FileOutputStream_write0(JNIEnv *, jclass, jlong handle, jint value)
{
	FILE *file = reinterpret_cast<FILE*>(static_cast<intptr_t>(handle));
	if (file)
	{
		fputc(value & 0xFF, file);
	}
}

void JNICALL Java_java_io_FileOutputStream_writeBytes0(JNIEnv *env, jclass, jlong handle,
	jbyteArray buffer, jint offset, jint length)
{
	FILE *file = reinterpret_cast<FILE*>(static_cast<intptr_t>(handle));
	if (!file || !buffer || offset < 0 || length < 0)
	{
		return;
	}
	const char *data = static_cast<const char*>(env->GetArrayDirectAddress(buffer));
	if (!data)
	{
		return;
	}
	fwrite(data + offset, 1, static_cast<size_t>(length), file);
}

void JNICALL Java_java_io_FileOutputStream_flush0(JNIEnv *, jclass, jlong handle)
{
	FILE *file = reinterpret_cast<FILE*>(static_cast<intptr_t>(handle));
	if (file)
	{
		fflush(file);
	}
}

void JNICALL Java_java_io_FileOutputStream_close0(JNIEnv *, jclass, jlong handle)
{
	FILE *file = reinterpret_cast<FILE*>(static_cast<intptr_t>(handle));
	if (file)
	{
		fclose(file);
	}
}

} // extern "C"
