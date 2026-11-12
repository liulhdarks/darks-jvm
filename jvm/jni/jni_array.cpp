// JNI array functions.

#include "stdafx.h"
#include "jni_impl.h"

#include <cstring>
#include <string>

namespace {

bool JniArrayInfo(jarray array, lpObject &obj, int &element_size)
{
	obj = JniResolveObject(reinterpret_cast<jobject>(array));
	if (!obj || !obj->array_var)
	{
		return false;
	}
	switch (obj->array_data_type)
	{
	case DT_BOOL:
		element_size = static_cast<int>(sizeof(jboolean));
		break;
	case DT_BYTE:
		element_size = static_cast<int>(sizeof(jbyte));
		break;
	case DT_CHAR:
		element_size = static_cast<int>(sizeof(jchar));
		break;
	case DT_SHORT:
		element_size = static_cast<int>(sizeof(jshort));
		break;
	case DT_INT:
		element_size = static_cast<int>(sizeof(jint));
		break;
	case DT_LONG:
		element_size = static_cast<int>(sizeof(jlong));
		break;
	case DT_FLOAT:
		element_size = static_cast<int>(sizeof(jfloat));
		break;
	case DT_DOUBLE:
		element_size = static_cast<int>(sizeof(jdouble));
		break;
	case DT_ARRAY:
	case DT_REF:
		element_size = static_cast<int>(sizeof(jobject));
		break;
	default:
		element_size = 1;
		break;
	}
	return true;
}

template <typename T>
jarray NewPrimitiveArray(const char *class_name, jsize length)
{
	lpClass clazz = NULL;
	if (!JniFindClass(NULL, class_name))
	{
		return NULL;
	}
	clazz = JniFindClass(NULL, class_name);
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewArrayObject(length, clazz, ref))
	{
		return NULL;
	}
	return reinterpret_cast<jarray>(JniObject(ref));
}

template <typename T>
T *ArrayElements(jarray array, jboolean *isCopy)
{
	lpObject obj = NULL;
	int size = 0;
	if (!JniArrayInfo(array, obj, size))
	{
		if (isCopy)
		{
			*isCopy = JNI_TRUE;
		}
		return NULL;
	}
	if (isCopy)
	{
		*isCopy = JNI_FALSE;
	}
	return static_cast<T *>(obj->array_var);
}

template <typename T>
void GetRegion(jarray array, jsize start, jsize len, T *buf)
{
	lpObject obj = NULL;
	int size = 0;
	if (!buf || !JniArrayInfo(array, obj, size))
	{
		return;
	}
	if (start < 0 || len < 0 || start + len > obj->array_length)
	{
		return;
	}
	memcpy(buf, static_cast<T *>(obj->array_var) + start,
		static_cast<size_t>(len) * sizeof(T));
}

template <typename T>
void SetRegion(jarray array, jsize start, jsize len, const T *buf)
{
	lpObject obj = NULL;
	int size = 0;
	if (!buf || !JniArrayInfo(array, obj, size))
	{
		return;
	}
	if (start < 0 || len < 0 || start + len > obj->array_length)
	{
		return;
	}
	memcpy(static_cast<T *>(obj->array_var) + start, buf,
		static_cast<size_t>(len) * sizeof(T));
}

} // namespace

extern "C" JNIEXPORT jsize JNICALL JNI_GetArrayLength(JNIEnv *env, jarray array)
{
	(void)env;
	lpObject obj = JniResolveObject(reinterpret_cast<jobject>(array));
	if (!obj)
	{
		return 0;
	}
	return static_cast<jsize>(obj->array_length);
}

extern "C" JNIEXPORT jobjectArray JNICALL JNI_NewObjectArray(JNIEnv *env, jsize length,
	jclass element_class, jobject initial)
{
	lpClass component = JniResolveClass(element_class);
	if (!component || !component->thisClass || !component->thisClass->GetName())
	{
		return NULL;
	}
	std::string name = component->thisClass->GetName();
	std::string array_name;
	if (!name.empty() && name[0] == '[')
	{
		array_name = "[" + name;
	}
	else
	{
		array_name = "[L" + name + ";";
	}
	lpClass array_class = JniFindClass(env, array_name.c_str());
	if (!array_class)
	{
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewObjectArrayObject(length, array_class, ref))
	{
		return NULL;
	}
	lpObject obj = NULL;
	if (VMContext::object_heap->GetObject(ref, obj) && obj && obj->array_var && initial)
	{
		jobject *elements = static_cast<jobject *>(obj->array_var);
		for (jsize i = 0; i < length; i++)
		{
			elements[i] = initial;
		}
	}
	jobject result = JniObject(ref);
	JniAddLocalRef(env, result);
	return reinterpret_cast<jobjectArray>(result);
}

extern "C" JNIEXPORT jobject JNICALL JNI_GetObjectArrayElement(JNIEnv *env, jobjectArray array,
	jsize index)
{
	lpObject obj = JniResolveObject(reinterpret_cast<jobject>(array));
	if (!obj || !obj->array_var || index < 0 || index >= obj->array_length)
	{
		return NULL;
	}
	jobject *elements = static_cast<jobject *>(obj->array_var);
	jobject result = elements[index];
	JniAddLocalRef(env, result);
	return result;
}

extern "C" JNIEXPORT void JNICALL JNI_SetObjectArrayElement(JNIEnv *env, jobjectArray array,
	jsize index, jobject value)
{
	(void)env;
	lpObject obj = JniResolveObject(reinterpret_cast<jobject>(array));
	if (!obj || !obj->array_var || index < 0 || index >= obj->array_length)
	{
		return;
	}
	jobject *elements = static_cast<jobject *>(obj->array_var);
	elements[index] = value;
}

extern "C" JNIEXPORT jbooleanArray JNICALL JNI_NewBooleanArray(JNIEnv *env, jsize length)
{
	jarray result = NewPrimitiveArray<jboolean>("[Z", length);
	JniAddLocalRef(env, reinterpret_cast<jobject>(result));
	return reinterpret_cast<jbooleanArray>(result);
}

extern "C" JNIEXPORT jbyteArray JNICALL JNI_NewByteArray(JNIEnv *env, jsize length)
{
	jarray result = NewPrimitiveArray<jbyte>("[B", length);
	JniAddLocalRef(env, reinterpret_cast<jobject>(result));
	return reinterpret_cast<jbyteArray>(result);
}

extern "C" JNIEXPORT jcharArray JNICALL JNI_NewCharArray(JNIEnv *env, jsize length)
{
	jarray result = NewPrimitiveArray<jchar>("[C", length);
	JniAddLocalRef(env, reinterpret_cast<jobject>(result));
	return reinterpret_cast<jcharArray>(result);
}

extern "C" JNIEXPORT jshortArray JNICALL JNI_NewShortArray(JNIEnv *env, jsize length)
{
	jarray result = NewPrimitiveArray<jshort>("[S", length);
	JniAddLocalRef(env, reinterpret_cast<jobject>(result));
	return reinterpret_cast<jshortArray>(result);
}

extern "C" JNIEXPORT jintArray JNICALL JNI_NewIntArray(JNIEnv *env, jsize length)
{
	jarray result = NewPrimitiveArray<jint>("[I", length);
	JniAddLocalRef(env, reinterpret_cast<jobject>(result));
	return reinterpret_cast<jintArray>(result);
}

extern "C" JNIEXPORT jlongArray JNICALL JNI_NewLongArray(JNIEnv *env, jsize length)
{
	jarray result = NewPrimitiveArray<jlong>("[J", length);
	JniAddLocalRef(env, reinterpret_cast<jobject>(result));
	return reinterpret_cast<jlongArray>(result);
}

extern "C" JNIEXPORT jfloatArray JNICALL JNI_NewFloatArray(JNIEnv *env, jsize length)
{
	jarray result = NewPrimitiveArray<jfloat>("[F", length);
	JniAddLocalRef(env, reinterpret_cast<jobject>(result));
	return reinterpret_cast<jfloatArray>(result);
}

extern "C" JNIEXPORT jdoubleArray JNICALL JNI_NewDoubleArray(JNIEnv *env, jsize length)
{
	jarray result = NewPrimitiveArray<jdouble>("[D", length);
	JniAddLocalRef(env, reinterpret_cast<jobject>(result));
	return reinterpret_cast<jdoubleArray>(result);
}

#define JNI_ARRAY_ELEMENTS(NAME, TYPE) \
extern "C" JNIEXPORT TYPE * JNICALL NAME(JNIEnv *env, jarray array, jboolean *isCopy) \
{ \
	(void)env; \
	return ArrayElements<TYPE>(array, isCopy); \
}

#define JNI_ARRAY_RELEASE(NAME, TYPE) \
extern "C" JNIEXPORT void JNICALL NAME(JNIEnv *env, jarray array, TYPE *elems, jint mode) \
{ \
	(void)env; (void)array; (void)elems; (void)mode; \
}

JNI_ARRAY_ELEMENTS(JNI_GetBooleanArrayElements, jboolean)
JNI_ARRAY_ELEMENTS(JNI_GetByteArrayElements, jbyte)
JNI_ARRAY_ELEMENTS(JNI_GetCharArrayElements, jchar)
JNI_ARRAY_ELEMENTS(JNI_GetShortArrayElements, jshort)
JNI_ARRAY_ELEMENTS(JNI_GetIntArrayElements, jint)
JNI_ARRAY_ELEMENTS(JNI_GetLongArrayElements, jlong)
JNI_ARRAY_ELEMENTS(JNI_GetFloatArrayElements, jfloat)
JNI_ARRAY_ELEMENTS(JNI_GetDoubleArrayElements, jdouble)

JNI_ARRAY_RELEASE(JNI_ReleaseBooleanArrayElements, jboolean)
JNI_ARRAY_RELEASE(JNI_ReleaseByteArrayElements, jbyte)
JNI_ARRAY_RELEASE(JNI_ReleaseCharArrayElements, jchar)
JNI_ARRAY_RELEASE(JNI_ReleaseShortArrayElements, jshort)
JNI_ARRAY_RELEASE(JNI_ReleaseIntArrayElements, jint)
JNI_ARRAY_RELEASE(JNI_ReleaseLongArrayElements, jlong)
JNI_ARRAY_RELEASE(JNI_ReleaseFloatArrayElements, jfloat)
JNI_ARRAY_RELEASE(JNI_ReleaseDoubleArrayElements, jdouble)

#define JNI_ARRAY_GET_REGION(NAME, TYPE) \
extern "C" JNIEXPORT void JNICALL NAME(JNIEnv *env, jarray array, jsize start, jsize len, TYPE *buf) \
{ \
	(void)env; \
	GetRegion<TYPE>(array, start, len, buf); \
}

#define JNI_ARRAY_SET_REGION(NAME, TYPE) \
extern "C" JNIEXPORT void JNICALL NAME(JNIEnv *env, jarray array, jsize start, jsize len, const TYPE *buf) \
{ \
	(void)env; \
	SetRegion<TYPE>(array, start, len, buf); \
}

JNI_ARRAY_GET_REGION(JNI_GetBooleanArrayRegion, jboolean)
JNI_ARRAY_GET_REGION(JNI_GetByteArrayRegion, jbyte)
JNI_ARRAY_GET_REGION(JNI_GetCharArrayRegion, jchar)
JNI_ARRAY_GET_REGION(JNI_GetShortArrayRegion, jshort)
JNI_ARRAY_GET_REGION(JNI_GetIntArrayRegion, jint)
JNI_ARRAY_GET_REGION(JNI_GetLongArrayRegion, jlong)
JNI_ARRAY_GET_REGION(JNI_GetFloatArrayRegion, jfloat)
JNI_ARRAY_GET_REGION(JNI_GetDoubleArrayRegion, jdouble)

JNI_ARRAY_SET_REGION(JNI_SetBooleanArrayRegion, jboolean)
JNI_ARRAY_SET_REGION(JNI_SetByteArrayRegion, jbyte)
JNI_ARRAY_SET_REGION(JNI_SetCharArrayRegion, jchar)
JNI_ARRAY_SET_REGION(JNI_SetShortArrayRegion, jshort)
JNI_ARRAY_SET_REGION(JNI_SetIntArrayRegion, jint)
JNI_ARRAY_SET_REGION(JNI_SetLongArrayRegion, jlong)
JNI_ARRAY_SET_REGION(JNI_SetFloatArrayRegion, jfloat)
JNI_ARRAY_SET_REGION(JNI_SetDoubleArrayRegion, jdouble)

extern "C" JNIEXPORT void * JNICALL JNI_GetPrimitiveArrayCritical(JNIEnv *env, jarray array,
	jboolean *isCopy)
{
	(void)env;
	lpObject obj = JniResolveObject(reinterpret_cast<jobject>(array));
	if (!obj)
	{
		return NULL;
	}
	if (isCopy)
	{
		*isCopy = JNI_FALSE;
	}
	return obj->array_var;
}

extern "C" JNIEXPORT void JNICALL JNI_ReleasePrimitiveArrayCritical(JNIEnv *env, jarray array,
	void *carray, jint mode)
{
	(void)env;
	(void)array;
	(void)carray;
	(void)mode;
}

// darks-jvm extensions used by the built-in runtime library.
extern "C" JNIEXPORT jint JNICALL JNI_GetArrayType(JNIEnv *env, jarray array)
{
	(void)env;
	lpObject obj = JniResolveObject(reinterpret_cast<jobject>(array));
	if (!obj)
	{
		return 0;
	}
	return static_cast<jint>(obj->array_data_type);
}

extern "C" JNIEXPORT void * JNICALL JNI_GetArrayDirectAddress(JNIEnv *env, jarray array)
{
	(void)env;
	lpObject obj = JniResolveObject(reinterpret_cast<jobject>(array));
	if (!obj)
	{
		return NULL;
	}
	return obj->array_var;
}

extern "C" JNIEXPORT jint JNICALL JNI_GetArrayTypeSize(JNIEnv *env, jarray array)
{
	(void)env;
	lpObject obj = JniResolveObject(reinterpret_cast<jobject>(array));
	if (!obj)
	{
		return 0;
	}
	lpObject ignored = NULL;
	int size = 0;
	if (!JniArrayInfo(array, ignored, size))
	{
		return 0;
	}
	(void)obj;
	return size;
}
