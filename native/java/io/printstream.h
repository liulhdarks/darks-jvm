#pragma once
/* JNI bindings for java.io.PrintStream (the minimal runtime shipped with the
 * cross-platform tests). */
#include <jni.h>

#ifndef _Included_java_io_PrintStream
#define _Included_java_io_PrintStream
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_java_io_PrintStream_write0
  (JNIEnv *, jobject, jstring);

JNIEXPORT void JNICALL Java_java_io_PrintStream_println
  (JNIEnv *, jobject, jstring);

#ifdef __cplusplus
}
#endif
#endif
