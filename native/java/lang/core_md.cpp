// Core java.lang natives: Object, Throwable, Math, Class and System extras.

#include "stdafx.h"
#include "jni_impl.h"

#include "platform/platform.h"
#include "env/env.h"
#include "vm/executor/javaexception.h"
#include "vm/executor/threadstack.h"
#include "vm/executor/vmmutex.h"
#include "vm/exceptions.h"

#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <map>
#include <string>
#include <vector>

namespace {

std::map<lpClassLoader, jobject> &ManagedLoaders()
{
	static std::map<lpClassLoader, jobject> *loaders = new std::map<lpClassLoader, jobject>();
	return *loaders;
}

void ReleaseManagedLoaders()
{
	for (std::map<lpClassLoader, jobject>::iterator it = ManagedLoaders().begin();
	     it != ManagedLoaders().end(); ++it)
	{
		delete it->first;
	}
	ManagedLoaders().clear();
}

lpClass ClassPointer(jobject mirror)
{
	return reinterpret_cast<lpClass>(static_cast<intptr_t>(JniGetLongFieldByName(mirror, "vmClass")));
}

// Reports a failed superclass/superinterface resolution the way HotSpot does:
// NoClassDefFoundError whose cause is a ClassNotFoundException naming the
// missing type.
void ThrowClassLinkageError(ClassLinkageException *error)
{
	if (!error)
	{
		return;
	}
	std::string missing = error->missing;
	delete error;
	LPOBJECT_REF cause = CreateJavaException("java/lang/ClassNotFoundException",
	                                         missing.c_str());
	LPOBJECT_REF thrown = CreateJavaExceptionWithCause("java/lang/NoClassDefFoundError",
	                                                   missing.c_str(), cause);
	if (thrown)
	{
		DarksVM_SetPendingException(JniObject(thrown));
	}
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

// Reflection helpers live in a different translation unit but need the same
// synthetic primitive mirrors as Integer.TYPE / int.class.
lpClass DarksVM_GetPrimitiveClass(const char *name)
{
	return IsPrimitiveName(name) ? PrimitiveClass(name) : NULL;
}

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

// Shallow copy: a fresh instance of the same class with every instance field
// copied by value (object references stay shared).  Arrays copy their backing
// storage.  Mirrors Object.clone in the JDK, which is a native method there as
// well; several H2/Spring data structures rely on it.
JNIEXPORT jobject JNICALL Java_java_lang_Object_clone(JNIEnv *env, jobject thiz)
{
	lpObject source = JniResolveObject(thiz);
	if (!source || !source->obj_class || !VMContext::object_heap)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/CloneNotSupportedException", "null receiver");
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (source->array_var)
	{
		if (!VMContext::object_heap->NewObjectArrayObject(
			    source->array_length, source->obj_class, ref) ||
		    !ref)
		{
			return NULL;
		}
		lpObject copy = NULL;
		if (!VMContext::object_heap->GetObject(ref, copy) || !copy)
		{
			return NULL;
		}
		size_t width = 0;
		switch (source->array_data_type)
		{
		case DT_INT:
			width = sizeof(jint);
			break;
		case DT_SHORT:
			width = sizeof(jshort);
			break;
		case DT_CHAR:
			width = sizeof(jchar);
			break;
		case DT_BYTE:
		case DT_BOOL:
			width = sizeof(jbyte);
			break;
		case DT_LONG:
			width = sizeof(jlong);
			break;
		case DT_FLOAT:
			width = sizeof(jfloat);
			break;
		case DT_DOUBLE:
			width = sizeof(jdouble);
			break;
		case DT_ARRAY:
		case DT_REF:
			width = sizeof(jobject);
			break;
		default:
			break;
		}
		if (width > 0 && source->array_length > 0 && copy->array_var)
		{
			memcpy(copy->array_var, source->array_var,
			       width * static_cast<size_t>(source->array_length));
		}
		jobject result = JniObject(ref);
		JniAddLocalRef(env, result);
		return result;
	}

	lpClass cloneable = JniFindClass(env, "java/lang/Cloneable");
	if (!cloneable || !IsInstanceOfClass(source, cloneable))
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/CloneNotSupportedException",
			"class does not implement Cloneable");
		return NULL;
	}
	if (!VMContext::object_heap->NewObject(source->obj_class, ref) || !ref)
	{
		return NULL;
	}
	lpObject copy = NULL;
	if (!VMContext::object_heap->GetObject(ref, copy) || !copy)
	{
		return NULL;
	}
	size_t slots = source->instance_slots.size();
	if (copy->instance_slots.size() < slots)
	{
		slots = copy->instance_slots.size();
	}
	for (size_t i = 0; i < slots; i++)
	{
		if (source->instance_slots[i] && copy->instance_slots[i])
		{
			copy->instance_slots[i]->field_value =
				source->instance_slots[i]->field_value;
		}
	}
	jobject result = JniObject(ref);
	JniAddLocalRef(env, result);
	return result;
}

// ---------------------------------------------------------------------------
// java.lang.Throwable
// ---------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_java_lang_Throwable_printStackTrace0(JNIEnv *env, jobject thiz,
	jstring text)
{
	(void)thiz;
	std::string message = JStringToUtf8(env, text);
	fprintf(stderr, "%s", message.c_str());
	fflush(stderr);
}

JNIEXPORT jobjectArray JNICALL Java_java_lang_Throwable_captureStack0(JNIEnv *env,
	jclass)
{
	jclass string_class = reinterpret_cast<jclass>(JniFindClass(env, "java/lang/String"));
	if (!string_class)
	{
		return NULL;
	}
	std::vector<ThreadStack::FrameInfo> frames;
	lpThreadStack stack = env->thread_stack
		? reinterpret_cast<lpThreadStack>(env->thread_stack) : NULL;
	if (stack)
	{
		stack->DescribeJavaFrames(frames);
	}
	// Drop the exception construction chain: Throwable and its subclass
	// constructors end with fillInStackTrace, so the first frame created by
	// the throw site is the first non-constructor below them.
	std::vector<ThreadStack::FrameInfo> visible;
	for (size_t i = 0; i < frames.size(); i++)
	{
		const ThreadStack::FrameInfo &frame = frames[i];
		if (frame.declaring_class == "java.lang.Throwable" &&
		    (frame.method_name == "<init>" || frame.method_name == "fillInStackTrace"))
		{
			continue;
		}
		visible.push_back(frame);
	}
	while (!visible.empty() &&
	       (visible[0].method_name == "<init>" ||
	        visible[0].method_name == "fillInStackTrace"))
	{
		visible.erase(visible.begin());
	}
	jobjectArray result = env->NewObjectArray(static_cast<jsize>(visible.size()),
		string_class, NULL);
	if (!result)
	{
		return NULL;
	}
	for (size_t i = 0; i < visible.size(); i++)
	{
		char line[32];
		snprintf(line, sizeof(line), "%d", visible[i].line_number);
		std::string entry = visible[i].declaring_class + "\t" + visible[i].method_name +
			"\t" + visible[i].file_name + "\t" + line;
		jstring text = env->NewStringUTF(entry.c_str());
		if (text)
		{
			env->SetObjectArrayElement(result, static_cast<jsize>(i), text);
		}
	}
	return result;
}

// ---------------------------------------------------------------------------
// java.lang.Math
// ---------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_java_lang_Float_floatToRawIntBits(JNIEnv *, jclass, jfloat value)
{
	jint bits;
	memcpy(&bits, &value, sizeof(bits));
	return bits;
}

JNIEXPORT jfloat JNICALL Java_java_lang_Float_intBitsToFloat(JNIEnv *, jclass, jint bits)
{
	jfloat value;
	memcpy(&value, &bits, sizeof(value));
	return value;
}

JNIEXPORT jlong JNICALL Java_java_lang_Double_doubleToRawLongBits(JNIEnv *, jclass, jdouble value)
{
	jlong bits;
	memcpy(&bits, &value, sizeof(bits));
	return bits;
}

JNIEXPORT jdouble JNICALL Java_java_lang_Double_longBitsToDouble(JNIEnv *, jclass, jlong bits)
{
	jdouble value;
	memcpy(&value, &bits, sizeof(value));
	return value;
}

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

JNIEXPORT jboolean JNICALL Java_java_lang_Class_desiredAssertionStatus(JNIEnv *env,
                                                                     jobject thiz)
{
	(void)env;
	(void)thiz;
	return Environment::AssertionsEnabled() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_java_lang_Class_isPrimitive(JNIEnv *env, jobject thiz)
{
	(void)env;
	lpClass clazz = ClassPointer(thiz);
	const char *name = (clazz && clazz->thisClass) ? clazz->thisClass->GetName() : NULL;
	return IsPrimitiveName(name) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_java_lang_Class_isAnnotation(JNIEnv *env, jobject thiz)
{
	(void)env;
	lpClass clazz = ClassPointer(thiz);
	return (clazz && clazz->isAnnotation()) ? JNI_TRUE : JNI_FALSE;
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

JNIEXPORT jstring JNICALL Java_java_lang_Class_getSource0(JNIEnv *env, jobject thiz)
{
	lpClass clazz = ClassPointer(thiz);
	if (!clazz || !clazz->thisClass || !clazz->thisClass->GetName())
	{
		return NULL;
	}
	const char *name = clazz->thisClass->GetName();
	if (name[0] == '[')
	{
		return NULL;
	}
	std::vector<uint8_t> data;
	std::string source;
	if (!DarksVM_FindClassBytes(std::string(name), data, &source) || source.empty())
	{
		return NULL;
	}
	return env->NewStringUTF(source.c_str());
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getSuperclass(JNIEnv *env, jobject thiz)
{
	lpClass clazz = ClassPointer(thiz);
	// An interface's class file names java/lang/Object as its superclass, but
	// Class.getSuperclass() reports null for interfaces (JDK semantics).  The
	// generic view follows the same rule, and callers such as ByteBuddy rely on
	// the two being consistent.
	if (!clazz || !clazz->super_class || clazz->isInterface())
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

JNIEXPORT jobject JNICALL Java_java_lang_Class_getClassLoader0(JNIEnv *env,
	jobject thiz)
{
	lpClass clazz = ClassPointer(thiz);
	if (!clazz || !clazz->classloader || clazz->classloader == VMContext::bootstrap_classloader)
	{
		return NULL;
	}
	jobject loader = NULL;
	{
		VMLock lk;
		std::map<lpClassLoader, jobject>::iterator it =
			ManagedLoaders().find(clazz->classloader);
		if (it != ManagedLoaders().end())
		{
			loader = it->second;
		}
	}
	JniAddLocalRef(env, loader);
	return loader;
}

JNIEXPORT void JNICALL Java_java_lang_Class_initialize0(JNIEnv *, jclass, jobject mirror)
{
	lpClass clazz = ClassPointer(mirror);
	if (clazz)
	{
		DarksVM_EnsureInitialized(clazz);
	}
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_forName(JNIEnv *env, jclass clazz,
	jstring name)
{
	(void)clazz;
	std::string requested = JStringToUtf8(env, name);
	std::string internal = requested;
	for (size_t i = 0; i < internal.size(); i++)
	{
		if (internal[i] == '.')
		{
			internal[i] = '/';
		}
	}
	// Class.forName(String) resolves through the defining loader of the
	// calling class, so runtime-defined classes (cglib, proxies) are visible
	// by name to their own code.
	lpClassLoader loader = NULL;
	ThreadStack *stack = env ? static_cast<ThreadStack *>(env->thread_stack) : NULL;
	if (stack)
	{
		loader = stack->TopFrameClassLoader();
	}
	lpClass found = NULL;
	if (loader)
	{
		try
		{
			loader->FindClass(internal.c_str(), found);
		}
		catch (VMException *error)
		{
			delete error;
			found = NULL;
		}
		catch (...)
		{
			found = NULL;
		}
	}
	if (!found)
	{
		found = JniFindClass(env, requested.c_str());
	}
	if (!found)
	{
		jclass error_class = reinterpret_cast<jclass>(JniFindClass(env, "java/lang/ClassNotFoundException"));
		if (error_class)
		{
			env->ThrowNew(error_class, requested.c_str());
		}
		return NULL;
	}
	DarksVM_EnsureInitialized(found);
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

// Class/ClassLoader.getResourceAsStream support.  The Java side resolves
// relative names to an absolute class-path name and wraps these bytes in a
// ByteArrayInputStream.
static jbyteArray ResourceBytes(JNIEnv *env, jstring name)
{
	std::string resource = JStringToUtf8(env, name);
	std::vector<uint8_t> bytes;
	if (!DarksVM_FindResourceBytes(resource, bytes, NULL))
	{
		return NULL;
	}
	jbyteArray result = env->NewByteArray(static_cast<jsize>(bytes.size()));
	if (!result)
	{
		return NULL;
	}
	if (!bytes.empty())
	{
		env->SetByteArrayRegion(result, 0, static_cast<jsize>(bytes.size()),
			reinterpret_cast<const jbyte*>(bytes.data()));
	}
	return result;
}

JNIEXPORT jbyteArray JNICALL Java_java_lang_Class_getResourceBytes0(JNIEnv *env,
	jclass, jstring name)
{
	return ResourceBytes(env, name);
}

JNIEXPORT jbyteArray JNICALL Java_java_lang_ClassLoader_getResourceBytes0(JNIEnv *env,
	jclass, jstring name)
{
	return ResourceBytes(env, name);
}

// ---------------------------------------------------------------------------
// ClassLoader lite: custom loaders and class-path resources
// ---------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_java_lang_ClassLoader_createLoader0(JNIEnv *env, jobject thiz)
{
	static bool cleanup_registered = false;
	lpClassLoader loader = new ClassLoader(VMContext::bootstrap_classloader);
	if (!loader)
	{
		return 0;
	}
	jobject global = env->NewGlobalRef(thiz);
	{
		VMLock lk;
		ManagedLoaders()[loader] = global;
	}
	DarksVM_RegisterJavaClassLoader(loader, JniRef(global));
	if (!cleanup_registered)
	{
		cleanup_registered = true;
		atexit(ReleaseManagedLoaders);
	}
	return reinterpret_cast<jlong>(loader);
}

JNIEXPORT jobject JNICALL Java_java_lang_ClassLoader_defineClass0(JNIEnv *env,
	jclass, jlong handle, jstring name, jbyteArray bytes, jint offset, jint length)
{
	lpClassLoader loader = reinterpret_cast<lpClassLoader>(static_cast<intptr_t>(handle));
	(void)name;
	if (!loader || !bytes || offset < 0 || length <= 0)
	{
		return NULL;
	}
	jsize array_length = env->GetArrayLength(bytes);
	if (offset + length > array_length)
	{
		return NULL;
	}
	LPBYTE data = new BYTE[static_cast<size_t>(length)];
	env->GetByteArrayRegion(bytes, offset, length, reinterpret_cast<jbyte*>(data));
	lpClass clazz = NULL;
	bool defined = false;
	try
	{
		VMLock lk;
		defined = loader->DefineClass(data, static_cast<int>(length), clazz);
	}
	catch (VMException *e)
	{
		delete e;
		defined = false;
	}
	catch (const std::exception &e)
	{
		defined = false;
	}
	catch (...)
	{
		defined = false;
	}
	if (!defined || !clazz)
	{
		return NULL;
	}
	clazz->classloader = loader;
	try
	{
		loader->LinkClass(clazz);
	}
	catch (ClassLinkageException *error)
	{
		loader->DiscardClass(clazz);
		ThrowClassLinkageError(error);
		return NULL;
	}
	catch (...)
	{
		loader->DiscardClass(clazz);
		return NULL;
	}
	// Classes defined through a Java class loader (Spring Boot's nested jar
	// loader) must run <clinit> like classes resolved through the native
	// loader; otherwise static state stays at its default values.
	bool initialized = false;
	try
	{
		initialized = clazz->initialize(true);
	}
	catch (VMException *error)
	{
		delete error;
	}
	if (!initialized)
	{
		if (getenv("DARKS_JVM_TRACE_DEFINE") && clazz && clazz->thisClass && clazz->thisClass->GetName())
		{
			fprintf(stderr, "[trace defineClass0] failed to initialize %s\n", clazz->thisClass->GetName());
			if (VMContext::pending_exception)
			{
				PrintUncaughtException(VMContext::pending_exception);
			}
			fflush(stderr);
		}
		// Leave the initialization error pending so the Java frame that called
		// defineClass observes it; returning a mirror would let the caller
		// continue with a class whose static state is only half built.
		return NULL;
	}
	jobject mirror = JniClassMirror(reinterpret_cast<jclass>(clazz));
	JniAddLocalRef(env, mirror);
	return mirror;
}

JNIEXPORT jobject JNICALL Java_java_lang_ClassLoader_findLoadedClass0(JNIEnv *env,
	jclass, jlong handle, jstring name)
{
	lpClassLoader loader = reinterpret_cast<lpClassLoader>(static_cast<intptr_t>(handle));
	if (!loader || !name)
	{
		return NULL;
	}
	std::string requested = JStringToUtf8(env, name);
	for (size_t i = 0; i < requested.size(); i++)
	{
		if (requested[i] == '.')
		{
			requested[i] = '/';
		}
	}
	lpClass clazz = NULL;
	{
		VMLock lk;
		bool found = loader->GetLoadedClass(requested.c_str(), clazz) && clazz;
		if (!found)
		{
			return NULL;
		}
	}
	jobject mirror = JniClassMirror(reinterpret_cast<jclass>(clazz));
	if (mirror)
	{
		JniAddLocalRef(env, mirror);
	}
	return mirror;
}

JNIEXPORT jobject JNICALL Java_java_lang_ClassLoader_loadClass0(JNIEnv *env,
	jclass, jstring name)
{
	std::string requested = JStringToUtf8(env, name);
	// The Java API accepts binary names while the loader keys classes by
	// internal (slash) names; without this conversion a second lookup defines
	// the class again and the two Class mirrors stop being identical.
	std::string internal = requested;
	for (size_t i = 0; i < internal.size(); i++)
	{
		if (internal[i] == '.')
		{
			internal[i] = '/';
		}
	}
	lpClass clazz = NULL;
	try
	{
		// ClassLoader.loadClass links but does not initialize: <clinit> only
		// runs on first active use (or via Class.forName(..., true, ...)).
		if (!VMContext::bootstrap_classloader->FindClass(internal.c_str(), clazz, false))
		{
			return NULL;
		}
	}
	catch (ClassLinkageException *error)
	{
		ThrowClassLinkageError(error);
		return NULL;
	}
	catch (MissingClassException *error)
	{
		delete error;
		return NULL;
	}
	catch (VMException *error)
	{
		delete error;
		return NULL;
	}
	catch (...)
	{
		return NULL;
	}
	if (!clazz)
	{
		return NULL;
	}
	jobject mirror = JniClassMirror(reinterpret_cast<jclass>(clazz));
	JniAddLocalRef(env, mirror);
	return mirror;
}

JNIEXPORT jbyteArray JNICALL Java_java_lang_ClassLoader_findClassBytes0(JNIEnv *env,
	jclass, jstring name)
{
	std::string requested = JStringToUtf8(env, name);
	std::vector<uint8_t> bytes;
	if (!DarksVM_FindClassBytes(requested, bytes, NULL) || bytes.empty())
	{
		return NULL;
	}
	jbyteArray result = env->NewByteArray(static_cast<jsize>(bytes.size()));
	if (!result)
	{
		return NULL;
	}
	env->SetByteArrayRegion(result, 0, static_cast<jsize>(bytes.size()),
		reinterpret_cast<const jbyte*>(bytes.data()));
	return result;
}

JNIEXPORT jobjectArray JNICALL Java_java_lang_ClassLoader_resourceUrls0(JNIEnv *env,
	jclass, jstring name)
{
	jclass string_class = reinterpret_cast<jclass>(JniFindClass(env, "java/lang/String"));
	if (!string_class)
	{
		return NULL;
	}
	std::string requested = JStringToUtf8(env, name);
	std::vector<std::string> urls;
	DarksVM_FindResourceUrls(requested, urls);
	jobjectArray result = env->NewObjectArray(static_cast<jsize>(urls.size()),
		string_class, NULL);
	if (!result)
	{
		return NULL;
	}
	for (size_t i = 0; i < urls.size(); i++)
	{
		jstring text = env->NewStringUTF(urls[i].c_str());
		if (text)
		{
			env->SetObjectArrayElement(result, static_cast<jsize>(i), text);
		}
	}
	return result;
}

// JDK-style java.lang.Object.<clinit> calls registerNatives; accepting it
// keeps such class libraries loadable.  darks-jvm's own Object has no
// <clinit> and binds hashCode/getClass directly by JNI symbol name.
JNIEXPORT void JNICALL Java_java_lang_Object_registerNatives(JNIEnv *, jclass)
{
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
