#pragma once

#include "common.h"

#define INVALID_OBJECT 0

// Fixed-width class file types: U8 used to be "unsigned long", which is 32-bit
// on Windows and 64-bit on LP64 platforms.
typedef uint8_t U1;
typedef uint16_t U2;
typedef uint32_t U4;
typedef uint64_t U8;

typedef enum ConstantEnum
{
	UTF8_CONSTANT = 1,
	INTEGER_CONSTANT = 3,
	FLOAT_CONSTANT, LONG_CONSTANT, DOUBLE_CONSTANT, 
	CLASS_CONSTANT, STRING_CONSTANT, FIELDREF_CONSTANT, 
	METHODREF_CONSTANT, INTERFACE_CONSTANT, NAMEANDTYPE_CONSTANT
} ConstantEnum;

typedef enum AccessFlag
{
	ACC_PUBLIC = 0x0001, 
	ACC_FINAL = 0x0010,
	ACC_INTERFACE = 0x0200, ACC_ABSTRACT = 0x0400,
	ACC_ANNOTATION = 0x2000, ACC_ENUM = 0x4000
} AccessFlag;

typedef enum AccessFieldFlag
{
	ACF_PUBLIC = 0x0001, ACF_PRIVATE = 0x0002, ACF_PROTECTED = 0x0004, 
	ACF_STATIC = 0x0008, ACF_FINAL = 0x0010, ACF_VOLATILE = 0x0040, 
	ACF_TRANSIENT = 0x0080, ACF_SYNTHETIC = 0x1000, ACF_ENUM = 0x4000,
} AccessFieldFlag;

typedef enum AccessMethodFlag
{
	ACM_PUBLIC = 0x0001, ACM_PRIVATE = 0x0002, ACM_PROTECTED = 0x0004, 
	ACM_STATIC = 0x0008, ACM_FINAL = 0x0010, ACM_SYNCHRONIZED = 0x0020, 
	ACM_BRIDGE = 0x0040, ACM_VARARGS = 0x0080, ACM_NATIVE = 0x0100,
	ACM_ABSTRACT = 0x0400, ACM_STRICT = 0x0800, ACM_SYNTHETIC = 0x1000,
} AccessMethodFlag;

typedef enum ClassType
{
	CT_OBJECT = 0, CT_ARRAY
} ClassType;

// Heap references are indices into ObjectHeap. Keep the width explicit so the
// value behaves identically on LP64 and LLP64 platforms.
typedef int64_t OBJECT_REF;
typedef OBJECT_REF *LPOBJECT_REF;

typedef union FloatByteValue
{
	float fValue;
	BYTE bytes[4];

	FloatByteValue(){}

	FloatByteValue(LPBYTE bits)
	{
		for (int i = 0; i < 4; i++)
		{
			bytes[i] = bits[i];
		}
	}

} FloatByteValue;

typedef union WByteValue
{
	LONG64 lValue;
	double dValue;
	BYTE bytes[8];

	WByteValue(){}

	WByteValue(LPBYTE bits1, LPBYTE bits2)
	{
		for (int i = 0; i < 4; i++)
		{
			bytes[i] = bits1[i];
			bytes[4 + i] = bits2[i];
		}
	}

} WByteValue;


class JObject;
typedef JObject *lpObject;
class JVM_API Class;
typedef Class *lpClass;
class JVM_API Field;
typedef Field *lpField;
class FieldValue;
typedef FieldValue *lpFieldValue;
class JVM_API Method;
typedef Method *lpMethod;

class JVM_API ThreadStack;
typedef ThreadStack *lpThreadStack;
typedef ThreadStack NativeStack;
typedef NativeStack *lpNativeStack;
class JVM_API StackFrame;
typedef StackFrame *lpStackFrame;
class ClassLoader;
typedef ClassLoader *lpClassLoader;

typedef void *JArray;
