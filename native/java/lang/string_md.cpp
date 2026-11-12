#include "stdafx.h"
#include "string_md.h"
#include "platform/platform.h"

#include <string>
#include <vector>

jbyteArray JNICALL Java_java_lang_StringCoding_encode
	(JNIEnv *env, jclass clazz, jcharArray chararg, jint offset, jint length)
{
	(void)clazz;
	if (!chararg || offset < 0 || length < 0)
	{
		return NULL;
	}
	jsize array_length = env->GetArrayLength(chararg);
	if (offset + length > array_length)
	{
		return NULL;
	}
	std::vector<jchar> units(static_cast<size_t>(length));
	if (length > 0)
	{
		env->GetCharArrayRegion(chararg, offset, length, &units[0]);
	}
	std::vector<uint16_t> u16(units.begin(), units.end());
	std::string utf8 = dk::Utf16ToUtf8(u16);
	jbyteArray bytearg = env->NewByteArray(static_cast<jsize>(utf8.size()));
	if (!bytearg)
	{
		return NULL;
	}
	if (!utf8.empty())
	{
		env->SetByteArrayRegion(bytearg, 0, static_cast<jsize>(utf8.size()),
			reinterpret_cast<const jbyte *>(utf8.data()));
	}
	return bytearg;
}

jcharArray JNICALL Java_java_lang_StringCoding_decode
	(JNIEnv *env, jclass clazz, jbyteArray bytearg, jint offset, jint length)
{
	(void)clazz;
	if (!bytearg || offset < 0 || length < 0)
	{
		return NULL;
	}
	jsize array_length = env->GetArrayLength(bytearg);
	if (offset + length > array_length)
	{
		return NULL;
	}
	std::vector<jbyte> bytes(static_cast<size_t>(length > 0 ? length : 1));
	if (length > 0)
	{
		env->GetByteArrayRegion(bytearg, offset, length, &bytes[0]);
	}
	std::vector<uint16_t> units = dk::Utf8ToUtf16(
		reinterpret_cast<const char *>(bytes.data()), static_cast<std::size_t>(length));
	jcharArray result = env->NewCharArray(static_cast<jsize>(units.size()));
	if (!result)
	{
		return NULL;
	}
	if (!units.empty())
	{
		std::vector<jchar> chars(units.begin(), units.end());
		env->SetCharArrayRegion(result, 0, static_cast<jsize>(chars.size()), &chars[0]);
	}
	return result;
}
