#include "stdafx.h"
#include "jclass.h"
#include "executor.h"
#include "methodarea.h"
#include "javaexception.h"
#include "scheduler.h"
#include "threadstack.h"
#include "context.h"
#include <map>
#include <string>
#include "utils/DebugUtils.h"

#include <set>
#include <string>
#include <utility>
#include <vector>

Class::Class()
{
	const_pool = new ConstantPool();
	thisClass = NULL;
	superClass = NULL;
	interfaces = NULL;
	fields = NULL;
	methods = NULL;
	interfaceCount = 0;
	source_file = NULL;
	mainMethod = NULL;
	class_type = CT_OBJECT;
	clinitMethod = NULL;
	classloader = NULL;
	super_class = NULL;
	component_class = NULL;
	initialized_state = false;
	initialization_failed = false;
	inner_classes = NULL;
	bootstrap_methods = NULL;
	major_version = 0;
	minor_version = 0;
	fieldCount = 0;
	methodCount = 0;
	accFlag = 0;
	instance_field_base = 0;
	instance_field_total = 0;
	instance_layout_ready = false;
}

Class::Class(ClassType class_type)
{
	const_pool = new ConstantPool();
	thisClass = NULL;
	superClass = NULL;
	interfaces = NULL;
	fields = NULL;
	methods = NULL;
	interfaceCount = 0;
	source_file = NULL;
	mainMethod = NULL;
	this->class_type = class_type;
	clinitMethod = NULL;
	classloader = NULL;
	super_class = NULL;
	component_class = NULL;
	initialized_state = false;
	initialization_failed = false;
	inner_classes = NULL;
	bootstrap_methods = NULL;
	major_version = 0;
	minor_version = 0;
	fieldCount = 0;
	methodCount = 0;
	accFlag = 0;
	instance_field_base = 0;
	instance_field_total = 0;
	instance_layout_ready = false;
}

Class::~Class()
{
	if (thisClass && thisClass->GetName())
	{
		DebugUtils::trace("~Class %s\n", thisClass->GetName());
	}
	if (fields)
	{
		for (U2 i = 0; i < fieldCount; i++)
		{
			delete fields[i];
		}
		delete[] fields;
		fields = NULL;
	}
	if (methods)
	{
		for (U2 i = 0; i < methodCount; i++)
		{
			if (methods[i])
			{
				delete methods[i];
			}
		}
		delete[] methods;
		methods = NULL;
	}
	if (source_file)
	{
		// source_file is a single object, not an array.
		delete source_file;
		source_file = NULL;
	}
	if (inner_classes)
	{
		delete inner_classes;
		inner_classes = NULL;
	}
	if (bootstrap_methods)
	{
		delete bootstrap_methods;
		bootstrap_methods = NULL;
	}
	if (interfaces)
	{
		delete[] interfaces;
		interfaces = NULL;
	}
	if (const_pool)
	{
		delete const_pool;
		const_pool = NULL;
	}
	classloader = NULL;
	super_class = NULL;
}

bool Class::initialize(bool load_super)
{
	if (initialized_state)
	{
		if (initialization_failed)
		{
			if (!VMContext::pending_exception)
			{
				std::string binary = thisClass && thisClass->GetName()
					? thisClass->GetName() : "?";
				for (size_t i = 0; i < binary.size(); i++)
				{
					if (binary[i] == '/')
					{
						binary[i] = '.';
					}
				}
				std::string message = "Could not initialize class ";
				message += binary;
				VMContext::pending_exception = CreateJavaException(
					"java/lang/NoClassDefFoundError", message.c_str());
			}
			return false;
		}
		return true;
	}
	initialized_state = true;
	initialization_failed = false;
	if (load_super && !LoadSuperClass())
	{
		// The superclass failed to initialize.  Its exception (JVMS 5.5: the
		// same ExceptionInInitializerError for a first attempt, or the
		// NoClassDefFoundError for an already erroneous superclass) is already
		// pending; this class is erroneous as well and its own <clinit> does
		// not run.
		initialization_failed = true;
		return false;
	}
	if (!clinitMethod)
	{
		return true;
	}
	lpVMExecutor exec = new VMExecutor();
	exec->Initialize();
	try
	{
		exec->Execute(this, clinitMethod);
	}
	catch (...)
	{
		delete exec;
		initialization_failed = true;
		throw;
	}
	delete exec;
	LPOBJECT_REF thrown = VMContext::pending_exception;
	if (thrown)
	{
		initialization_failed = true;
		// Errors thrown by <clinit> propagate unchanged; every other Throwable
		// is wrapped in ExceptionInInitializerError with the throwable as cause
		// (JVMS 5.5, second bullet of step 11).  The throwable stays reachable
		// through pending_exception while the wrapper is allocated.
		lpObject obj = NULL;
		bool is_error = false;
		if (VMContext::object_heap && VMContext::object_heap->GetObject(thrown, obj) && obj)
		{
			for (lpClass c = obj->obj_class; c; c = c->super_class)
			{
				const char *name = c->thisClass ? c->thisClass->GetName() : NULL;
				if (name && strcmp(name, "java/lang/Error") == 0)
				{
					is_error = true;
					break;
				}
			}
		}
		if (!is_error)
		{
			LPOBJECT_REF wrapper = CreateJavaExceptionWithCause(
				"java/lang/ExceptionInInitializerError", NULL, thrown);
			if (wrapper)
			{
				VMContext::pending_exception = wrapper;
			}
		}
		return false;
	}
	return true;
}

bool Class::LoadSuperClass()
{
	if (!superClass || !classloader)
	{
		return true;
	}
	if (!super_class)
	{
		try
		{
			if (!classloader->FindClass(superClass->GetName(), super_class, true) ||
			    !super_class)
			{
				return false;
			}
			return true;
		}
		catch (VMException *error)
		{
			delete error;
			return false;
		}
		catch (...)
		{
			return false;
		}
	}
	return super_class->initialize(true);
}

int Class::EnsureInstanceLayout()
{
	if (instance_layout_ready)
	{
		return instance_field_total;
	}
	// The superclass contributes its whole layout first.
	if (!super_class && superClass && classloader)
	{
		try
		{
			classloader->FindClass(superClass->GetName(), super_class);
		}
		catch (...)
		{
			super_class = NULL;
		}
	}
	int base = super_class ? super_class->EnsureInstanceLayout() : 0;
	instance_field_base = base;
	int slot = base;
	for (U2 i = 0; i < fieldCount; i++)
	{
		lpField field = fields ? fields[i] : NULL;
		if (!field || (field->accFlag & ACF_STATIC) != 0)
		{
			continue;
		}
		field->slot = slot++;
	}
	instance_field_total = slot;
	instance_layout_ready = true;
	return instance_field_total;
}

namespace {

bool SameClass(lpClass a, lpClass b)
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

lpMethod DeclaredMethod(lpClass clazz, const char *key)
{
	if (!clazz || !key)
	{
		return NULL;
	}
	for (U2 i = 0; i < clazz->methodCount; i++)
	{
		lpMethod m = clazz->methods ? clazz->methods[i] : NULL;
		if (!m || !m->methodName || !m->methodDesc)
		{
			continue;
		}
		const char *name = m->methodName->GetName();
		const char *desc = m->methodDesc->GetName();
		if (!name || !desc)
		{
			continue;
		}
		std::string candidate = std::string(name) + ":" + desc;
		if (candidate == key)
		{
			return m;
		}
	}
	return NULL;
}

lpClass ResolveInterface(lpClass owner, lpClassConstant ic)
{
	if (!owner || !ic || !ic->GetName() || !owner->classloader)
	{
		return NULL;
	}
	lpClass iface = NULL;
	try
	{
		if (owner->classloader->FindClass(ic->GetName(), iface, false))
		{
			return iface;
		}
	}
	catch (...)
	{
	}
	return NULL;
}

bool IsSubInterface(lpClass sub, lpClass sup, std::set<std::string> &visited)
{
	if (!sub || !sup)
	{
		return false;
	}
	if (SameClass(sub, sup))
	{
		return true;
	}
	const char *name = (sub->thisClass && sub->thisClass->GetName())
		? sub->thisClass->GetName() : NULL;
	if (name)
	{
		if (!visited.insert(std::string(name)).second)
		{
			return false;
		}
	}
	for (U2 i = 0; i < sub->interfaceCount; i++)
	{
		lpClass iface = ResolveInterface(sub, sub->interfaces ? sub->interfaces[i] : NULL);
		if (iface && IsSubInterface(iface, sup, visited))
		{
			return true;
		}
	}
	return false;
}

void CollectInterfaceMethods(lpClass iface, const std::string &key,
                             std::vector<std::pair<lpClass, lpMethod> > &out,
                             std::set<std::string> &visited)
{
	if (!iface)
	{
		return;
	}
	const char *name = (iface->thisClass && iface->thisClass->GetName())
		? iface->thisClass->GetName() : NULL;
	if (name && !visited.insert(std::string(name)).second)
	{
		return;
	}
	lpMethod declared = DeclaredMethod(iface, key.c_str());
	if (declared && (declared->accFlag & ACM_STATIC) == 0 &&
	    (declared->accFlag & ACM_PRIVATE) == 0)
	{
		out.push_back(std::make_pair(iface, declared));
	}
	for (U2 i = 0; i < iface->interfaceCount; i++)
	{
		lpClass parent = ResolveInterface(iface, iface->interfaces ? iface->interfaces[i] : NULL);
		if (parent)
		{
			CollectInterfaceMethods(parent, key, out, visited);
		}
	}
}

// Call sites whose declared return type differs from a covariant override match
// on name and parameters instead of the full key.  Keep the same precedence
// rules there: a subinterface default must shadow the default it inherits,
// otherwise `class C implements A, B` (B extends A) would silently run A's
// body.
bool SelectMaximallySpecific(
	const std::vector<std::pair<lpClass, lpMethod> > &candidates,
	lpMethod &method)
{
	lpMethod best = NULL;
	lpClass bestOwner = NULL;
	for (size_t i = 0; i < candidates.size(); i++)
	{
		bool superseded = false;
		for (size_t j = 0; j < candidates.size() && !superseded; j++)
		{
			if (i == j || SameClass(candidates[i].first, candidates[j].first))
			{
				continue;
			}
			std::set<std::string> seen;
			if (IsSubInterface(candidates[j].first, candidates[i].first, seen))
			{
				superseded = true;
			}
		}
		if (!superseded)
		{
			if (best && !SameClass(bestOwner, candidates[i].first))
			{
				return false;
			}
			best = candidates[i].second;
			bestOwner = candidates[i].first;
		}
	}
	if (!best)
	{
		return false;
	}
	method = best;
	return true;
}


static bool MethodParamsMatch(lpMethod method, LPCSTR name, LPCSTR params)
{
	if (!method || !method->methodName || !method->methodDesc ||
	    !method->methodName->GetName() || !method->methodDesc->GetName())
	{
		return false;
	}
	if (strcmp(method->methodName->GetName(), name) != 0)
	{
		return false;
	}
	if (method->isStatic() || method->isSpecial())
	{
		return false;
	}
	const char *desc = method->methodDesc->GetName();
	size_t length = strlen(params);
	return strncmp(desc, params, length) == 0;
}

lpMethod DeclaredMethodByParams(lpClass clazz, LPCSTR name, LPCSTR params)
{
	if (!clazz || !clazz->methods)
	{
		return NULL;
	}
	for (U2 i = 0; i < clazz->methodCount; i++)
	{
		if (MethodParamsMatch(clazz->methods[i], name, params))
		{
			return clazz->methods[i];
		}
	}
	return NULL;
}

void CollectInterfaceMethodsByParams(lpClass iface, LPCSTR name, LPCSTR params,
                                     std::vector<std::pair<lpClass, lpMethod> > &out,
                                     std::set<std::string> &visited)
{
	if (!iface)
	{
		return;
	}
	const char *iname = (iface->thisClass && iface->thisClass->GetName())
		? iface->thisClass->GetName() : NULL;
	if (iname && !visited.insert(std::string(iname)).second)
	{
		return;
	}
	lpMethod declared = DeclaredMethodByParams(iface, name, params);
	if (declared && (declared->accFlag & ACM_STATIC) == 0 &&
	    (declared->accFlag & ACM_PRIVATE) == 0)
	{
		out.push_back(std::make_pair(iface, declared));
	}
	for (U2 i = 0; i < iface->interfaceCount; i++)
	{
		lpClass parent = ResolveInterface(iface, iface->interfaces ? iface->interfaces[i] : NULL);
		if (parent)
		{
			CollectInterfaceMethodsByParams(parent, name, params, out, visited);
		}
	}
}

} // namespace

bool Class::FindMethod(LPSTR method_name, lpMethod& method)
{
	method = NULL;
	// 1. A method declared by this class always wins.
	lpMethod declared = MethodArea::GetMethod(this, method_name);
	if (declared)
	{
		method = declared;
		return true;
	}

	// A method declared by the class itself (or by a superclass) takes
	// precedence over interface defaults.  Method resolution also finds
	// inherited static methods (invokestatic may name a subclass); only private
	// members are excluded from inheritance.
	method = DeclaredMethod(this, method_name);
	if (method)
	{
		MethodArea::AddMethod(this, method);
		return true;
	}
	lpMethod abstractMatch = NULL;
	for (lpClass c = super_class; c; c = c->super_class)
	{
		lpMethod inherited = DeclaredMethod(c, method_name);
		if (!inherited || (inherited->accFlag & ACM_PRIVATE) != 0)
		{
			continue;
		}
		if ((inherited->accFlag & ACM_ABSTRACT) == 0)
		{
			method = inherited;
			MethodArea::AddMethod(this, method);
			return true;
		}
		if (!abstractMatch)
		{
			abstractMatch = inherited;
		}
	}

	// No class method applies: pick the maximally specific default method from
	// the interface graph.  A subinterface default shadows the one it inherits
	// (class Impl implements A, B where B extends A resolves to B).  Interfaces
	// contributed by superclasses participate as well.
	std::vector<std::pair<lpClass, lpMethod> > candidates;
	std::set<std::string> visited;
	std::string key(method_name ? method_name : "");
	for (lpClass c = this; c; c = c->super_class)
	{
		for (U2 i = 0; i < c->interfaceCount; i++)
		{
			lpClass iface = ResolveInterface(c, c->interfaces ? c->interfaces[i] : NULL);
			if (iface)
			{
				CollectInterfaceMethods(iface, key, candidates, visited);
			}
		}
	}
	lpMethod best = NULL;
	if (SelectMaximallySpecific(candidates, best))
	{
		method = best;
		MethodArea::AddMethod(this, method);
		return true;
	}
	if (abstractMatch)
	{
		method = abstractMatch;
		MethodArea::AddMethod(this, method);
		return true;
	}
	return false;
}

bool Class::FindMethodByParams(LPCSTR name, LPCSTR params, lpMethod& method)
{
	method = NULL;
	lpMethod declared = DeclaredMethodByParams(this, name, params);
	if (declared)
	{
		method = declared;
		return true;
	}
	lpMethod abstractMatch = NULL;
	for (lpClass c = super_class; c; c = c->super_class)
	{
		lpMethod inherited = DeclaredMethodByParams(c, name, params);
		if (!inherited || (inherited->accFlag & ACM_PRIVATE) != 0)
		{
			continue;
		}
		if ((inherited->accFlag & ACM_ABSTRACT) == 0)
		{
			method = inherited;
			return true;
		}
		if (!abstractMatch)
		{
			abstractMatch = inherited;
		}
	}
	std::vector<std::pair<lpClass, lpMethod> > candidates;
	std::set<std::string> visited;
	for (lpClass c = this; c; c = c->super_class)
	{
		for (U2 i = 0; i < c->interfaceCount; i++)
		{
			lpClass iface = ResolveInterface(c, c->interfaces ? c->interfaces[i] : NULL);
			if (iface)
			{
				CollectInterfaceMethodsByParams(iface, name, params, candidates, visited);
			}
		}
	}
	if (SelectMaximallySpecific(candidates, method))
	{
		return true;
	}
	if (abstractMatch)
	{
		method = abstractMatch;
		return true;
	}
	return false;
}
