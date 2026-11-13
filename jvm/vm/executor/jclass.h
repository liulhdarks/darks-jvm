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
	lpBootstrapMethods bootstrap_methods;
	ClassType class_type;
	lpClass super_class;

	// Class file version (major 45..55 supported; 52 == Java 8, 55 == Java 11).
	U2 major_version;
	U2 minor_version;

	// Nestmate linkage (Java 11).  nest_host is the internal name of the host
	// class (empty when the class is its own host); nest_members lists the
	// internal names declared by a NestMembers attribute.
	string nest_host;
	vector<string> nest_members;
	// Set once <clinit> has run (or when the class has none); guards against
	// repeated initialisation from JNI GetStaticMethodID/GetStaticFieldID.
	bool initialized_state;

	// Instance field layout: instance fields of the superclass chain occupy
	// slots [0, instance_field_total).  Fields declared by this class start at
	// instance_field_base, so a subclass field that hides a superclass field
	// still gets distinct storage.
	int instance_field_base;
	int instance_field_total;
	bool instance_layout_ready;

	// Name -> slot indexes for the whole superclass chain, built together with
	// the layout.  The key is Field::GetSimpleName() ("<name>:<descriptor>").
	// A subclass entry replaces the inherited one, so a hidden field resolves
	// to the subclass slot, matching the historical map-overwrite behaviour.
	// instance_name_index accepts the bare field name (used by JNI helpers);
	// for duplicate names the most-derived field wins.
	std::map<std::string, int> instance_slot_index;
	std::map<std::string, int> instance_name_index;

public:
	Class();

	Class(ClassType class_type);

	~Class();

public:

	void LoadSuperClass();
	bool initialize(bool load_super = true);
	bool FindMethod(LPSTR method_name, lpMethod& method);

	// Assigns Field::slot for every instance field in the superclass chain
	// and returns the total number of instance slots for objects of this
	// class.  Idempotent; safe to call before the class is initialised.
	int EnsureInstanceLayout();

	// Resolve "<name>:<descriptor>" (FindInstanceSlot) or a bare field name
	// (FindInstanceSlotByName) to a slot index.  Returns -1 when there is no
	// such instance field.  Both require EnsureInstanceLayout() to have run.
	int FindInstanceSlot(const std::string &key) const;
	int FindInstanceSlotByName(const char *name) const;

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
