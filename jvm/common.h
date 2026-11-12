#pragma once

#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <iostream>
#include <map>
#include <string>
#include <vector>

using namespace std;

#if defined(_WIN32)
#include <windows.h>
#ifndef JVM_EXPORTS
#define JVM_API __declspec(dllimport)
#else
#define JVM_API __declspec(dllexport)
#endif
#else
#define JVM_API __attribute__((visibility("default")))
#endif

#define C_JVM_API extern "C" JVM_API

#if !defined(_WIN32)
// Windows-style aliases used throughout the original sources. On Windows these
// come from windows.h, so they are only defined for the portable builds.
typedef char *LPSTR;
typedef const char *LPCSTR;
typedef unsigned char *LPUSTR;
typedef char CHAR;
typedef unsigned char UCHAR;
typedef UCHAR BYTE;
typedef BYTE *LPBYTE;
#endif

typedef int32_t INT32;
typedef uint32_t UINT32;
typedef short SHORT;
typedef unsigned short USHORT;
typedef int64_t LONG64;
typedef uint32_t ULONG;
typedef INT32 *LPINT32;
#ifndef _WIN32
typedef void *LPVOID;
#endif
