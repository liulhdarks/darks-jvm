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

namespace {

// Reads an attribute header (name_index + length) and leaves the cursor at the
// attribute payload. Unknown attributes are skipped instead of aborting the
// class parse (the original implementation threw for every unknown attribute).
struct AttributeHeader
{
	U2 name_index;
	U4 length;
	int payload_pos;
};

AttributeHeader read_attribute_header(BytesInputStream *bis)
{
	AttributeHeader h;
	h.name_index = bis->ReadU2();
	int length_pos = bis->Tell();
	h.length = static_cast<U4>(bis->ReadInt());
	h.payload_pos = bis->Tell();
	return h;
}

} // namespace

bool parse_class_bytes(LPBYTE bytes, int length, lpClass &clazz)
{
	BytesInputStream *bis = new BytesInputStream(bytes, length);
	if (!bis)
	{
		DebugUtils::print("Fail to create bytes input stream.");
		return false;
	}

	clazz = new Class();
	if (!read_file_header(bis, clazz))
	{
		DebugUtils::print("Fail to read file header.");
		bis->Close();
		delete bis;
		return false;
	}

	if (!read_constant_pool(bis, clazz))
	{
		DebugUtils::print("Fail to read constant pool.");
		bis->Close();
		delete bis;
		return false;
	}

	if (!read_class_header(bis, clazz))
	{
		DebugUtils::print("Fail to read class header.");
		bis->Close();
		delete bis;
		return false;
	}

	if (!read_fields_info(bis, clazz))
	{
		DebugUtils::print("Fail to read fields.");
		bis->Close();
		delete bis;
		return false;
	}

	if (!read_methods_info(bis, clazz))
	{
		DebugUtils::print("Fail to read methods.");
		bis->Close();
		delete bis;
		return false;
	}

	if (!read_attributes_info(bis, clazz))
	{
		DebugUtils::print("Fail to read class attributes.");
		bis->Close();
		delete bis;
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
	if (magic != 0xCAFEBABE)
	{
		DebugUtils::print("Magic number is invalid.");
		return false;
	}
	bis->ReadU2(); // minor
	bis->ReadU2(); // major
	return true;
}

bool read_constant_pool(BytesInputStream *bis, lpClass &clazz)
{
	U2 count = bis->ReadU2();
	if (count == 0)
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
			// Unsupported constant pool tags cannot be skipped safely because
			// their payload size is tag dependent. Fail loudly rather than
			// silently corrupting every following index.
			DebugUtils::trace("Unsupported constant tag %u\n", static_cast<unsigned>(tag));
			return false;
		}
	}
	clazz->const_pool->ReBuild();
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
	for (U2 i = 0 ; i < ifCount; i++)
	{
		U2 ifIndex = bis->ReadU2();
		clazz->interfaces[i] = (lpClassConstant) clazz->const_pool->GetConstant(ifIndex);
	}
	clazz->thisClass = (lpClassConstant) clazz->const_pool->GetConstant(classIndex);
	if (!clazz->thisClass || !clazz->thisClass->GetName())
	{
		DebugUtils::print("Class name constant is invalid.");
		return false;
	}
	DebugUtils::print(2, "class ", clazz->thisClass->GetName());

	if (superIndex > 0)
	{
		clazz->superClass = (lpClassConstant) clazz->const_pool->GetConstant(superIndex);
		if (clazz->superClass)
		{
			DebugUtils::print(2, "\textends ", clazz->superClass->GetName());
		}
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
	for (int i = 0; i < field->attrCount; i++)
	{
		AttributeHeader h = read_attribute_header(bis);
		lpUtf8Constant uc = (lpUtf8Constant) clazz->const_pool->GetConstant(h.name_index);
		const char *name = uc ? uc->GetName() : NULL;
		if (name && strcmp(name, ATTR_CONSTANTVAL) == 0)
		{
			bis->SetCursor(h.payload_pos - 4);
			field->constValue = new JConstantValue();
			field->constValue->Parse(bis, clazz->const_pool);
			field->constValue->attr_name = uc;
		}
		else if (name && field->ParseAttribute(uc, bis, clazz->const_pool))
		{
			// Parsed by the common attribute handler.
		}
		else
		{
			// Unknown attribute: skip its payload and keep going.
			bis->SetCursor(h.payload_pos + static_cast<int>(h.length));
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
	for (int i = 0; i < method->attrCount; i++)
	{
		AttributeHeader h = read_attribute_header(bis);
		lpUtf8Constant uc = (lpUtf8Constant) clazz->const_pool->GetConstant(h.name_index);
		const char *name = uc ? uc->GetName() : NULL;
		if (name && strcmp(name, ATTR_CODE) == 0)
		{
			bis->SetCursor(h.payload_pos - 4);
			method->code_attr = new JCodeAttribute();
			method->code_attr->Parse(bis, clazz->const_pool);
			method->code_attr->attr_name = uc;
		}
		else if (name && strcmp(name, ATTR_EXCEPTION) == 0)
		{
			bis->SetCursor(h.payload_pos - 4);
			method->exception_attr = new JException();
			method->exception_attr->Parse(bis, clazz->const_pool);
			method->exception_attr->attr_name = uc;
		}
		else if (name && method->ParseAttribute(uc, bis, clazz->const_pool))
		{
		}
		else
		{
			bis->SetCursor(h.payload_pos + static_cast<int>(h.length));
		}
	}
	if (!method->methodName || !method->methodDesc)
	{
		return false;
	}
	method->fullName = new char[512];
	snprintf(method->fullName, 512, "%s:%s", method->methodName->bytes, method->methodDesc->bytes);
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
	if (attr_count == 0)
	{
		return true;
	}
	for (int i = 0; i < attr_count; i++)
	{
		AttributeHeader h = read_attribute_header(bis);
		lpUtf8Constant uc = (lpUtf8Constant) clazz->const_pool->GetConstant(h.name_index);
		const char *name = uc ? uc->GetName() : NULL;
		if (name && strcmp(name, ATTR_AOURCEFILE) == 0)
		{
			bis->SetCursor(h.payload_pos - 4);
			clazz->source_file = new JSourceFile();
			clazz->source_file->Parse(bis, clazz->const_pool);
			clazz->source_file->attr_name = uc;
		}
		else if (name && strcmp(name, ATTR_INNERCLASSES) == 0)
		{
			bis->SetCursor(h.payload_pos - 4);
			clazz->inner_classes = new JInnerClasses();
			clazz->inner_classes->Parse(bis, clazz->const_pool);
			clazz->inner_classes->attr_name = uc;
		}
		else if (name && clazz->ParseAttribute(uc, bis, clazz->const_pool))
		{
		}
		else
		{
			bis->SetCursor(h.payload_pos + static_cast<int>(h.length));
		}
	}
	return true;
}
