// java.util.zip.Inflater natives (raw DEFLATE via zlib).

#include "stdafx.h"
#include "jni_impl.h"

#include <cstring>
#include <vector>

#ifdef DARKS_JVM_HAVE_ZLIB
#include <zlib.h>
#endif

namespace {

#ifdef DARKS_JVM_HAVE_ZLIB
struct InflaterState
{
	z_stream stream;
	std::vector<unsigned char> input;
	bool initialized;
	bool stream_end;
};

InflaterState *State(jlong handle)
{
	return reinterpret_cast<InflaterState*>(static_cast<intptr_t>(handle));
}
#endif

} // namespace

extern "C" JNIEXPORT jlong JNICALL Java_java_util_zip_Inflater_init0(
	JNIEnv *, jclass, jboolean nowrap)
{
#ifdef DARKS_JVM_HAVE_ZLIB
	InflaterState *state = new InflaterState();
	memset(&state->stream, 0, sizeof(state->stream));
	int window_bits = nowrap ? -MAX_WBITS : MAX_WBITS;
	if (inflateInit2(&state->stream, window_bits) != Z_OK)
	{
		delete state;
		return 0;
	}
	state->initialized = true;
	state->stream_end = false;
	return reinterpret_cast<jlong>(state);
#else
	(void)nowrap;
	return 0;
#endif
}

extern "C" JNIEXPORT void JNICALL Java_java_util_zip_Inflater_setInput0(
	JNIEnv *env, jclass, jlong handle, jbyteArray input, jint offset, jint length)
{
#ifdef DARKS_JVM_HAVE_ZLIB
	InflaterState *state = State(handle);
	if (!state || !input || offset < 0 || length < 0)
	{
		return;
	}
	state->input.resize(static_cast<size_t>(length));
	if (length > 0)
	{
		env->GetByteArrayRegion(input, offset, length,
			reinterpret_cast<jbyte*>(&state->input[0]));
	}
	state->stream.next_in = state->input.empty() ? NULL : &state->input[0];
	state->stream.avail_in = static_cast<uInt>(length);
#else
	(void)env; (void)handle; (void)input; (void)offset; (void)length;
#endif
}

extern "C" JNIEXPORT jint JNICALL Java_java_util_zip_Inflater_inflate0(
	JNIEnv *env, jclass, jlong handle, jbyteArray output, jint offset, jint length)
{
#ifdef DARKS_JVM_HAVE_ZLIB
	InflaterState *state = State(handle);
	if (!state || !output || offset < 0 || length < 0)
	{
		return -1;
	}
	if (length == 0)
	{
		return 0;
	}
	std::vector<unsigned char> buffer(static_cast<size_t>(length));
	state->stream.next_out = &buffer[0];
	state->stream.avail_out = static_cast<uInt>(length);
	int rc = inflate(&state->stream, Z_NO_FLUSH);
	if (rc == Z_STREAM_END)
	{
		state->stream_end = true;
	}
	else if (rc != Z_OK && rc != Z_BUF_ERROR)
	{
		return -1;
	}
	int produced = length - static_cast<int>(state->stream.avail_out);
	if (produced > 0)
	{
		env->SetByteArrayRegion(output, offset, produced,
			reinterpret_cast<const jbyte*>(&buffer[0]));
	}
	return produced;
#else
	(void)env; (void)handle; (void)output; (void)offset; (void)length;
	return -1;
#endif
}

extern "C" JNIEXPORT jboolean JNICALL Java_java_util_zip_Inflater_needsInput0(
	JNIEnv *, jclass, jlong handle)
{
#ifdef DARKS_JVM_HAVE_ZLIB
	InflaterState *state = State(handle);
	return (state && state->stream.avail_in == 0) ? JNI_TRUE : JNI_FALSE;
#else
	(void)handle;
	return JNI_TRUE;
#endif
}

extern "C" JNIEXPORT jboolean JNICALL Java_java_util_zip_Inflater_needsDictionary0(
	JNIEnv *, jclass, jlong handle)
{
	(void)handle;
	return JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_java_util_zip_Inflater_finished0(
	JNIEnv *, jclass, jlong handle)
{
#ifdef DARKS_JVM_HAVE_ZLIB
	InflaterState *state = State(handle);
	return (state && state->stream_end) ? JNI_TRUE : JNI_FALSE;
#else
	(void)handle;
	return JNI_TRUE;
#endif
}

extern "C" JNIEXPORT jint JNICALL Java_java_util_zip_Inflater_remaining0(
	JNIEnv *, jclass, jlong handle)
{
#ifdef DARKS_JVM_HAVE_ZLIB
	InflaterState *state = State(handle);
	return state ? static_cast<jint>(state->stream.avail_in) : 0;
#else
	(void)handle;
	return 0;
#endif
}

extern "C" JNIEXPORT void JNICALL Java_java_util_zip_Inflater_reset0(
	JNIEnv *, jclass, jlong handle)
{
#ifdef DARKS_JVM_HAVE_ZLIB
	InflaterState *state = State(handle);
	if (state)
	{
		inflateReset(&state->stream);
		state->input.clear();
		state->stream_end = false;
	}
#else
	(void)handle;
#endif
}

extern "C" JNIEXPORT void JNICALL Java_java_util_zip_Inflater_end0(
	JNIEnv *, jclass, jlong handle)
{
#ifdef DARKS_JVM_HAVE_ZLIB
	InflaterState *state = State(handle);
	if (state)
	{
		inflateEnd(&state->stream);
		delete state;
	}
#else
	(void)handle;
#endif
}
