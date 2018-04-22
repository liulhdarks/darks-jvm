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
