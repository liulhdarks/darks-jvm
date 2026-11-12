// JNI string functions. java.lang.String is a pure-Java class whose payload
// is the private char[] field "value".

#include "stdafx.h"
#include "jni_impl.h"

#include "platform/platform.h"

#include <cstring>
#include <string>
#include <vector>

namespace {

lpObject StringCharsObject(LPOBJECT_REF string_ref)
{
	lpObject string_obj = NULL;
	if (!string_ref || !VMContext::object_heap)
	{
		return NULL;
	}
	if (!VMContext::object_heap->GetObject(string_ref, string_obj) || !string_obj ||
	    !string_obj->obj_class)
	{
		return NULL;
	}
	std::map<std::string, lpField>::iterator it =
		string_obj->obj_class->fields_map.find("value:[C");
	if (it == string_obj->obj_class->fields_map.end())
	{
		return NULL;
	}
	lpFieldValue fv = string_obj->GetFieldValue(it->second);
	if (!fv || !fv->field_value.heapPtr)
	{
		return NULL;
	}
	lpObject chars = NULL;
	if (!VMContext::object_heap->GetObject(fv->field_value.heapPtr, chars))
	{
		return NULL;
	}
	return chars;
}

std::string Utf8Of(jstring str)
{
	lpObject chars = StringCharsObject(JniRef(reinterpret_cast<jobject>(str)));
	if (!chars || !chars->array_var)
	{
		return std::string();
	}
	const jchar *data = static_cast<const jchar *>(chars->array_var);
	std::vector<uint16_t> units(data, data + chars->array_length);
	return dk::Utf16ToUtf8(units);
}

} // namespace

bool JniStringChars(LPOBJECT_REF string_ref, jchar **chars, jsize *length)
{
	lpObject chars_obj = StringCharsObject(string_ref);
	if (!chars_obj || !chars_obj->array_var)
	{
		return false;
	}
	if (chars)
	{
		*chars = static_cast<jchar *>(chars_obj->array_var);
	}
	if (length)
	{
		*length = static_cast<jsize>(chars_obj->array_length);
	}
	return true;
}

jstring JniMakeString(JNIEnv *env, const char *utf8)
{
	if (!utf8)
	{
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewStringObject(const_cast<char *>(utf8),
			static_cast<int>(strlen(utf8)), ref))
	{
		return NULL;
	}
	jstring result = reinterpret_cast<jstring>(JniObject(ref));
	JniAddLocalRef(env, result);
	return result;
}

jstring JniMakeString(JNIEnv *env, const jchar *units, jsize length)
{
	if (!units || length < 0)
	{
		return NULL;
	}
	unsigned short *copy = new unsigned short[length > 0 ? length : 1];
	for (jsize i = 0; i < length; i++)
	{
		copy[i] = static_cast<unsigned short>(units[i]);
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewStringObject(copy, static_cast<int>(length), ref))
	{
		delete[] copy;
		return NULL;
	}
	jstring result = reinterpret_cast<jstring>(JniObject(ref));
	JniAddLocalRef(env, result);
	return result;
}

extern "C" JNIEXPORT jstring JNICALL JNI_NewString(JNIEnv *env, const jchar *unicode,
	jsize len)
{
	return JniMakeString(env, unicode, len);
}

extern "C" JNIEXPORT jstring JNICALL JNI_NewStringUTF(JNIEnv *env, const char *utf)
{
	return JniMakeString(env, utf);
}

extern "C" JNIEXPORT jsize JNICALL JNI_GetStringLength(JNIEnv *env, jstring str)
{
	(void)env;
	jsize length = 0;
	if (!JniStringChars(JniRef(reinterpret_cast<jobject>(str)), NULL, &length))
	{
		return 0;
	}
	return length;
}

extern "C" JNIEXPORT const jchar * JNICALL JNI_GetStringChars(JNIEnv *env, jstring str,
	jboolean *isCopy)
{
	(void)env;
	jchar *chars = NULL;
	if (!JniStringChars(JniRef(reinterpret_cast<jobject>(str)), &chars, NULL))
	{
		return NULL;
	}
	if (isCopy)
	{
		*isCopy = JNI_FALSE;
	}
	return chars;
}

extern "C" JNIEXPORT void JNICALL JNI_ReleaseStringChars(JNIEnv *env, jstring str,
	const jchar *chars)
{
	(void)env;
	(void)str;
	(void)chars;
}

extern "C" JNIEXPORT jsize JNICALL JNI_GetStringUTFLength(JNIEnv *env, jstring str)
{
	(void)env;
	return static_cast<jsize>(Utf8Of(str).size());
}

extern "C" JNIEXPORT const char * JNICALL JNI_GetStringUTFChars(JNIEnv *env, jstring str,
	jboolean *isCopy)
{
	(void)env;
	std::string utf8 = Utf8Of(str);
	char *buffer = new char[utf8.size() + 1];
	memcpy(buffer, utf8.c_str(), utf8.size() + 1);
	if (isCopy)
	{
		*isCopy = JNI_TRUE;
	}
	return buffer;
}

extern "C" JNIEXPORT void JNICALL JNI_ReleaseStringUTFChars(JNIEnv *env, jstring str,
	const char *chars)
{
	(void)env;
	(void)str;
	delete[] chars;
}

extern "C" JNIEXPORT void JNICALL JNI_GetStringRegion(JNIEnv *env, jstring str,
	jsize start, jsize len, jchar *buf)
{
	(void)env;
	jchar *chars = NULL;
	jsize length = 0;
	if (!buf || !JniStringChars(JniRef(reinterpret_cast<jobject>(str)), &chars, &length))
	{
		return;
	}
	if (start < 0 || len < 0 || start + len > length)
	{
		return;
	}
	memcpy(buf, chars + start, static_cast<size_t>(len) * sizeof(jchar));
}

extern "C" JNIEXPORT void JNICALL JNI_GetStringUTFRegion(JNIEnv *env, jstring str,
	jsize start, jsize len, char *buf)
{
	(void)env;
	jchar *chars = NULL;
	jsize length = 0;
	if (!buf || !JniStringChars(JniRef(reinterpret_cast<jobject>(str)), &chars, &length))
	{
		return;
	}
	if (start < 0 || len < 0 || start + len > length)
	{
		return;
	}
	std::vector<uint16_t> units(chars + start, chars + start + len);
	std::string utf8 = dk::Utf16ToUtf8(units);
	memcpy(buf, utf8.data(), utf8.size());
	buf[utf8.size()] = '\0';
}

extern "C" JNIEXPORT const jchar * JNICALL JNI_GetStringCritical(JNIEnv *env, jstring str,
	jboolean *isCopy)
{
	(void)env;
	return JNI_GetStringChars(env, str, isCopy);
}

extern "C" JNIEXPORT void JNICALL JNI_ReleaseStringCritical(JNIEnv *env, jstring str,
	const jchar *chars)
{
	JNI_ReleaseStringChars(env, str, chars);
}
