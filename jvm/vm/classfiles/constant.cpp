#include "stdafx.h"
#include "constant.h"
#include "utils/DebugUtils.h"
#include "vm/executor/context.h"

void Utf8Constant::Parse(BytesInputStream *bis)
{
	length = bis->ReadU2();
	if (length < 0)
	{
		throw new BytesException("byte length is less than 0");
	}
	bis->ReadString(length, bytes);
	name = bytes;
}

void IntegerConstant::Parse(BytesInputStream *bis)
{
	bytes = bis->ReadU4();
	name = new CHAR[64];
	sprintf(name, "%d", bytes);
}

void FloatConstant::Parse(BytesInputStream *bis)
{
	bis->ReadBytes(4, bytes, true);
	FloatByteValue fv(bytes);
	name = new CHAR[64];
	sprintf(name, "%f", fv.fValue);
}

void LongConstant::Parse(BytesInputStream *bis)
{
	bis->ReadBytes(4, bytes2, true);
	bis->ReadBytes(4, bytes1, true);
	WByteValue wv(bytes1, bytes2);
	name = new CHAR[128];
	sprintf(name, "%ld", wv.lValue);
}

void DoubleConstant::Parse(BytesInputStream *bis)
{
	bis->ReadBytes(4, bytes2, true);
	bis->ReadBytes(4, bytes1, true);
	WByteValue wv(bytes1, bytes2);
	name = new CHAR[128];
	sprintf(name, "%lf", wv.dValue);
}

void ClassConstant::Parse(BytesInputStream *bis)
{
	index = bis->ReadU2();
	if (index <= 0)
	{
		throw new BytesException("Index is less than 0");
	}
}

void ClassConstant::ToString(LPSTR &result)
{
	if (!utf8)
	{
		result = NULL;
		return;
	}
	CHAR tmp[1024];
	sprintf(tmp, "#%d\t//%s", index, utf8->GetName());
	result = tmp;
}

void StringConstant::Parse(BytesInputStream *bis)
{
	index = bis->ReadU2();
	if (index <= 0)
	{
		throw new BytesException("Index is less than 0");
	}
}

void StringConstant::ToString(LPSTR &result)
{
	if (!utf8)
	{
		result = NULL;
		return;
	}
	CHAR tmp[1024];
	sprintf(tmp, "#%d\t//%s", index, utf8->GetName());
	result = tmp;
}

LPOBJECT_REF StringConstant::GetString()
{
	if (string_ref)
		return string_ref;
	if (!utf8 || !utf8->bytes)
	{
		return NULL;
	}
	VMContext::object_heap->NewStringObject(utf8->bytes, utf8->length, string_ref);
	return string_ref;
}

void NameTypeConstant::Parse(BytesInputStream *bis)
{
	nameIndex = bis->ReadU2();
	if (nameIndex <= 0)
	{
		throw new BytesException("Name string index is less than 0");
	}
	descIndex = bis->ReadU2();
	if (descIndex <= 0)
	{
		throw new BytesException("Desc string index is less than 0");
	}
}

void NameTypeConstant::ToString(LPSTR &result)
{
	if (!nameUtf8 || !descIndex)
	{
		result = NULL;
		return;
	}
	CHAR tmp[1024];
	sprintf(tmp, "#%d:%d\t//%s", nameIndex, descIndex, this->name);
	result =  tmp;
}

void FieldRefConstant::Parse(BytesInputStream *bis)
{
	classIndex = bis->ReadU2();
	if (classIndex <= 0)
	{
		throw new BytesException("Class index is less than 0");
	}
	typeIndex = bis->ReadU2();
	if (classIndex <= 0)
	{
		throw new BytesException("NameType index is less than 0");
	}
}

void FieldRefConstant::ToString(LPSTR &result)
{
	if (!clazz && !nameType)
	{
		result = NULL;
		return;
	}
	CHAR tmp[1024];
	sprintf(tmp, "#%d.%d\t//%s.%s", classIndex, typeIndex
		, clazz->GetName(), nameType->GetName());
	result = tmp;
}

bool ConstantPool::BuildConstant(U1 tag, lpConstant &constant)
{
	bool result = true;
	switch (tag)
	{
	case UTF8_CONSTANT:
		constant = new Utf8Constant();
		break;
	case INTEGER_CONSTANT:
		constant = new IntegerConstant();
		break;
	case FLOAT_CONSTANT:
		constant = new FloatConstant();
		break;
	case LONG_CONSTANT:
		constant = new LongConstant();
		break;
	case DOUBLE_CONSTANT:
		constant = new DoubleConstant();
		break;
	case CLASS_CONSTANT:
		constant = new ClassConstant();
		break;
	case STRING_CONSTANT:
		constant = new StringConstant();
		break;
	case FIELDREF_CONSTANT:
		constant = new FieldRefConstant();
		break;
	case METHODREF_CONSTANT:
		constant = new MethodRefConstant();
		break;
	case INTERFACE_CONSTANT:
		constant = new InterfaceMethodConstant();
		break;
	case NAMEANDTYPE_CONSTANT:
		constant = new NameTypeConstant();
		break;
	default:
		result = false;
		constant = NULL;
		break;
	}
	return result;
}

void ConstantPool::ReBuild()
{
//	int size = constants.size();
	for (int i = 0; i < count; i++)
	{
		if (constants[i])
		{
			ReBuild(constants[i]);
		}
	}
}

void ConstantPool::ReBuild(Constant* constant)
{
	if (!constant)
	{
		return;
	}
	switch (constant->tag)
	{
	case CLASS_CONSTANT:
		{
			ClassConstant *ct = (ClassConstant*) constant;
			ct->utf8 = (Utf8Constant*)this->GetConstant(ct->index);
			int len = strlen(ct->utf8->bytes);
			ct->name = new CHAR[len + 1];
			strcpy(ct->name, ct->utf8->bytes);
			ct->name[len] = '\0';
			break;
		}
	case STRING_CONSTANT:
		{
			StringConstant *ct = (StringConstant*) constant;
			ct->utf8 = (Utf8Constant*)this->GetConstant(ct->index);
			int len = strlen(ct->utf8->bytes);
			ct->name = new CHAR[len + 1];
			strcpy(ct->name, ct->utf8->bytes);
			ct->name[len] = '\0';
			break;
		}
	case NAMEANDTYPE_CONSTANT:
		{
			NameTypeConstant *ct = (NameTypeConstant*) constant;
			ct->nameUtf8 = (Utf8Constant*)this->GetConstant(ct->nameIndex);
			ct->descUtf8 = (Utf8Constant*)this->GetConstant(ct->descIndex);
			ct->name = new CHAR[512];
			sprintf(ct->name, "%s:%s", ct->nameUtf8->bytes, ct->descUtf8->bytes);
			break;
		}
	case INTERFACE_CONSTANT:
	case METHODREF_CONSTANT:
	case FIELDREF_CONSTANT:
		{
			FieldRefConstant *ct = (FieldRefConstant*) constant;
			ct->clazz = (ClassConstant*)this->GetConstant(ct->classIndex);
			ct->nameType = (NameTypeConstant*)this->GetConstant(ct->typeIndex);

			NameTypeConstant *nt = ct->nameType;
			nt->nameUtf8 = (Utf8Constant*)this->GetConstant(nt->nameIndex);
			nt->descUtf8 = (Utf8Constant*)this->GetConstant(nt->descIndex);

			Utf8Constant* classUtf8 = (Utf8Constant*)this->GetConstant(ct->clazz->index);
			ct->name = new CHAR[512];
			sprintf(ct->name, "%s.%s:%s", classUtf8->bytes, nt->nameUtf8->bytes, nt->descUtf8->bytes);

			ct->simple_name = new CHAR[256];
			sprintf(ct->simple_name, "%s:%s", nt->nameUtf8->bytes, nt->descUtf8->bytes);
			break;
		}
	default:
		break;
	}
}

void ConstantPool::Destroy()
{
	if (!constants)
	{
		return;
	}
	//int size = constants.size();
	for (int i = 0; i < count; i++)
	{
		if (constants[i])
		{
			delete constants[i];
			constants[i] = NULL;
		}
	}
	delete[] constants;
	constants = NULL;
}

void ConstantPool::Debug()
{
	//int size = constants.size();
	for (int i = 0; i < count; i++)
	{
		lpConstant ct = constants[i];
		if (!ct)
		{
			continue;
		}
		LPSTR log = NULL;
		ct->ToString(log);
		printf("#%d = %s\t\t%s\n", i + 1, ct->flag, log);
	}
}