#pragma once

#include "vm/types.h"

typedef union Variant
{
	unsigned char blValue;
	wchar_t cValue;
	signed char bValue;
	short sValue;
	int iValue;
	float fValue;
	LPOBJECT_REF heapPtr;
	OBJECT_REF ptrValue; 
	BYTE bytes[4];

	LONG64 lValue;
	double dValue;

	Variant()
	{
		dValue = 0;
		iValue = 0;
	}

	Variant(LPBYTE bits, int size)
	{
		set(bits, size);
	}

	void set(LPBYTE bits, int size)
	{
		for (int i = 0; i < size; i++)
		{
			bytes[i] = bits[i];
		}
	}

} Slot, *lpSlot, *lpVariant;

typedef enum EventNotify
{
	EN_RETURN = 0, EN_KEEP, EN_POP, EN_EXCEPTION, EN_NOOP
} EventNotify;

typedef struct 
{
	lpThreadStack thread_stack;
} StackContext, *lpStackContext;
