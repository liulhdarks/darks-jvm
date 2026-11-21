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

// Array assignability compares element classes, which may come from an
// application loader rather than the bootstrap loader.
lpClass FindClassAnywhere(const char *name)
{
	lpClass clazz = NULL;
	if (name && DarksVM_LoadClassWithoutInitializing(std::string(name), clazz) && clazz)
	{
		return clazz;
	}
	return FindClassSafe(name);
}

// Interfaces are parsed lazily; resolve one through its defining loader.
// A missing interface simply leaves the type check incomplete.
lpClass ResolveInterface(lpClassLoader loader, const char *name)
{
	if (!loader || !name)
	{
		return NULL;
	}
	lpClass iface = NULL;
	try
	{
		if (!loader->FindClass(name, iface))
		{
			return NULL;
		}
	}
	catch (...)
	{
		return NULL;
	}
	return iface;
}

// Interfaces may extend other interfaces; a class is assignable to every
// superinterface, not only the ones listed directly in its own class file.
bool InterfaceAssignable(lpClass iface, const char *target_name, int depth)
{
	if (!iface || !target_name)
	{
		return false;
	}
	const char *name = iface->thisClass ? iface->thisClass->GetName() : NULL;
	if (name && strcmp(name, target_name) == 0)
	{
		return true;
	}
	if (depth <= 0)
	{
		return false;
	}
	for (U2 i = 0; i < iface->interfaceCount; i++)
	{
		lpClassConstant ic = iface->interfaces ? iface->interfaces[i] : NULL;
		if (!ic || !ic->GetName())
		{
			continue;
		}
		if (strcmp(ic->GetName(), target_name) == 0)
		{
			return true;
		}
		lpClass parent = ResolveInterface(iface->classloader, ic->GetName());
		if (parent && InterfaceAssignable(parent, target_name, depth - 1))
		{
			return true;
		}
	}
	return false;
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

// Returns the component type of an array class name ("[Ljava/lang/String;"
// -> "java/lang/String", "[[I" -> "[I", "[I" -> "I").
std::string ArrayComponentName(const char *array_name)
{
	if (!array_name || array_name[0] != '[')
	{
		return std::string();
	}
	const char *p = array_name + 1;
	if (*p == '[')
	{
		return std::string(p);
	}
	if (*p == 'L')
	{
		size_t len = strlen(p);
		if (len >= 2 && p[len - 1] == ';')
		{
			return std::string(p + 1, len - 2);
		}
		return std::string();
	}
	return std::string(1, *p);
}

bool IsPrimitiveDescriptorName(const std::string &name)
{
	return name.size() == 1 && strchr("ZBCSIJFD", name[0]) != NULL;
}

bool ClassIsAssignableDepth(lpClass target, lpClass src, int depth);

bool ArrayAssignable(lpClass target, lpClass src, int depth)
{
	const char *tname = target->thisClass->GetName();
	const char *sname = src->thisClass->GetName();
	if (strcmp(tname, "java/lang/Object") == 0 ||
	    strcmp(tname, "java/lang/Cloneable") == 0 ||
	    strcmp(tname, "java/io/Serializable") == 0)
	{
		return true;
	}
	if (tname[0] != '[')
	{
		return false;
	}
	std::string tc = ArrayComponentName(tname);
	std::string sc = ArrayComponentName(sname);
	if (tc.empty() || sc.empty())
	{
		return false;
	}
	if (IsPrimitiveDescriptorName(tc) || IsPrimitiveDescriptorName(sc))
	{
		return tc == sc;
	}
	if (depth <= 0)
	{
		return false;
	}
	lpClass tcomp = FindClassSafe(tc.c_str());
	lpClass scomp = FindClassSafe(sc.c_str());
	if (!tcomp || !scomp)
	{
		return false;
	}
	return ClassIsAssignableDepth(tcomp, scomp, depth - 1);
}

bool ClassIsAssignableDepth(lpClass target, lpClass src, int depth)
{
	if (!target || !src)
	{
		return false;
	}
	if (!target->thisClass || !target->thisClass->GetName() ||
	    !src->thisClass || !src->thisClass->GetName())
	{
		return SameClassName(target, src);
	}
	const char *tname = target->thisClass->GetName();
	const char *sname = src->thisClass->GetName();
	if (strcmp(tname, sname) == 0)
	{
		return true;
	}
	if (sname[0] == '[')
	{
		return ArrayAssignable(target, src, depth);
	}
	if (depth <= 0)
	{
		return false;
	}
	// Superclass chain, with interfaces resolved recursively so that
	// interface inheritance behaves like the JLS requires.
	for (lpClass c = src; c; c = c->super_class)
	{
		if (SameClassName(target, c))
		{
			return true;
		}
		for (U2 i = 0; i < c->interfaceCount; i++)
		{
			lpClassConstant ic = c->interfaces ? c->interfaces[i] : NULL;
			if (!ic)
			{
				continue;
			}
			const char *iname = ic->GetName();
			if (!iname)
			{
				continue;
			}
			if (strcmp(iname, tname) == 0)
			{
				return true;
			}
			lpClass iface = FindClassSafe(iname);
			if (iface && ClassIsAssignableDepth(target, iface, depth - 1))
			{
				return true;
			}
		}
	}
	return false;
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
					std::map<std::string, lpFieldValue>::iterator it =
						obj->varants.find(std::string("detailMessage:Ljava/lang/String;"));
					if (it != obj->varants.end() && it->second)
					{
						it->second->field_value.heapPtr = msg_ref;
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

LPOBJECT_REF CreateJavaExceptionWithCause(const char *class_name,
                                          const char *message,
                                          LPOBJECT_REF cause)
{
	LPOBJECT_REF ref = CreateJavaException(class_name, message);
	if (!ref || !VMContext::object_heap)
	{
		return ref;
	}
	GCRootScope root(VMContext::object_heap, &ref);
	GCRootScope cause_root(VMContext::object_heap, &cause);
	lpObject obj = NULL;
	if (!VMContext::object_heap->GetObject(ref, obj) || !obj)
	{
		return ref;
	}
	std::map<std::string, lpFieldValue>::iterator it =
		obj->varants.find(std::string("cause:Ljava/lang/Throwable;"));
	if (it != obj->varants.end() && it->second)
	{
		it->second->field_value.heapPtr = cause;
	}
	it = obj->varants.find(std::string("causeInitialized:Z"));
	if (it != obj->varants.end() && it->second)
	{
		it->second->field_value.blValue = 1;
	}
	return ref;
}

bool ClassIsAssignable(lpClass target, lpClass src)
{
	return ClassIsAssignableDepth(target, src, 32);
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
	lpFieldValue fv = str->varants.count(std::string("value:[C"))
		? str->varants[std::string("value:[C")] : NULL;
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
		std::map<std::string, lpFieldValue>::iterator it =
			obj->varants.find(std::string("detailMessage:Ljava/lang/String;"));
		if (it != obj->varants.end() && it->second && it->second->field_value.heapPtr)
		{
			has_message = JavaStringToUtf8(it->second->field_value.heapPtr, message);
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
