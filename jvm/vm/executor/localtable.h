#pragma once

#include "vm/types.h"
#include "exec_common.h"

class LocalVarTable
{
private:
	int init_size;
	Slot *slots;

public:
	LocalVarTable(int init_size)
	{
		this->init_size = init_size;
		slots = new Slot[init_size];
	}

	void set(int index, Slot &slot)
	{
		if (index < 0 || index >= init_size)
		{
			throw new VMExecutorException("Local variable table out of memory.");
		}
		slots[index] = slot;
	}

	Slot &get(int index)
	{
		if (index < 0 || index >= init_size)
		{
			throw new VMExecutorException("Local variable table out of memory.");
		}
		return slots[index];
	}
};

typedef LocalVarTable *lpLocalVarTable;