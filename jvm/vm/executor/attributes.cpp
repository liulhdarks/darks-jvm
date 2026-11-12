#include "stdafx.h"
#include "attributes.h"



void JBootstrapMethods::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	(void)pool;
	JAttribute::Parse(bis, pool);
	num_bootstrap_methods = bis->ReadU2();
	methods.clear();
	for (U2 i = 0; i < num_bootstrap_methods; i++)
	{
		BootstrapMethodInfo info;
		info.bootstrap_method_ref = bis->ReadU2();
		info.num_bootstrap_arguments = bis->ReadU2();
		for (U2 j = 0; j < info.num_bootstrap_arguments; j++)
		{
			info.bootstrap_arguments.push_back(bis->ReadU2());
		}
		methods.push_back(info);
	}
}


JException::~JException()
{
	if (exception_table)
	{
		delete[] exception_table;
		exception_table = NULL;
	}
}

void JException::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	JAttribute::Parse(bis, pool);
	exception_count = bis->ReadU2();
	if (exception_count > 0)
	{
		exception_table = new lpClassConstant[exception_count];
		for (int i = 0; i < exception_count; i++)
		{
			U2 classIndex = bis->ReadU2();
			exception_table[i] = (lpClassConstant)pool->GetConstant(classIndex);
		}
	}
}

void JCodeAttribute::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	JAttribute::Parse(bis, pool);
	max_stack = bis->ReadU2();
	max_local = bis->ReadU2();
	code_length = bis->ReadU4();
	bis->ReadBytes(code_length, codes);
	exception_length = bis->ReadU2();
	if (exception_length > 0)
	{
		exception_table = new ExceptionTable[exception_length];
		for (int i = 0; i < exception_length; i++)
		{
			exception_table[i].start_pc = bis->ReadU2();
			exception_table[i].end_pc = bis->ReadU2();
			exception_table[i].handler_pc = bis->ReadU2();
			exception_table[i].catch_type = bis->ReadU2();
		}
	}
	attr_count = bis->ReadU2();
	for (int i = 0; i < attr_count; i++)
	{
		U2 attrIndex = bis->ReadU2();
		int length_pos = bis->Tell();
		int attr_len = bis->ReadInt();
		int payload_pos = bis->Tell();
		lpUtf8Constant uc = (lpUtf8Constant) pool->GetConstant(attrIndex);
		const char *name = uc ? uc->GetName() : NULL;
		if (name && strcmp(name, ATTR_LINENUMBER) == 0)
		{
			bis->SetCursor(length_pos);
			line_number_table = new JLineNumberTable();
			line_number_table->Parse(bis, pool);
			line_number_table->attr_name = uc;
		}
		else if (name && strcmp(name, ATTR_LOCALVAR) == 0)
		{
			bis->SetCursor(length_pos);
			local_var_table = new JLocalVariableTable();
			local_var_table->Parse(bis, pool);
			local_var_table->attr_name = uc;
		}
		else if (name && strcmp(name, ATTR_STACKMAP) == 0)
		{
			bis->SetCursor(length_pos);
			stack_map_table = new JStackMapTable();
			stack_map_table->Parse(bis, pool);
			stack_map_table->attr_name = uc;
		}
		else
		{
			// Unknown code sub-attribute (e.g. LocalVariableTypeTable on some
			// compilers): skip it instead of desynchronising the stream.
			bis->SetCursor(payload_pos + attr_len);
		}
	}
}

JLineNumberTable::~JLineNumberTable()
{
	if (line_number_infos)
	{
		delete[] line_number_infos;
		line_number_infos = NULL;
	}
}

void JLineNumberTable::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	line_number_infos = NULL;
	JAttribute::Parse(bis, pool);
	line_table_count = bis->ReadU2();
	if (line_table_count > 0)
	{
		line_number_infos = new LineNumberInfo[line_table_count];
		for (int i = 0; i < line_table_count; i++)
		{
			line_number_infos[i].start_pc = bis->ReadU2();
			line_number_infos[i].line_number = bis->ReadU2();
		}
	}
}

JLocalVariableTable::~JLocalVariableTable()
{
	if (local_var_infos)
	{
		delete[] local_var_infos;
		local_var_infos = NULL;
	}
}

void JLocalVariableTable::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	local_var_infos = NULL;
	JAttribute::Parse(bis, pool);
	lcoalvar_table_count = bis->ReadU2();
	if (lcoalvar_table_count > 0)
	{
		local_var_infos = new LocalVarInfo[lcoalvar_table_count];
		for (int i = 0; i < lcoalvar_table_count; i++)
		{
			local_var_infos[i].start_pc = bis->ReadU2();
			local_var_infos[i].length = bis->ReadU2();
			U2 nameIndex = bis->ReadU2();
			U2 descIndex = bis->ReadU2();
			local_var_infos[i].name = (lpUtf8Constant) pool->GetConstant(nameIndex);
			local_var_infos[i].desc = (lpUtf8Constant) pool->GetConstant(descIndex);
			local_var_infos[i].slot_index = bis->ReadU2();
		}
	}
}

void JSourceFile::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	JAttribute::Parse(bis, pool);
	U2 source_index = bis->ReadU2();
	this->source_file = (lpUtf8Constant) pool->GetConstant(source_index);
}

void JConstantValue::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	JAttribute::Parse(bis, pool);
	U2 constantvalue_index = bis->ReadU2();
	this->constant = pool->GetConstant(constantvalue_index);
}

void JStackMapTable::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	JAttribute::Parse(bis, pool);
	bis->ReadBytes(this->attr_length, bytes);
}

void JInnerClasses::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	inner_classes = NULL;
	JAttribute::Parse(bis, pool);
	number_of_classes = bis->ReadU2();
	if (number_of_classes > 0)
	{
		inner_classes = new InnerClassesInfo[number_of_classes];
		for (int i = 0; i < number_of_classes; i++)
		{
			inner_classes[i].inner_class_info_index = bis->ReadU2();
			inner_classes[i].outer_class_info_index = bis->ReadU2();
			inner_classes[i].inner_name_index = bis->ReadU2();
			inner_classes[i].inner_class_access_flags = bis->ReadU2();
		}
	}
}

void JSignature::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	JAttribute::Parse(bis, pool);
	signature_index = bis->ReadU2();
}

void JEnclosingMethod::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	JAttribute::Parse(bis, pool);
	class_index = bis->ReadU2();
	method_index = bis->ReadU2();
}

void JRuntimeVisibleAnnotations::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	JAttribute::Parse(bis, pool);
	bis->ReadBytes(this->attr_length, bytes);
}

void JRuntimeInvisibleAnnotations::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	JAttribute::Parse(bis, pool);
	bis->ReadBytes(this->attr_length, bytes);
}

void JRuntimeVisibleParameterAnnotations::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	JAttribute::Parse(bis, pool);
	bis->ReadBytes(this->attr_length, bytes);
}

void JRuntimeInvisibleParameterAnnotations::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	JAttribute::Parse(bis, pool);
	bis->ReadBytes(this->attr_length, bytes);
}

void JAnnotationDefault::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	JAttribute::Parse(bis, pool);
	bis->ReadBytes(this->attr_length, bytes);
}

void JLocalVariableTypeTable::Parse(BytesInputStream *bis, ConstantPool *pool)
{
	JAttribute::Parse(bis, pool);
	bis->ReadBytes(this->attr_length, bytes);
}

CommonAttributes::CommonAttributes()
{
	deprecated_attr = NULL;
	synthetic_attr = NULL;
	signature_attr = NULL;
	enclose_method_attr = NULL;
	rt_visible_attr = NULL;
	rt_invisible_attr = NULL;
	rt_visible_param_attr = NULL;
	rt_invisible_param_attr = NULL;
	local_var_type = NULL;
	annotation_default = NULL;
}

CommonAttributes::~CommonAttributes()
{
	if (deprecated_attr)delete deprecated_attr;
	if (synthetic_attr)delete synthetic_attr;
	if (signature_attr)delete signature_attr;
	if (enclose_method_attr)delete enclose_method_attr;
	if (rt_visible_attr)delete rt_visible_attr;
	if (rt_invisible_attr)delete rt_invisible_attr;
	if (rt_visible_param_attr)delete rt_visible_param_attr;
	if (rt_invisible_param_attr)delete rt_invisible_param_attr;
	if (local_var_type)delete local_var_type;
	if (annotation_default)delete annotation_default;
	deprecated_attr = NULL;
	synthetic_attr = NULL;
	signature_attr = NULL;
	enclose_method_attr = NULL;
	rt_visible_attr = NULL;
	rt_invisible_attr = NULL;
	rt_visible_param_attr = NULL;
	rt_invisible_param_attr = NULL;
	local_var_type = NULL;
	annotation_default = NULL;
}

bool CommonAttributes::ParseAttribute(lpUtf8Constant uc_name, BytesInputStream *bis, ConstantPool *pool)
{
	if (!uc_name || !uc_name->bytes)
	{
		return false;
	}
	if (strcmp(uc_name->bytes, ATTR_SIGNATURE) == 0)
	{
		signature_attr = new JSignature();
		signature_attr->Parse(bis, pool);
		signature_attr->attr_name = uc_name;
	}
	else if (strcmp(uc_name->bytes, ATTR_DEPRECATED) == 0)
	{
		deprecated_attr = new JDeprecated();
		deprecated_attr->Parse(bis, pool);
		deprecated_attr->attr_name = uc_name;
	}
	else if (strcmp(uc_name->bytes, ATTR_SYNTHETIC) == 0)
	{
		synthetic_attr = new JSynthetic();
		synthetic_attr->Parse(bis, pool);
		synthetic_attr->attr_name = uc_name;
	}
	else if (strcmp(uc_name->bytes, ATTR_ENCLOSING_METHOD) == 0)
	{
		enclose_method_attr = new JEnclosingMethod();
		enclose_method_attr->Parse(bis, pool);
		enclose_method_attr->attr_name = uc_name;
	} 
	else if (strcmp(uc_name->bytes, ATTR_RT_VA) == 0)
	{
		rt_visible_attr = new JRuntimeVisibleAnnotations();
		rt_visible_attr->Parse(bis, pool);
		rt_visible_attr->attr_name = uc_name;
	} 
	else if (strcmp(uc_name->bytes, ATTR_RT_INVA) == 0)
	{
		rt_invisible_attr = new JRuntimeInvisibleAnnotations();
		rt_invisible_attr->Parse(bis, pool);
		rt_invisible_attr->attr_name = uc_name;
	} 
	else if (strcmp(uc_name->bytes, ATTR_RT_VPA) == 0)
	{
		rt_visible_param_attr = new JRuntimeVisibleParameterAnnotations();
		rt_visible_param_attr->Parse(bis, pool);
		rt_visible_param_attr->attr_name = uc_name;
	} 
	else if (strcmp(uc_name->bytes, ATTR_RT_INVPA) == 0)
	{
		rt_invisible_param_attr = new JRuntimeInvisibleParameterAnnotations();
		rt_invisible_param_attr->Parse(bis, pool);
		rt_invisible_param_attr->attr_name = uc_name;
	} 
	else if (strcmp(uc_name->bytes, ATTR_ANNOTATION_DEFAULT) == 0)
	{
		annotation_default = new JAnnotationDefault();
		annotation_default->Parse(bis, pool);
		annotation_default->attr_name = uc_name;
	}
	else if (strcmp(uc_name->bytes, ATTR_LOCALVAR_TYPE) == 0)
	{
		local_var_type = new JLocalVariableTypeTable();
		local_var_type->Parse(bis, pool);
		local_var_type->attr_name = uc_name;
	} 
	else
	{
		return false;
	}
	return true;
}