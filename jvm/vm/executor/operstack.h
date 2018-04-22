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
	OperStack(int init_size)
	{
		this->init_size = init_size;
		slots = new Slot[init_size];
		index = 0;
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
		push(slot2);
		push(slot1);
	}

	void push(double v)
	{
		WByteValue bv;
		bv.dValue = v;
		Slot slot1;
		slot1.set(bv.bytes, 4);
		Slot slot2;
		slot2.set(bv.bytes + 4, 4);
		push(slot2);
		push(slot1);
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
			throw new VMExecutorException("Operation stack out of memory.");
		}
		slots[index++] = slot;
	}

	bool pop(Slot &slot)
	{
		if (index <= 0)
		{
			throw new VMExecutorException("Operation stack out of memory.");
		}
		slot = slots[--index];
		return true;
	}

	Slot top()
	{
		if (index <= 0)
		{
			throw new VMExecutorException("Operation stack out of memory.");
		}
		return slots[index - 1];
	}

	bool top(int offset, Slot &slot)
	{
		int tmp = index - 1;
		tmp -= offset;
		if (tmp < 0 || tmp >= index)
		{
			throw new VMExecutorException("Operation stack out of memory.");
		}
		slot = slots[tmp];
		return true;
	}

	bool get(int i, Slot &slot)
	{
		if (i < 0 || i >= index)
		{
			throw new VMExecutorException("Operation stack out of memory.");
		}
		slot = slots[i];
		return true;
	}

	bool isEmpty()
	{
		return (index <= 0);
	}

	int size()
	{
		return index;
	}
};

typedef OperStack *lpOperStack;