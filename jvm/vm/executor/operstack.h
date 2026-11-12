#pragma once

#include "vm/types.h"
#include "exec_common.h"
#include "vm/exceptions.h"

class OperStack
{

private:
	int init_size;
	Slot *slots;
	int index;
public:
	const char *debug_method;
	int debug_pc;

public:
	OperStack(int init_size)
	{
		this->init_size = init_size;
		slots = new Slot[init_size];
		index = 0;
		debug_method = NULL;
		debug_pc = -1;
	}

	~OperStack()
	{
		delete[] slots;
		slots = NULL;
	}

	void push(short v)
	{
		Slot slot;
		slot.sValue = v;
		push(slot);
	}

	void push(int v)
	{
		Slot slot;
		slot.iValue = v;
		push(slot);
	}

	void push(float v)
	{
		Slot slot;
		slot.fValue = v;
		push(slot);
	}

	void push(LONG64 v)
	{
		WByteValue bv;
		bv.lValue = v;
		Slot slot1;
		slot1.set(bv.bytes, 4);
		Slot slot2;
		slot2.set(bv.bytes + 4, 4);
		// Bottom slot holds the low 32 bits, the top slot the high 32 bits,
		// matching ldc2_w/lload and popLong/popDouble.
		push(slot1);
		push(slot2);
	}

	void push(double v)
	{
		WByteValue bv;
		bv.dValue = v;
		Slot slot1;
		slot1.set(bv.bytes, 4);
		Slot slot2;
		slot2.set(bv.bytes + 4, 4);
		// Bottom slot holds the low 32 bits, the top slot the high 32 bits,
		// matching ldc2_w/lload and popLong/popDouble.
		push(slot1);
		push(slot2);
	}

	void pushObject(LPOBJECT_REF &v)
	{
		Slot slot;
		slot.heapPtr = v;
		push(slot);
	}

	void push(Slot &slot)
	{
		if (index >= init_size)
		{
			char msg[256];
			snprintf(msg, sizeof(msg), "Operation stack overflow (index=%d, capacity=%d) at %s pc=%d.", index, init_size, debug_method ? debug_method : "?", debug_pc);
			throw new VMExecutorException(msg);
		}
		slots[index++] = slot;
	}

	bool pop(Slot &slot)
	{
		if (index <= 0)
		{
			char msg[256];
			snprintf(msg, sizeof(msg), "Operation stack underflow (pop) at %s pc=%d.", debug_method ? debug_method : "?", debug_pc);
			throw new VMExecutorException(msg);
		}
		slot = slots[--index];
		return true;
	}

	Slot top()
	{
		if (index <= 0)
		{
			char msg[256];
			snprintf(msg, sizeof(msg), "Operation stack underflow (top) at %s pc=%d.", debug_method ? debug_method : "?", debug_pc);
			throw new VMExecutorException(msg);
		}
		return slots[index - 1];
	}

	bool top(int offset, Slot &slot)
	{
		int tmp = index - 1;
		tmp -= offset;
		if (tmp < 0 || tmp >= index)
		{
			char msg[128];
			snprintf(msg, sizeof(msg), "Operation stack top(offset) out of range (offset=%d, index=%d).", offset, index);
			throw new VMExecutorException(msg);
		}
		slot = slots[tmp];
		return true;
	}

	bool get(int i, Slot &slot)
	{
		if (i < 0 || i >= index)
		{
			char msg[128];
			snprintf(msg, sizeof(msg), "Operation stack get(%d) out of range (index=%d).", i, index);
			throw new VMExecutorException(msg);
		}
		slot = slots[i];
		return true;
	}

	bool isEmpty()
	{
		return (index <= 0);
	}

	void clear()
	{
		index = 0;
	}

	int size()
	{
		return index;
	}

	// Raw slot access for the conservative GC root scan. Only the live part
	// (0..size()) is meaningful.
	Slot *data()
	{
		return slots;
	}
};

typedef OperStack *lpOperStack;