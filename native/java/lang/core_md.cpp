// Core java.lang natives: Object, Throwable, Math, Class and System extras.

#include "stdafx.h"
#include "jni_impl.h"

#include "platform/platform.h"
#include "env/env.h"

#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <map>
#include <string>
#include <vector>

namespace {

lpClass ClassPointer(jobject mirror)
{
	return reinterpret_cast<lpClass>(static_cast<intptr_t>(JniGetLongFieldByName(mirror, "vmClass")));
}

bool IsPrimitiveName(const char *name)
{
	if (!name)
	{
		return false;
	}
	static const char *kNames[] = {
		"boolean", "byte", "char", "short", "int", "long", "float",
		"double", "void", NULL
	};
	for (int i = 0; kNames[i]; i++)
	{
		if (strcmp(name, kNames[i]) == 0)
		{
			return true;
		}
	}
	return false;
}

// Primitive Class mirrors (Integer.TYPE, int.class, ...) have no class file.
// A tiny synthetic lpClass gives every existing reflection helper a stable
// internal name without allocating a real class.
typedef std::map<std::string, lpClass> PrimitiveClassCache;

PrimitiveClassCache *g_primitive_classes = NULL;

// The mirrors live for the whole process.  They are released from an exit
// handler so that leak checkers do not flag a process-lifetime cache as a leak
// (globals of this dlopen'ed library are not visible to those tools).
void FreePrimitiveClasses()
{
	if (!g_primitive_classes)
	{
		return;
	}
	for (PrimitiveClassCache::iterator it = g_primitive_classes->begin();
	     it != g_primitive_classes->end(); ++it)
	{
		lpClass clazz = it->second;
		if (clazz)
		{
			// Class does not own its thisClass; the synthetic mirror does.
			delete clazz->thisClass;
			clazz->thisClass = NULL;
			delete clazz;
		}
	}
	g_primitive_classes->clear();
}

lpClass PrimitiveClass(const std::string &name)
{
	if (!g_primitive_classes)
	{
		g_primitive_classes = new PrimitiveClassCache();
		atexit(FreePrimitiveClasses);
	}
	PrimitiveClassCache::iterator it = g_primitive_classes->find(name);
	if (it != g_primitive_classes->end())
	{
		return it->second;
	}
	lpClass clazz = new Class(CT_OBJECT);
	clazz->thisClass = new ClassConstant();
	clazz->thisClass->name = new CHAR[name.size() + 1];
	memcpy(clazz->thisClass->name, name.c_str(), name.size() + 1);
	clazz->accFlag = 0x0411; // public | final | abstract, like JVM mirrors
	(*g_primitive_classes)[name] = clazz;
	return clazz;
}

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

} // namespace

// ---------------------------------------------------------------------------
// java.lang.Object
// ---------------------------------------------------------------------------

extern "C" {

JNIEXPORT jint JNICALL Java_java_lang_Object_hashCode(JNIEnv *env, jobject thiz)
{
	(void)env;
	lpObject object = JniResolveObject(thiz);
	if (!object)
	{
		return 0;
	}
	// Stable identity hash derived from the heap index.
	uint32_t value = static_cast<uint32_t>(object->index);
	value ^= value >> 16;
	value *= 0x7feb352dU;
	value ^= value >> 15;
	return static_cast<jint>(value & 0x7fffffff);
}

JNIEXPORT jobject JNICALL Java_java_lang_Object_getClass(JNIEnv *env, jobject thiz)
{
	lpObject object = JniResolveObject(thiz);
	if (!object || !object->obj_class)
	{
		return NULL;
	}
	jobject mirror = JniClassMirror(reinterpret_cast<jclass>(object->obj_class));
	JniAddLocalRef(env, mirror);
	return mirror;
}

// ---------------------------------------------------------------------------
// java.lang.Throwable
// ---------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_java_lang_Throwable_printStackTrace0(JNIEnv *env, jobject thiz,
	jstring text)
{
	(void)thiz;
	std::string message = JStringToUtf8(env, text);
	fprintf(stderr, "%s\n\tat <darks-jvm native frame>\n", message.c_str());
	fflush(stderr);
}

// ---------------------------------------------------------------------------
// java.lang.Math
// ---------------------------------------------------------------------------

JNIEXPORT jdouble JNICALL Java_java_lang_Math_sqrt(JNIEnv *env, jclass clazz, jdouble value)
{
	(void)env;
	(void)clazz;
	return std::sqrt(value);
}

JNIEXPORT jdouble JNICALL Java_java_lang_Math_pow(JNIEnv *env, jclass clazz, jdouble base,
	jdouble exponent)
{
	(void)env;
	(void)clazz;
	return std::pow(base, exponent);
}

JNIEXPORT jdouble JNICALL Java_java_lang_Math_floor(JNIEnv *env, jclass clazz, jdouble value)
{
	(void)env;
	(void)clazz;
	return std::floor(value);
}

JNIEXPORT jdouble JNICALL Java_java_lang_Math_ceil(JNIEnv *env, jclass clazz, jdouble value)
{
	(void)env;
	(void)clazz;
	return std::ceil(value);
}

// ---------------------------------------------------------------------------
// java.lang.Class
// ---------------------------------------------------------------------------

JNIEXPORT jboolean JNICALL Java_java_lang_Class_isInterface(JNIEnv *env, jobject thiz)
{
	(void)env;
	lpClass clazz = ClassPointer(thiz);
	return (clazz && clazz->isInterface()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_java_lang_Class_isArray(JNIEnv *env, jobject thiz)
{
	(void)env;
	lpClass clazz = ClassPointer(thiz);
	if (!clazz)
	{
		return JNI_FALSE;
	}
	const char *name = clazz->thisClass ? clazz->thisClass->GetName() : NULL;
	if (name && name[0] == '[')
	{
		return JNI_TRUE;
	}
	return (clazz->class_type == CT_ARRAY) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_java_lang_Class_isPrimitive(JNIEnv *env, jobject thiz)
{
	(void)env;
	lpClass clazz = ClassPointer(thiz);
	const char *name = (clazz && clazz->thisClass) ? clazz->thisClass->GetName() : NULL;
	return IsPrimitiveName(name) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getPrimitiveClass(JNIEnv *env,
	jclass unused, jstring name)
{
	(void)unused;
	std::string primitive = JStringToUtf8(env, name);
	if (!IsPrimitiveName(primitive.c_str()))
	{
		return NULL;
	}
	jobject mirror = JniClassMirror(reinterpret_cast<jclass>(PrimitiveClass(primitive)));
	if (mirror)
	{
		JniAddLocalRef(env, mirror);
	}
	return mirror;
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getSuperclass(JNIEnv *env, jobject thiz)
{
	lpClass clazz = ClassPointer(thiz);
	if (!clazz || !clazz->super_class)
	{
		return NULL;
	}
	jobject mirror = JniClassMirror(reinterpret_cast<jclass>(clazz->super_class));
	JniAddLocalRef(env, mirror);
	return mirror;
}

JNIEXPORT jboolean JNICALL Java_java_lang_Class_isInstance(JNIEnv *env, jobject thiz,
	jobject obj)
{
	(void)env;
	lpClass clazz = ClassPointer(thiz);
	lpObject object = JniResolveObject(obj);
	if (!clazz || !object)
	{
		return JNI_FALSE;
	}
	return IsInstanceOfClass(object, clazz) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_forName(JNIEnv *env, jclass clazz,
	jstring name)
{
	(void)clazz;
	std::string requested = JStringToUtf8(env, name);
	lpClass found = JniFindClass(env, requested.c_str());
	if (!found)
	{
		jclass error_class = reinterpret_cast<jclass>(JniFindClass(env, "java/lang/ClassNotFoundException"));
		if (error_class)
		{
			env->ThrowNew(error_class, requested.c_str());
		}
		return NULL;
	}
	jobject mirror = JniClassMirror(reinterpret_cast<jclass>(found));
	JniAddLocalRef(env, mirror);
	return mirror;
}

// ---------------------------------------------------------------------------
// java.lang.Enum
// ---------------------------------------------------------------------------

JNIEXPORT jobject JNICALL Java_java_lang_Enum_valueOf(JNIEnv *env, jclass enum_class,
	jobject type, jstring name)
{
	(void)enum_class;
	lpClass clazz = ClassPointer(type);
	if (!clazz || !clazz->thisClass)
	{
		return NULL;
	}
	// The enum constants are static fields whose values are produced by the
	// generated <clinit>; make sure it has run before reading them.
	DarksVM_EnsureInitialized(clazz);
	std::string constant = JStringToUtf8(env, name);
	std::string desc = std::string("L") + clazz->thisClass->GetName() + ";";
	lpField field = JniFindField(clazz, constant.c_str(), desc.c_str(), true);
	jobject result = NULL;
	if (field)
	{
		lpFieldValue value = JniStaticFieldValue(clazz, field, false);
		if (value && value->field_value.heapPtr)
		{
			result = JniObject(value->field_value.heapPtr);
			JniAddLocalRef(env, result);
		}
	}
	if (!result)
	{
		jclass error_class = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/IllegalArgumentException"));
		if (error_class)
		{
			std::string message = "No enum constant " +
				std::string(clazz->thisClass->GetName()) + "." + constant;
			env->ThrowNew(error_class, message.c_str());
		}
	}
	return result;
}

// ---------------------------------------------------------------------------
// java.lang.System (extras)
// ---------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_java_lang_System_exit(JNIEnv *env, jclass clazz, jint code)
{
	(void)env;
	(void)clazz;
	fflush(stdout);
	fflush(stderr);
	exit(code);
}

JNIEXPORT jstring JNICALL Java_java_lang_System_getProperty(JNIEnv *env, jclass clazz,
	jstring key)
{
	(void)clazz;
	std::string name = JStringToUtf8(env, key);
	std::string value;
	bool found = true;
	if (name == "java.version")
	{
		value = "11.0.2";
	}
	else if (name == "java.specification.version")
	{
		value = "11";
	}
	else if (name == "java.vendor")
	{
		value = "darks-jvm";
	}
	else if (name == "java.vm.name")
	{
		value = "darks-jvm";
	}
	else if (name == "os.name")
	{
#if defined(_WIN32)
		value = "Windows";
#elif defined(__APPLE__)
		value = "Mac OS X";
#else
		value = "Linux";
#endif
	}
	else if (name == "file.separator")
	{
		value = std::string(1, '/');
	}
	else if (name == "path.separator")
	{
		value = std::string(1, dk::PathListSeparator());
	}
	else if (name == "line.separator")
	{
		value = "\n";
	}
	else if (name == "user.dir")
	{
		value = ".";
	}
	else if (name == "java.class.path")
	{
		std::vector<std::string> classpath;
		Environment::GetClassPath(&classpath);
		for (size_t i = 0; i < classpath.size(); i++)
		{
			if (i)
			{
				value.push_back(dk::PathListSeparator());
			}
			value += classpath[i];
		}
	}
	else
	{
		found = false;
	}
	if (!found)
	{
		return NULL;
	}
	return env->NewStringUTF(value.c_str());
}

JNIEXPORT jint JNICALL Java_java_lang_System_identityHashCode(JNIEnv *env, jclass clazz,
	jobject obj)
{
	(void)env;
	(void)clazz;
	lpObject object = JniResolveObject(obj);
	if (!object)
	{
		return 0;
	}
	uint32_t value = static_cast<uint32_t>(object->index);
	value ^= value >> 16;
	value *= 0x7feb352dU;
	value ^= value >> 15;
	return static_cast<jint>(value & 0x7fffffff);
}

JNIEXPORT void JNICALL Java_java_lang_System_loadLibrary(JNIEnv *env, jclass clazz,
	jstring name)
{
	(void)clazz;
	std::string library = JStringToUtf8(env, name);
	if (DarksVM_LoadNativeLibrary(library.c_str()) != JNI_OK)
	{
		jclass error_class = reinterpret_cast<jclass>(JniFindClass(env, "java/lang/UnsatisfiedLinkError"));
		if (error_class)
		{
			std::string message = "no " + library + " in java.library.path";
			env->ThrowNew(error_class, message.c_str());
		}
	}
}

JNIEXPORT void JNICALL Java_java_lang_System_load(JNIEnv *env, jclass clazz, jstring path)
{
	(void)clazz;
	std::string file = JStringToUtf8(env, path);
	if (DarksVM_LoadNativeLibraryPath(file.c_str()) != JNI_OK)
	{
		jclass error_class = reinterpret_cast<jclass>(JniFindClass(env, "java/lang/UnsatisfiedLinkError"));
		if (error_class)
		{
			std::string message = file + ": cannot open shared object file";
			env->ThrowNew(error_class, message.c_str());
		}
	}
}

} // extern "C"
