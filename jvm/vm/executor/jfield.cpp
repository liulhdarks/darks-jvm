#include "stdafx.h"
#include "jfield.h"

Field::Field()
{
	fieldName = NULL;
	fieldName = NULL;
	constValue = NULL;
	attrCount = 0;
	simple_name = NULL;
}

Field::~Field()
{
	if (constValue)
	{
		delete constValue;
		constValue = NULL;
	}
}

LPSTR &Field::GetSimpleName()
{
	if (!simple_name)
	{
		int len = strlen(fieldName->GetName()) + strlen(fieldDesc->GetName()) + 2;
		simple_name = new CHAR[len];
		sprintf(simple_name, "%s:%s", fieldName->GetName(), fieldDesc->GetName());
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
	U1 tag = field->constValue->constant->tag;
	switch (tag)
	{
	case INTEGER_CONSTANT:
		{
			IntegerConstant *cv = (IntegerConstant *)field->constValue->constant;
			field_value.iValue = cv->bytes;
			break;
		}
	case FLOAT_CONSTANT:
		{
			FloatConstant *cv = (FloatConstant *)field->constValue->constant;
			field_value.set(cv->bytes, 4);
			break;
		}
	case LONG_CONSTANT:
		{
			LongConstant *cv = (LongConstant *)field->constValue->constant;
			WByteValue bv(cv->bytes1, cv->bytes2);
			field_value.lValue = bv.lValue;
			break;
		}
	case DOUBLE_CONSTANT:
		{
			DoubleConstant *cv = (DoubleConstant *)field->constValue->constant;
			WByteValue bv(cv->bytes2, cv->bytes1);
			field_value.dValue = bv.dValue;
			break;
		}
	case STRING_CONSTANT:
		{
			StringConstant *st = (StringConstant*)field->constValue->constant;
			field_value.heapPtr = st->GetString();
			break;
		}
	default:
		break;
	}
}