#pragma once

#include "vm/types.h"
#include "jobject.h"
#include "jclass.h"

class ObjectHeap
{

public:
	lpObject *objs;

	OBJECT_REF sequence;

public:
	ObjectHeap()
	{
		objs = NULL;
		sequence = 1;
	}

	~ObjectHeap()
	{
		if (objs)
		{
			delete[] objs;
			objs = NULL;
		}
		sequence = 0;
	}

	bool initialize(int init_size);

	void gc();
	
	bool NewObject(lpClass clazz, LPOBJECT_REF &object_ref);

	bool NewArrayObject(int size, lpClass clazz, LPOBJECT_REF &object_ref);

	bool NewObjectArrayObject(int size, lpClass clazz, LPOBJECT_REF &object_ref);

	bool NewMultiArrayObject(LPINT32 nums, int count, lpClass clazz, LPOBJECT_REF &object_ref);

	bool NewStringObject(LPSTR data, int length, LPOBJECT_REF &object_ref);

	bool NewStringObject(unsigned short*data, int length, LPOBJECT_REF &object_ref);

	bool GetObject(LPOBJECT_REF obj_ref, lpObject &obj)
	{
		if (!obj_ref)
		{
			obj = NULL;
			return false;
		}
		return GetObject(*obj_ref, obj);
	}

private:

	bool NewMultiArrayObject(lpObject parent, LPINT32 nums, int index, int count, lpClass clazz);

	bool GetObject(OBJECT_REF obj_ref, lpObject &obj);
};

typedef ObjectHeap *lpObjectHeap;