#include "stdafx.h"
#include "vm/executor/operstack.h"

#include <cstdio>

void OperStack::Overflow() const
{
	char msg[256];
	snprintf(msg, sizeof(msg),
	         "Operation stack overflow (index=%d, capacity=%d) at %s pc=%d.",
	         index, init_size, debug_method ? debug_method : "?", debug_pc);
	throw new VMExecutorException(msg);
}

void OperStack::UnderflowPop() const
{
	char msg[256];
	snprintf(msg, sizeof(msg), "Operation stack underflow (pop) at %s pc=%d.",
	         debug_method ? debug_method : "?", debug_pc);
	throw new VMExecutorException(msg);
}

void OperStack::UnderflowTop() const
{
	char msg[256];
	snprintf(msg, sizeof(msg), "Operation stack underflow (top) at %s pc=%d.",
	         debug_method ? debug_method : "?", debug_pc);
	throw new VMExecutorException(msg);
}

void OperStack::UnderflowTop(int offset) const
{
	char msg[128];
	snprintf(msg, sizeof(msg),
	         "Operation stack top(offset) out of range (offset=%d, index=%d).",
	         offset, index);
	throw new VMExecutorException(msg);
}

void OperStack::UnderflowGet(int i) const
{
	char msg[128];
	snprintf(msg, sizeof(msg),
	         "Operation stack get(%d) out of range (index=%d).", i, index);
	throw new VMExecutorException(msg);
}