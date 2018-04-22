#include "stdafx.h"
#include "objectheap.h"
#include "context.h"
#include "utils/CommonUtils.h"
#include "methodarea.h"
#include "executor.h"

bool ObjectHeap::initialize(int init_size)
{
	objs = new lpObject[init_size];
	memset(objs, NULL, init_size);
	return true;
}

void ObjectHeap::gc()
{

}

bool ObjectHeap::NewObject(lpClass clazz, LPOBJECT_REF &object_ref)
{
	lpObject obj = new JObject(clazz);
	if (!obj)
	{
		return false;
	}
	obj->index = sequence++;
	objs[obj->index] = obj;
	if (!obj->newInstance())
	{
		return false;
	}
	object_ref = &obj->index;
	return true;
}

bool ObjectHeap::NewArrayObject(int size, lpClass clazz, LPOBJECT_REF &object_ref)
{
	lpObject obj = new JObject(clazz);
	if (!obj)
	{
		return false;
	}
	obj->index = sequence++;
	objs[obj->index] = obj;
	if (!obj->newArrayInstance(size))
	{
		return false;
	}
	object_ref = &obj->index;
	return true;
}

bool ObjectHeap::NewObjectArrayObject(int size, lpClass clazz, LPOBJECT_REF &object_ref)
{
	lpObject obj = new JObject(clazz);
	if (!obj)
	{
		return false;
	}
	obj->index = sequence++;
	objs[obj->index] = obj;
	if (!obj->newArrayInstance(DT_REF, size))
	{
		return false;
	}
	object_ref = &obj->index;
	return true;
}

bool ObjectHeap::NewMultiArrayObject(LPINT32 nums, int count, lpClass clazz, LPOBJECT_REF &object_ref)
{
	lpObject obj = new JObject(clazz);
	if (!obj)
	{
		return false;
	}
	obj->index = sequence++;
	objs[obj->index] = obj;
	int index = 0;
	if (!obj->newArrayInstance(DT_REF, nums[index]))
	{
		return false;
	}
	object_ref = &obj->index;
	return NewMultiArrayObject(obj, nums, index, count, clazz);
}

bool ObjectHeap::NewMultiArrayObject(lpObject parent, LPINT32 nums, int index, int count, lpClass clazz)
{
	int ch_index = index + 1;
	int num = nums[index];
	for (int i = 0; i < num; i++)
	{
		lpClass clazz = new Class(CT_ARRAY);
		clazz->thisClass = new ClassConstant();
		clazz->thisClass->name = CommonUtils::SubString(clazz->thisClass->GetName(), 1);
		lpObject obj = new JObject(clazz);
		if (!obj)
		{
			return false;
		}
		obj->index = sequence++;
		objs[obj->index] = obj;
		int index = 0;
		if (ch_index < count - 1)
		{
			if (!obj->newArrayInstance(DT_REF, nums[ch_index]))
			{
				return false;
			}
			if (!NewMultiArrayObject(obj, nums, ch_index, count, clazz))
			{
				return false;
			}
		}
		else
		{
			if (!obj->newArrayInstance(nums[ch_index]))
			{
				return false;
			}
		}
		LPOBJECT_REF obj_ref = &obj->index;
		LPOBJECT_REF* refs = (LPOBJECT_REF*)parent->array_var;
		refs[i] = obj_ref;
	}
	return true;
}

//data为UTF8字符指针
bool ObjectHeap::NewStringObject(LPSTR data, int length, LPOBJECT_REF &object_ref)
{
	int unicode_len = 0;
	unsigned short* unicodes = CommonUtils::Utf8ToUnicode(data, unicode_len);
	if (!unicodes)
	{
		return false;
	}
	return NewStringObject(unicodes, length, object_ref);
}

//data为UNICODE字符指针
bool ObjectHeap::NewStringObject(unsigned short *data, int length, LPOBJECT_REF &object_ref)
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
	if (exec)
	{
		delete exec;
		exec = NULL;
	}
	return true;
}

bool ObjectHeap::GetObject(OBJECT_REF obj_ref, lpObject &obj)
{
	obj = objs[obj_ref];
	if (!obj)
	{
		return false;
	}
	return true;
}