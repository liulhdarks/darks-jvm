#pragma once

#include "vm/types.h"
#include "vm/classfiles/constant.h"
#include "vm/io/bytes_io.h"

#define ATTR_EXCEPTION "Exceptions"
#define ATTR_CODE "Code"
#define ATTR_LINENUMBER "LineNumberTable"
#define ATTR_LOCALVAR "LocalVariableTable"
#define ATTR_AOURCEFILE "SourceFile"
#define ATTR_CONSTANTVAL "ConstantValue"
#define ATTR_STACKMAP "StackMapTable"
#define ATTR_INNERCLASSES "InnerClasses"
#define ATTR_SYNTHETIC "Synthetic"
#define ATTR_DEPRECATED "Deprecated"
#define ATTR_SIGNATURE "Signature"
#define ATTR_ENCLOSING_METHOD "EnclosingMethod"
#define ATTR_RT_VA "RuntimeVisibleAnnotations"
#define ATTR_RT_INVA "RuntimeInvisibleAnnotations"
#define ATTR_RT_VPA "RuntimeVisibleParameterAnnotations"
#define ATTR_RT_INVPA "RuntimeInvisibleParameterAnnotations"
#define ATTR_LOCALVAR_TYPE "LocalVariableTypeTable"

typedef struct ExceptionTable
{
	U2 start_pc;
	U2 end_pc;
	U2 handler_pc;
	U2 catch_type;
} ExceptionTable, *lpExceptionTable;

typedef struct LineNumberInfo
{
	U2 start_pc;
	U2 line_number;
} LineNumberInfo, *lpLineNumberInfo;

typedef struct LocalVarInfo
{
	U2 start_pc;
	U2 length;
	lpUtf8Constant name;
	lpUtf8Constant desc;
	U2 slot_index;
} LocalVarInfo, *lpLocalVarInfo;

typedef struct InnerClassesInfo
{
	U2 inner_class_info_index;
	U2 outer_class_info_index;
	U2 inner_name_index;
	U2 inner_class_access_flags;
} InnerClassesInfo, *lpInnerClassesInfo;

typedef class JAttribute
{
public:
	lpUtf8Constant attr_name;
	int attr_length;


public:

	JAttribute(){}

	virtual void Parse(BytesInputStream *bis, ConstantPool *pool)
	{
		attr_length= bis->ReadInt();
	}
} lpAttribute;


class JException : public JAttribute
{

public:
	U2 exception_count;
	lpClassConstant *exception_table;

public:
	JException(){}

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JException *lpException;

class JLineNumberTable : public JAttribute
{

public:
	U2 line_table_count;
	lpLineNumberInfo line_number_infos;;

public:
	JLineNumberTable(){}

	~JLineNumberTable();

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JLineNumberTable *lpLineNumberTable;

class JLocalVariableTable : public JAttribute
{

public:
	U2 lcoalvar_table_count;
	lpLocalVarInfo local_var_infos;;

public:
	JLocalVariableTable(){}

	~JLocalVariableTable();

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JLocalVariableTable *lpLocalVariableTable;

class JStackMapTable : public JAttribute
{

public:
	U2 number_of_entries;
	LPBYTE bytes;

public:
	JStackMapTable(){
		number_of_entries = 0;
		bytes = NULL;
	}

	~JStackMapTable()
	{
		if (bytes)
		{
			delete[] bytes;
			bytes = NULL;
		}
	}

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JStackMapTable *lpStackMapTable;

class JCodeAttribute : public JAttribute
{

public:
	U2 max_stack;
	U2 max_local;
	U4 code_length;
	LPBYTE codes;
	U2 exception_length;
	lpExceptionTable exception_table;
	U2 attr_count;
	lpLineNumberTable line_number_table; 
	lpLocalVariableTable local_var_table;
	lpStackMapTable stack_map_table;
public:

	JCodeAttribute()
	{
		codes = NULL;
		line_number_table = NULL;
		local_var_table = NULL;
		exception_table = NULL;
		stack_map_table = NULL;
	}

	~JCodeAttribute()
	{
		if (codes) delete[] codes;
		if (line_number_table) delete[] line_number_table;
		if (local_var_table) delete[] local_var_table;
		if (exception_table) delete[] exception_table;
		codes = NULL;
		line_number_table = NULL;
		local_var_table = NULL;
		exception_table = NULL;
	}

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JCodeAttribute *lpCodeAttribute;

class JSourceFile : public JAttribute
{

public:
	lpUtf8Constant source_file;;

public:
	JSourceFile(){}

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JSourceFile *lpSourceFile;

class JConstantValue : public JAttribute
{

public:
	lpConstant constant;

public:
	JConstantValue(){}

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JConstantValue *lpConstantValue;

class JInnerClasses : public JAttribute
{

public:
	U2 number_of_classes;
	lpInnerClassesInfo inner_classes;
public:
	JInnerClasses(){}

	~JInnerClasses()
	{
		if (inner_classes)delete[] inner_classes;
		inner_classes = NULL;
	}

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JInnerClasses *lpInnerClasses;


class JDeprecated : public JAttribute
{

public:
	
public:
	JDeprecated(){}
};

typedef JDeprecated *lpDeprecated;


class JSynthetic : public JAttribute
{

public:

public:
	JSynthetic(){}
};

typedef JSynthetic *lpSynthetic;


class JSignature : public JAttribute
{

public:
	U2 signature_index;
public:
	JSignature(){}

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JSignature *lpSignature;

class JEnclosingMethod : public JAttribute
{

public:
	U2 class_index;
	U2 method_index;
public:
	JEnclosingMethod(){}

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JEnclosingMethod *lpEnclosingMethod;


class JRuntimeVisibleAnnotations : public JAttribute
{

public:
	LPBYTE bytes;

public:
	JRuntimeVisibleAnnotations(){
		bytes = NULL;
	}

	~JRuntimeVisibleAnnotations()
	{
		if (bytes)
		{
			delete[] bytes;
			bytes = NULL;
		}
	}

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JRuntimeVisibleAnnotations *lpRuntimeVisibleAnnotations;

class JRuntimeInvisibleAnnotations : public JAttribute
{

public:
	LPBYTE bytes;

public:
	JRuntimeInvisibleAnnotations(){
		bytes = NULL;
	}

	~JRuntimeInvisibleAnnotations()
	{
		if (bytes)
		{
			delete[] bytes;
			bytes = NULL;
		}
	}

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JRuntimeInvisibleAnnotations *lpRuntimeInvisibleAnnotations;


class JRuntimeVisibleParameterAnnotations : public JAttribute
{

public:
	LPBYTE bytes;

public:
	JRuntimeVisibleParameterAnnotations(){
		bytes = NULL;
	}

	~JRuntimeVisibleParameterAnnotations()
	{
		if (bytes)
		{
			delete[] bytes;
			bytes = NULL;
		}
	}

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JRuntimeVisibleParameterAnnotations *lpRuntimeVisibleParameterAnnotations;

class JRuntimeInvisibleParameterAnnotations : public JAttribute
{

public:
	LPBYTE bytes;

public:
	JRuntimeInvisibleParameterAnnotations(){
		bytes = NULL;
	}

	~JRuntimeInvisibleParameterAnnotations()
	{
		if (bytes)
		{
			delete[] bytes;
			bytes = NULL;
		}
	}

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JRuntimeInvisibleParameterAnnotations *lpRuntimeInvisibleParameterAnnotations;

class JLocalVariableTypeTable : public JAttribute
{

public:
	LPBYTE bytes;

public:
	JLocalVariableTypeTable(){
		bytes = NULL;
	}

	~JLocalVariableTypeTable()
	{
		if (bytes)
		{
			delete[] bytes;
			bytes = NULL;
		}
	}

	void Parse(BytesInputStream *bis, ConstantPool *pool);
};

typedef JLocalVariableTypeTable *lpLocalVariableTypeTable;

class JVM_API CommonAttributes
{
public:
	lpDeprecated deprecated_attr;
	lpSynthetic synthetic_attr;
	lpSignature signature_attr;
	lpEnclosingMethod enclose_method_attr;
	lpRuntimeVisibleAnnotations rt_visible_attr;
	lpRuntimeInvisibleAnnotations rt_invisible_attr;
	lpRuntimeVisibleParameterAnnotations rt_visible_param_attr;
	lpRuntimeInvisibleParameterAnnotations rt_invisible_param_attr;
	lpLocalVariableTypeTable local_var_type;

	CommonAttributes();

	~CommonAttributes();

	bool ParseAttribute(lpUtf8Constant uc_name, BytesInputStream *bis, ConstantPool *pool);
};