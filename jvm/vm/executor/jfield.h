#pragma once

#include "vm/types.h"
#include "exec_common.h"
#include "vm/classfiles/constant.h"
#include "attributes.h"

class JVM_API Field : public CommonAttributes
{
public:

	U2 accFlag;
	lpUtf8Constant fieldName;
	lpUtf8Constant fieldDesc;
	U2 attrCount;
	lpConstantValue constValue;

	LPSTR simple_name;

	// Index into JObject::instance_slots for instance fields.  Slots are
	// assigned once per class (superclass fields first) so that a field
	// hidden by a subclass still has its own storage.  -1 means the field is
	// static or its layout has not been computed yet.
	int slot;

	// Index into Class::static_slots for static fields.  Only fields
	// declared by the class own a slot; inherited statics keep the slot
	// assigned by their declaring class.  -1 means not a static field or
	// the class layout has not been built yet.
	int static_slot;

public:
	Field();

	~Field();

	LPSTR &GetSimpleName();
};

typedef Field *lpField;

class FieldValue
{
public:
	lpField field;
	Variant field_value;

public:
	FieldValue();

	FieldValue(lpField field);

	void initialize();

	// Atomic 64-bit view of the value.  All Java field values fit in the
	// 8-byte Variant, so a single load/store never tears (long/double) and
	// the compiler cannot reorder a volatile access past the call.
	Variant LoadValue() const;
	void StoreValue(const Variant &value);
};
