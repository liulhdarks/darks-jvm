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
};

// True when the field descriptor starts with 'L' or '[', i.e. the slot holds
// an object reference and writes to it must go through the GC barrier.
inline bool FieldHoldsReference(lpField field)
{
	if (!field || !field->fieldDesc)
	{
		return false;
	}
	LPSTR desc = field->fieldDesc->GetName();
	return desc != NULL && (desc[0] == 'L' || desc[0] == '[');
}
