#include "stdafx.h"
#include "objectheap.h"
#include "context.h"
#include "utils/CommonUtils.h"
#include "methodarea.h"
#include "executor.h"
#include "vm/exceptions.h"

#include <string>

ObjectHeap::~ObjectHeap()
{
	for (size_t i = 0; i < objs.size(); i++)
	{
		if (objs[i])
		{
			delete objs[i];
			objs[i] = NULL;
		}
	}
	objs.clear();
	sequence = 0;
}

bool ObjectHeap::initialize(int init_size)
{
	if (init_size < 1)
	{
		init_size = 1;
	}
	objs.assign(static_cast<size_t>(init_size), NULL);
	sequence = 1;
	return true;
}

void ObjectHeap::gc()
{
	// There is no mark-and-sweep collector yet; objects live until the heap is
	// destroyed.
}

bool ObjectHeap::Register(lpObject obj, LPOBJECT_REF &object_ref)
{
	if (!obj)
	{
		return false;
	}
	obj->index = sequence++;
	if (static_cast<size_t>(obj->index) >= objs.size())
	{
		objs.resize(static_cast<size_t>(obj->index) + 1, NULL);
	}
	objs[static_cast<size_t>(obj->index)] = obj;
	object_ref = &obj->index;
	return true;
}

bool ObjectHeap::NewObject(lpClass clazz, LPOBJECT_REF &object_ref)
{
	lpObject obj = new JObject(clazz);
	if (!obj->newInstance())
	{
		delete obj;
		return false;
	}
	return Register(obj, object_ref);
}

bool ObjectHeap::NewArrayObject(int size, lpClass clazz, LPOBJECT_REF &object_ref)
{
	if (size < 0)
	{
		return false;
	}
	lpObject obj = new JObject(clazz);
	LPOBJECT_REF ref = NULL;
	if (!Register(obj, ref) || !obj->newArrayInstance(size))
	{
		objs[static_cast<size_t>(obj->index)] = NULL;
		delete obj;
		return false;
	}
	object_ref = ref;
	return true;
}

bool ObjectHeap::NewObjectArrayObject(int size, lpClass clazz, LPOBJECT_REF &object_ref)
{
	if (size < 0)
	{
		return false;
	}
	lpObject obj = new JObject(clazz);
	LPOBJECT_REF ref = NULL;
	if (!Register(obj, ref) || !obj->newArrayInstance(DT_REF, size))
	{
		objs[static_cast<size_t>(obj->index)] = NULL;
		delete obj;
		return false;
	}
	object_ref = ref;
	return true;
}

bool ObjectHeap::CreateArrayDimension(lpClass array_class, LPINT32 nums, int level, int count,
                                      LPOBJECT_REF &object_ref)
{
	if (!array_class || !array_class->thisClass || !nums || level < 0 || level >= count)
	{
		return false;
	}
	lpObject obj = new JObject(array_class);
	LPOBJECT_REF ref = NULL;
	if (!Register(obj, ref))
	{
		delete obj;
		return false;
	}
	object_ref = ref;

	int length = nums[level];
	if (level + 1 == count)
	{
		// Innermost dimension: the array class name is a one-dimensional
		// descriptor such as "[I"; JObject infers the element type from it.
		if (!obj->newArrayInstance(length))
		{
			return false;
		}
		return true;
	}

	if (!obj->newArrayInstance(DT_REF, length))
	{
		return false;
	}
	const char *name = array_class->thisClass->GetName();
	if (!name || name[0] != '[')
	{
		return false;
	}
	lpClass child_class = NULL;
	if (!VMContext::bootstrap_classloader->FindClass(name + 1, child_class))
	{
		return false;
	}
	jobject *refs = static_cast<jobject*>(obj->array_var);
	for (int i = 0; i < length; i++)
	{
		LPOBJECT_REF child_ref = NULL;
		if (!CreateArrayDimension(child_class, nums, level + 1, count, child_ref))
		{
			return false;
		}
		refs[i] = reinterpret_cast<jobject>(child_ref);
	}
	return true;
}

bool ObjectHeap::NewMultiArrayObject(LPINT32 nums, int count, lpClass clazz, LPOBJECT_REF &object_ref)
{
	if (!nums || count <= 0 || !clazz)
	{
		return false;
	}
	for (int i = 0; i < count; i++)
	{
		if (nums[i] < 0)
		{
			return false;
		}
	}
	return CreateArrayDimension(clazz, nums, 0, count, object_ref);
}

// data is a UTF-8 byte string.
bool ObjectHeap::NewStringObject(LPSTR data, int length, LPOBJECT_REF &object_ref)
{
	int unicode_len = 0;
	unsigned short* unicodes = CommonUtils::Utf8ToUnicode(data, unicode_len);
	if (!unicodes)
	{
		return false;
	}
	// The UTF-16 code unit count is authoritative; the UTF-8 byte length is
	// not a valid character count.
	(void)length;
	return NewStringObject(unicodes, unicode_len, object_ref);
}

// data is a UTF-16 code unit buffer; ownership is transferred to the char[]
// object. length is the number of code units.
bool ObjectHeap::NewStringObject(unsigned short *data, int length, LPOBJECT_REF &object_ref)
{
	bool transferred = false;
	try
	{
		lpClass data_clazz = NULL;
		if (!VMContext::bootstrap_classloader->FindClass("[C", data_clazz))
		{
			throw new VMException("Fail to load char array class.");
		}
		LPOBJECT_REF chars_ref = NULL;
		if (!NewArrayObject(length, data_clazz, chars_ref))
		{
			throw new VMException("Fail to create char array.");
		}
		lpObject chars_obj = NULL;
		if (!GetObject(chars_ref, chars_obj))
		{
			throw new VMException("Fail to get char array direct object.");
		}
		chars_obj->SetData(DT_CHAR, (LPVOID)data, length);
		transferred = true;

		lpClass str_class = NULL;
		if (!VMContext::bootstrap_classloader->FindClass("java/lang/String", str_class))
		{
			throw new VMException("Fail to load string class.");
		}
		if (!NewObject(str_class, object_ref))
		{
			throw new VMException("Fail to new string object.");
		}
		lpMethod method = MethodArea::GetMethod(str_class, "<init>:([C)V");
		if (!method)
		{
			throw new VMException("Fail to find string init method.");
		}
		lpVariant vars = new Variant[2];
		vars[0].heapPtr = object_ref;
		vars[1].heapPtr = chars_ref;
		lpVMExecutor exec = new VMExecutor();
		exec->Initialize();
		exec->Execute(str_class, method, vars, 2);
		delete[] vars;
		delete exec;
	}
	catch (...)
	{
		if (!transferred)
		{
			delete[] data;
		}
		throw;
	}
	return true;
}

bool ObjectHeap::GetObject(OBJECT_REF obj_ref, lpObject &obj)
{
	obj = NULL;
	if (obj_ref <= 0 || static_cast<size_t>(obj_ref) >= objs.size())
	{
		return false;
	}
	obj = objs[static_cast<size_t>(obj_ref)];
	return obj != NULL;
}
