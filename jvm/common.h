#pragma once

#include <iostream>
#include <string>
#include <map>
#include <vector>
using namespace std;

#ifdef JVM_EXPORTS
#define JVM_API __declspec(dllexport)
#else
#define JVM_API __declspec(dllimport)
#endif

#define C_JVM_API extern "C" JVM_API

typedef char *LPSTR;
typedef const char *LPCSTR;
typedef unsigned char *LPUSTR;
typedef char CHAR;
typedef unsigned char UCHAR;
typedef UCHAR BYTE;
typedef BYTE *LPBYTE;
typedef int INT32;
typedef unsigned int UINT32;
typedef short SHORT;
typedef unsigned short USHORT;
typedef __int64 LONG64;
typedef unsigned long ULONG;
typedef INT32 *LPINT32;
