// java.lang.System natives: properties, environment, arraycopy, library
// loading and mapLibraryName.

#include "stdafx.h"
#include "system_md.h"
#include "jni_impl.h"
#include "env/env.h"
#include "vm/executor/context.h"
#include "vm/executor/objectheap.h"
#include "vm/executor/jobject.h"
#include "vm/executor/javaexception.h"
#include "platform/platform.h"

#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>

#if defined(_WIN32)
#include <windows.h>
#include <direct.h>
#else
#include <pwd.h>
#include <sys/utsname.h>
#include <unistd.h>
#if defined(__APPLE__)
#include <mach/host_info.h>
#include <mach/mach.h>
#include <mach/vm_statistics.h>
#include <sys/sysctl.h>
#endif
extern char **environ;
#endif

namespace {

std::string JStringToUtf8(JNIEnv *env, jstring str)
{
	if (!str)
	{
		return std::string();
	}
	const char *chars = env->GetStringUTFChars(str, NULL);
	if (!chars)
	{
		return std::string();
	}
	std::string result(chars);
	env->ReleaseStringUTFChars(str, chars);
	return result;
}

void ThrowByName(JNIEnv *env, const char *name, const std::string &message)
{
	jclass clazz = reinterpret_cast<jclass>(JniFindClass(env, name));
	if (clazz)
	{
		env->ThrowNew(clazz, message.c_str());
	}
}

std::string EnvOrEmpty(const char *name)
{
	const char *value = getenv(name);
	return value ? std::string(value) : std::string();
}

std::string CurrentDirectory()
{
#if defined(_WIN32)
	char buffer[_MAX_PATH];
	if (_getcwd(buffer, sizeof(buffer)))
	{
		return std::string(buffer);
	}
#else
	char buffer[4096];
	if (getcwd(buffer, sizeof(buffer)))
	{
		return std::string(buffer);
	}
#endif
	return std::string(".");
}

std::string UserName()
{
	std::string name = EnvOrEmpty("USER");
	if (name.empty())
	{
		name = EnvOrEmpty("LOGNAME");
	}
#if !defined(_WIN32)
	if (name.empty())
	{
		struct passwd *pw = getpwuid(getuid());
		if (pw && pw->pw_name)
		{
			name = pw->pw_name;
		}
	}
#endif
	return name.empty() ? std::string("unknown") : name;
}

std::string UserHome()
{
	std::string home = EnvOrEmpty("HOME");
#if defined(_WIN32)
	if (home.empty())
	{
		home = EnvOrEmpty("USERPROFILE");
	}
#else
	if (home.empty())
	{
		struct passwd *pw = getpwuid(getuid());
		if (pw && pw->pw_dir)
		{
			home = pw->pw_dir;
		}
	}
#endif
	return home;
}

std::string TempDirectory()
{
#if defined(_WIN32)
	std::string tmp = EnvOrEmpty("TEMP");
	if (tmp.empty())
	{
		tmp = EnvOrEmpty("TMP");
	}
	return tmp.empty() ? std::string(".") : tmp;
#else
	std::string tmp = EnvOrEmpty("TMPDIR");
	return tmp.empty() ? std::string("/tmp") : tmp;
#endif
}

std::string OsName()
{
#if defined(_WIN32)
	return "Windows";
#else
	struct utsname info;
	if (uname(&info) == 0 && info.sysname[0])
	{
		if (strcmp(info.sysname, "Darwin") == 0)
		{
			return "Mac OS X";
		}
		return info.sysname;
	}
	return "Unix";
#endif
}

std::string OsArch()
{
#if defined(_WIN32)
	SYSTEM_INFO info;
	GetNativeSystemInfo(&info);
	switch (info.wProcessorArchitecture)
	{
	case PROCESSOR_ARCHITECTURE_AMD64: return "amd64";
	case PROCESSOR_ARCHITECTURE_ARM64: return "aarch64";
	case PROCESSOR_ARCHITECTURE_INTEL: return "x86";
	default: return "unknown";
	}
#else
	struct utsname info;
	if (uname(&info) == 0 && info.machine[0])
	{
		return info.machine;
	}
	return "unknown";
#endif
}

std::string DefaultJavaHome()
{
	std::string home = EnvOrEmpty("JAVA_HOME");
	if (!home.empty())
	{
		return home;
	}
	std::string exe = dk::ExecutablePath();
	if (!exe.empty())
	{
		// <home>/bin/darks-jvm -> <home>
		return dk::DirectoryName(dk::DirectoryName(exe));
	}
	return CurrentDirectory();
}

void AppendPathList(std::string &out, std::vector<std::string> &seen,
                    const std::string &entry)
{
	if (entry.empty())
	{
		return;
	}
	for (size_t i = 0; i < seen.size(); i++)
	{
		if (seen[i] == entry)
		{
			return;
		}
	}
	seen.push_back(entry);
	if (!out.empty())
	{
		out.push_back(dk::PathListSeparator());
	}
	out += entry;
}

std::string DefaultLibraryPath()
{
	std::string out;
	std::vector<std::string> seen;
#if defined(_WIN32)
	std::string raw = EnvOrEmpty("PATH");
#elif defined(__APPLE__)
	std::string raw = EnvOrEmpty("DYLD_LIBRARY_PATH");
#else
	std::string raw = EnvOrEmpty("LD_LIBRARY_PATH");
#endif
	size_t start = 0;
	while (start <= raw.size())
	{
		size_t end = raw.find(dk::PathListSeparator(), start);
		if (end == std::string::npos)
		{
			end = raw.size();
		}
		if (end > start)
		{
			AppendPathList(out, seen, raw.substr(start, end - start));
		}
		start = end + 1;
	}
#if defined(_WIN32)
	// PATH already contains the conventional Windows library directories.
#elif defined(__APPLE__)
	AppendPathList(out, seen, "/usr/local/lib");
	AppendPathList(out, seen, "/opt/homebrew/lib");
	AppendPathList(out, seen, "/usr/lib");
	AppendPathList(out, seen, "/lib");
#else
	AppendPathList(out, seen, "/usr/local/lib");
	AppendPathList(out, seen, "/usr/lib");
	AppendPathList(out, seen, "/lib");
	AppendPathList(out, seen, "/usr/lib/x86_64-linux-gnu");
	AppendPathList(out, seen, "/usr/lib/aarch64-linux-gnu");
#endif
	return out;
}

std::string ClassPathValue()
{
	std::vector<std::string> classpath;
	Environment::GetClassPath(&classpath);
	std::string out;
	for (size_t i = 0; i < classpath.size(); i++)
	{
		if (i)
		{
			out.push_back(dk::PathListSeparator());
		}
		out += classpath[i];
	}
	return out;
}

void SeedDefaults()
{
	Environment::SetDefaultProperty("java.version", "11.0.2");
	Environment::SetDefaultProperty("java.specification.version", "11");
	Environment::SetDefaultProperty("java.vendor", "darks-jvm");
	Environment::SetDefaultProperty("java.vm.name", "darks-jvm");
	Environment::SetDefaultProperty("java.vm.version", "1.0");
	Environment::SetDefaultProperty("java.home", DefaultJavaHome());
	Environment::SetDefaultProperty("java.class.path", ClassPathValue());
	Environment::SetDefaultProperty("java.library.path", DefaultLibraryPath());
	Environment::SetDefaultProperty("os.name", OsName());
	Environment::SetDefaultProperty("os.arch", OsArch());
	Environment::SetDefaultProperty("file.separator",
#if defined(_WIN32)
		"\\"
#else
		"/"
#endif
	);
	Environment::SetDefaultProperty("path.separator", std::string(1, dk::PathListSeparator()));
	Environment::SetDefaultProperty("line.separator",
#if defined(_WIN32)
		"\r\n"
#else
		"\n"
#endif
	);
	Environment::SetDefaultProperty("user.dir", CurrentDirectory());
	Environment::SetDefaultProperty("user.home", UserHome());
	Environment::SetDefaultProperty("user.name", UserName());
	Environment::SetDefaultProperty("java.io.tmpdir", TempDirectory());
}

} // namespace

extern "C" {

/*
 * Class:     java_lang_System
 * Method:    registerNatives
 * Signature: ()V
 */
void JNICALL Java_java_lang_System_registerNatives
  (JNIEnv *, jclass)
{
	// The darks-jvm runtime binds its natives by JNI symbol name.  JDK-style
	// java.lang.System/Object classes call registerNatives from <clinit>;
	// accepting the call keeps those class libraries loadable.
}

void JNICALL Java_java_lang_System_setIn0
	(JNIEnv *, jclass, jobject)
{
}

void JNICALL Java_java_lang_System_setOut0
	(JNIEnv *, jclass, jobject)
{
}

void JNICALL Java_java_lang_System_setErr0
	(JNIEnv *, jclass, jobject)
{
}

jlong JNICALL Java_java_lang_System_currentTimeMillis
	(JNIEnv *, jclass)
{
	// Probe printed by the native method itself; the end-to-end test uses it
	// to prove that native dispatch reached this function.
	fprintf(stdout, "probe: entered native System.currentTimeMillis()\n");
	fflush(stdout);
	return OS::GetCurrentTimeMillis();
}

jlong JNICALL Java_java_lang_System_nanoTime
	(JNIEnv *, jclass)
{
	return static_cast<jlong>(dk::NanoTime());
}

static lpClass RuntimeArrayElement(lpObject array)
{
	if (!array || !array->obj_class || !array->obj_class->thisClass)
	{
		return NULL;
	}
	const char *name = array->obj_class->thisClass->GetName();
	if (!name || name[0] != '[')
	{
		return NULL;
	}
	const char *component = name + 1;
	std::string internal;
	if (component[0] == 'L' && component[strlen(component) - 1] == ';')
	{
		internal.assign(component + 1, strlen(component) - 2);
	}
	else if (component[0] == '[')
	{
		internal = component;
	}
	else
	{
		return NULL;
	}
	lpClass element = NULL;
	if (VMContext::bootstrap_classloader)
	{
		VMContext::bootstrap_classloader->GetLoadedClass(internal.c_str(), element);
	}
	return element;
}

static bool RuntimeValueAssignable(lpClass target, lpClass src)
{
	if (!target || !src)
	{
		return true;
	}
	if (ClassIsAssignable(target, src))
	{
		return true;
	}
	const char *tn = (target->thisClass && target->thisClass->GetName())
		? target->thisClass->GetName() : NULL;
	const char *sn = (src->thisClass && src->thisClass->GetName())
		? src->thisClass->GetName() : NULL;
	if (!tn || !sn)
	{
		return true;
	}
	if (sn[0] == '[' &&
	    (strcmp(tn, "java/lang/Object") == 0 ||
	     strcmp(tn, "java/lang/Cloneable") == 0 ||
	     strcmp(tn, "java/io/Serializable") == 0))
	{
		return true;
	}
	return strcmp(tn, sn) == 0;
}

void JNICALL Java_java_lang_System_arraycopy
	(JNIEnv *env, jclass, jobject src, jint src_pos, jobject dest, jint dest_pos, jint length)
{
	if (!src || !dest)
	{
		ThrowByName(env, "java/lang/NullPointerException", "arraycopy: source or destination is null");
		return;
	}
	jarray src_arg = (jarray) src;
	jarray dest_arg = (jarray) dest;
	int src_type_size = env->GetArrayTypeSize(src_arg);
	int dest_type_size = env->GetArrayTypeSize(dest_arg);
	if (src_type_size <= 0 || dest_type_size <= 0)
	{
		ThrowByName(env, "java/lang/ArrayStoreException", "arraycopy: argument is not an array");
		return;
	}
	if (src_type_size != dest_type_size)
	{
		ThrowByName(env, "java/lang/ArrayStoreException", "arraycopy: incompatible array types");
		return;
	}
	jsize src_length = env->GetArrayLength(src_arg);
	jsize dest_length = env->GetArrayLength(dest_arg);
	if (length < 0 || src_pos < 0 || dest_pos < 0 ||
	    src_pos > src_length - length || dest_pos > dest_length - length)
	{
		ThrowByName(env, "java/lang/IndexOutOfBoundsException", "arraycopy: index out of bounds");
		return;
	}
	if (length == 0)
	{
		return;
	}

	lpObject src_object = JniResolveObject(reinterpret_cast<jobject>(src));
	lpObject dest_object = JniResolveObject(reinterpret_cast<jobject>(dest));
	// Equal element widths are not enough for reference arrays: every copied
	// value must be assignable to the destination component type.
	if (src_object && dest_object &&
	    src_object->array_data_type == DT_REF &&
	    dest_object->array_data_type == DT_REF)
	{
		lpClass dest_element = RuntimeArrayElement(dest_object);
		if (dest_element)
		{
			jobject *src_refs = static_cast<jobject *>(src_object->array_var);
			for (jint i = 0; i < length; i++)
			{
				jobject value = src_refs[src_pos + i];
				if (!value)
				{
					continue;
				}
				lpObject value_object = JniResolveObject(value);
				if (value_object && !RuntimeValueAssignable(dest_element, value_object->obj_class))
				{
					ThrowByName(env, "java/lang/ArrayStoreException",
						"arraycopy: element type mismatch");
					return;
				}
			}
		}
	}

	char *src_data = (char*) env->GetArrayDirectAddress(src_arg);
	char *dest_data = (char*) env->GetArrayDirectAddress(dest_arg);
	if (!src_data || !dest_data)
	{
		ThrowByName(env, "java/lang/NullPointerException", "arraycopy: array data is unavailable");
		return;
	}
	// memmove is overlap-safe, which also covers arraycopy(a, 1, a, 0, n).
	memmove(dest_data + (size_t)dest_pos * dest_type_size,
	        src_data + (size_t)src_pos * src_type_size,
	        (size_t)length * src_type_size);
}

JNIEXPORT jint JNICALL Java_java_lang_Runtime_availableProcessors(JNIEnv *env, jobject thiz)
{
	(void)env;
	(void)thiz;
#if defined(_WIN32)
	SYSTEM_INFO info;
	GetSystemInfo(&info);
	return static_cast<jint>(info.dwNumberOfProcessors);
#else
	long cpus = sysconf(_SC_NPROCESSORS_ONLN);
	return cpus > 0 ? static_cast<jint>(cpus) : 1;
#endif
}

static jlong PhysicalMemory()
{
#if defined(_WIN32)
	MEMORYSTATUSEX status;
	status.dwLength = sizeof(status);
	if (GlobalMemoryStatusEx(&status))
	{
		return static_cast<jlong>(status.ullTotalPhys);
	}
	return 0;
#elif defined(__APPLE__)
	// macOS does not provide _SC_PHYS_PAGES; hw.memsize is the documented
	// physical memory size.
	uint64_t bytes = 0;
	size_t length = sizeof(bytes);
	if (sysctlbyname("hw.memsize", &bytes, &length, NULL, 0) == 0)
	{
		return static_cast<jlong>(bytes);
	}
	return 0;
#elif defined(__linux__) || defined(__FreeBSD__) || defined(__DragonFly__) || \
      defined(__NetBSD__) || defined(__OpenBSD__)
	long pages = sysconf(_SC_PHYS_PAGES);
	long page_size = sysconf(_SC_PAGESIZE);
	return (pages > 0 && page_size > 0)
		? static_cast<jlong>(pages) * static_cast<jlong>(page_size) : 0;
#else
	// Conservative fallback: unknown total memory.
	return 0;
#endif
}

static jlong AvailableMemory()
{
#if defined(_WIN32)
	MEMORYSTATUSEX status;
	status.dwLength = sizeof(status);
	if (GlobalMemoryStatusEx(&status))
	{
		return static_cast<jlong>(status.ullAvailPhys);
	}
	return 0;
#elif defined(__APPLE__)
	// macOS has no _SC_AVPHYS_PAGES.  Free plus inactive pages is the
	// conventional approximation of reclaimable memory.
	mach_port_t host = mach_host_self();
	if (host != MACH_PORT_NULL)
	{
		vm_statistics64_data_t vm_stat;
		mach_msg_type_number_t count = HOST_VM_INFO64_COUNT;
		if (host_statistics64(host, HOST_VM_INFO64,
		        reinterpret_cast<host_info64_t>(&vm_stat), &count) == KERN_SUCCESS)
		{
			long page_size = sysconf(_SC_PAGESIZE);
			uint64_t pages = static_cast<uint64_t>(vm_stat.free_count) +
			                 static_cast<uint64_t>(vm_stat.inactive_count);
			if (page_size > 0)
			{
				return static_cast<jlong>(pages * static_cast<uint64_t>(page_size));
			}
		}
	}
	return 0;
#elif defined(__linux__) || defined(__FreeBSD__) || defined(__DragonFly__) || \
      defined(__NetBSD__) || defined(__OpenBSD__)
	long pages = sysconf(_SC_AVPHYS_PAGES);
	long page_size = sysconf(_SC_PAGESIZE);
	return (pages > 0 && page_size > 0)
		? static_cast<jlong>(pages) * static_cast<jlong>(page_size) : 0;
#else
	// Conservative fallback: report total memory rather than an undefined
	// platform symbol.
	return PhysicalMemory();
#endif
}

JNIEXPORT jlong JNICALL Java_java_lang_Runtime_freeMemory(JNIEnv *env, jobject thiz)
{
	(void)env;
	(void)thiz;
	return AvailableMemory();
}

JNIEXPORT jlong JNICALL Java_java_lang_Runtime_totalMemory(JNIEnv *env, jobject thiz)
{
	(void)env;
	(void)thiz;
	return PhysicalMemory();
}

JNIEXPORT jlong JNICALL Java_java_lang_Runtime_maxMemory(JNIEnv *env, jobject thiz)
{
	(void)env;
	(void)thiz;
	return PhysicalMemory();
}

JNIEXPORT void JNICALL Java_java_lang_Runtime_gc(JNIEnv *env, jobject thiz)
{
	(void)env;
	(void)thiz;
	if (VMContext::object_heap)
	{
		VMContext::object_heap->gc();
	}
}

jobject JNICALL Java_java_lang_System_initProperties
	(JNIEnv *env, jclass, jobject target)
{
	SeedDefaults();
	if (!target)
	{
		return NULL;
	}
	jclass target_class = env->GetObjectClass(target);
	if (!target_class)
	{
		return target;
	}
	jmethodID setter = env->GetMethodID(target_class, "setProperty",
		"(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
	if (!setter)
	{
		return target;
	}
	const std::map<std::string, std::string> &properties = Environment::GetProperties();
	for (std::map<std::string, std::string>::const_iterator it = properties.begin();
	     it != properties.end(); ++it)
	{
		jstring key = env->NewStringUTF(it->first.c_str());
		jstring value = env->NewStringUTF(it->second.c_str());
		if (key && value)
		{
			env->CallObjectMethod(target, setter, key, value);
		}
	}
	return target;
}

jstring JNICALL Java_java_lang_System_mapLibraryName
	(JNIEnv *env, jclass, jstring name)
{
	if (!name)
	{
		return NULL;
	}
	std::string base = JStringToUtf8(env, name);
	std::string mapped = dk::MapLibraryName(base);
	return env->NewStringUTF(mapped.c_str());
}

jstring JNICALL Java_java_lang_System_getenv0
	(JNIEnv *env, jclass, jstring name)
{
	std::string key = JStringToUtf8(env, name);
	const char *value = getenv(key.c_str());
	if (!value)
	{
		return NULL;
	}
	return env->NewStringUTF(value);
}

void JNICALL Java_java_lang_System_loadLibrary0
	(JNIEnv *env, jclass, jstring name, jstring library_path)
{
	std::string library = JStringToUtf8(env, name);
	std::string path = JStringToUtf8(env, library_path);
	std::string mapped = dk::MapLibraryName(library);

	std::vector<std::string> directories;
	size_t start = 0;
	while (start <= path.size())
	{
		size_t end = path.find(dk::PathListSeparator(), start);
		if (end == std::string::npos)
		{
			end = path.size();
		}
		if (end > start)
		{
			directories.push_back(path.substr(start, end - start));
		}
		start = end + 1;
	}
	for (size_t i = 0; i < directories.size(); i++)
	{
		const std::string &dir = directories[i];
		std::vector<std::string> candidates;
		candidates.push_back(dk::JoinPath(dir, mapped));
		if (mapped != library)
		{
			candidates.push_back(dk::JoinPath(dir, library));
		}
		for (size_t j = 0; j < candidates.size(); j++)
		{
			if (DarksVM_LoadNativeLibraryPath(candidates[j].c_str()) == JNI_OK)
			{
				return;
			}
		}
	}
	// Fall back to the platform loader's own search path (RPATH, default
	// system directories, LD_LIBRARY_PATH).
	if (DarksVM_LoadNativeLibrary(library.c_str()) == JNI_OK)
	{
		return;
	}
	ThrowByName(env, "java/lang/UnsatisfiedLinkError",
		"no " + library + " in java.library.path: " + path);
}

} // extern "C"
