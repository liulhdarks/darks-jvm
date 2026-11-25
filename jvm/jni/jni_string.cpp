// JNI string functions. java.lang.String stores its payload as the private
// byte[] field "value" plus a "coder" byte: 0 means LATIN1 (one byte per
// character), 1 means UTF16 (little-endian code units).

#include "stdafx.h"
#include "jni_impl.h"

#include "platform/platform.h"

#include <cstring>
#include <string>
#include <vector>

namespace {

// Reads the value:[B and coder:B fields and expands them into UTF-16 code
// units. Returns false when the reference is not a usable String.
bool ReadStringUnits(LPOBJECT_REF string_ref, std::vector<uint16_t> &units)
{
	units.clear();
	if (!string_ref || !VMContext::object_heap)
	{
		return false;
	}
	lpObject string_obj = NULL;
	if (!VMContext::object_heap->GetObject(string_ref, string_obj) || !string_obj ||
	    !string_obj->obj_class)
	{
		return false;
	}
	lpFieldValue value_fv = string_obj->GetFieldValueByName("value:[B");
	if (!value_fv || !value_fv->field_value.heapPtr)
	{
		return false;
	}
	lpObject bytes = NULL;
	if (!VMContext::object_heap->GetObject(value_fv->field_value.heapPtr, bytes) ||
	    !bytes || !bytes->array_var || bytes->array_length < 0)
	{
		return false;
	}
	lpFieldValue coder_fv = string_obj->GetFieldValueByName("coder:B");
	bool utf16 = coder_fv && (coder_fv->field_value.bValue & 1) != 0;
	const uint8_t *data = static_cast<const uint8_t *>(bytes->array_var);
	int length = bytes->array_length;
	if (!utf16)
	{
		units.reserve(static_cast<size_t>(length));
		for (int i = 0; i < length; i++)
		{
			units.push_back(data[i]);
		}
	}
	else
	{
		units.reserve(static_cast<size_t>(length / 2));
		for (int i = 0; i + 1 < length; i += 2)
		{
			units.push_back(static_cast<uint16_t>(data[i] | (data[i + 1] << 8)));
		}
	}
	return true;
}

std::string Utf8Of(jstring str, bool &ok)
{
	std::vector<uint16_t> units;
	ok = ReadStringUnits(JniRef(reinterpret_cast<jobject>(str)), units);
	if (!ok)
	{
		return std::string();
	}
	return dk::Utf16ToUtf8(units);
}

} // namespace

bool JniReadString(LPOBJECT_REF string_ref, std::vector<uint16_t> &units)
{
	return ReadStringUnits(string_ref, units);
}

// Fills *chars with a freshly allocated copy of the string contents; the
// caller releases it with JNI_ReleaseStringChars.
bool JniStringChars(LPOBJECT_REF string_ref, jchar **chars, jsize *length)
{
	std::vector<uint16_t> units;
	if (!ReadStringUnits(string_ref, units))
	{
		return false;
	}
	if (length)
	{
		*length = static_cast<jsize>(units.size());
	}
	if (chars)
	{
		jchar *copy = new jchar[units.empty() ? 1 : units.size()];
		for (size_t i = 0; i < units.size(); i++)
		{
			copy[i] = static_cast<jchar>(units[i]);
		}
		*chars = copy;
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
		*isCopy = JNI_TRUE;
	}
	return chars;
}

extern "C" JNIEXPORT void JNICALL JNI_ReleaseStringChars(JNIEnv *env, jstring str,
	const jchar *chars)
{
	(void)env;
	(void)str;
	delete[] chars;
}

extern "C" JNIEXPORT jsize JNICALL JNI_GetStringUTFLength(JNIEnv *env, jstring str)
{
	(void)env;
	bool ok = false;
	return static_cast<jsize>(Utf8Of(str, ok).size());
}

extern "C" JNIEXPORT const char * JNICALL JNI_GetStringUTFChars(JNIEnv *env, jstring str,
	jboolean *isCopy)
{
	(void)env;
	bool ok = false;
	std::string utf8 = Utf8Of(str, ok);
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
	if (!buf)
	{
		return;
	}
	std::vector<uint16_t> units;
	if (!ReadStringUnits(JniRef(reinterpret_cast<jobject>(str)), units))
	{
		return;
	}
	if (start < 0 || len < 0 || start + len > static_cast<jsize>(units.size()))
	{
		return;
	}
	for (jsize i = 0; i < len; i++)
	{
		buf[i] = static_cast<jchar>(units[start + i]);
	}
}

extern "C" JNIEXPORT void JNICALL JNI_GetStringUTFRegion(JNIEnv *env, jstring str,
	jsize start, jsize len, char *buf)
{
	(void)env;
	if (!buf)
	{
		return;
	}
	std::vector<uint16_t> units;
	if (!ReadStringUnits(JniRef(reinterpret_cast<jobject>(str)), units))
	{
		return;
	}
	if (start < 0 || len < 0 || start + len > static_cast<jsize>(units.size()))
	{
		return;
	}
	std::vector<uint16_t> slice(units.begin() + start, units.begin() + start + len);
	std::string utf8 = dk::Utf16ToUtf8(slice);
	memcpy(buf, utf8.data(), utf8.size());
	buf[utf8.size()] = '\0';
}

extern "C" JNIEXPORT const jchar * JNICALL JNI_GetStringCritical(JNIEnv *env, jstring str,
	jboolean *isCopy)
{
	return JNI_GetStringChars(env, str, isCopy);
}

extern "C" JNIEXPORT void JNICALL JNI_ReleaseStringCritical(JNIEnv *env, jstring str,
	const jchar *chars)
{
	JNI_ReleaseStringChars(env, str, chars);
}
