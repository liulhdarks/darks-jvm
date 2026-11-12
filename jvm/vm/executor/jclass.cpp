#include "stdafx.h"
#include "jclass.h"
#include "executor.h"
#include "methodarea.h"

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
	inner_classes = NULL;
	fieldCount = 0;
	methodCount = 0;
	accFlag = 0;
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
	inner_classes = NULL;
	fieldCount = 0;
	methodCount = 0;
	accFlag = 0;
}

Class::~Class()
{
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
			delete methods[i];
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

bool Class::FindMethod(LPSTR method_name, lpMethod& method)
{
	method = MethodArea::GetMethod(this, method_name);
	if (!method)
	{
		if (super_class)
		{
			if (super_class->FindMethod(method_name, method))
			{
				MethodArea::AddMethod(this, method);
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}
	return true;
}
