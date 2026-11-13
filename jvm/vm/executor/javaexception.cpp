#include "stdafx.h"
#include "javaexception.h"
#include "vm/executor/context.h"
#include "vm/executor/objectheap.h"
#include "vm/executor/jobject.h"
#include "vm/executor/jclass.h"
#include "vm/executor/jfield.h"
#include "vm/executor/methodarea.h"
#include "vm/classfiles/classloader.h"
#include "platform/platform.h"
#include "vm/exceptions.h"

#include <cstdio>
#include <string>
#include <vector>

namespace {

lpClass FindClassSafe(const char *name)
{
	if (!name || !VMContext::bootstrap_classloader)
	{
		return NULL;
	}
	lpClass clazz = NULL;
	try
	{
		VMContext::bootstrap_classloader->FindClass(name, clazz);
	}
	catch (...)
	{
		clazz = NULL;
	}
	return clazz;
}

bool SameClassName(lpClass a, lpClass b)
{
	if (a == b)
	{
		return true;
	}
	if (!a || !b || !a->thisClass || !b->thisClass)
	{
		return false;
	}
	const char *an = a->thisClass->GetName();
	const char *bn = b->thisClass->GetName();
	return an && bn && strcmp(an, bn) == 0;
}

} // namespace

LPOBJECT_REF CreateJavaException(const char *class_name, const char *message)
{
	lpClass clazz = FindClassSafe(class_name);
	if (!clazz || !VMContext::object_heap)
	{
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	try
	{
		GCRootScope root(VMContext::object_heap, &ref);
		if (!VMContext::object_heap->NewObject(clazz, ref))
		{
			return NULL;
		}
		if (message)
		{
			LPOBJECT_REF msg_ref = NULL;
			GCRootScope msg_root(VMContext::object_heap, &msg_ref);
			if (VMContext::object_heap->NewStringObject(
					const_cast<char *>(message), static_cast<int>(strlen(message)), msg_ref))
			{
				lpObject obj = NULL;
				if (VMContext::object_heap->GetObject(ref, obj) && obj)
				{
					lpFieldValue message_value =
						obj->GetFieldValueByName(std::string("detailMessage:Ljava/lang/String;"));
					if (message_value)
					{
						message_value->field_value.heapPtr = msg_ref;
					}
				}
			}
		}
	}
	catch (...)
	{
		return NULL;
	}
	return ref;
}

bool ClassIsAssignable(lpClass target, lpClass src)
{
	if (!target || !src)
	{
		return false;
	}
	for (lpClass c = src; c; c = c->super_class)
	{
		if (SameClassName(target, c))
		{
			return true;
		}
	}
	// Direct interfaces of every class on the chain.
	for (lpClass c = src; c; c = c->super_class)
	{
		for (U2 i = 0; i < c->interfaceCount; i++)
		{
			lpClassConstant ic = c->interfaces ? c->interfaces[i] : NULL;
			if (!ic)
			{
				continue;
			}
			const char *iname = ic->GetName();
			if (iname && target->thisClass && target->thisClass->GetName() &&
			    strcmp(iname, target->thisClass->GetName()) == 0)
			{
				return true;
			}
		}
	}
	return false;
}

bool IsInstanceOfClass(lpObject obj, lpClass target)
{
	if (!obj || !target)
	{
		return false;
	}
	return ClassIsAssignable(target, obj->obj_class);
}

const char *ObjectClassName(lpObject obj)
{
	if (!obj || !obj->obj_class || !obj->obj_class->thisClass ||
	    !obj->obj_class->thisClass->GetName())
	{
		return "<unknown>";
	}
	return obj->obj_class->thisClass->GetName();
}

bool JavaStringToUtf8(LPOBJECT_REF ref, std::string &out)
{
	if (!ref || !VMContext::object_heap)
	{
		return false;
	}
	lpObject str = NULL;
	if (!VMContext::object_heap->GetObject(ref, str) || !str)
	{
		return false;
	}
	lpFieldValue fv = str->GetFieldValueByName(std::string("value:[C"));
	if (!fv || !fv->field_value.heapPtr)
	{
		out.clear();
		return true; // empty string
	}
	lpObject chars = NULL;
	if (!VMContext::object_heap->GetObject(fv->field_value.heapPtr, chars) || !chars ||
	    !chars->array_var)
	{
		return false;
	}
	std::vector<uint16_t> units(static_cast<uint16_t *>(chars->array_var),
	                            static_cast<uint16_t *>(chars->array_var) + chars->array_length);
	out = dk::Utf16ToUtf8(units);
	return true;
}

void PrintUncaughtException(LPOBJECT_REF exception)
{
	if (!exception)
	{
		return;
	}
	lpObject obj = NULL;
	std::string name = "<unknown>";
	if (VMContext::object_heap && VMContext::object_heap->GetObject(exception, obj))
	{
		name = ObjectClassName(obj);
	}
	std::string printable;
	for (size_t i = 0; i < name.size(); i++)
	{
		printable.push_back(name[i] == '/' ? '.' : name[i]);
	}

	std::string message;
	bool has_message = false;
	if (obj)
	{
		lpFieldValue message_value =
			obj->GetFieldValueByName(std::string("detailMessage:Ljava/lang/String;"));
		if (message_value && message_value->field_value.heapPtr)
		{
			has_message = JavaStringToUtf8(message_value->field_value.heapPtr, message);
		}
	}

	fprintf(stderr, "Exception in thread \"main\" %s", printable.c_str());
	if (has_message && !message.empty())
	{
		fprintf(stderr, ": %s", message.c_str());
	}
	fprintf(stderr, "\n\tat <darks-jvm native frame>\n");
	fflush(stderr);
}
