#include "stdafx.h"
#include "jclass.h"
#include "executor.h"
#include "methodarea.h"
#include "javaexception.h"
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

namespace
{
	void CollectSuperInterfaces(lpClass clazz, lpClassLoader loader,
	                            std::vector<lpClass> &out, int depth)
	{
		if (!clazz || !loader || depth <= 0)
		{
			return;
		}
		for (U2 i = 0; i < clazz->interfaceCount; i++)
		{
			lpClassConstant ic = clazz->interfaces ? clazz->interfaces[i] : NULL;
			if (!ic || !ic->GetName())
			{
				continue;
			}
			lpClass iface = NULL;
			if (!loader->FindClass(ic->GetName(), iface) || !iface)
			{
				continue;
			}
			bool seen = false;
			for (size_t k = 0; k < out.size(); k++)
			{
				if (out[k] == iface)
				{
					seen = true;
					break;
				}
			}
			if (seen)
			{
				continue;
			}
			out.push_back(iface);
			CollectSuperInterfaces(iface, loader, out, depth - 1);
		}
	}
}

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
	// 2. Declared superclass methods beat interface defaults.  An abstract
	// superclass declaration is only a fallback: a more specific interface
	// default may still provide the implementation.
	lpMethod fallback = NULL;
	for (lpClass c = super_class; c; c = c->super_class)
	{
		lpMethod m = MethodArea::GetMethod(c, method_name);
		if (!m)
		{
			continue;
		}
		if ((m->accFlag & ACM_ABSTRACT) == 0)
		{
			method = m;
			return true;
		}
		if (!fallback)
		{
			fallback = m;
		}
	}
	// 3. Superinterfaces.  The JVM selects the maximally specific non-abstract
	// default, so a subinterface override (IterableConfigurationPropertySource)
	// must win over its parent's default (ConfigurationPropertySource) even
	// when the parent is reached through an earlier superclass.
	std::vector<lpClass> ifaces;
	for (lpClass c = this; c; c = c->super_class)
	{
		CollectSuperInterfaces(c, classloader, ifaces, 32);
	}
	lpMethod best = NULL;
	lpClass best_iface = NULL;
	for (size_t i = 0; i < ifaces.size(); i++)
	{
		lpMethod candidate = MethodArea::GetMethod(ifaces[i], method_name);
		if (!candidate)
		{
			continue;
		}
		if ((candidate->accFlag & ACM_ABSTRACT) != 0)
		{
			if (!fallback)
			{
				fallback = candidate;
			}
			continue;
		}
		if (!best)
		{
			best = candidate;
			best_iface = ifaces[i];
			continue;
		}
		if (best != candidate && best_iface &&
		    ClassIsAssignable(best_iface, ifaces[i]))
		{
			// ifaces[i] is a subinterface of best_iface: more specific.
			best = candidate;
			best_iface = ifaces[i];
		}
	}
	if (best)
	{
		method = best;
		return true;
	}
	method = fallback;
	return method != NULL;
}
