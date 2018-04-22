#include "stdafx.h"
#include "parseclass.h"
#include "vm/io/bytes_io.h"
#include "vm/executor/methodarea.h"
#include "utils/DebugUtils.h"

#define MAIN_METHOD "main:([Ljava/lang/String;)V"
#define CLINIT_METHOD "<clinit>:()V"

bool read_file_header(BytesInputStream *bis, lpClass &clazz);
bool read_constant_pool(BytesInputStream *bis, lpClass &clazz);
bool read_class_header(BytesInputStream *bis, lpClass &clazz);
bool read_fields_info(BytesInputStream *bis, lpClass &clazz);
bool read_field_info(BytesInputStream *bis, lpClass &clazz, lpField &field);
bool read_methods_info(BytesInputStream *bis, lpClass &clazz);
bool read_method_info(BytesInputStream *bis, lpClass &clazz, lpMethod &method);
bool read_attributes_info(BytesInputStream *bis, lpClass &clazz);

bool parse_class_bytes(LPBYTE bytes, int length, lpClass &clazz)
{
	BytesInputStream *bis = new BytesInputStream(bytes, length);
	if (!bis)
	{
		cout<<"Fail to create bytes input stream."<<endl;
		return false;
	}

	clazz = new Class();
	if (!read_file_header(bis, clazz))
	{
		cout<<"Fail to read file header."<<endl;
		return false;
	}

	if (!read_constant_pool(bis, clazz))
	{
		cout<<"Fail to read constant pool."<<endl;
		return false;
	}

	if (!read_class_header(bis, clazz))
	{
		cout<<"Fail to read class header."<<endl;
		return false;
	}

	if (!read_fields_info(bis, clazz))
	{
		cout<<"Fail to read fields."<<endl;
		return false;
	}

	if (!read_methods_info(bis, clazz))
	{
		cout<<"Fail to read methods."<<endl;
		return false;
	}

	if (!read_attributes_info(bis, clazz))
	{
		cout<<"Fail to read class attributes."<<endl;
		return false;
	}

	if (bis)
	{
		bis->Close();
		delete bis;
		bis = NULL;
	}
	return true;
}

bool read_file_header(BytesInputStream *bis, lpClass &clazz)
{
	U4 magic = bis->ReadU4();
	//DebugUtils::trace("Magic: %x\n", magic);
	if (magic != 0xCAFEBABE)
	{
		OutputDebugString("Magic number is invalid.");
		return false;
	}
	U2 minor = bis->ReadU2();
	U2 major = bis->ReadU2();
	//DebugUtils::trace("Minor: %u\nmajor: %u\n", minor, major);
	return true;
}

bool read_constant_pool(BytesInputStream *bis, lpClass &clazz)
{
	U2 count = bis->ReadU2();
	if (count <= 0)
	{
		DebugUtils::trace("Constants count is zero.\n");
		return false;
	}
	clazz->const_pool->init(count);
	for (int i = 1; i <= count - 1; i++)
	{
		U1 tag = bis->ReadU1();
		lpConstant ct = NULL;
		if (clazz->const_pool->BuildConstant(tag, ct))
		{
			ct->Parse(bis);
			switch (ct->tag)
			{
			case LONG_CONSTANT:
			case DOUBLE_CONSTANT:
				clazz->const_pool->AddConstant(ct);
				clazz->const_pool->AddConstant(NULL);
				i++;
				break;
			default:
				clazz->const_pool->AddConstant(ct);
				break;
			}
			
		}
		else
		{
			DebugUtils::trace("Fail to parse constant tag %u\n", tag);
		}
	}
	clazz->const_pool->ReBuild();
	//clazz->const_pool->Debug();
	return true;
}

bool read_class_header(BytesInputStream *bis, lpClass &clazz)
{
	U2 flag = bis->ReadU2();
	clazz->accFlag = flag;
	U2 classIndex = bis->ReadU2();
	U2 superIndex = bis->ReadU2();
	U2 ifCount = bis->ReadU2();
	clazz->interfaceCount = ifCount;
	if (ifCount > 0)
	{
		clazz->interfaces = new lpClassConstant[ifCount];
	}
	lpClassConstant ct = NULL;
	for (U2 i = 0 ; i < ifCount; i++)
	{
		U2 ifIndex = bis->ReadU2();
		clazz->interfaces[i] = (lpClassConstant) clazz->const_pool->GetConstant(ifIndex);
	}
	clazz->thisClass = (lpClassConstant) clazz->const_pool->GetConstant(classIndex);
	DebugUtils::print(2, "class ", clazz->thisClass->GetName());

	if (superIndex > 0)
	{
		clazz->superClass = (lpClassConstant) clazz->const_pool->GetConstant(superIndex);
		DebugUtils::print(2, "\textends ", clazz->superClass->GetName());
	}
	return true;
}

bool read_fields_info(BytesInputStream *bis, lpClass &clazz)
{
	clazz->fieldCount = bis->ReadU2();
	if (clazz->fieldCount > 0)
	{
		clazz->fields = new lpField[clazz->fieldCount];
		for (U2 i = 0; i < clazz->fieldCount; i++)
		{
			lpField field = NULL;
			if (read_field_info(bis, clazz, field))
			{
				clazz->fields[i] = field;
				clazz->StoreField(field->GetSimpleName(), field);
			}
		}
	}
	return true;
}

bool read_field_info(BytesInputStream *bis, lpClass &clazz, lpField &field)
{
	field = new Field();
	field->accFlag = bis->ReadU2();
	U2 nameIndex = bis->ReadU2();
	U2 descIndex = bis->ReadU2();
	field->fieldName = (lpUtf8Constant) clazz->const_pool->GetConstant(nameIndex);
	field->fieldDesc = (lpUtf8Constant) clazz->const_pool->GetConstant(descIndex);
	field->attrCount = bis->ReadU2();
	if (field->attrCount > 0)
	{
		for (int i = 0; i < field->attrCount; i++)
		{
			U2 attrIndex = bis->ReadU2();
			lpUtf8Constant uc = (lpUtf8Constant) clazz->const_pool->GetConstant(attrIndex);
			if (strcmp(uc->bytes, ATTR_CONSTANTVAL) == 0)
			{
				field->constValue = new JConstantValue();
				field->constValue->Parse(bis, clazz->const_pool);
				field->constValue->attr_name = uc;
			}
			else if (!field->ParseAttribute(uc, bis, clazz->const_pool))
			{
				throw VMException("Can not find field attibute.");
			}
		}
	}
	return true;
}

bool read_methods_info(BytesInputStream *bis, lpClass &clazz)
{
	clazz->methodCount = bis->ReadU2();
	if (clazz->methodCount > 0)
	{
		clazz->methods = new lpMethod[clazz->methodCount];
		for (U2 i = 0; i < clazz->methodCount; i++)
		{
			lpMethod method = NULL;
			if (read_method_info(bis, clazz, method))
			{
				method->clazz = clazz;
				clazz->methods[i] = method;
				MethodArea::AddMethod(clazz, method);
			}
		}
	}
	return true;
}

bool read_method_info(BytesInputStream *bis, lpClass &clazz, lpMethod &method)
{
	method = new Method();
	method->accFlag = bis->ReadU2();
	U2 nameIndex = bis->ReadU2();
	U2 descIndex = bis->ReadU2();
	method->methodName = (lpUtf8Constant) clazz->const_pool->GetConstant(nameIndex);
	method->methodDesc = (lpUtf8Constant) clazz->const_pool->GetConstant(descIndex);
	method->attrCount = bis->ReadU2();
	if (method->attrCount > 0)
	{
		for (int i = 0; i < method->attrCount; i++)
		{
			U2 attrIndex = bis->ReadU2();
			lpUtf8Constant uc = (lpUtf8Constant) clazz->const_pool->GetConstant(attrIndex);
			if (strcmp(uc->bytes, ATTR_CODE) == 0)
			{
				method->code_attr = new JCodeAttribute();
				method->code_attr->Parse(bis, clazz->const_pool);
				method->code_attr->attr_name = uc;
			}
			else if (strcmp(uc->bytes, ATTR_EXCEPTION) == 0)
			{
				method->exception_attr = new JException();
				method->exception_attr->Parse(bis, clazz->const_pool);
				method->exception_attr->attr_name = uc;
			}
			else if (!method->ParseAttribute(uc, bis, clazz->const_pool))
			{
				throw VMException("Can not find method attibute.");
			}
		}
	}
	method->fullName = new char[512];
	sprintf(method->fullName, "%s:%s", method->methodName->bytes, method->methodDesc->bytes);
	if (strcmp(method->fullName, MAIN_METHOD) == 0)
	{
		clazz->mainMethod = method;
	}
	else if (strcmp(method->fullName, CLINIT_METHOD) == 0)
	{
		clazz->clinitMethod = method;
	}
	return true;
}

bool read_attributes_info(BytesInputStream *bis, lpClass &clazz)
{
	U2 attr_count = bis->ReadU2();
	if (attr_count <= 0)
	{
		return true;
	}
	for (int i = 0; i < attr_count; i++)
	{
		U2 attrIndex = bis->ReadU2();
		lpUtf8Constant uc = (lpUtf8Constant) clazz->const_pool->GetConstant(attrIndex);
		if (strcmp(uc->bytes, ATTR_AOURCEFILE) == 0)
		{
			clazz->source_file = new JSourceFile();
			clazz->source_file->Parse(bis, clazz->const_pool);
			clazz->source_file->attr_name = uc;
		}
		else if (strcmp(uc->bytes, ATTR_INNERCLASSES) == 0)
		{
			clazz->inner_classes = new JInnerClasses();
			clazz->inner_classes->Parse(bis, clazz->const_pool);
			clazz->inner_classes->attr_name = uc;
		}
		else if (!clazz->ParseAttribute(uc, bis, clazz->const_pool))
		{
			throw VMException("Can not find class attibute.");
		}
	}
	return true;
}
