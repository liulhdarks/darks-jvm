#include "stdafx.h"
#include "jfield.h"

Field::Field()
{
	fieldName = NULL;
	fieldDesc = NULL;
	constValue = NULL;
	attrCount = 0;
	accFlag = 0;
	simple_name = NULL;
}

Field::~Field()
{
	if (constValue)
	{
		delete constValue;
		constValue = NULL;
	}
	if (simple_name)
	{
		delete[] simple_name;
		simple_name = NULL;
	}
}

LPSTR &Field::GetSimpleName()
{
	if (!simple_name)
	{
		LPCSTR name = fieldName ? fieldName->GetName() : "";
		LPCSTR desc = fieldDesc ? fieldDesc->GetName() : "";
		if (!name) name = "";
		if (!desc) desc = "";
		size_t len = strlen(name) + strlen(desc) + 2;
		simple_name = new CHAR[len];
		snprintf(simple_name, len, "%s:%s", name, desc);
	}
	return simple_name;
}

FieldValue::FieldValue()
{
	field = NULL;
	field_value.dValue = 0;
	field_value.iValue = 0;
}

FieldValue::FieldValue(lpField field)
{
	field_value.dValue = 0;
	field_value.iValue = 0;
	this->field = field;
}

void FieldValue::initialize()
{
	if (!field)
	{
		return;
	}
	if (!field->constValue)
	{
		return;
	}
	lpConstant constant = field->constValue->constant;
	if (!constant)
	{
		return;
	}
	U1 tag = constant->tag;
	switch (tag)
	{
	case INTEGER_CONSTANT:
		{
			IntegerConstant *cv = (IntegerConstant *)constant;
			field_value.iValue = static_cast<int>(cv->bytes);
			break;
		}
	case FLOAT_CONSTANT:
		{
			FloatConstant *cv = (FloatConstant *)constant;
			field_value.set(cv->bytes, 4);
			break;
		}
	case LONG_CONSTANT:
		{
			LongConstant *cv = (LongConstant *)constant;
			WByteValue bv(cv->bytes1, cv->bytes2);
			field_value.lValue = bv.lValue;
			break;
		}
	case DOUBLE_CONSTANT:
		{
			DoubleConstant *cv = (DoubleConstant *)constant;
			WByteValue bv(cv->bytes2, cv->bytes1);
			field_value.dValue = bv.dValue;
			break;
		}
	case STRING_CONSTANT:
		{
			StringConstant *st = (StringConstant*)constant;
			field_value.heapPtr = st->GetString();
			break;
		}
	default:
		break;
	}
}
