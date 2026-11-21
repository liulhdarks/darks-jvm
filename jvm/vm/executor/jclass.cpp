#include "stdafx.h"
#include "jclass.h"
#include "executor.h"
#include "methodarea.h"
#include "scheduler.h"
#include "threadstack.h"
#include "context.h"
#include "javaexception.h"
#include <map>
#include <string>
#include "utils/DebugUtils.h"

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
		return true;
	}
	initialized_state = true;
	if (load_super)
	{
		LoadSuperClass();
	}
	if (!clinitMethod)
	{
		return true;
	}
	lpVMExecutor exec = new VMExecutor();
	exec->Initialize();
	exec->Execute(this, clinitMethod);
	if (VMContext::pending_exception)
	{
		VMContext::pending_exception = NULL;
	}
	if (exec)
	{
		delete exec;
		exec = NULL;
	}
	return true;
}

void Class::LoadSuperClass()
{
	if (!superClass || !classloader)
	{
		return;
	}
	classloader->FindClass(superClass->GetName(), super_class);
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

// Method keys include the return type, so an override that narrows a
// covariant return type is invisible to FindMethod.  Interface dispatch needs
// it: Spring's IterableConfigurationPropertySource overrides
// ConfigurationPropertySource.filter with a narrower return type.
//
// Class methods are searched before interface defaults, and the receiver's
// own interfaces before those inherited from superclasses: a class that
// implements ConfigurationPropertySource through a superclass and
// IterableConfigurationPropertySource directly must select the latter.
static bool FindDeclaredMethodByParams(lpClass clazz, LPCSTR name, LPCSTR params,
                                       lpMethod& method)
{
	for (lpClass c = clazz; c; c = c->super_class)
	{
		if (!c->methods)
		{
			continue;
		}
		for (U2 i = 0; i < c->methodCount; i++)
		{
			if (MethodParamsMatch(c->methods[i], name, params))
			{
				method = c->methods[i];
				return true;
			}
		}
	}
	return false;
}

static bool FindInterfaceMethodByParams(lpClass iface, LPCSTR name, LPCSTR params,
                                        lpMethod& method)
{
	if (!iface)
	{
		return false;
	}
	if (iface->methods)
	{
		for (U2 i = 0; i < iface->methodCount; i++)
		{
			if (MethodParamsMatch(iface->methods[i], name, params))
			{
				method = iface->methods[i];
				return true;
			}
		}
	}
	for (U2 i = 0; i < iface->interfaceCount; i++)
	{
		lpClassConstant ic = iface->interfaces ? iface->interfaces[i] : NULL;
		if (!ic || !ic->GetName() || !iface->classloader)
		{
			continue;
		}
		lpClass parent = NULL;
		if (iface->classloader->FindClass(ic->GetName(), parent) && parent &&
		    FindInterfaceMethodByParams(parent, name, params, method))
		{
			return true;
		}
	}
	return false;
}

bool Class::FindMethodByParams(LPCSTR name, LPCSTR params, lpMethod& method)
{
	if (FindDeclaredMethodByParams(this, name, params, method))
	{
		return true;
	}
	for (lpClass c = this; c; c = c->super_class)
	{
		for (U2 i = 0; i < c->interfaceCount; i++)
		{
			lpClassConstant ic = c->interfaces ? c->interfaces[i] : NULL;
			if (!ic || !ic->GetName() || !c->classloader)
			{
				continue;
			}
			lpClass iface = NULL;
			if (c->classloader->FindClass(ic->GetName(), iface) && iface &&
			    FindInterfaceMethodByParams(iface, name, params, method))
			{
				return true;
			}
		}
	}
	return false;
}

bool Class::FindMethod(LPSTR method_name, lpMethod& method)
{
method = MethodArea::GetMethod(this, method_name);
	if (method)
	{
		return true;
	}
	// A class method inherited from the superclass chain always wins over an
	// interface declaration (including abstract ones), so walk the superclass
	// first and only then fall back to interface default/static methods.
	if (super_class && super_class->FindMethod(method_name, method))
	{
		MethodArea::AddMethod(this, method);
		return true;
	}
	// Interface default/static methods are inherited by implementing classes,
	// so resolution has to walk the interface graph as well as the superclass
	// chain.  Interfaces are resolved lazily (they may not have been loaded
	// when this class was defined).
	for (U2 i = 0; i < interfaceCount; i++)
	{
		lpClassConstant ic = interfaces ? interfaces[i] : NULL;
		if (!ic || !ic->GetName() || !classloader)
		{
			continue;
		}
		lpClass iface = NULL;
		if (classloader->FindClass(ic->GetName(), iface) && iface &&
		    iface->FindMethod(method_name, method))
		{
			MethodArea::AddMethod(this, method);
			return true;
		}
	}
	return false;
}
