#pragma once;

#include "vm/types.h"
#include "vm/io/bytes_io.h"
#include "vm/exceptions.h"

class Constant
{
public:
	U1 tag;
	LPSTR flag;
	LPSTR name;

public:

	Constant(ConstantEnum tag, LPSTR flag)
	{
		this->tag = tag;
		this->flag = flag;
		name = NULL;
	}

	~Constant()
	{
		if (name) delete[] name;
		flag = NULL;
		name = NULL;
	}

	virtual void Parse(BytesInputStream *bis) = 0;

	virtual void ToString(LPSTR &result)
	{
		result =  name;
	}

	LPSTR GetName()
	{
		return name;
	}
};

typedef Constant *lpConstant;

class Utf8Constant : public Constant
{
public:
	U2 length;
	LPSTR bytes;

public:
	Utf8Constant() : Constant(UTF8_CONSTANT, "Utf8")
	{
		bytes = NULL;
		length = 0;
	}

	~Utf8Constant()
	{
		if (bytes) delete[] bytes;
		bytes = NULL;
	}

	void Parse(BytesInputStream *bis);
};

typedef Utf8Constant *lpUtf8Constant;


class IntegerConstant : public Constant
{
public:
	U4 bytes;

public:
	IntegerConstant() : Constant(INTEGER_CONSTANT, "Integer")
	{
		bytes = 0;
	}

	void Parse(BytesInputStream *bis);
};

class FloatConstant : public Constant
{
public:
	LPBYTE bytes;

public:
	FloatConstant() : Constant(FLOAT_CONSTANT, "Float")
	{
		bytes = NULL;
	}

	~FloatConstant()
	{
		if (bytes)
		{
			delete[] bytes;
			bytes = NULL;
		}
	}

	void Parse(BytesInputStream *bis);
};

class LongConstant : public Constant
{
public:
	//U8 bytes;
	LPBYTE bytes1;
	LPBYTE bytes2;

public:
	LongConstant() : Constant(LONG_CONSTANT, "Long")
	{
		bytes1 = NULL;
		bytes2 = NULL;
	}

	~LongConstant()
	{
		if (bytes1)
		{
			delete[] bytes1;
			bytes1 = NULL;
		}
		if (bytes2)
		{
			delete[] bytes2;
			bytes2 = NULL;
		}
	}

	void Parse(BytesInputStream *bis);
};

class DoubleConstant : public Constant
{
public:
	LPBYTE bytes1;
	LPBYTE bytes2;

public:
	DoubleConstant() : Constant(DOUBLE_CONSTANT, "Double")
	{
		bytes1 = NULL;
		bytes2 = NULL;
	}

	~DoubleConstant()
	{
		if (bytes1)
		{
			delete[] bytes1;
			bytes1 = NULL;
		}
		if (bytes2)
		{
			delete[] bytes2;
			bytes2 = NULL;
		}
	}

	void Parse(BytesInputStream *bis);
};

class ClassConstant : public Constant
{
public:
	U2 index;

	Utf8Constant *utf8;

public:
	ClassConstant() : Constant(CLASS_CONSTANT, "Class")
	{
		utf8 = NULL;
	}

	void Parse(BytesInputStream *bis);

	void ToString(LPSTR &result);
};

typedef ClassConstant *lpClassConstant;

class StringConstant : public Constant
{
public:
	U2 index;

	Utf8Constant *utf8;
	LPOBJECT_REF string_ref;

public:
	StringConstant() : Constant(STRING_CONSTANT, "String")
	{
		utf8 = NULL;
		string_ref = NULL;
	}

	void Parse(BytesInputStream *bis);

	LPOBJECT_REF GetString();

	void ToString(LPSTR &result);
};

class NameTypeConstant : public Constant
{
public:
	U2 nameIndex;
	U2 descIndex;

	Utf8Constant *nameUtf8;
	Utf8Constant *descUtf8;

public:
	NameTypeConstant() : Constant(NAMEANDTYPE_CONSTANT, "NameAndType")
	{
		nameUtf8 = NULL;
		descUtf8 = NULL;
	}

	void Parse(BytesInputStream *bis);

	void ToString(LPSTR &result);
};

class FieldRefConstant : public Constant
{
public:
	int classIndex;
	int typeIndex;

	ClassConstant *clazz;
	NameTypeConstant *nameType;
	LPSTR simple_name;

public:
	FieldRefConstant() : Constant(FIELDREF_CONSTANT, "Fieldref")
	{
		clazz = NULL;
		nameType = NULL;
		simple_name = NULL;
	}

	FieldRefConstant(ConstantEnum tag, LPSTR flag) : Constant(tag, flag)
	{
		clazz = NULL;
		nameType = NULL;
		simple_name = NULL;
	}

	void Parse(BytesInputStream *bis);

	void ToString(LPSTR &result);
};

class MethodRefConstant : public FieldRefConstant
{

public:
	MethodRefConstant() : FieldRefConstant(FIELDREF_CONSTANT, "Methodref")
	{
		
	}
};

class InterfaceMethodConstant : public FieldRefConstant
{

public:
	InterfaceMethodConstant() : FieldRefConstant(FIELDREF_CONSTANT, "Interface")
	{

	}
};

class JVM_API ConstantPool
{

private:
	//vector<Constant*> constants;
	lpConstant *constants;
	int count;

public:
	ConstantPool()
	{
		constants = NULL;
	}

	void init(int init_size)
	{
		constants = new lpConstant[init_size];
		count = 0;
	}

	bool BuildConstant(U1 tag, lpConstant &constant);

	void AddConstant(lpConstant constant)
	{
		//constants.push_back(constant);
		constants[count++] = constant;
	}

	Constant *GetConstant(int index)
	{
		if (count < index || index < 1)
		{
			return NULL;
		}
		return constants[index - 1];
	}

	void ReBuild();

	void ReBuild(Constant* constant);

	void Destroy();

	void Debug();

	int size()
	{
		return count;
	}
};
