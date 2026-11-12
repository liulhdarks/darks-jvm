#pragma once

#include "vm/types.h"
#include "jmethod.h"

#include <vector>

// Lightweight method JIT.
//
// The compiler is deliberately small: hot methods are decoded once into a
// linear array of internal instructions (threaded code) with operands already
// resolved. The execution loop in stackframe.cpp handles the common opcodes
// directly (constants, local loads/stores, arithmetic, branches, field/array
// access and direct calls) and falls back to the ordinary interpreter for the
// remaining ones. There is no machine-code generation, so semantics are exactly
// those of the interpreter.
//
// Configuration (read once per process):
//   DARKS_JVM_JIT=1|on|true      enable (default: disabled)
//   DARKS_JVM_JIT_THRESHOLD=N    invocations before compiling (default: 50)
//   DARKS_JVM_JIT_VERBOSE=1      log compiled/fallback decisions to stderr
namespace darksjit {

struct Insn
{
	int pc;
	int next_pc;
	int target;       // absolute branch target, or -1
	int a;            // pre-decoded operand / constant-pool index / local slot
	int b;            // second operand (iinc)
	unsigned char op; // JVM opcode
	bool fast;        // handled by the JIT fast path
};

struct Program
{
	std::vector<Insn> insns;
	std::vector<int> pc_index; // bytecode pc -> insn index, -1 when absent
	int code_length;

	Program() : code_length(0) {}
};

bool Enabled();
int Threshold();

// Counts one invocation of method and returns its compiled program once the
// threshold is reached. Returns NULL when JIT is disabled, the method is not
// compilable, or it has not become hot yet.
const Program *Acquire(lpMethod method);

} // namespace darksjit
