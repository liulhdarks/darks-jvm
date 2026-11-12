/*
 * jni_md.h - machine dependent JNI types, portable replacement.
 */

#ifndef _JAVASOFT_JNI_MD_H_
#define _JAVASOFT_JNI_MD_H_

#include <cstdint>

#if defined(_WIN32)
#define JNIEXPORT __declspec(dllexport)
#define JNIIMPORT __declspec(dllimport)
#if defined(_M_IX86)
#define JNICALL __stdcall
#else
#define JNICALL
#endif
#else
#define JNIEXPORT __attribute__((visibility("default")))
#define JNIIMPORT
#define JNICALL
#endif

/* Fixed width on every platform (the original used long/__int64, which made
   jint 64-bit wide on LP64 systems). */
typedef int32_t jint;
typedef int64_t jlong;
typedef signed char jbyte;

#endif /* !_JAVASOFT_JNI_MD_H_ */
