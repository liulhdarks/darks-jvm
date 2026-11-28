#pragma once

#include "vm/types.h"
#include "exec_common.h"
#include "vm/executor/vmpool.h"

#include <cstring>

class LocalVarTable
{
private:
	int init_size;
	Slot *slots;
	bool slots_pooled;

public:
	LocalVarTable(int init_size)
	{
		this->init_size = init_size;
		size_t bytes = sizeof(Slot) * (init_size > 0 ? init_size : 1);
		slots = (Slot *)VmPoolAlloc(bytes);
		slots_pooled = (slots != NULL);
		if (slots_pooled)
		{
			// MarkRoots scans the whole capacity, so a reused block must be
			// free of references left by the previous frame.
			memset(slots, 0, bytes);
		}
		else
		{
			slots = new Slot[init_size > 0 ? init_size : 1];
		}
	}

	~LocalVarTable()
	{
		if (slots_pooled)
		{
			VmPoolFree(slots);
		}
		else
		{
			delete[] slots;
		}
		slots = NULL;
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

	// Raw slot access for the conservative GC root scan. Every slot is
	// zero-initialised when the table is created (the constructor or the
	// pool) before it is used.
	Slot *data()
	{
		return slots;
	}

	int capacity()
	{
		return init_size;
	}
};

typedef LocalVarTable *lpLocalVarTable;