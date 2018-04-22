#pragma once

#include "vm/types.h"
#include "jclass.h"
#include "jmethod.h"
#include "localtable.h"
#include "operstack.h"
#include "exec_common.h"
#include "vm/classfiles/classloader.h"
#include "descriptor.h"
#include "jni/jni.h"

#define STATIC_METHOD 1
#define NORMAL_METHOD 2
#define SPECIAL_METHOD 3

class JVM_API StackFrame
{

private:
	int pc;
	int last_pc;
	lpClassLoader class_loader;
	lpClass clazz;
	lpMethod method;
	bool inited;
	LPBYTE codes;
	U4 code_length;
	bool static_method;
	lpThreadStack thread_stack;
	lpStackFrame prev_frame;
public:
	lpOperStack op_stack;
	lpLocalVarTable local_var_table;
	JNIEnv *jni_env;

public:
	StackFrame();

	StackFrame(lpThreadStack thread_stack, lpClass clazz, lpMethod method, lpClassLoader class_loader);

	~StackFrame();

	EventNotify Execute();

	void init(lpVariant vars = NULL, int count = 0);

private:

	EventNotify ExecuteCode();

	U2 ReadU2();

	short ReadShort();
	int ReadInt();

	bool LoadConstant(int index, Slot &slot);

	bool LoadConstantW(int index, Slot &slot1, Slot &slot2);
	bool ExecuteNewObject(U2 index);
	bool ExecuteNewArray(U1 index);
	bool ExecuteNewObjectArray(U2 index);
	bool ExecuteNewMultiArray(U2 index, int count);
	bool ExecuteSpecial(int index);
	bool ExecuteStatic(int index);
	bool ExecuteVirtual(int index, int type);
	bool ExecuteNormalMethod(lpClass clazz, lpMethod method, vector<TypeDesc> &type_desc);
	bool ExecuteNativeMethod(lpClass clazz, lpMethod method, vector<TypeDesc> &type_desc, TypeDesc &result_desc);

	void SetArrayIndex(int index, Slot &slot);
	void LoadArray(DATA_TYPE data_type);
	void ArrayLength();

	void PutStatic(U2 index);
	void GetStatic(U2 index);
	void GetField(U2 index);
	void PutField(U2 index);
	void TableSwitch();
	void LookupSwitch();
private:

	double popDouble();
	LONG64 popLong();
	bool StackToLocal(lpStackFrame frame, vector<TypeDesc> &type_desc, bool this_var = true);
};
