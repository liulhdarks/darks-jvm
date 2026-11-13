#include "stdafx.h"
#include "jit.h"
#include "code.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <mutex>
#include <string>
#include <vector>

namespace darksjit {

namespace {

// Compiled programs live for the lifetime of the process.  They are owned
// here, not by the Method objects (methods are never reclaimed), so that the
// allocations are still released by a static destructor and leak checkers
// stay clean after a JIT-enabled run.
struct ProgramPool
{
	std::mutex mutex;
	std::vector<Program *> programs;

	~ProgramPool()
	{
		for (size_t i = 0; i < programs.size(); i++)
		{
			delete programs[i];
		}
	}
};

ProgramPool &GetProgramPool()
{
	static ProgramPool pool;
	return pool;
}

bool ReadEnabled()
{
	const char *value = getenv("DARKS_JVM_JIT");
	if (!value || !value[0])
	{
		return false;
	}
	if (strcmp(value, "0") == 0 || strcmp(value, "off") == 0 ||
	    strcmp(value, "false") == 0 || strcmp(value, "no") == 0)
	{
		return false;
	}
	return true;
}

int ReadThreshold()
{
	const char *value = getenv("DARKS_JVM_JIT_THRESHOLD");
	if (!value || !value[0])
	{
		return 50;
	}
	char *end = NULL;
	long parsed = strtol(value, &end, 10);
	if (end == value || parsed <= 0)
	{
		return 50;
	}
	if (parsed > 1000000)
	{
		parsed = 1000000;
	}
	return static_cast<int>(parsed);
}

bool Verbose()
{
	const char *value = getenv("DARKS_JVM_JIT_VERBOSE");
	return value && value[0] && strcmp(value, "0") != 0;
}

unsigned short U2At(const U1 *codes, int pc)
{
	return static_cast<unsigned short>((codes[pc] << 8) | codes[pc + 1]);
}

short S2At(const U1 *codes, int pc)
{
	return static_cast<short>(U2At(codes, pc));
}

// Decodes the method bytecode into internal instructions. Returns false when
// an opcode cannot be represented safely (wide, jsr/ret, switch, unknown); the
// caller then keeps using the interpreter.
bool Compile(lpMethod method, Program &program)
{
	if (!method || !method->code_attr || !method->code_attr->codes)
	{
		return false;
	}
	const U1 *codes = method->code_attr->codes;
	const int length = static_cast<int>(method->code_attr->code_length);
	if (length <= 0)
	{
		return false;
	}
	program.insns.clear();
	program.pc_index.assign(length, -1);
	program.code_length = length;

	int pc = 0;
	while (pc < length)
	{
		const int start = pc;
		const U1 op = codes[pc];
		Insn insn;
		insn.pc = start;
		insn.next_pc = -1;
		insn.target = -1;
		insn.a = 0;
		insn.b = 0;
		insn.op = op;
		insn.fast = true;
		int next = -1;

		switch (op)
		{
		case nop:
		case aconst_null:
		case iconst_m1: case iconst_0: case iconst_1: case iconst_2:
		case iconst_3: case iconst_4: case iconst_5:
		case lconst_0: case lconst_1:
		case fconst_0: case fconst_1: case fconst_2:
		case dconst_0: case dconst_1:
		case iaload: case laload: case faload: case daload:
		case aaload: case baload: case caload: case saload:
		case iastore: case lastore: case fastore: case dastore:
		case aastore: case bastore: case castore: case sastore:
		case _pop: case _pop2:
		case dup: case dup_x1: case dup_x2:
		case dup2: case dup2_x1: case dup2_x2:
		case iadd: case ladd: case fadd: case dadd:
		case isub: case lsub: case fsub: case dsub:
		case imul: case lmul: case fmul: case dmul:
		case idiv: case ldiv: case fdiv: case ddiv:
		case irem: case lrem: case frem: case drem:
		case ineg: case lneg: case fneg: case dneg:
		case ishl: case lshl: case ishr: case lshr:
		case iushr: case lushr:
		case iand: case land: case ior: case lor:
		case ixor: case lxor:
		case i2l: case i2f: case i2d:
		case l2i: case l2f: case l2d:
		case f2i: case f2l: case f2d:
		case d2i: case d2l: case d2f:
		case i2b: case i2c: case i2s:
		case lcmp: case fcmpl: case fcmpg: case dcmpl: case dcmpg:
		case ireturn: case lreturn: case freturn: case dreturn:
		case areturn: case _return:
		case arraylength:
			next = start + 1;
			break;

		case bipush:
			insn.a = static_cast<signed char>(codes[start + 1]);
			next = start + 2;
			break;
		case sipush:
			insn.a = S2At(codes, start + 1);
			next = start + 3;
			break;
		case ldc:
			insn.a = codes[start + 1];
			next = start + 2;
			break;
		case ldc_w:
		case ldc2_w:
			insn.a = U2At(codes, start + 1);
			next = start + 3;
			break;

		case iload: case lload: case fload: case dload: case aload:
		case istore: case lstore: case fstore: case dstore: case astore:
			insn.a = codes[start + 1];
			next = start + 2;
			break;
		case iload_0: case iload_1: case iload_2: case iload_3:
			insn.a = op - iload_0;
			next = start + 1;
			break;
		case lload_0: case lload_1: case lload_2: case lload_3:
			insn.a = op - lload_0;
			next = start + 1;
			break;
		case fload_0: case fload_1: case fload_2: case fload_3:
			insn.a = op - fload_0;
			next = start + 1;
			break;
		case dload_0: case dload_1: case dload_2: case dload_3:
			insn.a = op - dload_0;
			next = start + 1;
			break;
		case aload_0: case aload_1: case aload_2: case aload_3:
			insn.a = op - aload_0;
			next = start + 1;
			break;
		case istore_0: case istore_1: case istore_2: case istore_3:
			insn.a = op - istore_0;
			next = start + 1;
			break;
		case lstore_0: case lstore_1: case lstore_2: case lstore_3:
			insn.a = op - lstore_0;
			next = start + 1;
			break;
		case fstore_0: case fstore_1: case fstore_2: case fstore_3:
			insn.a = op - fstore_0;
			next = start + 1;
			break;
		case dstore_0: case dstore_1: case dstore_2: case dstore_3:
			insn.a = op - dstore_0;
			next = start + 1;
			break;
		case astore_0: case astore_1: case astore_2: case astore_3:
			insn.a = op - astore_0;
			next = start + 1;
			break;

		case iinc:
			insn.a = codes[start + 1];
			insn.b = static_cast<signed char>(codes[start + 2]);
			next = start + 3;
			break;

		case ifeq: case ifne: case iflt: case ifge: case ifgt: case ifle:
		case if_icmpeq: case if_icmpne: case if_icmplt:
		case if_icmpge: case if_icmpgt: case if_icmple:
		case if_acmpeq: case if_acmpne:
		case ifnull: case ifnonnull:
		{
			short offset = S2At(codes, start + 1);
			insn.target = start + offset;
			next = start + 3;
			break;
		}
		case _goto:
			insn.target = start + S2At(codes, start + 1);
			next = start + 3;
			break;

		case getstatic: case putstatic: case getfield: case putfield:
		case invokevirtual: case invokespecial: case invokestatic:
		case _new: case anewarray:
			insn.a = U2At(codes, start + 1);
			next = start + 3;
			break;
		case newarray:
			insn.a = codes[start + 1];
			next = start + 2;
			break;
		case invokeinterface:
		case invokedynamic:
			insn.a = U2At(codes, start + 1);
			next = start + 5;
			break;

		// Delegated to the interpreter (operands pre-decoded where useful).
		case checkcast:
		case instanceof:
			insn.a = U2At(codes, start + 1);
			insn.fast = false;
			next = start + 3;
			break;
		case athrow:
		case monitorenter:
		case monitorexit:
			insn.fast = false;
			next = start + 1;
			break;
		case multianewarray:
			insn.a = U2At(codes, start + 1);
			insn.b = codes[start + 3];
			insn.fast = false;
			next = start + 4;
			break;

		// Not supported by the lightweight compiler: keep the interpreter.
		case tableswitch:
		case lookupswitch:
		case 95: // swap
		case 168: // jsr
		case 169: // ret
		case 196: // wide
		case 200: // goto_w
		case 201: // jsr_w
		default:
			return false;
		}

		if (next <= start || next > length)
		{
			return false;
		}
		insn.next_pc = next;
		program.pc_index[start] = static_cast<int>(program.insns.size());
		program.insns.push_back(insn);
		pc = next;
	}
	return !program.insns.empty();
}

std::string MethodName(lpMethod method)
{
	if (!method)
	{
		return std::string("?");
	}
	std::string name = (method->clazz && method->clazz->thisClass &&
	                    method->clazz->thisClass->GetName())
		? method->clazz->thisClass->GetName() : "?";
	if (method->methodName && method->methodName->GetName())
	{
		name += ".";
		name += method->methodName->GetName();
	}
	if (method->methodDesc && method->methodDesc->GetName())
	{
		name += method->methodDesc->GetName();
	}
	return name;
}

} // namespace

bool Enabled()
{
	static const bool enabled = ReadEnabled();
	return enabled;
}

int Threshold()
{
	static const int threshold = ReadThreshold();
	return threshold;
}

const Program *Acquire(lpMethod method)
{
	if (!Enabled() || !method || method->isNative() || !method->code_attr)
	{
		return NULL;
	}
	// Fast path: no lock, one acquire load of the per-method state.
	int state = method->jit_state.load(std::memory_order_acquire);
	if (state == 2)
	{
		return method->jit_program.load(std::memory_order_acquire);
	}
	if (state == 3 || state == 1)
	{
		// Unsupported, or another coroutine is already compiling: interpret
		// for now.  Waiting here would block a worker under M:N scheduling.
		return NULL;
	}
	if (method->jit_hit_count.fetch_add(1, std::memory_order_relaxed) + 1 <
	    Threshold())
	{
		return NULL;
	}
	int expected = 0;
	if (!method->jit_state.compare_exchange_strong(expected, 1,
	        std::memory_order_acq_rel))
	{
		// Lost the race (another thread is compiling or already finished).
		return expected == 2
			? method->jit_program.load(std::memory_order_acquire)
			: NULL;
	}
	Program *program = new Program();
	if (!Compile(method, *program))
	{
		delete program;
		method->jit_state.store(3, std::memory_order_release);
		if (Verbose())
		{
			fprintf(stderr, "[jit] fallback %s (unsupported bytecode)\n",
				MethodName(method).c_str());
		}
		return NULL;
	}
	// Publish the program before advertising the compiled state.
	{
		ProgramPool &pool = GetProgramPool();
		std::lock_guard<std::mutex> lock(pool.mutex);
		pool.programs.push_back(program);
	}
	method->jit_program.store(program, std::memory_order_release);
	method->jit_state.store(2, std::memory_order_release);
	if (Verbose())
	{
		fprintf(stderr, "[jit] compiled %s (%d insns)\n",
			MethodName(method).c_str(),
			static_cast<int>(program->insns.size()));
	}
	return program;
}

} // namespace darksjit
