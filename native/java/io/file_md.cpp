// java.io.File / FileInputStream / FileOutputStream natives.

#include "stdafx.h"
#include "jni_impl.h"
#include "platform/platform.h"
#include "vm/executor/deterministic.h"

#include <algorithm>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>

#if defined(_WIN32)
#include <direct.h>
#include <io.h>
#include <sys/stat.h>
#include <sys/utime.h>
#include <windows.h>
#else
#include <dirent.h>
#include <sys/stat.h>
#include <utime.h>
#include <sys/statvfs.h>
#include <sys/types.h>
#include <unistd.h>
#endif

namespace {

#if defined(_WIN32)
// Paths reach the natives as UTF-8 (from JNI); the narrow Windows CRT/file
// APIs expect the ANSI code page, so use the wide variants throughout.
std::wstring WidePath(const std::string &path)
{
	std::vector<uint16_t> units = dk::Utf8ToUtf16(path);
	return std::wstring(units.begin(), units.end());
}
#endif

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
	if (_wstat64(WidePath(path).c_str(), &info) != 0)
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
	return _wmkdir(WidePath(path).c_str()) == 0;
#else
	return mkdir(path.c_str(), 0755) == 0;
#endif
}

std::vector<std::string> &DeleteOnExitPaths()
{
	static std::vector<std::string> paths;
	return paths;
}

bool delete_on_exit_registered = false;

// Space accounting for a path: total bytes, bytes free for unprivileged users
// (usable) and bytes free for privileged users. Returns false on failure, in
// which case every value stays zero.
bool StatSpace(const std::string &path, long long &total, long long &free, long long &usable)
{
	total = 0;
	free = 0;
	usable = 0;
#if defined(_WIN32)
	ULARGE_INTEGER available;
	ULARGE_INTEGER total_bytes;
	ULARGE_INTEGER free_bytes;
	if (!GetDiskFreeSpaceExW(WidePath(path).c_str(), &available, &total_bytes, &free_bytes))
	{
		return false;
	}
	total = static_cast<long long>(total_bytes.QuadPart);
	free = static_cast<long long>(free_bytes.QuadPart);
	usable = static_cast<long long>(available.QuadPart);
	return true;
#else
	struct statvfs info;
	if (statvfs(path.c_str(), &info) != 0)
	{
		return false;
	}
	unsigned long long unit = info.f_frsize ? info.f_frsize : info.f_bsize;
	total = static_cast<long long>(info.f_blocks * unit);
	free = static_cast<long long>(info.f_bfree * unit);
	usable = static_cast<long long>(info.f_bavail * unit);
	return true;
#endif
}

void DeleteOnExitFiles()
{
	std::vector<std::string> &paths = DeleteOnExitPaths();
	for (size_t i = 0; i < paths.size(); i++)
	{
		std::remove(paths[i].c_str());
	}
	paths.clear();
}

} // namespace

extern "C" {

JNIEXPORT jboolean JNICALL Java_java_io_File_exists0(JNIEnv *env, jobject thiz)
{
	bool is_file = false;
	bool is_dir = false;
	long long size = 0;
	return StatPath(FilePath(env, thiz), is_file, is_dir, size) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_java_io_File_isFile0(JNIEnv *env, jobject thiz)
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

JNIEXPORT jboolean JNICALL Java_java_io_File_isDirectory0(JNIEnv *env, jobject thiz)
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

JNIEXPORT jboolean JNICALL Java_java_io_File_canRead0(JNIEnv *env, jobject thiz)
{
	std::string path = FilePath(env, thiz);
#if defined(_WIN32)
	return GetFileAttributesW(WidePath(path).c_str()) != INVALID_FILE_ATTRIBUTES ? JNI_TRUE : JNI_FALSE;
#else
	return access(path.c_str(), R_OK) == 0 ? JNI_TRUE : JNI_FALSE;
#endif
}

JNIEXPORT jboolean JNICALL Java_java_io_File_canWrite0(JNIEnv *env, jobject thiz)
{
	std::string path = FilePath(env, thiz);
#if defined(_WIN32)
	return GetFileAttributesW(WidePath(path).c_str()) != INVALID_FILE_ATTRIBUTES ? JNI_TRUE : JNI_FALSE;
#else
	return access(path.c_str(), W_OK) == 0 ? JNI_TRUE : JNI_FALSE;
#endif
}

JNIEXPORT jboolean JNICALL Java_java_io_File_canExecute0(JNIEnv *env, jobject thiz)
{
	std::string path = FilePath(env, thiz);
#if defined(_WIN32)
	return GetFileAttributesW(WidePath(path).c_str()) != INVALID_FILE_ATTRIBUTES ? JNI_TRUE : JNI_FALSE;
#else
	return access(path.c_str(), X_OK) == 0 ? JNI_TRUE : JNI_FALSE;
#endif
}

JNIEXPORT jlong JNICALL Java_java_io_File_length0(JNIEnv *env, jobject thiz)
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

JNIEXPORT jlong JNICALL Java_java_io_File_getTotalSpace0(JNIEnv *env, jobject thiz)
{
	long long total = 0;
	long long free_bytes = 0;
	long long usable = 0;
	StatSpace(FilePath(env, thiz), total, free_bytes, usable);
	return static_cast<jlong>(total);
}

JNIEXPORT jlong JNICALL Java_java_io_File_getFreeSpace0(JNIEnv *env, jobject thiz)
{
	long long total = 0;
	long long free_bytes = 0;
	long long usable = 0;
	StatSpace(FilePath(env, thiz), total, free_bytes, usable);
	return static_cast<jlong>(free_bytes);
}

JNIEXPORT jlong JNICALL Java_java_io_File_getUsableSpace0(JNIEnv *env, jobject thiz)
{
	long long total = 0;
	long long free_bytes = 0;
	long long usable = 0;
	StatSpace(FilePath(env, thiz), total, free_bytes, usable);
	return static_cast<jlong>(usable);
}

JNIEXPORT jboolean JNICALL Java_java_io_File_delete0(JNIEnv *env, jobject thiz)
{
	std::string path = FilePath(env, thiz);
#if defined(_WIN32)
	if (RemoveDirectoryW(WidePath(path).c_str()))
	{
		return JNI_TRUE;
	}
	return DeleteFileW(WidePath(path).c_str()) ? JNI_TRUE : JNI_FALSE;
#else
	return remove(path.c_str()) == 0 ? JNI_TRUE : JNI_FALSE;
#endif
}

JNIEXPORT jboolean JNICALL Java_java_io_File_mkdirs0(JNIEnv *env, jobject thiz)
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

JNIEXPORT jboolean JNICALL Java_java_io_File_mkdir0(JNIEnv *env, jobject thiz)
{
	std::string path = FilePath(env, thiz);
	if (path.empty())
	{
		return JNI_FALSE;
	}
	if (!MakeDirectory(path))
	{
		bool is_file = false;
		bool is_dir = false;
		long long size = 0;
		return (StatPath(path, is_file, is_dir, size) && is_dir) ? JNI_TRUE : JNI_FALSE;
	}
	return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_java_io_File_rename0(JNIEnv *env, jobject thiz, jstring target)
{
	if (!target)
	{
		return JNI_FALSE;
	}
	std::string from = FilePath(env, thiz);
	const char *chars = env->GetStringUTFChars(target, NULL);
	if (!chars)
	{
		return JNI_FALSE;
	}
	std::string to(chars);
	env->ReleaseStringUTFChars(target, chars);
	return std::rename(from.c_str(), to.c_str()) == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_java_io_File_hidden0(JNIEnv *env, jobject thiz)
{
	std::string path = FilePath(env, thiz);
	if (path.empty())
	{
		return JNI_FALSE;
	}
#if defined(_WIN32)
	DWORD attrs = GetFileAttributesW(WidePath(path).c_str());
	if (attrs == INVALID_FILE_ATTRIBUTES)
	{
		return JNI_FALSE;
	}
	return (attrs & FILE_ATTRIBUTE_HIDDEN) ? JNI_TRUE : JNI_FALSE;
#else
	const char *slash = strrchr(path.c_str(), '/');
	const char *name = slash ? slash + 1 : path.c_str();
	return name[0] == '.' ? JNI_TRUE : JNI_FALSE;
#endif
}

JNIEXPORT jlong JNICALL Java_java_io_File_lastModified0(JNIEnv *env, jobject thiz)
{
	std::string path = FilePath(env, thiz);
#if defined(_WIN32)
	struct _stat64 st;
	if (_wstat64(WidePath(path).c_str(), &st) != 0)
	{
		return 0;
	}
#else
	struct stat st;
	if (::stat(path.c_str(), &st) != 0)
	{
		return 0;
	}
#endif
	return static_cast<jlong>(st.st_mtime) * 1000;
}

JNIEXPORT jboolean JNICALL Java_java_io_File_setLastModified0(JNIEnv *env, jobject thiz,
                                                    jlong millis)
{
	std::string path = FilePath(env, thiz);
	if (path.empty() || millis < 0)
	{
		return JNI_FALSE;
	}
#if defined(_WIN32)
	struct _utimbuf times;
	times.actime = static_cast<time_t>(millis / 1000);
	times.modtime = times.actime;
	return _wutime(WidePath(path).c_str(), &times) == 0 ? JNI_TRUE : JNI_FALSE;
#else
	struct utimbuf times;
	times.actime = static_cast<time_t>(millis / 1000);
	times.modtime = times.actime;
	return ::utime(path.c_str(), &times) == 0 ? JNI_TRUE : JNI_FALSE;
#endif
}

JNIEXPORT jboolean JNICALL Java_java_io_File_setPermission0(JNIEnv *env, jobject thiz,
                                                  jint bits, jboolean enable,
                                                  jboolean owner_only)
{
	std::string path = FilePath(env, thiz);
	if (path.empty())
	{
		return JNI_FALSE;
	}
#if defined(_WIN32)
	int mode = 0;
	if (bits & 4)
	{
		mode |= _S_IREAD;
	}
	if (bits & 2)
	{
		mode |= _S_IWRITE;
	}
	if ((bits & 1) != 0)
	{
		// Windows has no execute bit on regular files; report failure so
		// callers can fall back, matching File.setExecutable's contract.
		return JNI_FALSE;
	}
	return (enable ? _wchmod(WidePath(path).c_str(), mode) : 0) == 0 ? JNI_TRUE : JNI_FALSE;
#else
	struct stat st;
	if (::stat(path.c_str(), &st) != 0)
	{
		return JNI_FALSE;
	}
	mode_t mode = st.st_mode;
	mode_t owner = 0;
	mode_t group = 0;
	if (bits & 4)
	{
		owner |= S_IRUSR;
		group |= S_IRGRP | S_IROTH;
	}
	if (bits & 2)
	{
		owner |= S_IWUSR;
		group |= S_IWGRP | S_IWOTH;
	}
	if (bits & 1)
	{
		owner |= S_IXUSR;
		group |= S_IXGRP | S_IXOTH;
	}
	if (owner_only)
	{
		group = 0;
	}
	if (enable)
	{
		mode |= owner | group;
	}
	else
	{
		mode &= ~(owner | group);
	}
	return ::chmod(path.c_str(), mode) == 0 ? JNI_TRUE : JNI_FALSE;
#endif
}

JNIEXPORT jobjectArray JNICALL Java_java_io_File_list0(JNIEnv *env, jobject thiz)
{
	std::string path = FilePath(env, thiz);
	std::vector<std::string> names;
#if defined(_WIN32)
	WIN32_FIND_DATAW data;
	HANDLE handle = FindFirstFileW(WidePath(path + "\\*").c_str(), &data);
	if (handle == INVALID_HANDLE_VALUE)
	{
		return NULL;
	}
	do
	{
		std::wstring wide_name(data.cFileName);
		std::string name = dk::Utf16ToUtf8(
			reinterpret_cast<const uint16_t*>(wide_name.data()), wide_name.size());
		if (name != "." && name != "..")
		{
			names.push_back(name);
		}
	}
	while (FindNextFileW(handle, &data));
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

JNIEXPORT void JNICALL Java_java_io_File_deleteOnExit0(JNIEnv *env, jobject thiz)
{
	std::string path = FilePath(env, thiz);
	if (path.empty())
	{
		return;
	}
	if (!delete_on_exit_registered)
	{
		std::atexit(DeleteOnExitFiles);
		delete_on_exit_registered = true;
	}
	DeleteOnExitPaths().push_back(path);
}

// ---------------------------------------------------------------------------
// FileInputStream
// ---------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_java_io_FileInputStream_open0(JNIEnv *env, jclass, jstring name)
{
	// Real I/O is outside the deterministic model: mark the boundary so a
	// replay can show exactly where the run depended on the host.
	DarksDeterministic::Instance().Boundary("io.FileInputStream.open");
	std::string path;
	const char *chars = name ? env->GetStringUTFChars(name, NULL) : NULL;
	if (chars)
	{
		path.assign(chars);
		env->ReleaseStringUTFChars(name, chars);
	}
	FILE *file = dk::FopenUtf8(path, "rb");
	return reinterpret_cast<jlong>(file);
}

JNIEXPORT jint JNICALL Java_java_io_FileInputStream_read0(JNIEnv *, jclass, jlong handle)
{
	FILE *file = reinterpret_cast<FILE*>(static_cast<intptr_t>(handle));
	if (!file)
	{
		return -1;
	}
	int value = fgetc(file);
	return value == EOF ? -1 : value;
}

JNIEXPORT jint JNICALL Java_java_io_FileInputStream_readBytes0(JNIEnv *env, jclass, jlong handle,
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
	DarksDeterministic::Instance().Boundary("io.FileInputStream.read");
	size_t read = fread(data + offset, 1, static_cast<size_t>(length), file);
	return static_cast<jint>(read);
}

JNIEXPORT void JNICALL Java_java_io_FileInputStream_close0(JNIEnv *, jclass, jlong handle)
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

JNIEXPORT jlong JNICALL Java_java_io_FileOutputStream_open0(JNIEnv *env, jclass, jstring name,
	jboolean append)
{
	std::string path;
	const char *chars = name ? env->GetStringUTFChars(name, NULL) : NULL;
	if (chars)
	{
		path.assign(chars);
		env->ReleaseStringUTFChars(name, chars);
	}
	FILE *file = dk::FopenUtf8(path, append ? "ab" : "wb");
	return reinterpret_cast<jlong>(file);
}

JNIEXPORT void JNICALL Java_java_io_FileOutputStream_write0(JNIEnv *, jclass, jlong handle, jint value)
{
	FILE *file = reinterpret_cast<FILE*>(static_cast<intptr_t>(handle));
	if (file)
	{
		fputc(value & 0xFF, file);
	}
}

JNIEXPORT void JNICALL Java_java_io_FileOutputStream_writeBytes0(JNIEnv *env, jclass, jlong handle,
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
	DarksDeterministic::Instance().Boundary("io.FileOutputStream.write");
	fwrite(data + offset, 1, static_cast<size_t>(length), file);
}

JNIEXPORT void JNICALL Java_java_io_FileOutputStream_flush0(JNIEnv *, jclass, jlong handle)
{
	FILE *file = reinterpret_cast<FILE*>(static_cast<intptr_t>(handle));
	if (file)
	{
		fflush(file);
	}
}

JNIEXPORT void JNICALL Java_java_io_FileOutputStream_close0(JNIEnv *, jclass, jlong handle)
{
	FILE *file = reinterpret_cast<FILE*>(static_cast<intptr_t>(handle));
	if (file)
	{
		fclose(file);
	}
}

} // extern "C"
