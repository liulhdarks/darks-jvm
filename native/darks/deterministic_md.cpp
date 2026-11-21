// native methods of darks.Deterministic (deterministic execution hooks).

#include "stdafx.h"
#include "jni_impl.h"

#include "vm/executor/deterministic.h"
#include "platform/platform.h"

#include <string>

namespace {

DarksDeterministic &Det()
{
	return DarksDeterministic::Instance();
}

std::string JStringUtf8(JNIEnv *env, jstring str)
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

extern "C" {

JNIEXPORT jboolean JNICALL Java_darks_Deterministic_enabled(JNIEnv *, jclass)
{
	return Det().Enabled() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_darks_Deterministic_eventHash(JNIEnv *, jclass)
{
	return static_cast<jlong>(Det().EventHash());
}

JNIEXPORT jint JNICALL Java_darks_Deterministic_eventCount(JNIEnv *, jclass)
{
	return static_cast<jint>(Det().EventCount());
}

JNIEXPORT jlong JNICALL Java_darks_Deterministic_nanoTime(JNIEnv *, jclass)
{
	if (Det().Enabled())
	{
		return static_cast<jlong>(Det().NanoTime());
	}
	return static_cast<jlong>(dk::NanoTime());
}

JNIEXPORT jlong JNICALL Java_darks_Deterministic_currentTimeMillis(JNIEnv *, jclass)
{
	if (Det().Enabled())
	{
		return static_cast<jlong>(Det().CurrentTimeMillis());
	}
	return static_cast<jlong>(dk::CurrentTimeMillis());
}

JNIEXPORT jint JNICALL Java_darks_Deterministic_nextRandom(JNIEnv *, jclass, jint bits,
                                                           jint value)
{
	return static_cast<jint>(Det().NextRandom(static_cast<int>(bits),
	                                          static_cast<int>(value)));
}

JNIEXPORT void JNICALL Java_darks_Deterministic_boundary(JNIEnv *env, jclass, jstring what)
{
	std::string name = JStringUtf8(env, what);
	Det().Boundary(name.c_str());
}

} // extern "C"
