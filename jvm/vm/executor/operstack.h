#pragma once

#include "vm/types.h"
#include "exec_common.h"
#include "vm/exceptions.h"
#include "vm/executor/vmpool.h"

#include <cstring>

// The hot operand-stack operations are a bounds test and an 8-byte copy; the
// diagnostics (snprintf plus a thrown exception) are what kept the compiler
// from inlining them at ~500 interpreter sites.  Keep the failure paths out of
// line so the fast paths can be inlined.
#if defined(__GNUC__)
#define DARKS_UNLIKELY(x) __builtin_expect(!!(x), 0)
#define DARKS_NOINLINE __attribute__((noinline))
#define DARKS_ALWAYS_INLINE __attribute__((always_inline))
#else
#define DARKS_UNLIKELY(x) (x)
#define DARKS_NOINLINE __declspec(noinline)
#define DARKS_ALWAYS_INLINE
#endif

class OperStack
{

private:
	int init_size;
	Slot *slots;
	bool slots_pooled;
	int index;

	DARKS_NOINLINE void Overflow() const;
	DARKS_NOINLINE void UnderflowPop() const;
	DARKS_NOINLINE void UnderflowTop() const;
	DARKS_NOINLINE void UnderflowTop(int offset) const;
	DARKS_NOINLINE void UnderflowGet(int i) const;

public:
	const char *debug_method;
	int debug_pc;

public:
	OperStack(int init_size)
	{
		this->init_size = init_size;
		size_t bytes = sizeof(Slot) * (init_size > 0 ? init_size : 1);
		slots = (Slot *)VmPoolAlloc(bytes);
		slots_pooled = (slots != NULL);
		if (slots_pooled)
		{
			// The conservative root scan reads live slots only, but reusing a
			// block must not leave stale references in it.
			memset(slots, 0, bytes);
		}
		else
		{
			slots = new Slot[init_size > 0 ? init_size : 1];
		}
		index = 0;
		debug_method = NULL;
		debug_pc = -1;
	}

	~OperStack()
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

	DARKS_ALWAYS_INLINE void push(short v)
	{
		Slot slot;
		slot.iValue = v;
		push(slot);
	}

	DARKS_ALWAYS_INLINE void push(int v)
	{
		Slot slot;
		slot.iValue = v;
		push(slot);
	}

	DARKS_ALWAYS_INLINE void push(float v)
	{
		Slot slot;
		slot.fValue = v;
		push(slot);
	}

	DARKS_ALWAYS_INLINE void push(LONG64 v)
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

	DARKS_ALWAYS_INLINE void push(double v)
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

	DARKS_ALWAYS_INLINE void pushObject(LPOBJECT_REF &v)
	{
		Slot slot;
		slot.heapPtr = v;
		push(slot);
	}

	DARKS_ALWAYS_INLINE void push(Slot &slot)
	{
		if (DARKS_UNLIKELY(index >= init_size))
		{
			Overflow();
		}
		slots[index++] = slot;
	}

	DARKS_ALWAYS_INLINE bool pop(Slot &slot)
	{
		if (DARKS_UNLIKELY(index <= 0))
		{
			UnderflowPop();
		}
		slot = slots[--index];
		return true;
	}

	DARKS_ALWAYS_INLINE Slot top()
	{
		if (DARKS_UNLIKELY(index <= 0))
		{
			UnderflowTop();
		}
		return slots[index - 1];
	}

	DARKS_ALWAYS_INLINE bool top(int offset, Slot &slot)
	{
		int tmp = index - 1 - offset;
		if (DARKS_UNLIKELY(tmp < 0 || tmp >= index))
		{
			UnderflowTop(offset);
		}
		slot = slots[tmp];
		return true;
	}

	DARKS_ALWAYS_INLINE bool get(int i, Slot &slot)
	{
		if (DARKS_UNLIKELY(i < 0 || i >= index))
		{
			UnderflowGet(i);
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