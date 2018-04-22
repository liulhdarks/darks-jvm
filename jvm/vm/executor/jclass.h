#pragma once

#include "vm/types.h"
#include "vm/executor/jfield.h"
#include "vm/executor/jmethod.h"
#include "vm/classfiles/constant.h"
#include "vm/executor/attributes.h"
#include "vm/classfiles/classloader.h"
#include <map>
using namespace std;

class JVM_API Class : public CommonAttributes
{
public:
	ConstantPool *const_pool;
	lpClassLoader classloader;
	U2 accFlag;
	lpClassConstant thisClass;
	lpClassConstant superClass;
	lpClassConstant *interfaces;
	U2 interfaceCount;

	U2 fieldCount;
	U2 methodCount;
	lpField *fields;
	map<string, lpField> fields_map;
	lpMethod *methods;
	lpMethod mainMethod;
	lpMethod clinitMethod;

	lpSourceFile source_file;
	lpInnerClasses inner_classes;
	ClassType class_type;
	lpClass super_class;

public:
	Class();

	Class(ClassType class_type);

	~Class();

public:

	void LoadSuperClass();
	bool initialize(bool load_super = true);
	bool FindMethod(LPSTR method_name, lpMethod& method);

	void StoreField(LPSTR field_name, lpField field)
	{
		fields_map[string(field_name)] = field;
	}

	bool isInterface()
	{
		return (accFlag & ACC_INTERFACE) != 0;
	}

	bool isEnum()
	{
		return (accFlag & ACC_ENUM) != 0;
	}

	bool isAnnotation()
	{
		return (accFlag & ACC_ANNOTATION) != 0;
	}

	bool isClass()
	{
		return !isInterface() && !isEnum() && !isAnnotation();
	}
};
