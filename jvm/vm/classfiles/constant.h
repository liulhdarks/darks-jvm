#pragma once

#include "vm/types.h"
#include "vm/io/bytes_io.h"
#include "vm/exceptions.h"

#include <string>

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

	virtual ~Constant()
	{
		// Only owned strings are freed here. Utf8Constant keeps its payload in
		// "bytes" and never aliases it into "name".
		if (name) delete[] name;
		name = NULL;
		flag = NULL;
	}

	virtual void Parse(BytesInputStream *bis) = 0;

	virtual std::string ToString()
	{
		return name ? std::string(name) : std::string();
	}

	virtual LPSTR GetName()
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
	Utf8Constant() : Constant(UTF8_CONSTANT, (LPSTR)"Utf8")
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

	LPSTR GetName()
	{
		return bytes;
	}

	std::string ToString()
	{
		return bytes ? std::string(bytes, length) : std::string();
	}
};

typedef Utf8Constant *lpUtf8Constant;


class IntegerConstant : public Constant
{
public:
	U4 bytes;

public:
	IntegerConstant() : Constant(INTEGER_CONSTANT, (LPSTR)"Integer")
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
	FloatConstant() : Constant(FLOAT_CONSTANT, (LPSTR)"Float")
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
	LPBYTE bytes1;
	LPBYTE bytes2;

public:
	LongConstant() : Constant(LONG_CONSTANT, (LPSTR)"Long")
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
	DoubleConstant() : Constant(DOUBLE_CONSTANT, (LPSTR)"Double")
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
	ClassConstant() : Constant(CLASS_CONSTANT, (LPSTR)"Class")
	{
		utf8 = NULL;
	}

	void Parse(BytesInputStream *bis);

	std::string ToString();
};

typedef ClassConstant *lpClassConstant;

class StringConstant : public Constant
{
public:
	U2 index;

	Utf8Constant *utf8;
	LPOBJECT_REF string_ref;

public:
	StringConstant() : Constant(STRING_CONSTANT, (LPSTR)"String")
	{
		utf8 = NULL;
		string_ref = NULL;
	}

	void Parse(BytesInputStream *bis);

	LPOBJECT_REF GetString();

	std::string ToString();
};

class NameTypeConstant : public Constant
{
public:
	U2 nameIndex;
	U2 descIndex;

	Utf8Constant *nameUtf8;
	Utf8Constant *descUtf8;

public:
	NameTypeConstant() : Constant(NAMEANDTYPE_CONSTANT, (LPSTR)"NameAndType")
	{
		nameUtf8 = NULL;
		descUtf8 = NULL;
	}

	void Parse(BytesInputStream *bis);

	std::string ToString();
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
	FieldRefConstant() : Constant(FIELDREF_CONSTANT, (LPSTR)"Fieldref")
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

	~FieldRefConstant()
	{
		if (simple_name)
		{
			delete[] simple_name;
			simple_name = NULL;
		}
	}

	void Parse(BytesInputStream *bis);

	std::string ToString();
};

class MethodRefConstant : public FieldRefConstant
{

public:
	MethodRefConstant() : FieldRefConstant(METHODREF_CONSTANT, (LPSTR)"Methodref")
	{
		
	}
};

class InterfaceMethodConstant : public FieldRefConstant
{

public:
	InterfaceMethodConstant() : FieldRefConstant(INTERFACE_CONSTANT, (LPSTR)"Interface")
	{

	}
};

class JVM_API ConstantPool
{

private:
	lpConstant *constants;
	int count;
	int capacity;

public:
	ConstantPool()
	{
		constants = NULL;
		count = 0;
		capacity = 0;
	}

	~ConstantPool()
	{
		Destroy();
	}

	void init(int init_size)
	{
		constants = new lpConstant[init_size > 0 ? init_size : 1];
		for (int i = 0; i < init_size; i++)
		{
			constants[i] = NULL;
		}
		capacity = init_size > 0 ? init_size : 1;
		count = 0;
	}

	bool BuildConstant(U1 tag, lpConstant &constant);

	void AddConstant(lpConstant constant)
	{
		if (count >= capacity)
		{
			return;
		}
		constants[count++] = constant;
	}

	Constant *GetConstant(int index)
	{
		if (index < 1 || index > count)
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
