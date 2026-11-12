#include "stdafx.h"
#include "string_md.h"
#include "platform/platform.h"
#include "jni/jni_external.h"
#include "vm/executor/context.h"

#include <string>
#include <vector>

jbyteArray JNICALL Java_java_lang_StringCoding_encode
	(JNIEnv *env, jclass clazz, jcharArray chararg, jint offset, jint length)
{
	if (!chararg || offset < 0 || length < 0)
	{
		return NULL;
	}
	lpObject chars_obj = GetVMObject(chararg);
	if (!chars_obj || !chars_obj->array_var ||
	    offset + length > chars_obj->array_length)
	{
		return NULL;
	}
	const jchar *chars = static_cast<const jchar*>(chars_obj->array_var) + offset;
	std::vector<uint16_t> units(chars, chars + length);
	std::string utf8 = dk::Utf16ToUtf8(units);
	jbyteArray bytearg = env->NewByteArray(static_cast<jsize>(utf8.size()));
	if (!bytearg)
	{
		return NULL;
	}
	env->SetByteArrayRegion(bytearg, 0, static_cast<jsize>(utf8.size()),
		reinterpret_cast<const jbyte*>(utf8.data()));
	return bytearg;
}

jcharArray JNICALL Java_java_lang_StringCoding_decode
	(JNIEnv *env, jclass clazz, jbyteArray bytearg, jint offset, jint length)
{
	if (!bytearg || offset < 0 || length < 0)
	{
		return NULL;
	}
	lpObject bytes_obj = GetVMObject((jobject)bytearg);
	if (!bytes_obj || !bytes_obj->array_var ||
	    offset + length > bytes_obj->array_length)
	{
		return NULL;
	}
	const jbyte *bytes = static_cast<const jbyte*>(bytes_obj->array_var) + offset;
	std::vector<uint16_t> units = dk::Utf8ToUtf16(
		reinterpret_cast<const char*>(bytes), static_cast<std::size_t>(length));

	lpClass char_array_class = NULL;
	if (!VMContext::bootstrap_classloader->FindClass("[C", char_array_class))
	{
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewArrayObject(static_cast<int>(units.size()),
		char_array_class, ref))
	{
		return NULL;
	}
	jcharArray result = (jcharArray)GetJniObject(ref);
	lpObject result_obj = GetVMObject((jobject)result);
	if (result_obj && result_obj->array_var && !units.empty())
	{
		memcpy(result_obj->array_var, units.data(), units.size() * sizeof(jchar));
	}
	return result;
}
