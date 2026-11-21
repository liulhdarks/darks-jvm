// java.util.zip.ZipFile natives backed by the VM's read-only zip reader.

#include "stdafx.h"
#include "jni_impl.h"
#include "vm/io/zipreader.h"

#include <string>
#include <vector>

namespace {

std::string JStringToUtf8(JNIEnv *env, jstring value)
{
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

ZipFile *ZipHandle(jlong handle)
{
	return reinterpret_cast<ZipFile*>(static_cast<intptr_t>(handle));
}

} // namespace

extern "C" JNIEXPORT jlong JNICALL Java_java_util_zip_ZipFile_open0(
	JNIEnv *env, jclass, jstring path)
{
	ZipFile *zip = new ZipFile();
	if (!zip->Open(JStringToUtf8(env, path)))
	{
		delete zip;
		return 0;
	}
	return reinterpret_cast<jlong>(zip);
}

extern "C" JNIEXPORT jlong JNICALL Java_java_util_zip_ZipFile_openMemory0(
	JNIEnv *env, jclass, jbyteArray bytes)
{
	if (!bytes)
	{
		return 0;
	}
	jsize length = env->GetArrayLength(bytes);
	std::vector<uint8_t> data(static_cast<size_t>(length));
	if (length > 0)
	{
		env->GetByteArrayRegion(bytes, 0, length,
			reinterpret_cast<jbyte*>(&data[0]));
	}
	ZipFile *zip = new ZipFile();
	if (!zip->OpenMemory(data.empty() ? NULL : &data[0], data.size()))
	{
		delete zip;
		return 0;
	}
	return reinterpret_cast<jlong>(zip);
}

extern "C" JNIEXPORT jobjectArray JNICALL Java_java_util_zip_ZipFile_names0(
	JNIEnv *env, jclass, jlong handle)
{
	ZipFile *zip = ZipHandle(handle);
	jclass string_class = reinterpret_cast<jclass>(JniFindClass(env, "java/lang/String"));
	if (!zip || !string_class)
	{
		return NULL;
	}
	const std::map<std::string, ZipFile::Entry> &entries = zip->Entries();
	jobjectArray result = env->NewObjectArray(
		static_cast<jsize>(entries.size()), string_class, NULL);
	if (!result)
	{
		return NULL;
	}
	jsize index = 0;
	for (std::map<std::string, ZipFile::Entry>::const_iterator it = entries.begin();
	     it != entries.end(); ++it)
	{
		jstring name = env->NewStringUTF(it->first.c_str());
		if (name)
		{
			env->SetObjectArrayElement(result, index, name);
		}
		index++;
	}
	return result;
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_java_util_zip_ZipFile_read0(
	JNIEnv *env, jclass, jlong handle, jstring name)
{
	ZipFile *zip = ZipHandle(handle);
	if (!zip)
	{
		return NULL;
	}
	std::vector<uint8_t> data;
	if (!zip->ReadEntry(JStringToUtf8(env, name), data))
	{
		return NULL;
	}
	jbyteArray result = env->NewByteArray(static_cast<jsize>(data.size()));
	if (!result)
	{
		return NULL;
	}
	if (!data.empty())
	{
		env->SetByteArrayRegion(result, 0, static_cast<jsize>(data.size()),
			reinterpret_cast<const jbyte*>(&data[0]));
	}
	return result;
}

extern "C" JNIEXPORT jlong JNICALL Java_java_util_zip_ZipFile_size0(
	JNIEnv *env, jclass, jlong handle, jstring name)
{
	ZipFile *zip = ZipHandle(handle);
	if (!zip)
	{
		return -1;
	}
	const std::map<std::string, ZipFile::Entry> &entries = zip->Entries();
	std::map<std::string, ZipFile::Entry>::const_iterator it =
		entries.find(JStringToUtf8(env, name));
	return it == entries.end() ? -1 : static_cast<jlong>(it->second.uncompressed_size);
}

extern "C" JNIEXPORT jlong JNICALL Java_java_util_zip_ZipFile_compressedSize0(
	JNIEnv *env, jclass, jlong handle, jstring name)
{
	ZipFile *zip = ZipHandle(handle);
	if (!zip)
	{
		return -1;
	}
	const std::map<std::string, ZipFile::Entry> &entries = zip->Entries();
	std::map<std::string, ZipFile::Entry>::const_iterator it =
		entries.find(JStringToUtf8(env, name));
	return it == entries.end() ? -1 : static_cast<jlong>(it->second.compressed_size);
}

extern "C" JNIEXPORT jint JNICALL Java_java_util_zip_ZipFile_method0(
	JNIEnv *env, jclass, jlong handle, jstring name)
{
	ZipFile *zip = ZipHandle(handle);
	if (!zip)
	{
		return -1;
	}
	const std::map<std::string, ZipFile::Entry> &entries = zip->Entries();
	std::map<std::string, ZipFile::Entry>::const_iterator it =
		entries.find(JStringToUtf8(env, name));
	return it == entries.end() ? -1 : static_cast<jint>(it->second.method);
}

extern "C" JNIEXPORT void JNICALL Java_java_util_zip_ZipFile_close0(
	JNIEnv *, jclass, jlong handle)
{
	ZipFile *zip = ZipHandle(handle);
	if (zip)
	{
		delete zip;
	}
}
