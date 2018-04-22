#include "stdafx.h"
#include "stackframe.h"
#include "vm/exceptions.h"
#include "code.h"
#include "vm/classfiles/constant.h"
#include "context.h"
#include "utils/DebugUtils.h"
#include "exec_common.h"
#include "threadstack.h"
#include "methodarea.h"

StackFrame::StackFrame()
{
	inited = false;
	op_stack = NULL;
	local_var_table = NULL;
	clazz = NULL;
	method = NULL;
	codes = NULL;
	prev_frame = NULL;
	jni_env = NULL;
}

StackFrame::StackFrame(lpThreadStack thread_stack, lpClass clazz, lpMethod method, lpClassLoader class_loader)
{
	this->clazz = clazz;
	this->method = method;
	this->class_loader = class_loader;
	this->thread_stack = thread_stack;
	inited = false;
	op_stack = NULL;
	local_var_table = NULL;
	codes = NULL;
	prev_frame = NULL;
	jni_env = NULL;
	static_method = method->isStatic();
}

StackFrame::~StackFrame()
{
	if (op_stack)
	{
		delete[] op_stack;
		op_stack = NULL;
	}
	if (local_var_table)
	{
		delete[] local_var_table;
		local_var_table = NULL;
	}
}

void StackFrame::init(lpVariant vars, int count)
{
	if (!method || !method->code_attr)
	{
		throw new VMExecutorException("Stack frame method is invalid.");
	}
	pc = 0;
	inited = true;
	codes = method->code_attr->codes;
	if (!codes)
	{
		throw new VMExecutorException("Stack frame method codes is invalid.");
	}

	code_length = method->code_attr->code_length;
	int max_local = method->code_attr->max_local;
	int max_stack = method->code_attr->max_stack;
	op_stack = new OperStack(max_stack);
	local_var_table = new LocalVarTable(max_local);
	for (int i = 0; i < count; i++)
	{
		local_var_table->set(i, vars[i]);
	}
	DebugUtils::print(4, "Invoke method:", clazz->thisClass->GetName(), "@", method->fullName);
	DebugUtils::print("Code:", false);
	DebugUtils::print(codes, code_length);
	DebugUtils::print("");
}

EventNotify StackFrame::Execute()
{
	if (!inited)
	{
		init();
	}

	while (1)
	{
		EventNotify ret = ExecuteCode();
		if (ret != EN_NOOP)
		{
			return ret;
		}
	}
}

EventNotify StackFrame::ExecuteCode()
{
	if (pc >= code_length)
	{
		return EN_EXCEPTION;
	}
	Slot s1, s2, s3;
	int index = 0;
	U2 u2 = 0;
	LONG64 lValue,lValue2;
	double dValue,dValue2;
	last_pc = pc;
	BYTE code = codes[pc++];
	switch (code)
	{
	case nop:
		break;

	///////////////// Stack Operations ////////////////
	//Instructions that push a constant onto the stack
	case iconst_m1:
	case iconst_0:
	case iconst_1:
	case iconst_2:
	case iconst_3:
	case iconst_4:
	case iconst_5:
		op_stack->push((int)(code - iconst_0));
		break;

	case aconst_null:			
		op_stack->push(0);
		break;

	case lconst_0:// 9 /*(0x9)*/ 
	case lconst_1:// 10 /*(0xa)*/
		op_stack->push((long)(code - lconst_0));
		break;
	case fconst_0:
	case fconst_1:
	case fconst_2:
		op_stack->push((float)(code - fconst_0));
		break;
	case dconst_0:// 9 /*(0x9)*/ 
	case dconst_1:// 10 /*(0xa)*/
		op_stack->push((double)(code - dconst_0));
		break;

	case bipush:// 16 /*(0x10)*/
		op_stack->push((U1) codes[pc]);
		pc++;
		break;
	case sipush:// 17 /*(0x11)*/
		u2 = ReadU2();
		op_stack->push((short) (u2 & 0xFFFF));
		break;

	case ldc: //Push item from constant pool 			
		LoadConstant(codes[pc++], s1);
		op_stack->push(s1);
		break;

	case ldc2_w:// 20 /*(0x14)*/		
		u2 = ReadU2();
		LoadConstantW(u2, s1, s2);
		op_stack->push(s1);
		op_stack->push(s2);
		break;

		//Instructions that load a local variable onto the stack
	case aload:// 25 /*(0x19)*/
		s1 = local_var_table->get(codes[pc]);
		op_stack->push(s1);
		pc++;
		break;

	case iload:// 21 /*(0x15)*/
		s1 = local_var_table->get(codes[pc]);
		op_stack->push(s1);
		pc++;
		break;
	case iload_0: //26 Load int from local variable 0 
	case iload_1: //27 Load int from local variable 1 
	case iload_2: //28 Load int from local variable 2 
	case iload_3: //29 Load int from local variable 3 
		s1 = local_var_table->get(code - iload_0);
		op_stack->push(s1);	
		break;


	case lload:// 22 /*(0x16)*/
		index = codes[pc];
		s1 = local_var_table->get(index);
		s2 = local_var_table->get(index + 1);
		op_stack->push(s1);
		op_stack->push(s2);
		break;
	case lload_0:// 30 /*(0x1e) */
	case lload_1:// 31 /*(0x1f) */
	case lload_2:// 32 /*(0x20) */
	case lload_3:// 33 /*(0x21) */
		index = code - lload_0;
		s1 = local_var_table->get(index);
		s2 = local_var_table->get(index + 1);
		op_stack->push(s1);
		op_stack->push(s2);
		break;

	case fload_0: // 34 /*(0x22)*/ 
	case fload_1: // 35 /*(0x23) */
	case fload_2: // 36 /*(0x24) */
	case fload_3: // 37 /*(0x25)*/
		s1 = local_var_table->get(code - fload_0);
		op_stack->push(s1);			
		break;		

	case aload_0:  //42 Load reference from local variable 0
	case aload_1:  //Load reference from local variable 1
	case aload_2:  //Load reference from local variable 2
	case aload_3:  //Load reference from local variable 3
		s1 = local_var_table->get(code - aload_0);
		op_stack->push(s1);
		break;

	case iaload:// 46 /*(0x2e)*/Load int from array
		LoadArray(DT_INT);
		break;
	case laload:
		LoadArray(DT_LONG);
		break;
	case faload:
		LoadArray(DT_FLOAT);
		break;
	case daload:
		LoadArray(DT_DOUBLE);
		break;
	case aaload://50 
		LoadArray(DT_REF);
		break;
	case baload:
		LoadArray(DT_BYTE);
		break;
	case caload:
		LoadArray(DT_CHAR);
		break;
	case saload:
		LoadArray(DT_SHORT);
		break;
		//Instructions that store a value from the stack into a local variable

	case istore:// 54 /*(0x36)*/
		op_stack->pop(s1);
		local_var_table->set(codes[pc], s1);
		pc++;
		break;
	case lstore:
		index = codes[pc];
		op_stack->pop(s1);
		local_var_table->set(index + 1, s1);
		op_stack->pop(s1);
		local_var_table->set(index, s1);
		pc++;
		break;
	case fstore:
		op_stack->pop(s1);
		local_var_table->set(codes[pc], s1);
		pc++;
		break;
	case dstore:
		index = codes[pc];
		op_stack->pop(s1);
		local_var_table->set(index + 1, s1);
		op_stack->pop(s1);
		local_var_table->set(index, s1);
		pc++;
		break;
	case astore:// 58 (0x3a)
		op_stack->pop(s1);
		local_var_table->set(codes[pc], s1);
		pc++;
		break;
	case istore_0: // 59 /*(0x3b)*/ 
	case istore_1: // 60 /*(0x3c) */
	case istore_2: // 61 /*(0x3d) */
	case istore_3: // 62 /*(0x3e)*/		
		op_stack->pop(s1);
		local_var_table->set(code - istore_0, s1);
		break;

	case lstore_0: // 63 /*(0x3f) */
	case lstore_1: // 64 /*(0x40) */
	case lstore_2: // 65 /*(0x41) */
	case lstore_3: // 66 /*(0x42) */
		index = code - lstore_0;
		op_stack->pop(s1);
		local_var_table->set(index + 1, s1);
		op_stack->pop(s1);
		local_var_table->set(index, s1);
		break;

	case fstore_0:
	case fstore_1:
	case fstore_2:
	case fstore_3:
		op_stack->pop(s1);
		local_var_table->set(code - fstore_0, s1);
		break;

	case dstore_0: 
	case dstore_1: 
	case dstore_2: 
	case dstore_3: 
		op_stack->pop(s1);
		index = code - dstore_0;
		local_var_table->set(index + 1, s1);
		op_stack->pop(s1);
		local_var_table->set(index, s1);
		break;

	case astore_0:// 75 /*(0x4b) Store reference into local variable 0*/
	case astore_1:// 76 /*(0x4c) */
	case astore_2:// 77 /*(0x4d) */
	case astore_3:// 78 /*(0x4e)*/
		op_stack->pop(s1);
		local_var_table->set(code - astore_0, s1);
		break;

	case iastore:// 79 /*(0x4f)*/
	case fastore:
	case bastore:
	case castore:
	case sastore:
	case aastore: // 83 - both seems same (TODO: Check it)
		op_stack->pop(s1);
		op_stack->pop(s2);
		SetArrayIndex(s2.iValue, s1);
		break;
	case lastore:
		s1.lValue = popLong();
		op_stack->pop(s2);
		SetArrayIndex(s2.iValue, s1);
		break;
	case dastore: // 83 - both seems same (TODO: Check it)
		s1.dValue = popDouble();
		op_stack->pop(s2);
		SetArrayIndex(s2.iValue, s1);
		break;

		//Generic (typeless) stack operations
	case _pop:
		op_stack->pop(s1);
		break;
	case _pop2:
		op_stack->pop(s1);
		op_stack->pop(s2);
		break;

	case dup:// 89 /*(0x59)*/
		s1 = op_stack->top();
		op_stack->push(s1);
		break;
	case dup_x1:// 90 /*(0x5a)*/
		s1 = op_stack->top();
		op_stack->push(s1);
		op_stack->push(s1);
		break;
	case dup_x2:// 91 /*(0x5b)*/
		s1 = op_stack->top();
		op_stack->push(s1);
		op_stack->push(s1);
		op_stack->push(s1);
		break;

	case dup2:
		s1 = op_stack->top();
		op_stack->top(1, s2);
		op_stack->push(s2);
		op_stack->push(s1);
		break;
	case dup2_x1:
		s1 = op_stack->top();
		op_stack->top(1, s2);
		op_stack->push(s2);
		op_stack->push(s1);
		op_stack->push(s2);
		op_stack->push(s1);
		break;
	case dup2_x2:
		s1 = op_stack->top();
		op_stack->top(1, s2);
		op_stack->push(s2);
		op_stack->push(s1);
		op_stack->push(s2);
		op_stack->push(s1);
		op_stack->push(s2);
		op_stack->push(s1);
		break;
		
		//Type Conversion

		//Integer Arithmetic 

	case iadd: //96
		op_stack->pop(s1);
		op_stack->pop(s2);
		op_stack->push(s1.iValue + s2.iValue);
		break;
	case ladd:// 97 /*(0x61)*/
		lValue = popLong() + popLong();
		op_stack->push(lValue);
		break;
	case fadd:// 98 /*(0x61)*/
		op_stack->pop(s1);
		op_stack->pop(s2);
		op_stack->push(s1.fValue + s2.fValue);
		break;
	case dadd:// 99 /*(0x61)*/
		dValue = popDouble() + popDouble();
		op_stack->push(dValue);
		break;
	case isub: //100
		op_stack->pop(s1);
		op_stack->pop(s2);
		op_stack->push(s2.iValue - s1.iValue);
		break;
	case lsub: //101
		lValue = popLong();
		lValue2 = popLong();
		op_stack->push(lValue2 - lValue);
		break;
	case fsub: //102
		op_stack->pop(s1);
		op_stack->pop(s2);
		op_stack->push(s2.fValue - s1.fValue);
		break;
	case dsub: //103
		dValue = popDouble();
		dValue2 = popDouble();
		op_stack->push(dValue2 - dValue);
		break;
	case imul://104
		op_stack->pop(s1);
		op_stack->pop(s2);
		op_stack->push(s2.iValue * s1.iValue);	
		break;
	case lmul://105
		lValue = popLong() * popLong();
		op_stack->push(lValue);
		break;
	case fmul://106
		op_stack->pop(s1);
		op_stack->pop(s2);
		op_stack->push(s2.fValue * s1.fValue);	
		break;
	case dmul://107
		dValue = popDouble() * popDouble();
		op_stack->push(dValue);
		break;
	case idiv:
		op_stack->pop(s1);
		op_stack->pop(s2);
		op_stack->push(s2.iValue / s1.iValue);	
		break;
	case ldiv:
		lValue = popLong() / popLong();
		op_stack->push(lValue);
		break;
	case fdiv:
		op_stack->pop(s1);
		op_stack->pop(s2);
		op_stack->push(s2.fValue / s1.fValue);	
		break;
	case ddiv:
		dValue = popDouble() / popDouble();
		op_stack->push(dValue);
		break;
	case irem:
		op_stack->pop(s1);
		op_stack->pop(s2);
		op_stack->push(s2.iValue % s1.iValue);	
		break;
	case lrem:
		lValue = popLong() % popLong();
		op_stack->push(lValue);
		break;
	case frem:
		op_stack->pop(s1);
		op_stack->pop(s2);
		op_stack->push(fmodf(s2.fValue, s1.fValue));	
		break;
	case drem:
		dValue = fmodl(popDouble(), popDouble());
		op_stack->push(dValue);
		break;
	case ineg:
		op_stack->pop(s1);
		s1.iValue = -s1.iValue;
		op_stack->push(s1);
		break;
	case lneg:
		lValue = popLong();
		op_stack->push(-lValue);
		break;
	case fneg:
		op_stack->pop(s1);
		s1.fValue = -s1.fValue;
		op_stack->push(s1);
		break;
	case dneg:
		dValue = popDouble();
		op_stack->push(-dValue);
		break;
	case ishl:
		op_stack->pop(s1);
		op_stack->pop(s2);
		s3.iValue =s2.iValue << s1.iValue;
		op_stack->push(s3);
		break;
	case lshl:
		op_stack->pop(s1);
		lValue = popLong();
		lValue = lValue << s1.iValue;
		op_stack->push(lValue);
		break;
	case ishr:
		op_stack->pop(s1);
		op_stack->pop(s2);
		s3.iValue = s2.iValue >> s1.iValue;
		op_stack->push(s3);
		break;
	case lshr:
		op_stack->pop(s1);
		lValue = popLong();
		lValue = lValue >> s1.iValue;
		op_stack->push(lValue);
		break;
	case iushr:
		op_stack->pop(s1);
		op_stack->pop(s2);
		s3.iValue = ((unsigned int)s2.iValue) >> s1.iValue;
		op_stack->push(s3);
		break;
	case lushr:
		op_stack->pop(s1);
		lValue = popLong();
		lValue = ((unsigned __int64)lValue) >> s1.iValue;
		op_stack->push(lValue);
		break;
	case iand:
		op_stack->pop(s1);
		op_stack->pop(s2);
		s3.iValue = s2.iValue & s1.iValue;
		op_stack->push(s3);
		break;
	case land:
		lValue = popLong();
		lValue2 = popLong();
		lValue = lValue & lValue2;
		op_stack->push(lValue);
		break;
	case ior:
		op_stack->pop(s1);
		op_stack->pop(s2);
		s3.iValue = s2.iValue | s1.iValue;
		op_stack->push(s3);
		break;
	case lor:
		lValue = popLong();
		lValue2 = popLong();
		lValue = lValue | lValue2;
		op_stack->push(lValue);
		break;
	case ixor:
		op_stack->pop(s1);
		op_stack->pop(s2);
		s3.iValue = s2.iValue ^ s1.iValue;
		op_stack->push(s3);
		break;
	case lxor:
		lValue = popLong();
		lValue2 = popLong();
		lValue = lValue ^ lValue2;
		op_stack->push(lValue);
		break;


	case iinc:// 132 /*(0x84)*/ Increment local variable by constant
		index = codes[pc++];
		s1 = local_var_table->get(index);
		s1.iValue += (CHAR)(codes[pc++] & 0xff);
		local_var_table->set(index, s1);
		break;
	case i2l://133
		op_stack->pop(s1);
		op_stack->push((LONG64) s1.iValue);
		break;
	case i2f://134
		op_stack->pop(s1);
		op_stack->push((float) s1.iValue);
		break;
	case i2d://135
		op_stack->pop(s1);
		op_stack->push((double) s1.iValue);
		break;
	case l2i:
		lValue = popLong();
		op_stack->push((int) lValue);
		break;
	case l2f:
		lValue = popLong();
		op_stack->push((float) lValue);
		break;
	case l2d:
		lValue = popLong();
		op_stack->push((double) lValue);
		break;
	case f2i:
		op_stack->pop(s1);
		op_stack->push((int) s1.fValue);
		break;
	case f2l:
		op_stack->pop(s1);
		op_stack->push((LONG64) s1.fValue);
		break;
	case f2d:
		op_stack->pop(s1);
		op_stack->push((double) s1.fValue);
		break;
	case d2i:
		dValue = popDouble();
		op_stack->push((int) dValue);
		break;
	case d2l:
		dValue = popDouble();
		op_stack->push((LONG64) dValue);
		break;
	case d2f:
		dValue = popDouble();
		op_stack->push((float) dValue);
		break;
	case i2b:
		op_stack->pop(s1);
		s2.bValue = (signed char) s1.iValue;
		op_stack->push(s2);
		break;
	case i2c:
		op_stack->pop(s1);
		s2.cValue = (wchar_t) s1.iValue;
		op_stack->push(s2);
		break;
	case i2s:
		op_stack->pop(s1);
		s2.sValue = (short) s1.iValue;
		op_stack->push(s2);
		break;
		////////////////////// Logic ///////////////////

		//Shift operations

		//Bitwise boolean operations

		//Floating Point Arithmetic 



		/////////////// Objects and Arrays  ////////////

		//Instructions that deal with objects

	case _new:// 187 (0xbb)
		ExecuteNewObject(ReadU2());
		break;
	case putfield: //181 (0xb5): Set field in object
		PutField(ReadU2());
		break;

	case getfield: //180 (0xb4) Fetch field from object
		GetField(ReadU2());
		break;

		//Instructions that deal with arrays

	case newarray:// 188 /*(0xbc)*/
		ExecuteNewArray(codes[pc++]);
		break;

	case anewarray: //189
		u2 = ReadU2();
		ExecuteNewObjectArray(u2);
		break;
	case arraylength:
		ArrayLength();
		break;

		////////////// Control Flow /////////////////////

		//Conditional branch instructions

	case if_icmpeq: // 159 /*(0x9f) */
		index = ReadShort();
		op_stack->pop(s2);
		op_stack->pop(s1);
		if (s1.iValue == s2.iValue)
		{
			pc = last_pc + index;
		}
		break;
	case if_icmpne: //160 /*(0xa0) */
		index = ReadShort();
		op_stack->pop(s2);
		op_stack->pop(s1);
		if (s1.iValue != s2.iValue)
		{
			pc = last_pc + index;
		}
		break;
	case if_icmplt: // 161 /*(0xa1) */
		index = ReadShort();
		op_stack->pop(s2);
		op_stack->pop(s1);
		if (s1.iValue < s2.iValue)
		{
			pc = last_pc + index;
		}
		break;
	case if_icmpge: // 162 /*(0xa2) */
		index = ReadShort();
		op_stack->pop(s2);
		op_stack->pop(s1);
		if (s1.iValue >= s2.iValue)
		{
			pc = last_pc + index;
		}
		break;
	case if_icmpgt: // 163 /*(0xa3) */
		index = ReadShort();
		op_stack->pop(s2);
		op_stack->pop(s1);
		if (s1.iValue > s2.iValue)
		{
			pc = last_pc + index;
		}
		break;
	case if_icmple: // 164 /*(0xa4)*/
		index = ReadShort();
		op_stack->pop(s2);
		op_stack->pop(s1);
		if (s1.iValue <= s2.iValue)
		{
			pc = last_pc + index;
		}
		break;

	case ifeq:// 153 /*(0x99) */
		index = ReadShort();
		op_stack->pop(s1);
		if (s1.iValue == 0)
		{
			pc = last_pc + index;
		}
		break;
	case ifne:// 154 /*(0x9a) */
		index = ReadShort();
		op_stack->pop(s1);
		if (s1.iValue != 0)
		{
			pc = last_pc + index;
		}
		break;
	case iflt:// 155 /*(0x9b) */
		index = ReadShort();
		op_stack->pop(s1);
		if (s1.iValue < 0)
		{
			pc = last_pc + index;
		}
		break;
	case ifge:// 156 /*(0x9c) */
		index = ReadShort();
		op_stack->pop(s1);
		if (s1.iValue >= 0)
		{
			pc = last_pc + index;
		}
		break;
	case ifgt:// 157 /*(0x9d) */
		index = ReadShort();
		op_stack->pop(s1);
		if (s1.iValue > 0)
		{
			pc = last_pc + index;
		}
		break;
	case ifle:// 158 /*(0x9e)*/
		index = ReadShort();
		op_stack->pop(s1);
		if (s1.iValue <= 0)
		{
			pc = last_pc + index;
		}
		break;
	case tableswitch:
		TableSwitch();
		break;

	case lookupswitch:
		LookupSwitch();
		break;
		//Comparison instructions

		//Unconditional branch instructions
	case  _goto: // 167 /*(0xa7)*/
		pc = last_pc + ReadShort();
		break;
		//Table jumping instructions

		////////////// Exceptions ///////////////////////
	case athrow:
/*		error =1;*/
		return EN_EXCEPTION;

		//////////////////////// Method Invocation and Return ////////

		//Method invocation instructions
	case invokespecial:
		index = ReadU2();
		if (ExecuteSpecial(index))
		{
			return EN_KEEP;
		}
		else
		{
			return EN_EXCEPTION;
		}
		break;

	case invokevirtual: //182
		index = ReadU2();
		if (ExecuteVirtual(index, NORMAL_METHOD))
		{
			return EN_KEEP;
		}
		else
		{
			return EN_EXCEPTION;
		}

	case invokestatic:// 184 
		index = ReadU2();
		if (ExecuteStatic(index))
		{
			return EN_KEEP;
		}
		else
		{
			return EN_EXCEPTION;
		}
		
		//Method return instructions
	case ireturn: //172 (0xac)		
	case freturn: //174 (0xae)		
	case areturn: //176 (0xb0)
		op_stack->pop(s1);
		if (prev_frame)
		{
			prev_frame->op_stack->push(s1);
		}
		return EN_POP;

	case lreturn: //173 (0xad)		
	case dreturn: //175 (0xaf)		
		op_stack->pop(s1);
		op_stack->pop(s2);
		if (prev_frame)
		{
			prev_frame->op_stack->push(s2);
			prev_frame->op_stack->push(s1);
		}
		return EN_POP;

	case _return: //177 (0xb1): Return (void) from method			
		return EN_POP;
	case getstatic: //178 (0xb2): Return (void) from method			
		index = ReadU2();
		GetStatic(index);
		break;
	case putstatic: //179 (0xb3): Return (void) from method			
		index = ReadU2();
		PutStatic(index);
		break;
		//////////////// Thread Synchronization ////////////////////

	case monitorenter:// Enter and acquire object monitor 
	case monitorexit:// Release and exit object monitor
/*		error = 1;*/
		break;
	case multianewarray:// Release and exit object monitor
		u2 = ReadU2();
		ExecuteNewMultiArray(u2, codes[pc++]);
		break;
	case ifnull:
		index = ReadShort();
		op_stack->pop(s1);
		if (!s1.heapPtr)
		{
			pc = last_pc + index;
		}
		break;
	case ifnonnull:
		index = ReadShort();
		op_stack->pop(s1);
		if (s1.heapPtr)
		{
			pc = last_pc + index;
		}
		break;
	default:
		throw VMExecutorException("Unknown code.");
		break;
	}
	return EN_NOOP;
}

U2 StackFrame::ReadU2()
{
	U1 u1 = codes[pc++];
	U1 u2 = codes[pc++];
	return (u1 << 8) + (u2 << 0);
}

short StackFrame::ReadShort()
{
	U1 u1 = codes[pc++];
	U1 u2 = codes[pc++];
	return ((u1 & 0xff) << 8) + ((u2 & 0xff) << 0);
}

int StackFrame::ReadInt()
{
	U1 u1 = codes[pc++];
	U1 u2 = codes[pc++];
	U1 u3 = codes[pc++];
	U1 u4 = codes[pc++];
	return ((u1 & 0xff) << 24)
		+ ((u2 & 0xff) << 16) 
		+ ((u3 & 0xff) << 8) 
		+ ((u4 & 0xff) << 0);
}

bool StackFrame::LoadConstant(int index, Slot &slot)
{
	lpConstant ct = clazz->const_pool->GetConstant(index);
	switch (ct->tag)
	{
	case INTEGER_CONSTANT:
		{
			IntegerConstant *ic = (IntegerConstant *)ct;
			slot.iValue = ic->bytes;
			break;
		}
	case FLOAT_CONSTANT:
		{
			FloatConstant *fc = (FloatConstant *)ct;
			slot.set(fc->bytes, 4);
			break;
		}

	case STRING_CONSTANT:
		{
			StringConstant *st = (StringConstant*)ct;
			slot.heapPtr = st->GetString();
			break;
		}
	default:
		return false;
	}
	return true;
}

bool StackFrame::LoadConstantW(int index, Slot &slot1, Slot &slot2)
{
	Slot st1, st2;
	lpConstant ct = clazz->const_pool->GetConstant(index);
	switch (ct->tag)
	{

	case DOUBLE_CONSTANT:
		{
			DoubleConstant *dc = (DoubleConstant *)ct;
			st1.set(dc->bytes1, 4);
			st2.set(dc->bytes2, 4);
			break;
		}

	case LONG_CONSTANT:
		{
			LongConstant *dc = (LongConstant *)ct;
			st1.set(dc->bytes1, 4);
			st2.set(dc->bytes2, 4);
			break;
		}
	default:
		return false;
	}
	slot1 = st1;
	slot2 = st2;
	return true;
}

double StackFrame::popDouble()
{
	Slot s1, s2;
	op_stack->pop(s2);
	op_stack->pop(s1);
	WByteValue bv(s1.bytes, s2.bytes);
	return bv.dValue;
}

LONG64 StackFrame::popLong()
{
	Slot s1, s2;
	op_stack->pop(s2);
	op_stack->pop(s1);
	WByteValue bv(s1.bytes, s2.bytes);
	return bv.lValue;
}

bool StackFrame::ExecuteNewObject(U2 index)
{
	lpClassConstant cc = (lpClassConstant)clazz->const_pool->GetConstant(index);
	if (!cc)
	{
		return false;
	}
	if (!class_loader)
	{
		return false;
	}
	lpClass clazz = NULL;
	class_loader->FindClass(cc->utf8->bytes, clazz);
	LPOBJECT_REF obj_ref = NULL;
	if (!VMContext::object_heap->NewObject(clazz, obj_ref))
	{
		return false;
	}
	op_stack->pushObject(obj_ref);
	return true;
}

bool StackFrame::ExecuteSpecial(int index)
{
	return ExecuteVirtual(index, SPECIAL_METHOD);
}

bool StackFrame::ExecuteStatic(int index)
{
	return ExecuteVirtual(index, STATIC_METHOD);
}


bool StackFrame::ExecuteVirtual(int index, int type)
{
	MethodRefConstant *method_ref = (MethodRefConstant*)this->clazz->const_pool->GetConstant(index);
	if (!method_ref)
	{
		cout<<"Cannot find method index"<<index<<endl;
		return false;
	}
	lpClass clazz = NULL;
	class_loader->FindClass(method_ref->clazz->GetName(), clazz);
	if (!clazz)
	{
		cout<<"Cannot find class "<<method_ref->clazz->utf8->bytes<<endl;
		return false;
	}
	//lpMethod method = MethodArea::GetMethod(method_ref->GetName());
	lpMethod method = NULL;
	if (!clazz->FindMethod(method_ref->simple_name, method))
	{
		cout<<"Cannot find method "<<method_ref->GetName()<<endl;
		return false;
	}

	vector<TypeDesc> type_desc;
	TypeDesc result_desc;
	if (!parse_method_desc_type(method->methodDesc->GetName(), type_desc, result_desc))
	{
		return false;
	}
	//动态分派 多态处理-------------------------------------
	if (!method->isSpecial()
		&& !method->isStatic()
		&& !method->isFinal())
	{
		Slot slot_this;
		op_stack->top(type_desc.size(), slot_this);
		LPOBJECT_REF obj_ref = slot_this.heapPtr;
		lpObject obj = NULL;
		if (!VMContext::object_heap->GetObject(obj_ref, obj))
		{
			throw new VMExecutorException("Target object is invalid.");
		}
		lpMethod tmp_method = NULL;
		if (obj->obj_class->FindMethod(method->fullName, tmp_method))
		{
			method = tmp_method;
			clazz = method->clazz;
		}
	}
	//----------------------------------------------------------------------------------------------
	if (!method->isNative())
	{
		return ExecuteNormalMethod(clazz, method, type_desc);
	}
	else
	{
		return ExecuteNativeMethod(clazz, method, type_desc, result_desc);
	}
}

bool StackFrame::ExecuteNormalMethod(lpClass clazz, lpMethod method, vector<TypeDesc> &type_desc)
{
	lpStackFrame frame = NULL;
	thread_stack->AddFrame(clazz, method, frame);
	if (!frame)
	{
		return false;
	}
	frame->prev_frame = this;
	frame->init();

	return StackToLocal(frame, type_desc, !method->isStatic());
}

bool StackFrame::ExecuteNativeMethod(lpClass clazz, lpMethod method, vector<TypeDesc> &type_desc, TypeDesc &result_desc)
{
	int count = type_desc.size();
	Slot s1, s2;
	int bytesize = 8;
	for (int i = count - 1; i >=0; i--)
	{
		TypeDesc desc = type_desc[i];
		DATA_TYPE data_type = desc.data_type;
		if (desc.check_array)
		{
			data_type = DT_ARRAY;
		}
		switch (data_type)
		{
		case DT_BOOL:
			{
				bytesize += 1;
				op_stack->pop(s1);
				jboolean v = s1.blValue;
				_asm
				{
					mov eax,dword ptr [v]
					push eax
				}
				break;
			}
		case DT_BYTE:
			{
				bytesize += 1;
				op_stack->pop(s1);
				jbyte v = s1.bValue;
				_asm
				{
					mov eax,dword ptr [v]
					push eax
				}
				break;
			}
		case DT_CHAR:
			{
				bytesize += 2;
				op_stack->pop(s1);
				jchar v = s1.cValue;
				_asm
				{
					mov eax,dword ptr [v]
					push eax
				}
				break;
			}
		case DT_FLOAT:
			{
				bytesize += 4;
				op_stack->pop(s1);
				jfloat v = s1.fValue;
				_asm
				{
					mov eax,dword ptr [v]
					push eax
				}
				break;
			}
		case DT_SHORT:
			{
				bytesize += 2;
				op_stack->pop(s1);
				jshort v = s1.sValue;
				_asm
				{
					mov eax,dword ptr [v]
					push eax
				}
				break;
			}
		case DT_INT:
			{
				bytesize += 4;
				op_stack->pop(s1);
				jint v = s1.iValue;
				_asm
				{
					mov eax,dword ptr [v]
					push eax
				}
				break;
			}
		case DT_ARRAY:
		case DT_REF:
			{
				bytesize += 4;
				op_stack->pop(s1);
				LPOBJECT_REF v = s1.heapPtr;
				_asm
				{
					mov eax,v
					push eax
				}
				break;
			}
		case DT_LONG:
		case DT_DOUBLE:
			{
				bytesize += 8;
				op_stack->pop(s1);
				op_stack->pop(s2);
				int l = s1.iValue;
				int h = s2.iValue;
				_asm
				{
					mov eax,dword ptr [l]
					mov edx,dword ptr [h]
					push edx
					push eax
				}
				break;
			}
		default:
			break;
		}
	}

	NATIVE_FUNC native_func = NULL;
	if (!VMContext::native_libs->GetNativeFunction(method->GetJniName(bytesize), native_func))
	{
		throw VMExecutorException("Cannot find native method in library.");
	}

	long jni_env_addr = (long) jni_env;
	if (method->isStatic())
	{
		_asm
		{
			mov eax,clazz
			push eax
		}
	}
	else
	{
		op_stack->pop(s1);
		LPOBJECT_REF obj_ref = s1.heapPtr;
		_asm
		{
			mov eax,obj_ref
			push eax
		}
	}

	int high_val = 0;
	int low_val = 0;
	_asm
	{
		mov eax,jni_env_addr
		push eax
		call native_func
		mov low_val,eax
		mov high_val,edx
	}

	DATA_TYPE data_type = result_desc.data_type;
	if (result_desc.check_array)
	{
		data_type = DT_ARRAY;
	}
	switch (data_type)
	{
	case DT_BOOL:
	case DT_BYTE:
	case DT_CHAR:
	case DT_FLOAT:
	case DT_SHORT:
	case DT_INT:
	case DT_ARRAY:
	case DT_REF:
		s1.iValue = low_val;
		op_stack->push(s1);
		break;
	case DT_DOUBLE:
		break;
	case DT_LONG:
		{
			LONG64 lowValue = (low_val & 0xFFFFFFFF);
			LONG64 highValue = (high_val & 0xFFFFFFFF);
			LONG64 lValue = (highValue << 0x20) | lowValue;
			op_stack->push(lValue);
			break;
		}
	case DT_VOID:
		break;
	default:
		break;
	}
	return true;
}

bool StackFrame::StackToLocal(lpStackFrame frame, vector<TypeDesc> &type_desc, bool this_var)
{
	Slot s1, s2;
	int count = type_desc.size();
	lpOperStack tmp_stack = new OperStack(count * 2);
	for (int i = count - 1; i >= 0; i--)
	{
		TypeDesc desc = type_desc[i];
		DATA_TYPE data_type = desc.data_type;
		if (desc.check_array)
		{
			data_type = DT_ARRAY;
		}
		switch (data_type)
		{
		case DT_BOOL:
		case DT_BYTE:
		case DT_CHAR:
		case DT_FLOAT:
		case DT_SHORT:
		case DT_INT:
		case DT_ARRAY:
		case DT_REF:
			if (op_stack->pop(s1))
			{
				tmp_stack->push(s1);
			}
			break;
		case DT_LONG:
		case DT_DOUBLE:
			op_stack->pop(s1);
			tmp_stack->push(s1);
			op_stack->pop(s1);
			tmp_stack->push(s1);
			break;
		default:
			break;
		}
	}
	int index = 0; //帧本地变量包索引
	//this引用出栈进帧本地变量表
	if (this_var)
	{
		if (op_stack->pop(s1))
		{
			frame->local_var_table->set(index++, s1);
		}
	}

	for (int i = 0; i < count; i++)
	{
		TypeDesc desc = type_desc[i];
		DATA_TYPE data_type = desc.data_type;
		if (desc.check_array)
		{
			data_type = DT_ARRAY;
		}
		switch (data_type)
		{
		case DT_BOOL:
		case DT_BYTE:
		case DT_CHAR:
		case DT_FLOAT:
		case DT_SHORT:
		case DT_INT:
		case DT_ARRAY:
		case DT_REF:
			if (tmp_stack->pop(s1))
			{
				frame->local_var_table->set(index++, s1);
			}
			break;
		case DT_LONG:
		case DT_DOUBLE:
			tmp_stack->pop(s1);
			tmp_stack->pop(s2);
			frame->local_var_table->set(index++, s1);
			frame->local_var_table->set(index++, s2);
			break;
		default:
			break;
		}
	}

	if (tmp_stack)
	{
		delete tmp_stack;
		tmp_stack = NULL;
	}
	return true;
}

bool StackFrame::ExecuteNewArray(U1 index)
{
	lpClass clazz = new Class(CT_ARRAY);
	clazz->thisClass = new ClassConstant();
	clazz->thisClass->name = new CHAR[3];
	clazz->thisClass->name[0] = '[';
	if (index == 0x0A)
	{
		clazz->thisClass->name[1] = 'I';
	}
	else if (index == 0x05)
	{
		clazz->thisClass->name[1] = 'C';
	}
	clazz->thisClass->name[2] = '\0';
	class_loader->StoreClass(clazz);
	Slot slot;
	op_stack->pop(slot);
	LPOBJECT_REF obj_ref = NULL;
	if (!VMContext::object_heap->NewArrayObject(slot.iValue, clazz, obj_ref))
	{
		return false;
	}
	op_stack->pushObject(obj_ref);
	return true;
}

bool StackFrame::ExecuteNewObjectArray(U2 index)
{
	lpClassConstant cc = (lpClassConstant)clazz->const_pool->GetConstant(index);
	if (!cc)
	{
		return false;
	}
	if (!class_loader)
	{
		return false;
	}
	lpClass clazz = NULL;
	class_loader->FindClass(cc->utf8->bytes, clazz);

	Slot slot;
	op_stack->pop(slot);
	LPOBJECT_REF obj_ref = NULL;
	if (!VMContext::object_heap->NewObjectArrayObject(slot.iValue, clazz, obj_ref))
	{
		return false;
	}
	op_stack->pushObject(obj_ref);
	return true;
}

bool StackFrame::ExecuteNewMultiArray(U2 index, int count)
{
	lpClassConstant cc = (lpClassConstant)clazz->const_pool->GetConstant(index);
	if (!cc)
	{
		return false;
	}
	if (!class_loader)
	{
		return false;
	}
	lpClass clazz = NULL;
	class_loader->FindClass(cc->utf8->bytes, clazz);

	Slot slot;
	LPINT32 nums = new INT32[count];
	for (int i = count - 1; i >= 0; i--)
	{
		op_stack->pop(slot);
		nums[i] = slot.iValue;
	}
	LPOBJECT_REF obj_ref = NULL;
	if (!VMContext::object_heap->NewMultiArrayObject(nums, count, clazz, obj_ref))
	{
		return false;
	}
	op_stack->pushObject(obj_ref);
	return true;
}

void StackFrame::SetArrayIndex(int index, Slot &slot)
{
	Slot ary;
	op_stack->pop(ary);
	LPOBJECT_REF obj_ref = ary.heapPtr;
	if (!obj_ref)
	{
		throw new VMExecutorException("Array object heap ptr is invalid.");
	}
	lpObject obj = NULL;
	if (!VMContext::object_heap->GetObject(obj_ref, obj))
	{
		throw new VMExecutorException("Array object is invalid.");
	}
	if (!obj->array_var)
	{
		throw new VMExecutorException("Array object is not inited.");
	}
	if (index >= obj->array_length)
	{
		throw new VMExecutorException("Array index out of memory.");
	}
	obj->SetArrayIndex(index, slot);
}

void StackFrame::LoadArray(DATA_TYPE data_type)
{
	Slot ary, slot;
	op_stack->pop(slot);
	op_stack->pop(ary);
	LPOBJECT_REF obj_ref = ary.heapPtr;
	if (!obj_ref)
	{
		throw new VMExecutorException("Array object heap ptr is invalid.");
	}
	lpObject obj = NULL;
	if (!VMContext::object_heap->GetObject(obj_ref, obj))
	{
		throw new VMExecutorException("Array object is invalid.");
	}
	if (!obj->array_var)
	{
		throw new VMExecutorException("Array object is not inited.");
	}
	if (slot.iValue >= obj->array_length)
	{
		throw new VMExecutorException("Array index out of memory.");
	}
	Slot ret;
	switch (data_type)
	{
	case DT_BYTE:
		{
			jbyte *bytes = (jbyte*)obj->array_var;
			ret.bValue = bytes[slot.iValue];
			break;
		}
	case DT_CHAR:
		{
			jchar *vals = (jchar*)obj->array_var;
			ret.cValue = vals[slot.iValue];
			break;
		}
	case DT_DOUBLE:
		{
			jdouble *vals = (jdouble*)obj->array_var;
			ret.dValue = vals[slot.iValue];
			break;
		}
	case DT_FLOAT:
		{
			jfloat *vals = (jfloat*)obj->array_var;
			ret.fValue = vals[slot.iValue];
			break;
		}
	case DT_INT:
		{
			jint *vals = (jint*)obj->array_var;
			ret.iValue = vals[slot.iValue];
			break;
		}
	case DT_LONG:
		{
			jlong *vals = (jlong*)obj->array_var;
			ret.lValue = vals[slot.iValue];
			break;
		}
	case DT_SHORT:
		{
			jshort *vals = (jshort*)obj->array_var;
			ret.sValue = vals[slot.iValue];
			break;
		}
	case DT_ARRAY:
	case DT_REF:
		{
			LPOBJECT_REF* refs = (LPOBJECT_REF*)obj->array_var;
			ret.heapPtr = refs[slot.iValue];
			break;
		}
	}
	op_stack->push(ret);
}

void StackFrame::ArrayLength()
{
	Slot slot;
	op_stack->pop(slot);
	LPOBJECT_REF obj_ref = slot.heapPtr;
	if (!obj_ref)
	{
		throw new VMExecutorException("Array object heap ptr is invalid.");
	}
	lpObject obj = NULL;
	if (!VMContext::object_heap->GetObject(obj_ref, obj))
	{
		throw new VMExecutorException("Array object is invalid.");
	}
	if (!obj->array_var)
	{
		throw new VMExecutorException("Array object is not inited.");
	}
	op_stack->push(obj->array_length);
}

void StackFrame::PutStatic(U2 index)
{
	FieldRefConstant *frc = (FieldRefConstant*) clazz->const_pool->GetConstant(index);
	if (!frc)
	{
		throw new VMExecutorException("Put static field have the invalid index.");
	}
	lpField field = clazz->fields_map[string(frc->simple_name)];
	if (!field)
	{
		throw new VMExecutorException("Class field does not exists.");
	}
	lpFieldValue fv = new FieldValue(field);

	TypeDesc type_desc;
	LPSTR desc = frc->nameType->descUtf8->GetName();
	parse_desc_type(desc, type_desc, strlen(desc));
	switch (type_desc.data_type)
	{
	case DT_BOOL:
	case DT_BYTE:
	case DT_CHAR:
	case DT_FLOAT:
	case DT_SHORT:
	case DT_INT:
	case DT_ARRAY:
	case DT_REF:
		op_stack->pop(fv->field_value);
		break;
	case DT_LONG:
		fv->field_value.lValue = popLong();
		break;
	case DT_DOUBLE:
		fv->field_value.dValue = popDouble();
		break;
	default:
		break;
	}

	MethodArea::AddStaticField(frc->GetName(), fv);
}

void StackFrame::GetStatic(U2 index)
{
	FieldRefConstant *frc = (FieldRefConstant*) clazz->const_pool->GetConstant(index);
	if (!frc)
	{
		throw new VMExecutorException("Get static field have the invalid index.");
	}
	
	lpClass clazz = NULL;
	if (!class_loader->FindClass(frc->clazz->GetName(), clazz))
	{
		throw new VMExecutorException("Fail to find class when get static field.");
	}

	lpFieldValue fv = MethodArea::GetStaticField(frc->GetName());
	if (!fv)
	{
		throw new VMExecutorException("Static field does not exists.");
	}
	TypeDesc type_desc;
	LPSTR desc = frc->nameType->descUtf8->GetName();
	parse_desc_type(desc, type_desc, strlen(desc));
	switch (type_desc.data_type)
	{
	case DT_BOOL:
	case DT_BYTE:
	case DT_CHAR:
	case DT_FLOAT:
	case DT_SHORT:
	case DT_INT:
	case DT_ARRAY:
	case DT_REF:
		op_stack->push(fv->field_value);
		break;
	case DT_LONG:
		op_stack->push(fv->field_value.lValue);
		break;
	case DT_DOUBLE:
		op_stack->push(fv->field_value.dValue);
		break;
	default:
		break;
	}
}

void StackFrame::GetField(U2 index)
{
	FieldRefConstant *frc = (FieldRefConstant*) clazz->const_pool->GetConstant(index);
	if (!frc)
	{
		throw new VMExecutorException("Get field have the invalid index.");
	}
	Slot slot_obj;
	op_stack->pop(slot_obj);
	LPOBJECT_REF obj_ref = slot_obj.heapPtr;
	if (!obj_ref)
	{
		throw new VMExecutorException("Get field object heap ptr is invalid.");
	}
	lpObject obj = NULL;
	if (!VMContext::object_heap->GetObject(obj_ref, obj))
	{
		throw new VMExecutorException("Get field object is invalid.");
	}
	lpFieldValue fv = obj->varants[string(frc->simple_name)];
	if (!fv)
	{
		throw new VMExecutorException("Get field value from object is invalid");
	}
	TypeDesc type_desc;
	LPSTR desc = frc->nameType->descUtf8->GetName();
	parse_desc_type(desc, type_desc, strlen(desc));
	switch (type_desc.data_type)
	{
	case DT_BOOL:
	case DT_BYTE:
	case DT_CHAR:
	case DT_FLOAT:
	case DT_SHORT:
	case DT_INT:
	case DT_ARRAY:
	case DT_REF:
		op_stack->push(fv->field_value);
		break;
	case DT_LONG:
		op_stack->push(fv->field_value.lValue);
		break;
	case DT_DOUBLE:
		op_stack->push(fv->field_value.dValue);
		break;
	default:
		break;
	}
}

void StackFrame::PutField(U2 index)
{
	FieldRefConstant *frc = (FieldRefConstant*) clazz->const_pool->GetConstant(index);
	if (!frc)
	{
		throw new VMExecutorException("Put field have the invalid index.");
	}

	Slot slot_val;
	TypeDesc type_desc;
	LPSTR desc = frc->nameType->descUtf8->GetName();
	parse_desc_type(desc, type_desc, strlen(desc));
	switch (type_desc.data_type)
	{
	case DT_BOOL:
	case DT_BYTE:
	case DT_CHAR:
	case DT_FLOAT:
	case DT_SHORT:
	case DT_INT:
	case DT_ARRAY:
	case DT_REF:
		op_stack->pop(slot_val);
		break;
	case DT_LONG:
		slot_val.lValue = popLong();
		break;
	case DT_DOUBLE:
		slot_val.dValue = popDouble();
		break;
	default:
		break;
	}

	Slot slot_obj;
	op_stack->pop(slot_obj);
	LPOBJECT_REF obj_ref = slot_obj.heapPtr;
	if (!obj_ref)
	{
		throw new VMExecutorException("Put field object heap ptr is invalid.");
	}
	lpObject obj = NULL;
	if (!VMContext::object_heap->GetObject(obj_ref, obj))
	{
		throw new VMExecutorException("Put field object is invalid.");
	}
	lpFieldValue fv = obj->varants[string(frc->simple_name)];
	if (!fv)
	{
		throw new VMExecutorException("Put field value from object is invalid");
	}
	
	fv->field_value = slot_val;
}

void StackFrame::TableSwitch()
{
	int default_offset = ReadInt();
	int low = ReadInt();
	int high = ReadInt();
	int n = high - low + 1;
	int *table = new int[n];
	for (int i = 0; i < n; i++)
	{
		table[i] = ReadInt();
	}
	Slot slot;
	op_stack->pop(slot);
	int val = slot.iValue;               
	if (val < low || val > high) 
	{ 
		pc = last_pc + default_offset;             
	} 
	else 
	{                      
		pc = last_pc + table[val - low];   
	}

	delete[] table;
	table = NULL;
}

void StackFrame::LookupSwitch()
{
	Slot slot;
	op_stack->pop(slot);
	int val = slot.iValue;        
	int reserved = ReadShort();
	int default_offset = ReadInt();
	int n = ReadInt();
	bool matched = false;
	int new_pc = pc;
	for (int i = 0; i < n; i++)
	{
		int key = ReadInt();
		int offset = ReadInt();
		if (key == val)
		{
			matched = true;
			new_pc = last_pc + offset;
		}
	}

	if (matched)
	{
		pc = new_pc;
	}
	else
	{
		pc = last_pc + default_offset;
	}
}