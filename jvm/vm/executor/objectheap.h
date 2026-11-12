#pragma once

#include "vm/types.h"
#include "jobject.h"
#include "jclass.h"
#include <vector>

class ObjectHeap
{

public:
	// Heap references are indices into this vector; slot 0 is unused so that a
	// zero reference means "null".
	std::vector<lpObject> objs;

	OBJECT_REF sequence;

public:
	ObjectHeap()
	{
		sequence = 1;
	}

	~ObjectHeap();

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

	bool Register(lpObject obj, LPOBJECT_REF &object_ref);

	// Creates one array dimension. level is the index into nums; the array
	// class name must contain (count - level) leading '[' characters.
	bool CreateArrayDimension(lpClass array_class, LPINT32 nums, int level, int count,
	                          LPOBJECT_REF &object_ref);

	bool GetObject(OBJECT_REF obj_ref, lpObject &obj);
};

typedef ObjectHeap *lpObjectHeap;
