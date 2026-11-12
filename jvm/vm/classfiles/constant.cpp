#include "stdafx.h"
#include "constant.h"
#include "utils/DebugUtils.h"
#include "vm/executor/context.h"

#include <string>

void Utf8Constant::Parse(BytesInputStream *bis)
{
	length = bis->ReadU2();
	bis->ReadString(length, bytes);
	// NOTE: deliberately do not alias "name" to "bytes"; both destructors would
	// free the same allocation (double free in the original implementation).
	name = NULL;
}

void IntegerConstant::Parse(BytesInputStream *bis)
{
	bytes = bis->ReadU4();
	name = new CHAR[64];
	sprintf(name, "%d", static_cast<int>(bytes));
}

void FloatConstant::Parse(BytesInputStream *bis)
{
	bis->ReadBytes(4, bytes, true);
	FloatByteValue fv(bytes);
	// "%g" keeps the text bounded even for very large/small values; the old
	// fixed 64-byte "%f" buffer overflowed for values such as 1e38.
	char tmp[64];
	snprintf(tmp, sizeof(tmp), "%g", fv.fValue);
	name = new CHAR[strlen(tmp) + 1];
	memcpy(name, tmp, strlen(tmp) + 1);
}

void LongConstant::Parse(BytesInputStream *bis)
{
	bis->ReadBytes(4, bytes2, true);
	bis->ReadBytes(4, bytes1, true);
	WByteValue wv(bytes1, bytes2);
	std::string text = std::to_string(wv.lValue);
	name = new CHAR[text.size() + 1];
	memcpy(name, text.c_str(), text.size() + 1);
}

void DoubleConstant::Parse(BytesInputStream *bis)
{
	bis->ReadBytes(4, bytes2, true);
	bis->ReadBytes(4, bytes1, true);
	WByteValue wv(bytes1, bytes2);
	char tmp[64];
	snprintf(tmp, sizeof(tmp), "%g", wv.dValue);
	name = new CHAR[strlen(tmp) + 1];
	memcpy(name, tmp, strlen(tmp) + 1);
}

void ClassConstant::Parse(BytesInputStream *bis)
{
	index = bis->ReadU2();
	if (index == 0)
	{
		throw new BytesException((LPSTR)"Index is less than 1");
	}
}

std::string ClassConstant::ToString()
{
	if (!utf8)
	{
		return std::string();
	}
	char tmp[1024];
	snprintf(tmp, sizeof(tmp), "#%d\t//%s", static_cast<int>(index), utf8->GetName() ? utf8->GetName() : "");
	return std::string(tmp);
}

void StringConstant::Parse(BytesInputStream *bis)
{
	index = bis->ReadU2();
	if (index == 0)
	{
		throw new BytesException((LPSTR)"Index is less than 1");
	}
}

std::string StringConstant::ToString()
{
	if (!utf8)
	{
		return std::string();
	}
	char tmp[1024];
	snprintf(tmp, sizeof(tmp), "#%d\t//%s", static_cast<int>(index), utf8->GetName() ? utf8->GetName() : "");
	return std::string(tmp);
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
	if (nameIndex == 0)
	{
		throw new BytesException((LPSTR)"Name string index is less than 1");
	}
	descIndex = bis->ReadU2();
	if (descIndex == 0)
	{
		throw new BytesException((LPSTR)"Desc string index is less than 1");
	}
}

std::string NameTypeConstant::ToString()
{
	if (!nameUtf8 || !descIndex)
	{
		return std::string();
	}
	char tmp[1024];
	snprintf(tmp, sizeof(tmp), "#%d:%d\t//%s:%s", static_cast<int>(nameIndex),
		static_cast<int>(descIndex),
		nameUtf8->GetName() ? nameUtf8->GetName() : "",
		descUtf8 && descUtf8->GetName() ? descUtf8->GetName() : "");
	return std::string(tmp);
}

void FieldRefConstant::Parse(BytesInputStream *bis)
{
	classIndex = bis->ReadU2();
	if (classIndex == 0)
	{
		throw new BytesException((LPSTR)"Class index is less than 1");
	}
	typeIndex = bis->ReadU2();
	if (typeIndex == 0)
	{
		throw new BytesException((LPSTR)"NameType index is less than 1");
	}
}

std::string FieldRefConstant::ToString()
{
	if (!clazz || !nameType)
	{
		return std::string();
	}
	char tmp[1024];
	snprintf(tmp, sizeof(tmp), "#%d.%d\t//%s.%s", classIndex, typeIndex,
		clazz->GetName() ? clazz->GetName() : "",
		nameType->GetName() ? nameType->GetName() : "");
	return std::string(tmp);
}


void MethodHandleConstant::Parse(BytesInputStream *bis)
{
	reference_kind = bis->ReadU1();
	reference_index = bis->ReadU2();
}

void MethodTypeConstant::Parse(BytesInputStream *bis)
{
	descriptor_index = bis->ReadU2();
}

void InvokeDynamicConstant::Parse(BytesInputStream *bis)
{
	bootstrap_method_attr_index = bis->ReadU2();
	name_and_type_index = bis->ReadU2();
}

void ModuleConstant::Parse(BytesInputStream *bis)
{
	name_index = bis->ReadU2();
}

void PackageConstant::Parse(BytesInputStream *bis)
{
	name_index = bis->ReadU2();
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
	case METHODHANDLE_CONSTANT:
		constant = new MethodHandleConstant();
		break;
	case METHODTYPE_CONSTANT:
		constant = new MethodTypeConstant();
		break;
	case DYNAMIC_CONSTANT:
		constant = new DynamicConstant();
		break;
	case INVOKEDYNAMIC_CONSTANT:
		constant = new InvokeDynamicConstant();
		break;
	case MODULE_CONSTANT:
		constant = new ModuleConstant();
		break;
	case PACKAGE_CONSTANT:
		constant = new PackageConstant();
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
	// Resolve plain constants first: a Fieldref/Methodref may appear before
	// the Class/NameAndType entries it points at, and its "simple_name" needs
	// those entries to be ready.
	for (int pass = 0; pass < 2; pass++)
	{
		for (int i = 0; i < count; i++)
		{
			lpConstant constant = constants[i];
			if (!constant)
			{
				continue;
			}
			bool reference = constant->tag == FIELDREF_CONSTANT ||
				constant->tag == METHODREF_CONSTANT ||
				constant->tag == INTERFACE_CONSTANT ||
				constant->tag == METHODHANDLE_CONSTANT ||
				constant->tag == INVOKEDYNAMIC_CONSTANT ||
				constant->tag == DYNAMIC_CONSTANT;
			if ((pass == 0) == reference)
			{
				continue;
			}
			ReBuild(constant);
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
			if (!ct->utf8 || !ct->utf8->bytes)
			{
				break;
			}
			std::string text(ct->utf8->bytes, ct->utf8->length);
			ct->name = new CHAR[text.size() + 1];
			memcpy(ct->name, text.c_str(), text.size() + 1);
			break;
		}
	case STRING_CONSTANT:
		{
			StringConstant *ct = (StringConstant*) constant;
			ct->utf8 = (Utf8Constant*)this->GetConstant(ct->index);
			if (!ct->utf8 || !ct->utf8->bytes)
			{
				break;
			}
			std::string text(ct->utf8->bytes, ct->utf8->length);
			ct->name = new CHAR[text.size() + 1];
			memcpy(ct->name, text.c_str(), text.size() + 1);
			break;
		}
	case NAMEANDTYPE_CONSTANT:
		{
			NameTypeConstant *ct = (NameTypeConstant*) constant;
			ct->nameUtf8 = (Utf8Constant*)this->GetConstant(ct->nameIndex);
			ct->descUtf8 = (Utf8Constant*)this->GetConstant(ct->descIndex);
			if (!ct->nameUtf8 || !ct->descUtf8)
			{
				break;
			}
			std::string text = std::string(ct->nameUtf8->bytes, ct->nameUtf8->length) + ":" +
				std::string(ct->descUtf8->bytes, ct->descUtf8->length);
			ct->name = new CHAR[text.size() + 1];
			memcpy(ct->name, text.c_str(), text.size() + 1);
			break;
		}
	case INTERFACE_CONSTANT:
	case METHODREF_CONSTANT:
	case FIELDREF_CONSTANT:
		{
			FieldRefConstant *ct = (FieldRefConstant*) constant;
			ct->clazz = (ClassConstant*)this->GetConstant(ct->classIndex);
			ct->nameType = (NameTypeConstant*)this->GetConstant(ct->typeIndex);
			if (!ct->clazz || !ct->clazz->utf8 || !ct->nameType)
			{
				break;
			}
			NameTypeConstant *nt = ct->nameType;
			if (!nt->nameUtf8 || !nt->descUtf8)
			{
				break;
			}
			std::string class_name(ct->clazz->utf8->bytes, ct->clazz->utf8->length);
			std::string member = std::string(nt->nameUtf8->bytes, nt->nameUtf8->length) + ":" +
				std::string(nt->descUtf8->bytes, nt->descUtf8->length);
			std::string full = class_name + "." + member;
			ct->name = new CHAR[full.size() + 1];
			memcpy(ct->name, full.c_str(), full.size() + 1);

			ct->simple_name = new CHAR[member.size() + 1];
			memcpy(ct->simple_name, member.c_str(), member.size() + 1);
			break;
		}
	case METHODHANDLE_CONSTANT:
		{
			MethodHandleConstant *ct = (MethodHandleConstant*) constant;
			ct->reference = this->GetConstant(ct->reference_index);
			break;
		}
	case METHODTYPE_CONSTANT:
		{
			MethodTypeConstant *ct = (MethodTypeConstant*) constant;
			ct->descriptor = (Utf8Constant*)this->GetConstant(ct->descriptor_index);
			if (ct->descriptor && ct->descriptor->bytes)
			{
				std::string text(ct->descriptor->bytes, ct->descriptor->length);
				ct->name = new CHAR[text.size() + 1];
				memcpy(ct->name, text.c_str(), text.size() + 1);
			}
			break;
		}
	case INVOKEDYNAMIC_CONSTANT:
	case DYNAMIC_CONSTANT:
		{
			InvokeDynamicConstant *ct = (InvokeDynamicConstant*) constant;
			ct->nameType = (NameTypeConstant*)this->GetConstant(ct->name_and_type_index);
			if (ct->nameType && ct->nameType->name)
			{
				std::string text(ct->nameType->name);
				ct->name = new CHAR[text.size() + 1];
				memcpy(ct->name, text.c_str(), text.size() + 1);
			}
			break;
		}
	case MODULE_CONSTANT:
		{
			ModuleConstant *ct = (ModuleConstant*) constant;
			ct->utf8 = (Utf8Constant*)this->GetConstant(ct->name_index);
			break;
		}
	case PACKAGE_CONSTANT:
		{
			PackageConstant *ct = (PackageConstant*) constant;
			ct->utf8 = (Utf8Constant*)this->GetConstant(ct->name_index);
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
	count = 0;
	capacity = 0;
}

void ConstantPool::Debug()
{
	for (int i = 0; i < count; i++)
	{
		lpConstant ct = constants[i];
		if (!ct)
		{
			continue;
		}
		std::string log = ct->ToString();
		printf("#%d = %s\t\t%s\n", i + 1, ct->flag ? ct->flag : "", log.c_str());
	}
}
