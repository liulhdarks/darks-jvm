#include "stdafx.h"
#include "stackframe.h"
#include "vm/exceptions.h"
#include "code.h"
#include "jit.h"
#include "vm/classfiles/constant.h"
#include "context.h"
#include "utils/DebugUtils.h"
#include "exec_common.h"
#include "threadstack.h"
#include "methodarea.h"
#include "javaexception.h"
#include "jni/jni_impl.h"
#include "scheduler.h"
#include "vmmutex.h"

#include <ffi.h>

#include <string>
#include <atomic>
#include <vector>
#include <map>

namespace {

struct LambdaCapture
{
	DATA_TYPE type;      // DT_* of the captured value (DT_REF/DT_ARRAY for refs)
	bool wide;           // long/double occupy two slots
	Slot low;
	Slot high;
};

struct LambdaInfo
{
	int kind;                 // MethodHandle reference_kind (5..9)
	lpClass target_class;
	lpMethod target_method;
	std::string sam_name;
	std::vector<LambdaCapture> captures;
};

// Lambda objects are ordinary heap objects of a synthetic class.  Captured
// values live in instance fields ($capN) so the collector traces them; the
// C++-only method identity lives in this side table keyed by a monotonically
// increasing id stored in the object ($lambda:id).
// Kept alive until process exit: the table is bookkeeping for objects that
// live as long as the interpreter, and a heap-allocated map keeps its
// entries reachable from a global for leak checkers.
std::map<int, LambdaInfo> &LambdaTable()
{
	static std::map<int, LambdaInfo> *table = new std::map<int, LambdaInfo>();
	return *table;
}
#define g_lambdas LambdaTable()
int g_next_lambda_id = 1;

lpClass FindSyntheticLambdaClass(const std::string &iface_name,
                                 lpClassLoader loader)
{
	static std::map<std::string, lpClass> *cache = new std::map<std::string, lpClass>();
	std::map<std::string, lpClass>::iterator it = cache->find(iface_name);
	if (it != cache->end())
	{
		return it->second;
	}
	lpClass synth = new Class(CT_OBJECT);
	std::string synth_name = "darks/Lambda$" + iface_name;
	synth->thisClass = new ClassConstant();
	synth->thisClass->name = new CHAR[synth_name.size() + 1];
	memcpy(synth->thisClass->name, synth_name.c_str(), synth_name.size() + 1);
	synth->superClass = new ClassConstant();
	const char *object_name = "java/lang/Object";
	size_t object_name_len = strlen(object_name) + 1;
	synth->superClass->name = new CHAR[object_name_len];
	memcpy(synth->superClass->name, object_name, object_name_len);
	synth->classloader = loader;
	lpClass object_class = NULL;
	if (loader)
	{
		loader->FindClass("java/lang/Object", object_class);
	}
	synth->super_class = object_class;
	synth->interfaceCount = 1;
	synth->interfaces = new lpClassConstant[1];
	synth->interfaces[0] = new ClassConstant();
	synth->interfaces[0]->name = new CHAR[iface_name.size() + 1];
	memcpy(synth->interfaces[0]->name, iface_name.c_str(), iface_name.size() + 1);
	synth->accFlag = 0;
	(*cache)[iface_name] = synth;
	return synth;
}

void StoreSlotField(lpObject obj, const std::string &name, const Slot &slot)
{
	lpFieldValue fv = new FieldValue();
	fv->field_value = slot;
	obj->AdoptFieldValue(name, fv);
}

bool LoadSlotField(lpObject obj, const std::string &name, Slot &slot)
{
	lpFieldValue fv = obj->GetFieldValueByName(name);
	if (!fv)
	{
		return false;
	}
	slot = fv->field_value;
	return true;
}

// Bounded inlining: only tiny, side-effect-free methods are recognised.
// Everything with parameters beyond a setter, branches, exception handlers,
// synchronization or native/abstract modifiers keeps the normal call path.
enum InlineKind
{
	IK_UNKNOWN = 0,
	IK_NONE = -1,
	IK_EMPTY = 1,
	IK_CONST_INT = 2,
	IK_CONST_LONG = 3,
	IK_CONST_FLOAT = 4,
	IK_CONST_DOUBLE = 5,
	IK_CONST_NULL = 6,
	IK_GETTER = 7,
	IK_SETTER = 8
};

int AnalyzeInline(lpMethod method, int &cp_index)
{
	cp_index = 0;
	if (!method || !method->code_attr || !method->code_attr->codes)
	{
		return IK_NONE;
	}
	if (method->isNative() || (method->accFlag & (ACM_ABSTRACT | ACM_SYNCHRONIZED)) != 0)
	{
		return IK_NONE;
	}
	if (method->code_attr->exception_length != 0)
	{
		return IK_NONE;
	}
	if (method->methodName)
	{
		const char *name = method->methodName->GetName();
		if (name && (strcmp(name, "<init>") == 0 || strcmp(name, "<clinit>") == 0))
		{
			return IK_NONE;
		}
	}
	const U1 *codes = method->code_attr->codes;
	int len = static_cast<int>(method->code_attr->code_length);
	if (len == 1 && codes[0] == _return)
	{
		return IK_EMPTY;
	}
	if (len == 2)
	{
		U1 value = codes[0];
		U1 ret_op = codes[1];
		if (value >= iconst_m1 && value <= iconst_5 && ret_op == ireturn)
		{
			return IK_CONST_INT;
		}
		if ((value == lconst_0 || value == lconst_1) && ret_op == lreturn)
		{
			return IK_CONST_LONG;
		}
		if (value >= fconst_0 && value <= fconst_2 && ret_op == freturn)
		{
			return IK_CONST_FLOAT;
		}
		if ((value == dconst_0 || value == dconst_1) && ret_op == dreturn)
		{
			return IK_CONST_DOUBLE;
		}
		if (value == aconst_null && ret_op == areturn)
		{
			return IK_CONST_NULL;
		}
		return IK_NONE;
	}
	// aload_0; getfield <cp>; xreturn
	if (len == 5 && codes[0] == aload_0 && codes[1] == getfield)
	{
		U1 ret_op = codes[4];
		if (ret_op == ireturn || ret_op == lreturn || ret_op == freturn ||
		    ret_op == dreturn || ret_op == areturn)
		{
			cp_index = (static_cast<int>(codes[2]) << 8) | codes[3];
			return IK_GETTER;
		}
	}
	// aload_0; <x>load_1; putfield <cp>; return
	if (len == 6 && codes[0] == aload_0 && codes[2] == putfield && codes[5] == _return)
	{
		U1 load_op = codes[1];
		if (load_op == iload_1 || load_op == fload_1 || load_op == aload_1 ||
		    load_op == lload_1 || load_op == dload_1)
		{
			cp_index = (static_cast<int>(codes[3]) << 8) | codes[4];
			return IK_SETTER;
		}
	}
	return IK_NONE;
}

// Resolves a class for the interpreter, translating a load failure into a
// pending NoClassDefFoundError instead of letting the C++ exception escape
// (which would abort the process with the generic VM error code).
bool FindClassOrError(lpClassLoader loader, const char *name, lpClass &out,
                      const char *error_class = "java/lang/NoClassDefFoundError");

bool ResolveInstanceField(lpClassLoader loader, FieldRefConstant *frc, lpField &field)
{
	field = frc->resolved_field.load(std::memory_order_acquire);
	if (field)
	{
		return true;
	}
	lpClass target = NULL;
	if (!FindClassOrError(loader, frc->clazz ? frc->clazz->GetName() : NULL, target))
	{
		return false;
	}
	const char *name = (frc->nameType && frc->nameType->nameUtf8)
		? frc->nameType->nameUtf8->bytes : NULL;
	const char *desc = (frc->nameType && frc->nameType->descUtf8)
		? frc->nameType->descUtf8->bytes : NULL;
	field = JniFindField(target, name, desc, false);
	if (!field)
	{
		std::string message = (target->thisClass && target->thisClass->GetName())
			? target->thisClass->GetName() : "?";
		message += ".";
		message += name ? name : "?";
		message += ":";
		message += desc ? desc : "?";
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NoSuchFieldError", message.c_str());
		return false;
	}
	frc->resolved_field.store(field, std::memory_order_release);
	return true;
}

bool ResolveStaticField(lpClassLoader loader, FieldRefConstant *frc,
                        lpField &field, lpFieldValue &value)
{
	field = frc->resolved_field.load(std::memory_order_acquire);
	value = frc->resolved_static_value.load(std::memory_order_acquire);
	if (field && value)
	{
		return true;
	}
	lpClass target = NULL;
	if (!FindClassOrError(loader, frc->clazz ? frc->clazz->GetName() : NULL, target))
	{
		return false;
	}
	if (!field)
	{
		const char *name = (frc->nameType && frc->nameType->nameUtf8)
			? frc->nameType->nameUtf8->bytes : NULL;
		const char *desc = (frc->nameType && frc->nameType->descUtf8)
			? frc->nameType->descUtf8->bytes : NULL;
		field = JniFindField(target, name, desc, true);
		if (!field)
		{
			std::string message = (target->thisClass && target->thisClass->GetName())
				? target->thisClass->GetName() : "?";
			message += ".";
			message += name ? name : "?";
			message += ":";
			message += desc ? desc : "?";
			VMContext::pending_exception = CreateJavaException(
				"java/lang/NoSuchFieldError", message.c_str());
			return false;
		}
		frc->resolved_field.store(field, std::memory_order_release);
	}
	if (!value)
	{
		// A static field without an explicit initializer has no <clinit>
		// entry; create its default value lazily on first access.
		value = JniStaticFieldValue(target, field, true);
		if (!value)
		{
			VMContext::pending_exception = CreateJavaException(
				"java/lang/NoSuchFieldError", "static field has no storage");
			return false;
		}
		frc->resolved_static_value.store(value, std::memory_order_release);
	}
	return true;
}

bool FindClassOrError(lpClassLoader loader, const char *name, lpClass &out,
                      const char *error_class)
{
	out = NULL;
	if (!loader || !name)
	{
		VMContext::pending_exception = CreateJavaException(
			error_class, name ? name : "null class name");
		return false;
	}
	try
	{
		if (loader->FindClass(name, out) && out)
		{
			return true;
		}
	}
	catch (VMException *e)
	{
		delete e;
	}
	catch (...)
	{
	}
	VMContext::pending_exception = CreateJavaException(error_class, name);
	return false;
}

} // namespace


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
	pending_sync_obj = NULL;
	pending_sync_class = NULL;
	pending_sync_held = false;
	jit_checked = false;
	jit_program = NULL;
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
	pending_sync_obj = NULL;
	pending_sync_class = NULL;
	pending_sync_held = false;
	static_method = method->isStatic();
	jit_checked = false;
	jit_program = NULL;
}

StackFrame::~StackFrame()
{
	if (op_stack)
	{
		delete op_stack;
		op_stack = NULL;
	}
	if (local_var_table)
	{
		delete local_var_table;
		local_var_table = NULL;
	}
}

void StackFrame::init(lpVariant vars, int count)
{
	if (!method || !method->code_attr)
	{
		fprintf(stderr, "[frame] no code: %s.%s\n",
			(method && method->clazz && method->clazz->thisClass) ? method->clazz->thisClass->GetName() : "?",
			(method && method->fullName) ? method->fullName : "?");
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

void StackFrame::MarkRoots(ObjectHeap *heap)
{
	if (!heap)
	{
		return;
	}
	if (local_var_table)
	{
		Slot *slots = local_var_table->data();
		int count = local_var_table->capacity();
		for (int i = 0; i < count; i++)
		{
			heap->MarkObjectRef(slots[i].heapPtr);
		}
	}
	if (op_stack)
	{
		Slot *slots = op_stack->data();
		int count = op_stack->size();
		for (int i = 0; i < count; i++)
		{
			heap->MarkObjectRef(slots[i].heapPtr);
		}
	}
}

EventNotify StackFrame::Execute()
{
	if (!inited)
	{
		init();
	}
	if (!jit_checked)
	{
		jit_checked = true;
		jit_program = darksjit::Acquire(method);
	}
	if (jit_program)
	{
		return ExecuteJit(jit_program);
	}
	return Interpret();
}

EventNotify StackFrame::Interpret()
{
	while (1)
	{
		EventNotify ret = ExecuteCode();
		if (ret != EN_NOOP)
		{
			return ret;
		}
	}
}

EventNotify StackFrame::BeginInstruction()
{
	if (pc >= code_length)
	{
		return EN_EXCEPTION;
	}
	if (thread_stack && thread_stack->injected_exception)
	{
		// A blocking call was interrupted (or a monitor grant carried an
		// exception).  Raise it at the suspended call site so that ordinary
		// exception-table unwinding (including finally blocks) runs.
		VMContext::pending_exception = thread_stack->injected_exception;
		thread_stack->injected_exception = NULL;
		return ExceptionEvent();
	}
	if (thread_stack && thread_stack->yield_pending)
	{
		// A blocking primitive or contended monitor parked this coroutine from
		// inside an instruction handler that reported success; suspend now.
		thread_stack->yield_pending = false;
		return EN_YIELD;
	}
	if (thread_stack)
	{
		if (--thread_stack->safepoint_countdown <= 0)
		{
			thread_stack->safepoint_countdown = 512;
			if (Scheduler::Instance().PollSafepoint(thread_stack))
			{
				return EN_SAFEPOINT;
			}
		}
		if (--thread_stack->fairness_countdown <= 0)
		{
			thread_stack->fairness_countdown = 32768;
			if (Scheduler::Instance().FairnessYield(thread_stack))
			{
				return EN_YIELD;
			}
		}
	}
	last_pc = pc;
	if (op_stack)
	{
		op_stack->debug_method = method && method->fullName ? method->fullName : "?";
		op_stack->debug_pc = pc;
	}
	return EN_NOOP;
}

EventNotify StackFrame::ExecuteJit(const darksjit::Program *program)
{
	while (1)
	{
		if (!program || pc < 0 || static_cast<U4>(pc) >= code_length)
		{
			return EN_EXCEPTION;
		}
		int insn_index = program->pc_index[pc];
		if (insn_index < 0)
		{
			// Should not happen for compiled methods; fall back to the
			// interpreter without losing the current pc.
			return Interpret();
		}
		const darksjit::Insn &insn = program->insns[insn_index];
		if (!insn.fast)
		{
			EventNotify delegated = ExecuteCode();
			if (delegated != EN_NOOP)
			{
				return delegated;
			}
			continue;
		}
		EventNotify pre = BeginInstruction();
		if (pre != EN_NOOP)
		{
			return pre;
		}
		pc = insn.next_pc;
		Slot s1, s2, s3, s4;
		LONG64 lValue, lValue2;
		double dValue, dValue2;
		switch (insn.op)
		{
		case nop:
			break;

		case aconst_null:
			op_stack->push(0);
			break;
		case iconst_m1:
		case iconst_0:
		case iconst_1:
		case iconst_2:
		case iconst_3:
		case iconst_4:
		case iconst_5:
			op_stack->push((int)(insn.op - iconst_0));
			break;
		case lconst_0:
		case lconst_1:
			op_stack->push(static_cast<LONG64>(insn.op - lconst_0));
			break;
		case fconst_0:
		case fconst_1:
		case fconst_2:
			op_stack->push((float)(insn.op - fconst_0));
			break;
		case dconst_0:
		case dconst_1:
			op_stack->push((double)(insn.op - dconst_0));
			break;
		case bipush:
			op_stack->push(insn.a);
			break;
		case sipush:
			op_stack->push(insn.a);
			break;
		case ldc:
		case ldc_w:
			LoadConstant(insn.a, s1);
			op_stack->push(s1);
			break;
		case ldc2_w:
			LoadConstantW(static_cast<U2>(insn.a), s1, s2);
			op_stack->push(s1);
			op_stack->push(s2);
			break;

		case iload:
		case iload_0:
		case iload_1:
		case iload_2:
		case iload_3:
		case fload:
		case fload_0:
		case fload_1:
		case fload_2:
		case fload_3:
		case aload:
		case aload_0:
		case aload_1:
		case aload_2:
		case aload_3:
			s1 = local_var_table->get(insn.a);
			op_stack->push(s1);
			break;
		case lload:
		case lload_0:
		case lload_1:
		case lload_2:
		case lload_3:
		case dload:
		case dload_0:
		case dload_1:
		case dload_2:
		case dload_3:
			s1 = local_var_table->get(insn.a);
			s2 = local_var_table->get(insn.a + 1);
			op_stack->push(s1);
			op_stack->push(s2);
			break;

		case istore:
		case istore_0:
		case istore_1:
		case istore_2:
		case istore_3:
		case fstore:
		case fstore_0:
		case fstore_1:
		case fstore_2:
		case fstore_3:
		case astore:
		case astore_0:
		case astore_1:
		case astore_2:
		case astore_3:
			op_stack->pop(s1);
			local_var_table->set(insn.a, s1);
			break;
		case lstore:
		case lstore_0:
		case lstore_1:
		case lstore_2:
		case lstore_3:
		case dstore:
		case dstore_0:
		case dstore_1:
		case dstore_2:
		case dstore_3:
			op_stack->pop(s1);
			local_var_table->set(insn.a + 1, s1);
			op_stack->pop(s1);
			local_var_table->set(insn.a, s1);
			break;
		case iinc:
			s1 = local_var_table->get(insn.a);
			s1.iValue += insn.b;
			local_var_table->set(insn.a, s1);
			break;

		case iaload:
			if (!LoadArray(DT_INT))
			{
				return ExceptionEvent();
			}
			break;
		case laload:
			if (!LoadArray(DT_LONG))
			{
				return ExceptionEvent();
			}
			break;
		case faload:
			if (!LoadArray(DT_FLOAT))
			{
				return ExceptionEvent();
			}
			break;
		case daload:
			if (!LoadArray(DT_DOUBLE))
			{
				return ExceptionEvent();
			}
			break;
		case aaload:
			if (!LoadArray(DT_REF))
			{
				return ExceptionEvent();
			}
			break;
		case baload:
			if (!LoadArray(DT_BYTE))
			{
				return ExceptionEvent();
			}
			break;
		case caload:
			if (!LoadArray(DT_CHAR))
			{
				return ExceptionEvent();
			}
			break;
		case saload:
			if (!LoadArray(DT_SHORT))
			{
				return ExceptionEvent();
			}
			break;
		case iastore:
		case fastore:
		case bastore:
		case castore:
		case sastore:
		case aastore:
			if (!SetArrayIndex(1))
			{
				return ExceptionEvent();
			}
			break;
		case lastore:
		case dastore:
			if (!SetArrayIndex(2))
			{
				return ExceptionEvent();
			}
			break;

		case _pop:
			op_stack->pop(s1);
			break;
		case _pop2:
			op_stack->pop(s1);
			op_stack->pop(s2);
			break;
		case dup:
			s1 = op_stack->top();
			op_stack->push(s1);
			break;
		case dup_x1:
			op_stack->pop(s1);
			op_stack->pop(s2);
			op_stack->push(s1);
			op_stack->push(s2);
			op_stack->push(s1);
			break;
		case dup_x2:
			op_stack->pop(s1);
			op_stack->pop(s2);
			op_stack->pop(s3);
			op_stack->push(s1);
			op_stack->push(s3);
			op_stack->push(s2);
			op_stack->push(s1);
			break;
		case dup2:
			op_stack->pop(s1);
			op_stack->pop(s2);
			op_stack->push(s2);
			op_stack->push(s1);
			op_stack->push(s2);
			op_stack->push(s1);
			break;
		case dup2_x1:
			op_stack->pop(s1);
			op_stack->pop(s2);
			op_stack->pop(s3);
			op_stack->push(s2);
			op_stack->push(s1);
			op_stack->push(s3);
			op_stack->push(s2);
			op_stack->push(s1);
			break;
		case dup2_x2:
			op_stack->pop(s1);
			op_stack->pop(s2);
			op_stack->pop(s3);
			op_stack->pop(s4);
			op_stack->push(s2);
			op_stack->push(s1);
			op_stack->push(s4);
			op_stack->push(s3);
			op_stack->push(s2);
			op_stack->push(s1);
			break;

		case iadd:
			op_stack->pop(s1);
			op_stack->pop(s2);
			op_stack->push(s1.iValue + s2.iValue);
			break;
		case ladd:
			lValue = popLong() + popLong();
			op_stack->push(lValue);
			break;
		case fadd:
			op_stack->pop(s1);
			op_stack->pop(s2);
			op_stack->push(s1.fValue + s2.fValue);
			break;
		case dadd:
			dValue = popDouble() + popDouble();
			op_stack->push(dValue);
			break;
		case isub:
			op_stack->pop(s1);
			op_stack->pop(s2);
			op_stack->push(s2.iValue - s1.iValue);
			break;
		case lsub:
			lValue = popLong();
			lValue2 = popLong();
			op_stack->push(lValue2 - lValue);
			break;
		case fsub:
			op_stack->pop(s1);
			op_stack->pop(s2);
			op_stack->push(s2.fValue - s1.fValue);
			break;
		case dsub:
			dValue = popDouble();
			dValue2 = popDouble();
			op_stack->push(dValue2 - dValue);
			break;
		case imul:
			op_stack->pop(s1);
			op_stack->pop(s2);
			op_stack->push(s2.iValue * s1.iValue);
			break;
		case lmul:
			lValue = popLong() * popLong();
			op_stack->push(lValue);
			break;
		case fmul:
			op_stack->pop(s1);
			op_stack->pop(s2);
			op_stack->push(s2.fValue * s1.fValue);
			break;
		case dmul:
			dValue = popDouble() * popDouble();
			op_stack->push(dValue);
			break;
		case idiv:
			op_stack->pop(s1);
			op_stack->pop(s2);
			if (s1.iValue == 0)
			{
				VMContext::pending_exception = CreateJavaException(
					"java/lang/ArithmeticException", "/ by zero");
				return ExceptionEvent();
			}
			if (s1.iValue == -1 && s2.iValue == (-2147483647 - 1))
			{
				// JLS 15.17.2: MIN_VALUE / -1 wraps to MIN_VALUE.  The C++
				// operation would be undefined and traps with SIGFPE.
				op_stack->push((-2147483647 - 1));
			}
			else
			{
				op_stack->push(s2.iValue / s1.iValue);
			}
			break;
		case ldiv:
			lValue = popLong();
			lValue2 = popLong();
			if (lValue == 0)
			{
				VMContext::pending_exception = CreateJavaException(
					"java/lang/ArithmeticException", "/ by zero");
				return ExceptionEvent();
			}
			if (lValue == -1 && lValue2 == (-9223372036854775807LL - 1))
			{
				op_stack->push(static_cast<LONG64>((-9223372036854775807LL - 1)));
			}
			else
			{
				op_stack->push(lValue2 / lValue);
			}
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
			if (s1.iValue == 0)
			{
				VMContext::pending_exception = CreateJavaException(
					"java/lang/ArithmeticException", "/ by zero");
				return ExceptionEvent();
			}
			if (s1.iValue == -1)
			{
				// JLS 15.17.3: MIN_VALUE % -1 == 0 (and avoids SIGFPE).
				op_stack->push(0);
			}
			else
			{
				op_stack->push(s2.iValue % s1.iValue);
			}
			break;
		case lrem:
			lValue = popLong();
			lValue2 = popLong();
			if (lValue == 0)
			{
				VMContext::pending_exception = CreateJavaException(
					"java/lang/ArithmeticException", "/ by zero");
				return ExceptionEvent();
			}
			if (lValue == -1)
			{
				op_stack->push(static_cast<LONG64>(0));
			}
			else
			{
				op_stack->push(lValue2 % lValue);
			}
			break;
		case frem:
			op_stack->pop(s1);
			op_stack->pop(s2);
			op_stack->push(fmodf(s2.fValue, s1.fValue));
			break;
		case drem:
			dValue = popDouble();
			dValue2 = popDouble();
			op_stack->push(fmod(dValue2, dValue));
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
			s3.iValue = (int)((uint32_t)s2.iValue << (s1.iValue & 0x1f));
			op_stack->push(s3);
			break;
		case lshl:
			op_stack->pop(s1);
			lValue = popLong();
			lValue = (LONG64)((uint64_t)lValue << (s1.iValue & 0x3f));
			op_stack->push(lValue);
			break;
		case ishr:
			op_stack->pop(s1);
			op_stack->pop(s2);
			s3.iValue = s2.iValue >> (s1.iValue & 0x1f);
			op_stack->push(s3);
			break;
		case lshr:
			op_stack->pop(s1);
			lValue = popLong();
			lValue = lValue >> (s1.iValue & 0x3f);
			op_stack->push(lValue);
			break;
		case iushr:
			op_stack->pop(s1);
			op_stack->pop(s2);
			s3.iValue = (int)(((uint32_t)s2.iValue) >> (s1.iValue & 0x1f));
			op_stack->push(s3);
			break;
		case lushr:
			op_stack->pop(s1);
			lValue = popLong();
			lValue = (LONG64)(((uint64_t)lValue) >> (s1.iValue & 0x3f));
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
			op_stack->push(lValue & lValue2);
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
			op_stack->push(lValue | lValue2);
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
			op_stack->push(lValue ^ lValue2);
			break;

		case i2l:
			op_stack->pop(s1);
			op_stack->push((LONG64)s1.iValue);
			break;
		case i2f:
			op_stack->pop(s1);
			op_stack->push((float)s1.iValue);
			break;
		case i2d:
			op_stack->pop(s1);
			op_stack->push((double)s1.iValue);
			break;
		case l2i:
			lValue = popLong();
			op_stack->push((int)lValue);
			break;
		case l2f:
			lValue = popLong();
			op_stack->push((float)lValue);
			break;
		case l2d:
			lValue = popLong();
			op_stack->push((double)lValue);
			break;
		case f2i:
			op_stack->pop(s1);
			op_stack->push((int)s1.fValue);
			break;
		case f2l:
			op_stack->pop(s1);
			op_stack->push((LONG64)s1.fValue);
			break;
		case f2d:
			op_stack->pop(s1);
			op_stack->push((double)s1.fValue);
			break;
		case d2i:
			dValue = popDouble();
			op_stack->push((int)dValue);
			break;
		case d2l:
			dValue = popDouble();
			op_stack->push((LONG64)dValue);
			break;
		case d2f:
			dValue = popDouble();
			op_stack->push((float)dValue);
			break;
		case i2b:
			op_stack->pop(s1);
			s2.bValue = (signed char)s1.iValue;
			op_stack->push(s2);
			break;
		case i2c:
			op_stack->pop(s1);
			s2.cValue = (uint16_t)s1.iValue;
			op_stack->push(s2);
			break;
		case i2s:
			op_stack->pop(s1);
			s2.sValue = (short)s1.iValue;
			op_stack->push(s2);
			break;
		case lcmp:
			lValue2 = popLong();
			lValue = popLong();
			s1.iValue = (lValue > lValue2) ? 1 : ((lValue == lValue2) ? 0 : -1);
			op_stack->push(s1);
			break;
		case fcmpl:
			op_stack->pop(s2);
			op_stack->pop(s1);
			if (s1.fValue > s2.fValue) s3.iValue = 1;
			else if (s1.fValue == s2.fValue) s3.iValue = 0;
			else if (s1.fValue < s2.fValue) s3.iValue = -1;
			else s3.iValue = -1;
			op_stack->push(s3);
			break;
		case fcmpg:
			op_stack->pop(s2);
			op_stack->pop(s1);
			if (s1.fValue > s2.fValue) s3.iValue = 1;
			else if (s1.fValue == s2.fValue) s3.iValue = 0;
			else if (s1.fValue < s2.fValue) s3.iValue = -1;
			else s3.iValue = 1;
			op_stack->push(s3);
			break;
		case dcmpl:
			dValue2 = popDouble();
			dValue = popDouble();
			if (dValue > dValue2) s1.iValue = 1;
			else if (dValue == dValue2) s1.iValue = 0;
			else if (dValue < dValue2) s1.iValue = -1;
			else s1.iValue = -1;
			op_stack->push(s1);
			break;
		case dcmpg:
			dValue2 = popDouble();
			dValue = popDouble();
			if (dValue > dValue2) s1.iValue = 1;
			else if (dValue == dValue2) s1.iValue = 0;
			else if (dValue < dValue2) s1.iValue = -1;
			else s1.iValue = 1;
			op_stack->push(s1);
			break;

		case ifeq:
			op_stack->pop(s1);
			if (s1.iValue == 0) pc = insn.target;
			break;
		case ifne:
			op_stack->pop(s1);
			if (s1.iValue != 0) pc = insn.target;
			break;
		case iflt:
			op_stack->pop(s1);
			if (s1.iValue < 0) pc = insn.target;
			break;
		case ifge:
			op_stack->pop(s1);
			if (s1.iValue >= 0) pc = insn.target;
			break;
		case ifgt:
			op_stack->pop(s1);
			if (s1.iValue > 0) pc = insn.target;
			break;
		case ifle:
			op_stack->pop(s1);
			if (s1.iValue <= 0) pc = insn.target;
			break;
		case if_icmpeq:
			op_stack->pop(s2);
			op_stack->pop(s1);
			if (s1.iValue == s2.iValue) pc = insn.target;
			break;
		case if_icmpne:
			op_stack->pop(s2);
			op_stack->pop(s1);
			if (s1.iValue != s2.iValue) pc = insn.target;
			break;
		case if_icmplt:
			op_stack->pop(s2);
			op_stack->pop(s1);
			if (s1.iValue < s2.iValue) pc = insn.target;
			break;
		case if_icmpge:
			op_stack->pop(s2);
			op_stack->pop(s1);
			if (s1.iValue >= s2.iValue) pc = insn.target;
			break;
		case if_icmpgt:
			op_stack->pop(s2);
			op_stack->pop(s1);
			if (s1.iValue > s2.iValue) pc = insn.target;
			break;
		case if_icmple:
			op_stack->pop(s2);
			op_stack->pop(s1);
			if (s1.iValue <= s2.iValue) pc = insn.target;
			break;
		case if_acmpeq:
			op_stack->pop(s2);
			op_stack->pop(s1);
			if (s1.heapPtr == s2.heapPtr) pc = insn.target;
			break;
		case if_acmpne:
			op_stack->pop(s2);
			op_stack->pop(s1);
			if (s1.heapPtr != s2.heapPtr) pc = insn.target;
			break;
		case ifnull:
			op_stack->pop(s1);
			if (!s1.heapPtr) pc = insn.target;
			break;
		case ifnonnull:
			op_stack->pop(s1);
			if (s1.heapPtr) pc = insn.target;
			break;
		case _goto:
			pc = insn.target;
			break;

		case getstatic:
			if (!GetStatic(static_cast<U2>(insn.a)))
			{
				return ExceptionEvent();
			}
			break;
		case putstatic:
			if (!PutStatic(static_cast<U2>(insn.a)))
			{
				return ExceptionEvent();
			}
			break;
		case getfield:
			if (!GetField(static_cast<U2>(insn.a)))
			{
				return ExceptionEvent();
			}
			break;
		case putfield:
			if (!PutField(static_cast<U2>(insn.a)))
			{
				return ExceptionEvent();
			}
			break;
		case _new:
			if (!ExecuteNewObject(static_cast<U2>(insn.a)))
			{
				return ExceptionEvent();
			}
			break;
		case newarray:
			if (!ExecuteNewArray(static_cast<U1>(insn.a)))
			{
				return ExceptionEvent();
			}
			break;
		case anewarray:
			if (!ExecuteNewObjectArray(static_cast<U2>(insn.a)))
			{
				return ExceptionEvent();
			}
			break;
		case arraylength:
			if (!ArrayLength())
			{
				return ExceptionEvent();
			}
			break;

		case invokespecial:
			if (ExecuteSpecial(static_cast<int>(insn.a)))
			{
				return EN_KEEP;
			}
			return ExceptionEvent();
		case invokeinterface:
		case invokevirtual:
			if (ExecuteVirtual(static_cast<int>(insn.a), NORMAL_METHOD))
			{
				return EN_KEEP;
			}
			return ExceptionEvent();
		case invokestatic:
			if (ExecuteStatic(static_cast<int>(insn.a)))
			{
				return EN_KEEP;
			}
			return ExceptionEvent();
		case invokedynamic:
			if (ExecuteInvokeDynamic(static_cast<U2>(insn.a)))
			{
				break;
			}
			return ExceptionEvent();

		case ireturn:
		case freturn:
		case areturn:
			op_stack->pop(s1);
			if (prev_frame)
			{
				prev_frame->op_stack->push(s1);
			}
			else
			{
				thread_stack->return_value = s1;
				thread_stack->return_wide = false;
				thread_stack->has_return = true;
			}
			return EN_POP;
		case lreturn:
		case dreturn:
			op_stack->pop(s1);
			op_stack->pop(s2);
			if (prev_frame)
			{
				prev_frame->op_stack->push(s2);
				prev_frame->op_stack->push(s1);
			}
			else
			{
				thread_stack->return_value = s2;
				thread_stack->return_value_hi = s1;
				thread_stack->return_wide = true;
				thread_stack->has_return = true;
			}
			return EN_POP;
		case _return:
			if (!prev_frame)
			{
				thread_stack->has_return = true;
				thread_stack->return_wide = false;
			}
			return EN_POP;

		default:
			// Should be unreachable because the decoder only marks known
			// opcodes as fast.
			pc = insn.pc;
			return Interpret();
		}
	}
}

EventNotify StackFrame::ExecuteCode()
{
	EventNotify pre = BeginInstruction();
	if (pre != EN_NOOP)
	{
		return pre;
	}
	Slot s1, s2, s3;
	int index = 0;
	U2 u2 = 0;
	LONG64 lValue,lValue2;
	double dValue,dValue2;
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
		op_stack->push(static_cast<LONG64>(code - lconst_0));
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
		// The operand is a signed byte.
		op_stack->push((int)(signed char)codes[pc]);
		pc++;
		break;
	case sipush:// 17 /*(0x11)*/
		u2 = ReadU2();
		op_stack->push((int)(short)(u2 & 0xFFFF));
		break;

	case ldc: //Push item from constant pool 			
		LoadConstant(codes[pc++], s1);
		op_stack->push(s1);
		break;

	case ldc_w: // Push item from constant pool (wide index)
		LoadConstant(ReadU2(), s1);
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
		index = codes[pc++];
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

	case fload:// 23 /*(0x17)*/
		s1 = local_var_table->get(codes[pc++]);
		op_stack->push(s1);
		break;
	case dload:// 24 /*(0x18)*/
		index = codes[pc];
		s1 = local_var_table->get(index);
		s2 = local_var_table->get(index + 1);
		op_stack->push(s1);
		op_stack->push(s2);
		pc++;
		break;
	case dload_0:// 38 /*(0x26) */
	case dload_1:// 39 /*(0x27) */
	case dload_2:// 40 /*(0x28) */
	case dload_3:// 41 /*(0x29) */
		index = code - dload_0;
		s1 = local_var_table->get(index);
		s2 = local_var_table->get(index + 1);
		op_stack->push(s1);
		op_stack->push(s2);
		break;

	case aload_0:  //42 Load reference from local variable 0
	case aload_1:  //Load reference from local variable 1
	case aload_2:  //Load reference from local variable 2
	case aload_3:  //Load reference from local variable 3
		s1 = local_var_table->get(code - aload_0);
		op_stack->push(s1);
		break;

	case iaload:// 46 /*(0x2e)*/Load int from array
		if (!LoadArray(DT_INT))
		{
			return ExceptionEvent();
		}
		break;
	case laload:
		if (!LoadArray(DT_LONG))
		{
			return ExceptionEvent();
		}
		break;
	case faload:
		if (!LoadArray(DT_FLOAT))
		{
			return ExceptionEvent();
		}
		break;
	case daload:
		if (!LoadArray(DT_DOUBLE))
		{
			return ExceptionEvent();
		}
		break;
	case aaload://50 
		if (!LoadArray(DT_REF))
		{
			return ExceptionEvent();
		}
		break;
	case baload:
		if (!LoadArray(DT_BYTE))
		{
			return ExceptionEvent();
		}
		break;
	case caload:
		if (!LoadArray(DT_CHAR))
		{
			return ExceptionEvent();
		}
		break;
	case saload:
		if (!LoadArray(DT_SHORT))
		{
			return ExceptionEvent();
		}
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
	case aastore:
		if (!SetArrayIndex(1))
		{
			return ExceptionEvent();
		}
		break;
	case lastore:
	case dastore:
		if (!SetArrayIndex(2))
		{
			return ExceptionEvent();
		}
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
		op_stack->pop(s1);          // value1
		op_stack->pop(s2);          // value2
		op_stack->push(s1);
		op_stack->push(s2);
		op_stack->push(s1);
		break;
	case dup_x2:// 91 /*(0x5b)*/
		op_stack->pop(s1);          // value1
		op_stack->pop(s2);          // value2
		op_stack->pop(s3);          // value3
		op_stack->push(s1);
		op_stack->push(s3);
		op_stack->push(s2);
		op_stack->push(s1);
		break;

	case dup2:
		op_stack->pop(s1);          // value1
		op_stack->pop(s2);          // value2
		op_stack->push(s2);
		op_stack->push(s1);
		op_stack->push(s2);
		op_stack->push(s1);
		break;
	case dup2_x1:
		op_stack->pop(s1);          // value1
		op_stack->pop(s2);          // value2
		op_stack->pop(s3);          // value3
		op_stack->push(s2);
		op_stack->push(s1);
		op_stack->push(s3);
		op_stack->push(s2);
		op_stack->push(s1);
		break;
	case dup2_x2:
		{
			Slot s4;
			op_stack->pop(s1);      // value1
			op_stack->pop(s2);      // value2
			op_stack->pop(s3);      // value3
			op_stack->pop(s4);      // value4
			op_stack->push(s2);
			op_stack->push(s1);
			op_stack->push(s4);
			op_stack->push(s3);
			op_stack->push(s2);
			op_stack->push(s1);
		}
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
		if (s1.iValue == 0)
		{
			VMContext::pending_exception = CreateJavaException(
				"java/lang/ArithmeticException", "/ by zero");
			return ExceptionEvent();
		}
		if (s1.iValue == -1 && s2.iValue == (-2147483647 - 1))
		{
			op_stack->push((-2147483647 - 1));
		}
		else
		{
			op_stack->push(s2.iValue / s1.iValue);
		}
		break;
	case ldiv:
		lValue = popLong();
		lValue2 = popLong();
		if (lValue == 0)
		{
			VMContext::pending_exception = CreateJavaException(
				"java/lang/ArithmeticException", "/ by zero");
			return ExceptionEvent();
		}
		if (lValue == -1 && lValue2 == (-9223372036854775807LL - 1))
		{
			op_stack->push(static_cast<LONG64>((-9223372036854775807LL - 1)));
		}
		else
		{
			op_stack->push(lValue2 / lValue);
		}
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
		if (s1.iValue == 0)
		{
			VMContext::pending_exception = CreateJavaException(
				"java/lang/ArithmeticException", "/ by zero");
			return ExceptionEvent();
		}
		if (s1.iValue == -1)
		{
			op_stack->push(0);
		}
		else
		{
			op_stack->push(s2.iValue % s1.iValue);
		}
		break;
	case lrem:
		lValue = popLong();
		lValue2 = popLong();
		if (lValue == 0)
		{
			VMContext::pending_exception = CreateJavaException(
				"java/lang/ArithmeticException", "/ by zero");
			return ExceptionEvent();
		}
		if (lValue == -1)
		{
			op_stack->push(static_cast<LONG64>(0));
		}
		else
		{
			op_stack->push(lValue2 % lValue);
		}
		break;
	case frem:
		op_stack->pop(s1);
		op_stack->pop(s2);
		op_stack->push(fmodf(s2.fValue, s1.fValue));	
		break;
	case drem:
		dValue = popDouble();
		dValue2 = popDouble();
		op_stack->push(fmod(dValue2, dValue));
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
		s3.iValue = (int)((uint32_t)s2.iValue << (s1.iValue & 0x1f));
		op_stack->push(s3);
		break;
	case lshl:
		op_stack->pop(s1);
		lValue = popLong();
		lValue = (LONG64)((uint64_t)lValue << (s1.iValue & 0x3f));
		op_stack->push(lValue);
		break;
	case ishr:
		op_stack->pop(s1);
		op_stack->pop(s2);
		s3.iValue = s2.iValue >> (s1.iValue & 0x1f);
		op_stack->push(s3);
		break;
	case lshr:
		op_stack->pop(s1);
		lValue = popLong();
		lValue = lValue >> (s1.iValue & 0x3f);
		op_stack->push(lValue);
		break;
	case iushr:
		op_stack->pop(s1);
		op_stack->pop(s2);
		s3.iValue = (int)(((uint32_t)s2.iValue) >> (s1.iValue & 0x1f));
		op_stack->push(s3);
		break;
	case lushr:
		op_stack->pop(s1);
		lValue = popLong();
		lValue = (LONG64)(((uint64_t)lValue) >> (s1.iValue & 0x3f));
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
		s1.iValue += (int)(signed char)codes[pc++];
		local_var_table->set(index, s1);
		break;
	case _ret:// 169 /*(0xa9)*/ Return from subroutine (legacy jsr/ret bytecode)
		index = codes[pc++];
		pc = local_var_table->get(index).iValue;
		break;
	case _wide:// 196 /*(0xc4)*/ Modify local variable instruction with a U2 index
		{
			BYTE wide_op = codes[pc++];
			index = (static_cast<int>(codes[pc]) << 8) | codes[pc + 1];
			pc += 2;
			switch (wide_op)
			{
			case iload:
			case fload:
			case aload:
				op_stack->push(local_var_table->get(index));
				break;
			case lload:
			case dload:
				s1 = local_var_table->get(index);
				s2 = local_var_table->get(index + 1);
				op_stack->push(s1);
				op_stack->push(s2);
				break;
			case istore:
			case fstore:
			case astore:
				op_stack->pop(s1);
				local_var_table->set(index, s1);
				break;
			case lstore:
			case dstore:
				op_stack->pop(s1);
				local_var_table->set(index + 1, s1);
				op_stack->pop(s1);
				local_var_table->set(index, s1);
				break;
			case iinc:
				{
					// The wide form's increment is a signed 16-bit value
					// (not the signed byte used by the short form).
					int increment = (static_cast<int>(static_cast<signed char>(codes[pc])) << 8)
						| codes[pc + 1];
					pc += 2;
					s1 = local_var_table->get(index);
					s1.iValue += increment;
					local_var_table->set(index, s1);
				}
				break;
			case _ret:
				pc = local_var_table->get(index).iValue;
				break;
			default:
				throw new VMExecutorException("Unsupported wide opcode.");
			}
		}
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
		s2.cValue = (uint16_t) s1.iValue;
		op_stack->push(s2);
		break;
	case i2s:
		op_stack->pop(s1);
		s2.sValue = (short) s1.iValue;
		op_stack->push(s2);
		break;

	case lcmp://148
		lValue2 = popLong();
		lValue = popLong();
		s1.iValue = (lValue > lValue2) ? 1 : ((lValue == lValue2) ? 0 : -1);
		op_stack->push(s1);
		break;
	case fcmpl://149
		op_stack->pop(s2);
		op_stack->pop(s1);
		if (s1.fValue > s2.fValue) s3.iValue = 1;
		else if (s1.fValue == s2.fValue) s3.iValue = 0;
		else if (s1.fValue < s2.fValue) s3.iValue = -1;
		else s3.iValue = -1; // NaN
		op_stack->push(s3);
		break;
	case fcmpg://150
		op_stack->pop(s2);
		op_stack->pop(s1);
		if (s1.fValue > s2.fValue) s3.iValue = 1;
		else if (s1.fValue == s2.fValue) s3.iValue = 0;
		else if (s1.fValue < s2.fValue) s3.iValue = -1;
		else s3.iValue = 1; // NaN
		op_stack->push(s3);
		break;
	case dcmpl://151
		dValue2 = popDouble();
		dValue = popDouble();
		if (dValue > dValue2) s1.iValue = 1;
		else if (dValue == dValue2) s1.iValue = 0;
		else if (dValue < dValue2) s1.iValue = -1;
		else s1.iValue = -1; // NaN
		op_stack->push(s1);
		break;
	case dcmpg://152
		dValue2 = popDouble();
		dValue = popDouble();
		if (dValue > dValue2) s1.iValue = 1;
		else if (dValue == dValue2) s1.iValue = 0;
		else if (dValue < dValue2) s1.iValue = -1;
		else s1.iValue = 1; // NaN
		op_stack->push(s1);
		break;
		////////////////////// Logic ///////////////////

		//Shift operations

		//Bitwise boolean operations

		//Floating Point Arithmetic 



		/////////////// Objects and Arrays  ////////////

		//Instructions that deal with objects

	case _new:// 187 (0xbb)
		if (!ExecuteNewObject(ReadU2()))
		{
			return ExceptionEvent();
		}
		break;
	case putfield: //181 (0xb5): Set field in object
		if (!PutField(ReadU2()))
		{
			return ExceptionEvent();
		}
		break;

	case getfield: //180 (0xb4) Fetch field from object
		if (!GetField(ReadU2()))
		{
			return ExceptionEvent();
		}
		break;

		//Instructions that deal with arrays

	case newarray:// 188 /*(0xbc)*/
		if (!ExecuteNewArray(codes[pc++]))
		{
			return ExceptionEvent();
		}
		break;

	case anewarray: //189
		u2 = ReadU2();
		if (!ExecuteNewObjectArray(u2))
		{
			return ExceptionEvent();
		}
		break;
	case arraylength:
		if (!ArrayLength())
		{
			return ExceptionEvent();
		}
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

	case if_acmpeq: // 165
		index = ReadShort();
		op_stack->pop(s2);
		op_stack->pop(s1);
		if (s1.heapPtr == s2.heapPtr)
		{
			pc = last_pc + index;
		}
		break;
	case if_acmpne: // 166
		index = ReadShort();
		op_stack->pop(s2);
		op_stack->pop(s1);
		if (s1.heapPtr != s2.heapPtr)
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
	case _jsr: // 168 /*(0xa8)*/ Jump to subroutine, pushing the return address
		{
			int offset = ReadShort();
			Slot return_address;
			return_address.iValue = pc;
			op_stack->push(return_address);
			pc = last_pc + offset;
		}
		break;
	case _jsr_w: // 201 /*(0xc9)*/
		{
			int offset = ReadInt();
			Slot return_address;
			return_address.iValue = pc;
			op_stack->push(return_address);
			pc = last_pc + offset;
		}
		break;
		//Table jumping instructions

		////////////// Exceptions ///////////////////////
	case athrow:
		{
			Slot ex_slot;
			op_stack->pop(ex_slot);
			LPOBJECT_REF ex_ref = ex_slot.heapPtr;
			if (!ex_ref)
			{
				ex_ref = CreateJavaException("java/lang/NullPointerException",
					"cannot throw a null object");
			}
			VMContext::pending_exception = ex_ref;
			return ExceptionEvent();
		}

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
			return ExceptionEvent();
		}
		break;

	case invokeinterface: //185: index(2) + count(1) + zero(1)
		index = ReadU2();
		pc += 2;
		if (ExecuteVirtual(index, NORMAL_METHOD))
		{
			return EN_KEEP;
		}
		return ExceptionEvent();

	case invokevirtual: //182
		index = ReadU2();
		if (ExecuteVirtual(index, NORMAL_METHOD))
		{
			return EN_KEEP;
		}
		return ExceptionEvent();

	case invokestatic:// 184 
		index = ReadU2();
		if (ExecuteStatic(index))
		{
			return EN_KEEP;
		}
		return ExceptionEvent();

	case invokedynamic:// 186 (0xba): index(2) + zero(2)
		index = ReadU2();
		pc += 2;
		if (ExecuteInvokeDynamic(static_cast<U2>(index)))
		{
			break;
		}
		return ExceptionEvent();
		
		//Method return instructions
	case ireturn: //172 (0xac)		
	case freturn: //174 (0xae)		
	case areturn: //176 (0xb0)
		op_stack->pop(s1);
		if (prev_frame)
		{
			prev_frame->op_stack->push(s1);
		}
		else
		{
			thread_stack->return_value = s1;
			thread_stack->return_wide = false;
			thread_stack->has_return = true;
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
		else
		{
			thread_stack->return_value = s2;
			thread_stack->return_value_hi = s1;
			thread_stack->return_wide = true;
			thread_stack->has_return = true;
		}
		return EN_POP;

	case _return: //177 (0xb1): Return (void) from method			
		if (!prev_frame)
		{
			thread_stack->has_return = true;
			thread_stack->return_wide = false;
		}
		return EN_POP;
	case getstatic: //178 (0xb2): Return (void) from method
		index = ReadU2();
		if (!GetStatic(index))
		{
			return ExceptionEvent();
		}
		break;
	case putstatic: //179 (0xb3): Return (void) from method
		index = ReadU2();
		if (!PutStatic(index))
		{
			return ExceptionEvent();
		}
		break;
		//////////////// Thread Synchronization ////////////////////

	case monitorenter:// Enter and acquire object monitor
		{
			Slot obj_slot;
			op_stack->top(0, obj_slot);
			if (!obj_slot.heapPtr)
			{
				VMContext::pending_exception = CreateJavaException(
					"java/lang/NullPointerException", "monitorenter on null object");
				return ExceptionEvent();
			}
			if (!Scheduler::Instance().MonitorEnter(this, obj_slot.heapPtr))
			{
				// Suspend; when granted the instruction is retried.  The
				// objectref stays on the stack for the retry.
				pc = last_pc;
				return EN_YIELD;
			}
			Slot acquired_slot;
			op_stack->pop(acquired_slot);
		}
		break;

	case monitorexit:// Release and exit object monitor
		{
			Slot obj_slot;
			op_stack->pop(obj_slot);
			if (obj_slot.heapPtr)
			{
				for (size_t i = monitors_held.size(); i > 0; i--)
				{
					if (monitors_held[i - 1].second == NULL &&
					    monitors_held[i - 1].first == obj_slot.heapPtr)
					{
						monitors_held.erase(monitors_held.begin() + (i - 1));
						break;
					}
				}
				Scheduler::Instance().MonitorExit(obj_slot.heapPtr);
			}
		}
		break;

	case checkcast:
		{
			u2 = ReadU2();
			Slot obj_slot;
			op_stack->pop(obj_slot);
			LPOBJECT_REF cast_ref = obj_slot.heapPtr;
			if (cast_ref)
			{
				lpClassConstant cc = (lpClassConstant)clazz->const_pool->GetConstant(u2);
				lpClass target = NULL;
				if (cc && cc->utf8)
				{
					class_loader->FindClass(cc->utf8->bytes, target);
				}
				lpObject cast_obj = NULL;
				if (target && VMContext::object_heap->GetObject(cast_ref, cast_obj) &&
				    !IsInstanceOfClass(cast_obj, target))
				{
					VMContext::pending_exception = CreateJavaException(
						"java/lang/ClassCastException", cc && cc->utf8 ? cc->utf8->bytes : NULL);
					return ExceptionEvent();
				}
			}
			op_stack->push(obj_slot);
		}
		break;

	case instanceof:
		{
			u2 = ReadU2();
			Slot obj_slot;
			op_stack->pop(obj_slot);
			lpClassConstant cc = (lpClassConstant)clazz->const_pool->GetConstant(u2);
			lpClass target = NULL;
			if (cc && cc->utf8)
			{
				class_loader->FindClass(cc->utf8->bytes, target);
			}
			lpObject inst_obj = NULL;
			bool yes = obj_slot.heapPtr &&
				VMContext::object_heap->GetObject(obj_slot.heapPtr, inst_obj) &&
				IsInstanceOfClass(inst_obj, target);
			op_stack->push(yes ? 1 : 0);
		}
		break;
	case multianewarray:
		u2 = ReadU2();
		if (!ExecuteNewMultiArray(u2, codes[pc++]))
		{
			return ExceptionEvent();
		}
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

bool StackFrame::HasPendingException() const
{
	return VMContext::pending_exception != NULL;
}

EventNotify StackFrame::ExceptionEvent()
{
	if (VMContext::pending_exception && HandleException())
	{
		return EN_NOOP;
	}
	return EN_EXCEPTION;
}

bool StackFrame::HandleException()
{
	if (!VMContext::pending_exception || !method || !method->code_attr)
	{
		return false;
	}
	lpCodeAttribute code = method->code_attr;
	if (!code->exception_table || code->exception_length == 0)
	{
		return false;
	}
	lpObject exception = NULL;
	lpClass exception_class = NULL;
	if (VMContext::object_heap->GetObject(VMContext::pending_exception, exception) && exception)
	{
		exception_class = exception->obj_class;
	}
	for (U2 i = 0; i < code->exception_length; i++)
	{
		const ExceptionTable &entry = code->exception_table[i];
		if (last_pc < static_cast<int>(entry.start_pc) ||
		    last_pc >= static_cast<int>(entry.end_pc))
		{
			continue;
		}
		bool matches = (entry.catch_type == 0);
		if (!matches && exception_class)
		{
			lpClassConstant cc = (lpClassConstant)clazz->const_pool->GetConstant(entry.catch_type);
			if (cc && cc->utf8 && cc->utf8->bytes)
			{
				lpClass catch_class = NULL;
				try
				{
					class_loader->FindClass(cc->utf8->bytes, catch_class);
				}
				catch (...)
				{
					catch_class = NULL;
				}
				matches = catch_class && ClassIsAssignable(catch_class, exception_class);
			}
		}
		if (matches)
		{
			op_stack->clear();
			Slot ex_slot;
			ex_slot.heapPtr = VMContext::pending_exception;
			op_stack->push(ex_slot);
			// The handler receives the object on the operand stack; the
			// pending-exception flag must be cleared or the exception would
			// still be reported as uncaught after the catch completes.
			VMContext::pending_exception = NULL;
			pc = static_cast<int>(entry.handler_pc);
			last_pc = pc;
			return true;
		}
	}
	return false;
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

	case CLASS_CONSTANT:
		{
			// Java 5+ class literals: ldc of a CONSTANT_Class produces the
			// java.lang.Class mirror of the referenced type.
			ClassConstant *cc = (ClassConstant*)ct;
			lpClass target = NULL;
			if (!class_loader->FindClass(cc->GetName(), target) || !target)
			{
				return false;
			}
			target->initialize();
			jobject mirror = JniClassMirror(reinterpret_cast<jclass>(target));
			if (!mirror)
			{
				return false;
			}
			slot.heapPtr = JniRef(mirror);
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
	if (!cc || !cc->utf8)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NoClassDefFoundError", "invalid class constant");
		return false;
	}
	lpClass target = NULL;
	if (!FindClassOrError(class_loader, cc->utf8->bytes, target))
	{
		return false;
	}
	LPOBJECT_REF obj_ref = NULL;
	if (!VMContext::object_heap->NewObject(target, obj_ref))
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/OutOfMemoryError", "cannot allocate object");
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

bool StackFrame::InvokeJavaSync(lpClass target, lpMethod method,
                                const std::vector<Slot> &vars, Slot &result,
                                bool &wide)
{
	wide = false;
	if (!target || !method || !method->code_attr)
	{
		return false;
	}
	Slot *argv = vars.empty() ? NULL : const_cast<Slot *>(&vars[0]);
	EventNotify ev = thread_stack->execute(target, method, argv,
		static_cast<int>(vars.size()));
	if (ev == EN_EXCEPTION || VMContext::pending_exception)
	{
		return false;
	}
	if (!thread_stack->has_return)
	{
		result = Slot();
		return true;
	}
	result = thread_stack->return_value;
	wide = thread_stack->return_wide;
	return true;
}

bool StackFrame::AppendStringRefUnits(LPOBJECT_REF ref, std::vector<uint16_t> &out)
{
	if (!ref)
	{
		static const char *kNul = "null";
		for (const char *p = kNul; *p; p++)
		{
			out.push_back(static_cast<uint16_t>(static_cast<unsigned char>(*p)));
		}
		return true;
	}
	lpObject str = NULL;
	if (!VMContext::object_heap->GetObject(ref, str) || !str)
	{
		return false;
	}
	const std::string key("value:[C");
	lpFieldValue value = str->GetFieldValueByName(key);
	if (!value || !value->field_value.heapPtr)
	{
		return true; // empty string
	}
	lpObject chars = NULL;
	if (!VMContext::object_heap->GetObject(value->field_value.heapPtr, chars) ||
	    !chars || !chars->array_var)
	{
		return false;
	}
	uint16_t *units = static_cast<uint16_t *>(chars->array_var);
	for (int i = 0; i < chars->array_length; i++)
	{
		out.push_back(units[i]);
	}
	return true;
}

static void AppendAscii(std::vector<uint16_t> &out, const char *text)
{
	if (!text)
	{
		return;
	}
	for (const char *p = text; *p; p++)
	{
		out.push_back(static_cast<uint16_t>(static_cast<unsigned char>(*p)));
	}
}

bool StackFrame::AppendValueUnits(const TypeDesc &desc, Slot *slots,
                                  std::vector<uint16_t> &out)
{
	DATA_TYPE type = desc.data_type;
	if (desc.check_array)
	{
		type = DT_ARRAY;
	}
	char tmp[64];
	switch (type)
	{
	case DT_BOOL:
		AppendAscii(out, slots[0].blValue ? "true" : "false");
		return true;
	case DT_BYTE:
		snprintf(tmp, sizeof(tmp), "%d", static_cast<int>(slots[0].bValue));
		AppendAscii(out, tmp);
		return true;
	case DT_SHORT:
		snprintf(tmp, sizeof(tmp), "%d", static_cast<int>(slots[0].sValue));
		AppendAscii(out, tmp);
		return true;
	case DT_CHAR:
		out.push_back(slots[0].cValue);
		return true;
	case DT_INT:
		snprintf(tmp, sizeof(tmp), "%d", slots[0].iValue);
		AppendAscii(out, tmp);
		return true;
	case DT_LONG:
		{
			WByteValue bv(slots[0].bytes, slots[1].bytes);
			snprintf(tmp, sizeof(tmp), "%lld", static_cast<long long>(bv.lValue));
			AppendAscii(out, tmp);
			return true;
		}
	case DT_FLOAT:
	case DT_DOUBLE:
		{
			double value;
			if (type == DT_FLOAT)
			{
				value = static_cast<double>(slots[0].fValue);
			}
			else
			{
				WByteValue bv(slots[0].bytes, slots[1].bytes);
				value = bv.dValue;
			}
			lpClass dbl = NULL;
			if (!class_loader->FindClass("java/lang/Double", dbl))
			{
				return false;
			}
			lpMethod m = NULL;
			if (!dbl->FindMethod("toString:(D)Ljava/lang/String;", m) || !m)
			{
				return false;
			}
			WByteValue bv;
			bv.dValue = value;
			std::vector<Slot> vars(2);
			vars[0].set(bv.bytes, 4);
			vars[1].set(bv.bytes + 4, 4);
			Slot res;
			bool wide = false;
			if (!InvokeJavaSync(dbl, m, vars, res, wide))
			{
				return false;
			}
			return AppendStringRefUnits(res.heapPtr, out);
		}
	case DT_REF:
	case DT_ARRAY:
		{
			if (!slots[0].heapPtr)
			{
				AppendAscii(out, "null");
				return true;
			}
			lpObject obj = NULL;
			if (!VMContext::object_heap->GetObject(slots[0].heapPtr, obj) || !obj)
			{
				return false;
			}
			if (obj->obj_class && obj->obj_class->thisClass &&
			    strcmp(obj->obj_class->thisClass->GetName(), "java/lang/String") == 0)
			{
				return AppendStringRefUnits(slots[0].heapPtr, out);
			}
			lpClass str_class = NULL;
			if (!class_loader->FindClass("java/lang/String", str_class))
			{
				return false;
			}
			lpMethod m = NULL;
			if (!str_class->FindMethod("valueOf:(Ljava/lang/Object;)Ljava/lang/String;", m) || !m)
			{
				return false;
			}
			std::vector<Slot> vars(1);
			vars[0] = slots[0];
			Slot res;
			bool wide = false;
			if (!InvokeJavaSync(str_class, m, vars, res, wide))
			{
				return false;
			}
			return AppendStringRefUnits(res.heapPtr, out);
		}
	default:
		return false;
	}
}

bool StackFrame::ExecuteInvokeDynamic(U2 index)
{
	InvokeDynamicConstant *indy =
		(InvokeDynamicConstant *)clazz->const_pool->GetConstant(index);
	if (!indy || !indy->nameType || !indy->nameType->descUtf8)
	{
		return false;
	}
	const char *desc_bytes = indy->nameType->descUtf8->bytes;
	std::string desc(desc_bytes ? desc_bytes : "",
	                 indy->nameType->descUtf8->length);
	std::vector<TypeDesc> arg_types;
	TypeDesc result_type;
	if (!parse_method_desc_type(desc.c_str(), arg_types, result_type))
	{
		return false;
	}

	lpBootstrapMethods bootstrap = clazz->bootstrap_methods;
	if (!bootstrap || indy->bootstrap_method_attr_index >= bootstrap->methods.size())
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/UnsupportedOperationException",
			"invokedynamic call site has no BootstrapMethods entry");
		return false;
	}
	BootstrapMethodInfo &bm = bootstrap->methods[indy->bootstrap_method_attr_index];
	MethodHandleConstant *mh =
		(MethodHandleConstant *)clazz->const_pool->GetConstant(bm.bootstrap_method_ref);
	if (!mh || !mh->reference)
	{
		return false;
	}
	MethodRefConstant *boot = (MethodRefConstant *)mh->reference;
	const char *boot_class = boot->clazz && boot->clazz->utf8 ? boot->clazz->utf8->bytes : "";
	const char *boot_name = boot->nameType && boot->nameType->nameUtf8
		? boot->nameType->nameUtf8->bytes : "";

	if (strcmp(boot_class, "java/lang/invoke/LambdaMetafactory") == 0)
	{
		DebugUtils::trace("lambda bootstrap %s.%s\n", boot_class, boot_name);
		if (bm.bootstrap_arguments.size() < 3)
		{
			return false;
		}
		Constant *impl_constant =
			clazz->const_pool->GetConstant(bm.bootstrap_arguments[1]);
		MethodHandleConstant *impl_mh = (MethodHandleConstant *)impl_constant;
		if (!impl_mh || !impl_mh->reference)
		{
			return false;
		}
		MethodRefConstant *impl_ref = (MethodRefConstant *)impl_mh->reference;
		if (!impl_ref->clazz || !impl_ref->clazz->utf8 || !impl_ref->nameType)
		{
			return false;
		}
		lpClass impl_class = NULL;
		if (!class_loader->FindClass(impl_ref->clazz->utf8->bytes, impl_class) || !impl_class)
		{
			return false;
		}
		lpMethod impl_method = NULL;
		if (!impl_class->FindMethod(impl_ref->simple_name, impl_method) || !impl_method)
		{
			return false;
		}
		// The functional interface is the return type of the call site.
		std::string iface = result_type.class_name;
		if (iface.empty())
		{
			return false;
		}
		lpClass synth = FindSyntheticLambdaClass(iface, class_loader);

		int cap_slots = 0;
		for (size_t i = 0; i < arg_types.size(); i++)
		{
			cap_slots += (arg_types[i].data_type == DT_LONG ||
			              arg_types[i].data_type == DT_DOUBLE) ? 2 : 1;
		}
		int cap_base = op_stack->size() - cap_slots;
		if (cap_base < 0)
		{
			return false;
		}
		LPOBJECT_REF lambda_ref = NULL;
		if (!VMContext::object_heap->NewObject(synth, lambda_ref))
		{
			return false;
		}
		lpObject lambda_obj = NULL;
		if (!VMContext::object_heap->GetObject(lambda_ref, lambda_obj) || !lambda_obj)
		{
			return false;
		}
		LambdaInfo info;
		info.kind = impl_mh->reference_kind;
		info.target_class = impl_class;
		info.target_method = impl_method;
		info.sam_name = indy->nameType->nameUtf8 && indy->nameType->nameUtf8->bytes
			? indy->nameType->nameUtf8->bytes : "";
		int slot_index = cap_base;
		int cap_index = 0;
		for (size_t i = 0; i < arg_types.size(); i++)
		{
			LambdaCapture cap;
			cap.type = arg_types[i].data_type;
			if (arg_types[i].check_array)
			{
				cap.type = DT_ARRAY;
			}
			cap.wide = (cap.type == DT_LONG || cap.type == DT_DOUBLE);
			if (!op_stack->get(slot_index, cap.low))
			{
				return false;
			}
			char name[32];
			snprintf(name, sizeof(name), "$cap%d", cap_index);
			StoreSlotField(lambda_obj, name, cap.low);
			if (cap.wide)
			{
				if (!op_stack->get(slot_index + 1, cap.high))
				{
					return false;
				}
				snprintf(name, sizeof(name), "$caph%d", cap_index);
				StoreSlotField(lambda_obj, name, cap.high);
				slot_index += 2;
			}
			else
			{
				slot_index += 1;
			}
			info.captures.push_back(cap);
			cap_index++;
		}
		int lambda_id = g_next_lambda_id++;
		Slot id_slot;
		id_slot.iValue = lambda_id;
		StoreSlotField(lambda_obj, "$lambda:id", id_slot);
		g_lambdas[lambda_id] = info;
		for (int i = 0; i < cap_slots; i++)
		{
			Slot ignored;
			op_stack->pop(ignored);
		}
		op_stack->pushObject(lambda_ref);
		return true;
	}
	if (strcmp(boot_class, "java/lang/invoke/StringConcatFactory") != 0)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/UnsupportedOperationException",
			(std::string("invokedynamic bootstrap not supported: ") +
			 boot_class + "." + boot_name).c_str());
		return false;
	}

	// String concat: the recipe is bootstrap argument 0 for
	// makeConcatWithConstants; makeConcat has no recipe and concatenates every
	// dynamic argument in order.
	std::vector<uint16_t> recipe;
	if (strcmp(boot_name, "makeConcatWithConstants") == 0)
	{
		if (bm.bootstrap_arguments.empty())
		{
			return false;
		}
		Constant *c = clazz->const_pool->GetConstant(bm.bootstrap_arguments[0]);
		if (!c || c->tag != STRING_CONSTANT)
		{
			return false;
		}
		StringConstant *sc = (StringConstant *)c;
		if (sc->utf8 && sc->utf8->bytes)
		{
			recipe = dk::Utf8ToUtf16(sc->utf8->bytes, sc->utf8->length);
		}
	}
	else if (strcmp(boot_name, "makeConcat") != 0)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/UnsupportedOperationException",
			(std::string("StringConcatFactory method not supported: ") + boot_name).c_str());
		return false;
	}

	int total_slots = 0;
	for (size_t i = 0; i < arg_types.size(); i++)
	{
		DATA_TYPE t = arg_types[i].data_type;
		total_slots += (t == DT_LONG || t == DT_DOUBLE) ? 2 : 1;
	}
	int base = op_stack->size() - total_slots;
	if (base < 0)
	{
		return false;
	}

	std::vector<uint16_t> out;
	if (recipe.empty())
	{
		// makeConcat: concatenate every dynamic argument.
		int slot_index = base;
		for (size_t i = 0; i < arg_types.size(); i++)
		{
			Slot pair[2];
			if (!op_stack->get(slot_index, pair[0]))
			{
				return false;
			}
			if (arg_types[i].data_type == DT_LONG || arg_types[i].data_type == DT_DOUBLE)
			{
				if (!op_stack->get(slot_index + 1, pair[1]))
				{
					return false;
				}
				slot_index += 2;
			}
			else
			{
				slot_index += 1;
			}
			if (!AppendValueUnits(arg_types[i], pair, out))
			{
				return false;
			}
		}
	}
	else
	{
		int slot_index = base;
		size_t arg_index = 0;
		size_t const_index = 1; // bootstrap argument 0 is the recipe itself
		for (size_t i = 0; i < recipe.size(); i++)
		{
			uint16_t unit = recipe[i];
			if (unit == 0x0001)
			{
				if (arg_index >= arg_types.size())
				{
					return false;
				}
				Slot pair[2];
				if (!op_stack->get(slot_index, pair[0]))
				{
					return false;
				}
				if (arg_types[arg_index].data_type == DT_LONG ||
				    arg_types[arg_index].data_type == DT_DOUBLE)
				{
					if (!op_stack->get(slot_index + 1, pair[1]))
					{
						return false;
					}
					slot_index += 2;
				}
				else
				{
					slot_index += 1;
				}
				if (!AppendValueUnits(arg_types[arg_index], pair, out))
				{
					return false;
				}
				arg_index++;
			}
			else if (unit == 0x0002)
			{
				if (const_index < bm.bootstrap_arguments.size())
				{
					Constant *c = clazz->const_pool->GetConstant(
						bm.bootstrap_arguments[const_index]);
					if (c)
					{
						if (c->tag == STRING_CONSTANT)
						{
							StringConstant *sc = (StringConstant *)c;
							if (sc->utf8 && sc->utf8->bytes)
							{
								std::vector<uint16_t> units = dk::Utf8ToUtf16(
									sc->utf8->bytes, sc->utf8->length);
								out.insert(out.end(), units.begin(), units.end());
							}
						}
						else
						{
							AppendAscii(out, c->name);
						}
					}
					const_index++;
				}
			}
			else
			{
				out.push_back(unit);
			}
		}
	}

	for (int i = 0; i < total_slots; i++)
	{
		Slot ignored;
		op_stack->pop(ignored);
	}

	// NewStringObject takes ownership of the buffer.
	size_t count = out.size();
	unsigned short *buf = new unsigned short[count ? count : 1];
	for (size_t i = 0; i < count; i++)
	{
		buf[i] = out[i];
	}
	LPOBJECT_REF str_ref = NULL;
	if (!VMContext::object_heap->NewStringObject(buf, static_cast<int>(count), str_ref))
	{
		return false;
	}
	op_stack->pushObject(str_ref);
	return true;
}


bool StackFrame::CloneArrayReceiver()
{
	Slot slot_obj;
	op_stack->pop(slot_obj);
	LPOBJECT_REF source_ref = slot_obj.heapPtr;
	lpObject source = NULL;
	if (!source_ref || !VMContext::object_heap->GetObject(source_ref, source) || !source)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NullPointerException", "null array reference");
		return false;
	}
	int element_size = 1;
	switch (source->array_data_type)
	{
	case DT_BOOL:    element_size = static_cast<int>(sizeof(jboolean)); break;
	case DT_BYTE:    element_size = static_cast<int>(sizeof(jbyte)); break;
	case DT_CHAR:    element_size = static_cast<int>(sizeof(jchar)); break;
	case DT_SHORT:   element_size = static_cast<int>(sizeof(jshort)); break;
	case DT_INT:     element_size = static_cast<int>(sizeof(jint)); break;
	case DT_LONG:    element_size = static_cast<int>(sizeof(jlong)); break;
	case DT_FLOAT:   element_size = static_cast<int>(sizeof(jfloat)); break;
	case DT_DOUBLE:  element_size = static_cast<int>(sizeof(jdouble)); break;
	case DT_ARRAY:
	case DT_REF:     element_size = static_cast<int>(sizeof(jobject)); break;
	default:         break;
	}
	LPOBJECT_REF copy_ref = NULL;
	if (!VMContext::object_heap->NewArrayObject(source->array_length,
		source->obj_class, copy_ref))
	{
		return false;
	}
	lpObject copy = NULL;
	if (!VMContext::object_heap->GetObject(copy_ref, copy) || !copy)
	{
		return false;
	}
	if (source->array_var && copy->array_var && source->array_length > 0)
	{
		memcpy(copy->array_var, source->array_var,
			static_cast<size_t>(source->array_length) * element_size);
	}
	op_stack->pushObject(copy_ref);
	return true;
}

bool StackFrame::ExecuteVirtual(int index, int type)
{
	MethodRefConstant *method_ref = (MethodRefConstant*)this->clazz->const_pool->GetConstant(index);
	if (!method_ref)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NoSuchMethodError", "invalid method constant index");
		return false;
	}
	// Array classes have no parsed method table, so the generated
	// `$VALUES.clone()` in enum values() (and manual array copying) is
	// handled here as a shallow copy.
	if (method_ref->clazz && method_ref->clazz->GetName() &&
	    method_ref->clazz->GetName()[0] == '[' && method_ref->simple_name &&
	    strcmp(method_ref->simple_name, "clone:()Ljava/lang/Object;") == 0)
	{
		return CloneArrayReceiver();
	}
	// Resolution is cached on the constant-pool entry; invokestatic and
	// invokespecial resolve to a fixed method, and the first call of a
	// virtual site is a useful fallback before dispatch.
	lpMethod method = method_ref->resolved_method.load(std::memory_order_acquire);
	lpClass clazz = (method && method->clazz) ? method->clazz : NULL;
	if (!method)
	{
		if (!FindClassOrError(class_loader, method_ref->clazz->GetName(), clazz))
		{
			return false;
		}
		if (!clazz->FindMethod(method_ref->simple_name, method))
		{
			std::string message = method_ref->clazz->GetName();
			if (method_ref->nameType && method_ref->nameType->nameUtf8)
			{
				message += ".";
				message += method_ref->nameType->nameUtf8->bytes;
			}
			if (method_ref->nameType && method_ref->nameType->descUtf8)
			{
				message += method_ref->nameType->descUtf8->bytes;
			}
			VMContext::pending_exception = CreateJavaException(
				"java/lang/NoSuchMethodError", message.c_str());
			return false;
		}
		// The resolved method may be declared by a superclass or interface.
		// The frame must execute with the declaring class.
		if (method->clazz)
		{
			clazz = method->clazz;
		}
		method_ref->resolved_method.store(method, std::memory_order_release);
	}

	vector<TypeDesc> type_desc;
	TypeDesc result_desc;
	if (!parse_method_desc_type(method->methodDesc->GetName(), type_desc, result_desc))
	{
		return false;
	}
	// Annotation instances materialised from RuntimeVisibleAnnotations store
	// their element values under "ann:<name>".  Element methods are abstract
	// (no bytecode), so answer the call from the stored value.
	if (type != STATIC_METHOD && !method->isSpecial() && method->methodName)
	{
		int ann_arg_slots = 0;
		for (size_t ti = 0; ti < type_desc.size(); ti++)
		{
			DATA_TYPE at = type_desc[ti].data_type;
			ann_arg_slots += (at == DT_LONG || at == DT_DOUBLE) ? 2 : 1;
		}
		Slot ann_recv;
		op_stack->top(ann_arg_slots, ann_recv);
		lpObject ann_obj = NULL;
		if (ann_recv.heapPtr &&
		    VMContext::object_heap->GetObject(ann_recv.heapPtr, ann_obj) && ann_obj)
		{
			std::string ann_key = std::string("ann:") + method->methodName->GetName();
			lpFieldValue ann_value_field = ann_obj->GetFieldValueByName(ann_key);
			if (ann_value_field)
			{
				for (int k = 0; k < ann_arg_slots; k++)
				{
					Slot dropped;
					op_stack->pop(dropped);
				}
				op_stack->pop(ann_recv);
				Variant ann_value = ann_value_field->field_value;
				switch (result_desc.data_type)
				{
				case DT_VOID:
					break;
				case DT_LONG:
					op_stack->push(ann_value.lValue);
					break;
				case DT_DOUBLE:
					op_stack->push(ann_value.dValue);
					break;
				default:
					op_stack->push(ann_value);
					break;
				}
				return true;
			}
		}
	}
	//动态分派 多态处理-------------------------------------
	// ---- coroutine-blocking primitives --------------------------------
	// Thread.sleep/yield/join and Object.wait/notify are native API entry
	// points.  They are handled here (before libffi dispatch) so that the
	// coroutine is suspended instead of occupying an OS thread.
	{
		const char *decl = (clazz && clazz->thisClass && clazz->thisClass->GetName())
			? clazz->thisClass->GetName() : "";
		const char *mn = method->methodName ? method->methodName->GetName() : "";
		if (strcmp(decl, "java/lang/Thread") == 0 && method->isStatic() &&
		    strcmp(mn, "sleep0") == 0)
		{
			LONG64 millis = popLong();
			bool suspended = Scheduler::Instance().SleepCurrent(thread_stack, millis);
			if (VMContext::pending_exception)
			{
				return ExceptionEvent();
			}
			if (suspended)
			{
				thread_stack->yield_pending = true;
			}
			return true;
		}
		if (strcmp(decl, "java/lang/Thread") == 0 && method->isStatic() &&
		    strcmp(mn, "yield0") == 0)
		{
			if (Scheduler::Instance().YieldCurrent(thread_stack))
			{
				thread_stack->yield_pending = true;
			}
			return true;
		}
		if (strcmp(decl, "java/lang/Thread") == 0 && !method->isStatic() &&
		    strcmp(mn, "join0") == 0)
		{
			LONG64 millis = popLong();
			Slot recv;
			op_stack->pop(recv);
			bool suspended = Scheduler::Instance().JoinCurrent(thread_stack, recv.heapPtr, millis);
			if (VMContext::pending_exception)
			{
				return ExceptionEvent();
			}
			if (suspended)
			{
				thread_stack->yield_pending = true;
			}
			return true;
		}
		if (strcmp(decl, "java/lang/Object") == 0 && !method->isStatic() &&
		    strcmp(mn, "wait") == 0)
		{
			// wait(), wait(long) and wait(long,int) differ in the number of
			// argument slots.  The old code popped a single long for every
			// non-empty descriptor, so wait(long,int) popped the receiver as
			// the long and then read a stale slot as the receiver, raising
			// IllegalMonitorStateException.  nanos is accepted and ignored
			// (millisecond granularity).
			int64_t millis = 0;
			if (type_desc.size() == 2)
			{
				Slot nanos;
				op_stack->pop(nanos);
				millis = popLong();
				// wait(0, n>0) is a timed wait, not an indefinite one.
				if (millis == 0 && nanos.iValue > 0)
				{
					millis = 1;
				}
			}
			else if (type_desc.size() == 1)
			{
				millis = popLong();
			}
			Slot recv;
			op_stack->pop(recv);
			bool suspended = Scheduler::Instance().WaitCurrent(thread_stack, recv.heapPtr, millis);
			if (VMContext::pending_exception)
			{
				return ExceptionEvent();
			}
			if (suspended)
			{
				thread_stack->yield_pending = true;
			}
			return true;
		}
		if (strcmp(decl, "java/lang/Object") == 0 && !method->isStatic() &&
		    (strcmp(mn, "notify") == 0 || strcmp(mn, "notifyAll") == 0))
		{
			Slot recv;
			op_stack->pop(recv);
			Scheduler::Instance().NotifyCurrent(thread_stack, recv.heapPtr,
				strcmp(mn, "notifyAll") == 0);
			if (VMContext::pending_exception)
			{
				return ExceptionEvent();
			}
			return true;
		}
	}

	if (type != SPECIAL_METHOD
		&& !method->isSpecial()
		&& !method->isStatic()
		&& !method->isFinal())
	{
		Slot slot_this;
		// long/double arguments occupy two stack slots; the receiver sits
		// below all of them.
		int arg_slots = 0;
		for (size_t ti = 0; ti < type_desc.size(); ti++)
		{
			DATA_TYPE at = type_desc[ti].data_type;
			arg_slots += (at == DT_LONG || at == DT_DOUBLE) ? 2 : 1;
		}
		op_stack->top(arg_slots, slot_this);
		LPOBJECT_REF obj_ref = slot_this.heapPtr;
		lpObject obj = NULL;
		if (!obj_ref || !VMContext::object_heap->GetObject(obj_ref, obj))
		{
			VMContext::pending_exception = CreateJavaException(
				"java/lang/NullPointerException", "null object reference");
			return false;
		}
		int lambda_id = 0;
		lpFieldValue lambda_field = obj->GetFieldValueByName("$lambda:id");
		if (lambda_field)
		{
			lambda_id = lambda_field->field_value.iValue;
		}
		if (lambda_id > 0)
		{
			std::map<int, LambdaInfo>::iterator li = g_lambdas.find(lambda_id);
			if (li != g_lambdas.end() && method->methodName &&
			    method->methodName->GetName())
			{
				const char *call_name = method->methodName->GetName();
				const std::string &sam = li->second.sam_name;
				if (sam.size() > 0 &&
				    strncmp(call_name, sam.c_str(), sam.size()) == 0 &&
				    call_name[sam.size()] == '\0')
				{
					return InvokeLambda(lambda_id, method, type_desc);
				}
			}
		}
		lpMethod tmp_method = NULL;
		lpClass receiver_class = obj->obj_class;
		if (method_ref->cached_receiver_class.load(std::memory_order_acquire) == receiver_class)
		{
			tmp_method = method_ref->cached_method.load(std::memory_order_acquire);
		}
		if (!tmp_method)
		{
			if (receiver_class->FindMethod(method_ref->simple_name, tmp_method))
			{
				// Publish the method before the receiver class that validates
				// the cache entry.
				method_ref->cached_method.store(tmp_method, std::memory_order_release);
				method_ref->cached_receiver_class.store(receiver_class,
					std::memory_order_release);
			}
		}
		if (tmp_method)
		{
			method = tmp_method;
			if (method->clazz)
			{
				clazz = method->clazz;
			}
		}
	}
	if ((method->accFlag & ACM_ABSTRACT) != 0)
	{
		std::string message = (method->clazz && method->clazz->thisClass)
			? method->clazz->thisClass->GetName() : "";
		if (method->fullName)
		{
			message += ".";
			message += method->fullName;
		}
		VMContext::pending_exception = CreateJavaException(
			"java/lang/AbstractMethodError", message.c_str());
		return false;
	}
	if (!method->isNative() && !method->code_attr)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/AbstractMethodError", "method has no code");
		return false;
	}
	// Bounded inlining: execute trivial getters/setters, constant returns and
	// empty bodies at the call site.  A rejection leaves the normal call path
	// untouched; a failure inside the inlined body (NPE, missing field) sets a
	// pending exception that must be propagated.
	if (type != STATIC_METHOD && !VMContext::pending_exception &&
	    TryInline(clazz, method, type_desc, result_desc))
	{
		return true;
	}
	if (VMContext::pending_exception)
	{
		return false;
	}
	//----------------------------------------------------------------------------------------------
	// Synchronized methods acquire the receiver (or the declaring class for
	// static methods) before the call; the monitor is released when the callee
	// frame is popped, or right after a native call returns.
	if ((method->accFlag & ACM_SYNCHRONIZED) != 0)
	{
		bool acquired = false;
		if (method->isStatic())
		{
			acquired = Scheduler::Instance().MonitorEnterClass(this, clazz);
			if (acquired)
			{
				pending_sync_obj = NULL;
				pending_sync_class = clazz;
				pending_sync_held = true;
			}
		}
		else
		{
			int arg_slots = 0;
			for (size_t ti = 0; ti < type_desc.size(); ti++)
			{
				DATA_TYPE at = type_desc[ti].data_type;
				arg_slots += (at == DT_LONG || at == DT_DOUBLE) ? 2 : 1;
			}
			Slot recv;
			op_stack->top(arg_slots, recv);
			if (!recv.heapPtr)
			{
				VMContext::pending_exception = CreateJavaException(
					"java/lang/NullPointerException", "null object reference");
				return false;
			}
			acquired = Scheduler::Instance().MonitorEnter(this, recv.heapPtr);
			if (acquired)
			{
				pending_sync_obj = recv.heapPtr;
				pending_sync_class = NULL;
				pending_sync_held = true;
			}
		}
		if (!acquired)
		{
			pc = last_pc;
			thread_stack->yield_pending = true;
			return true;
		}
	}
	if (!method->isNative())
	{
		return ExecuteNormalMethod(clazz, method, type_desc);
	}
	else
	{
		return ExecuteNativeMethod(clazz, method, type_desc, result_desc);
	}
}

// Executes a trivial method body directly at the call site.  The bounded set
// (empty body, primitive/null constant, field getter, field setter) is
// recognised once per Method by AnalyzeInline and cached in the method's
// atomics, so the hot path is a single acquire load.
bool StackFrame::TryInline(lpClass declaring, lpMethod method,
                           vector<TypeDesc> &type_desc, TypeDesc &result_desc)
{
	(void)declaring;
	if (!method || method->isStatic() || method->isSpecial())
	{
		return false;
	}
	int kind = method->inline_kind.load(std::memory_order_acquire);
	if (kind == 0)
	{
		int cp = 0;
		int analysed = AnalyzeInline(method, cp);
		// Publish the index before the kind so a concurrent reader that sees
		// the kind also sees the matching constant index.
		method->inline_cp.store(cp, std::memory_order_relaxed);
		method->inline_kind.store(analysed, std::memory_order_release);
		kind = analysed;
	}
	if (kind == IK_NONE || kind == 0)
	{
		return false;
	}
	if (method->isNative() || !method->code_attr ||
	    (method->accFlag & (ACM_ABSTRACT | ACM_SYNCHRONIZED)) != 0)
	{
		return false;
	}

	int arg_slots = 0;
	for (size_t ti = 0; ti < type_desc.size(); ti++)
	{
		DATA_TYPE at = type_desc[ti].data_type;
		arg_slots += (at == DT_LONG || at == DT_DOUBLE) ? 2 : 1;
	}

	lpField field = NULL;
	TypeDesc field_type;
	if (kind == IK_GETTER || kind == IK_SETTER)
	{
		int cp_index = method->inline_cp.load(std::memory_order_relaxed);
		ConstantPool *pool = method->clazz ? method->clazz->const_pool : NULL;
		FieldRefConstant *frc = pool
			? (FieldRefConstant*)pool->GetConstant(cp_index) : NULL;
		if (!frc)
		{
			return false;
		}
		// Resolve while the receiver is still on the operand stack: class
		// loading can collect, and the stack is a GC root.
		lpClassLoader loader = method->clazz ? method->clazz->classloader : NULL;
		if (!ResolveInstanceField(loader, frc, field))
		{
			return false;
		}
		LPSTR fdesc = (frc->nameType && frc->nameType->descUtf8)
			? frc->nameType->descUtf8->GetName() : NULL;
		if (!fdesc ||
		    !parse_desc_type(fdesc, field_type, static_cast<int>(strlen(fdesc))))
		{
			return false;
		}
		if (field_type.check_array)
		{
			field_type.data_type = DT_ARRAY;
		}
		bool ref_kind = (field_type.data_type == DT_REF ||
		                 field_type.data_type == DT_ARRAY);
		if (kind == IK_GETTER)
		{
			bool ret_ref = (result_desc.data_type == DT_REF ||
			                result_desc.data_type == DT_ARRAY);
			if ((ref_kind != ret_ref) ||
			    (!ref_kind && field_type.data_type != result_desc.data_type))
			{
				return false;
			}
		}
		else
		{
			if (type_desc.size() != 1)
			{
				return false;
			}
			bool arg_ref = (type_desc[0].data_type == DT_REF ||
			                type_desc[0].data_type == DT_ARRAY);
			if ((ref_kind != arg_ref) ||
			    (!ref_kind && field_type.data_type != type_desc[0].data_type))
			{
				return false;
			}
		}
	}
	else if (!type_desc.empty())
	{
		// Constants and empty bodies only have a receiver on the stack.
		return false;
	}
	if (kind == IK_EMPTY && result_desc.data_type != DT_VOID)
	{
		return false;
	}

	Slot recv;
	op_stack->top(arg_slots, recv);
	LPOBJECT_REF obj_ref = recv.heapPtr;
	if (!obj_ref)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NullPointerException", "null object reference");
		return false;
	}

	Slot dropped;
	if (kind == IK_SETTER)
	{
		Slot value;
		switch (field_type.data_type)
		{
		case DT_LONG:
			value.lValue = popLong();
			break;
		case DT_DOUBLE:
			value.dValue = popDouble();
			break;
		default:
			op_stack->pop(value);
			break;
		}
		op_stack->pop(dropped); // receiver
		lpObject obj = NULL;
		if (!VMContext::object_heap->GetObject(obj_ref, obj) || !obj)
		{
			VMContext::pending_exception = CreateJavaException(
				"java/lang/NullPointerException", "invalid object reference");
			return false;
		}
		lpFieldValue fv = obj->GetFieldValue(field);
		if (!fv)
		{
			VMContext::pending_exception = CreateJavaException(
				"java/lang/NoSuchFieldError", "field storage is missing");
			return false;
		}
		fv->field_value = value;
		return true;
	}

	op_stack->pop(dropped); // receiver
	if (kind == IK_EMPTY)
	{
		return true;
	}
	if (kind == IK_CONST_INT)
	{
		const U1 *codes = method->code_attr->codes;
		op_stack->push(static_cast<int>(codes[0]) - static_cast<int>(iconst_0));
		return true;
	}
	if (kind == IK_CONST_LONG)
	{
		const U1 *codes = method->code_attr->codes;
		op_stack->push(static_cast<LONG64>(codes[0] == lconst_1 ? 1 : 0));
		return true;
	}
	if (kind == IK_CONST_FLOAT)
	{
		const U1 *codes = method->code_attr->codes;
		op_stack->push(static_cast<float>(codes[0] - fconst_0));
		return true;
	}
	if (kind == IK_CONST_DOUBLE)
	{
		const U1 *codes = method->code_attr->codes;
		op_stack->push(static_cast<double>(codes[0] == dconst_1 ? 1.0 : 0.0));
		return true;
	}
	if (kind == IK_CONST_NULL)
	{
		Slot null_slot;
		null_slot.heapPtr = NULL;
		op_stack->push(null_slot);
		return true;
	}
	if (kind == IK_GETTER)
	{
		lpObject obj = NULL;
		if (!VMContext::object_heap->GetObject(obj_ref, obj) || !obj)
		{
			VMContext::pending_exception = CreateJavaException(
				"java/lang/NullPointerException", "invalid object reference");
			return false;
		}
		lpFieldValue fv = obj->GetFieldValue(field);
		if (!fv)
		{
			VMContext::pending_exception = CreateJavaException(
				"java/lang/NoSuchFieldError", "field storage is missing");
			return false;
		}
		switch (field_type.data_type)
		{
		case DT_LONG:
			op_stack->push(fv->field_value.lValue);
			break;
		case DT_DOUBLE:
			op_stack->push(fv->field_value.dValue);
			break;
		default:
			op_stack->push(fv->field_value);
			break;
		}
		return true;
	}
	return false;
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

	if (pending_sync_held)
	{
		frame->monitors_held.push_back(std::make_pair(pending_sync_obj, pending_sync_class));
		pending_sync_held = false;
		pending_sync_obj = NULL;
		pending_sync_class = NULL;
	}

	return StackToLocal(frame, type_desc, !method->isStatic());
}

bool StackFrame::ExecuteNativeMethod(lpClass clazz, lpMethod method, vector<TypeDesc> &type_desc, TypeDesc &result_desc)
{
	int count = static_cast<int>(type_desc.size());
	const char *jni_name = method->GetJniName();
	if (!jni_name)
	{
		throw new VMExecutorException("Native method has no JNI name.");
	}

	// Registered natives (RegisterNatives) take precedence over dlsym. The
	// short JNI name is tried first, then the "long" name that encodes the
	// method descriptor so overloaded natives resolve correctly.
	NATIVE_FUNC native_func = NULL;
	void *registered = DarksVM_GetRegisteredNative(method);
	if (registered)
	{
		native_func = reinterpret_cast<NATIVE_FUNC>(registered);
	}
	if (!native_func)
	{
		// Long JNI name: Java_<class>_<method>__<mangled argument types>.
		// The parentheses and the return type are not part of the suffix
		// (javah/javac convention), e.g. (I)I -> "..._m__I" and
		// (Ljava/lang/String;)V -> "..._m__Ljava_lang_String_2".
		std::string long_name = std::string(jni_name) + "__";
		const char *desc = method->methodDesc ? method->methodDesc->GetName() : "";
		const char *args_begin = desc ? strchr(desc, '(') : NULL;
		const char *args_end = args_begin ? strchr(args_begin, ')') : NULL;
		if (args_begin && args_end)
		{
			for (const char *p = args_begin + 1; p < args_end; p++)
			{
				char c = *p;
				if (c == '/')
				{
					long_name += '_';
				}
				else if (c == '_')
				{
					long_name += "_1";
				}
				else if (c == ';')
				{
					long_name += "_2";
				}
				else if (c == '[')
				{
					long_name += "_3";
				}
				else
				{
					long_name += c;
				}
			}
		}
		if (!VMContext::native_libs->GetNativeFunction(long_name.c_str(), native_func) ||
		    !native_func)
		{
			if (!VMContext::native_libs->GetNativeFunction(jni_name, native_func) ||
			    !native_func)
			{
				std::string message = "Cannot find native method in library: ";
				message += jni_name;
				VMContext::pending_exception = CreateJavaException(
					"java/lang/UnsatisfiedLinkError", message.c_str());
				return false;
			}
		}
	}

	// libffi needs one storage cell per C argument. The JNIEnv pointer and the
	// receiver (jobject or jclass) come first, followed by the Java arguments.
	struct NativeValue
	{
		union
		{
			ffi_arg a;
			int32_t i;
			int64_t l;
			float f;
			double d;
			void *p;
		};
		NativeValue() { a = 0; }
	};

	vector<ffi_type*> arg_types;
	vector<void*> arg_values;
	vector<NativeValue> values;
	arg_types.reserve(static_cast<size_t>(count) + 2);
	arg_values.reserve(static_cast<size_t>(count) + 2);
	// Reserve up front so that pointers stored in arg_values stay valid.
	values.resize(static_cast<size_t>(count) + 2);

	arg_types.push_back(&ffi_type_pointer);          // JNIEnv*
	arg_values.push_back(&values[0].p);
	values[0].p = jni_env;

	arg_types.push_back(&ffi_type_pointer);      // receiver (jclass/jobject)
	arg_values.push_back(&values[1].p);
	if (method->isStatic())
	{
		values[1].p = (void*)clazz;
	}

	// Pop the Java arguments in reverse order, filling the value cells from
	// the end towards the front.
	size_t base = arg_values.size();
	arg_values.resize(base + count);
	arg_types.resize(base + count);
	for (int i = count - 1; i >= 0; i--)
	{
		TypeDesc desc = type_desc[i];
		DATA_TYPE data_type = desc.data_type;
		if (desc.check_array)
		{
			data_type = DT_ARRAY;
		}
		NativeValue &value = values[base + i];
		ffi_type *type = &ffi_type_sint32;
		switch (data_type)
		{
		case DT_BOOL:
			{
				Slot slot;
				op_stack->pop(slot);
				value.i = slot.blValue ? 1 : 0;
				type = &ffi_type_sint32;
				break;
			}
		case DT_BYTE:
			{
				Slot slot;
				op_stack->pop(slot);
				value.i = static_cast<int32_t>(slot.bValue);
				type = &ffi_type_sint32;
				break;
			}
		case DT_CHAR:
			{
				Slot slot;
				op_stack->pop(slot);
				value.i = static_cast<int32_t>(slot.cValue);
				type = &ffi_type_sint32;
				break;
			}
		case DT_SHORT:
			{
				Slot slot;
				op_stack->pop(slot);
				value.i = static_cast<int32_t>(slot.sValue);
				type = &ffi_type_sint32;
				break;
			}
		case DT_INT:
			{
				Slot slot;
				op_stack->pop(slot);
				value.i = slot.iValue;
				type = &ffi_type_sint32;
				break;
			}
		case DT_FLOAT:
			{
				Slot slot;
				op_stack->pop(slot);
				value.f = slot.fValue;
				type = &ffi_type_float;
				break;
			}
		case DT_LONG:
			{
				value.l = popLong();
				type = &ffi_type_sint64;
				break;
			}
		case DT_DOUBLE:
			{
				value.d = popDouble();
				type = &ffi_type_double;
				break;
			}
		case DT_ARRAY:
		case DT_REF:
			{
				Slot slot;
				op_stack->pop(slot);
				value.p = (void*)slot.heapPtr;
				type = &ffi_type_pointer;
				break;
			}
		default:
			throw new VMExecutorException("Unsupported native argument type.");
		}
		arg_types[base + i] = type;
		arg_values[base + i] = &value;
	}

	// The receiver sits below all arguments on the operand stack, so it is
	// popped only after every Java argument has been consumed.
	if (!method->isStatic())
	{
		Slot this_slot;
		op_stack->pop(this_slot);
		values[1].p = (void*)this_slot.heapPtr;
	}

	// JNI local references: every object handed to the native function is
	// pinned in a JNI local frame, which the collector treats as a GC root.
	// This replaces the old blanket "disable GC while native runs".
	JniPushLocalFrame(jni_env);
	for (int i = 0; i < count; i++)
	{
		DATA_TYPE arg_type = type_desc[i].data_type;
		if (type_desc[i].check_array)
		{
			arg_type = DT_ARRAY;
		}
		if (arg_type == DT_REF || arg_type == DT_ARRAY)
		{
			JniAddLocalRef(jni_env, (jobject)values[base + static_cast<size_t>(i)].p);
		}
	}
	if (!method->isStatic())
	{
		JniAddLocalRef(jni_env, (jobject)values[1].p);
	}
	JNIEnv *prev_jni_env = JniGetCurrentEnv();
	JniSetCurrentEnv(jni_env);

	DATA_TYPE result_type = result_desc.data_type;
	if (result_desc.check_array)
	{
		result_type = DT_ARRAY;
	}

	ffi_type *result_ffi_type = &ffi_type_void;
	switch (result_type)
	{
	case DT_BOOL:
		// JNI boolean is an unsigned char; using a 32-bit ffi result would
		// read undefined upper register bits from natives that only set AL.
		result_ffi_type = &ffi_type_uint8;
		break;
	case DT_BYTE:
	case DT_CHAR:
	case DT_SHORT:
	case DT_INT:
		result_ffi_type = &ffi_type_sint32;
		break;
	case DT_FLOAT:
		result_ffi_type = &ffi_type_float;
		break;
	case DT_LONG:
		result_ffi_type = &ffi_type_sint64;
		break;
	case DT_DOUBLE:
		result_ffi_type = &ffi_type_double;
		break;
	case DT_ARRAY:
	case DT_REF:
		result_ffi_type = &ffi_type_pointer;
		break;
	case DT_VOID:
	default:
		result_ffi_type = &ffi_type_void;
		break;
	}

	ffi_cif cif;
	if (ffi_prep_cif(&cif, FFI_DEFAULT_ABI, static_cast<unsigned int>(arg_types.size()),
	                 result_ffi_type, arg_types.data()) != FFI_OK)
	{
		throw new VMExecutorException("Failed to prepare libffi call interface.");
	}

	NativeValue ret;
	Scheduler::Instance().EnterNative();
	ffi_call(&cif, (void (*)(void))native_func, &ret, arg_values.data());
	Scheduler::Instance().ExitNative();
	JniSetCurrentEnv(prev_jni_env);
	if (pending_sync_held)
	{
		pending_sync_held = false;
		if (pending_sync_class)
		{
			Scheduler::Instance().MonitorExitClass(pending_sync_class);
		}
		else
		{
			Scheduler::Instance().MonitorExit(pending_sync_obj);
		}
		pending_sync_obj = NULL;
		pending_sync_class = NULL;
	}

	bool raised = VMContext::pending_exception != NULL;
	jobject result_ref = NULL;
	if (!raised && (result_type == DT_REF || result_type == DT_ARRAY))
	{
		result_ref = (jobject)ret.p;
	}
	// Pop the native frame; a returned reference is promoted into the caller
	// frame (normal JNI local-reference lifetime).
	JniPopLocalFrame(jni_env, result_ref);
	if (raised)
	{
		return false;
	}

	switch (result_type)
	{
	case DT_BOOL:
		{
			Slot slot;
			slot.iValue = (ret.a != 0) ? 1 : 0;
			op_stack->push(slot);
			break;
		}
	case DT_BYTE:
		{
			Slot slot;
			slot.iValue = static_cast<signed char>(ret.a);
			op_stack->push(slot);
			break;
		}
	case DT_CHAR:
		{
			Slot slot;
			slot.cValue = static_cast<uint16_t>(ret.a);
			op_stack->push(slot);
			break;
		}
	case DT_SHORT:
		{
			Slot slot;
			slot.sValue = static_cast<int16_t>(ret.a);
			op_stack->push(slot);
			break;
		}
	case DT_INT:
		{
			Slot slot;
			slot.iValue = static_cast<int32_t>(ret.a);
			op_stack->push(slot);
			break;
		}
	case DT_FLOAT:
		op_stack->push(ret.f);
		break;
	case DT_LONG:
		op_stack->push(static_cast<LONG64>(ret.l));
		break;
	case DT_DOUBLE:
		op_stack->push(ret.d);
		break;
	case DT_ARRAY:
	case DT_REF:
		{
			Slot slot;
			slot.heapPtr = (LPOBJECT_REF)ret.p;
			op_stack->push(slot);
			break;
		}
	case DT_VOID:
	default:
		break;
	}
	return true;
}

namespace {

// Extracts a primitive value from a wrapper object field (Integer.value:I,
// Long.value:J, ...).  Used when a generic functional interface passes
// Object arguments to a primitive-typed implementation method.
bool UnboxWrapper(LPOBJECT_REF ref, DATA_TYPE target, Slot &value, bool &wide)
{
	wide = false;
	lpObject obj = NULL;
	if (!ref || !VMContext::object_heap->GetObject(ref, obj) || !obj)
	{
		return false;
	}
	const char *key = NULL;
	switch (target)
	{
	case DT_BOOL: key = "value:Z"; break;
	case DT_BYTE: key = "value:B"; break;
	case DT_CHAR: key = "value:C"; break;
	case DT_SHORT: key = "value:S"; break;
	case DT_INT: key = "value:I"; break;
	case DT_FLOAT: key = "value:F"; break;
	case DT_LONG: key = "value:J"; break;
	case DT_DOUBLE: key = "value:D"; break;
	default: return false;
	}
	lpFieldValue field_value = obj->GetFieldValueByName(std::string(key));
	if (!field_value)
	{
		return false;
	}
	Slot stored = field_value->field_value;
	if (target == DT_LONG || target == DT_DOUBLE)
	{
		WByteValue bv;
		if (target == DT_LONG) bv.lValue = stored.lValue; else bv.dValue = stored.dValue;
		value.set(bv.bytes, 4);
		// Caller reads the high half separately in this case.
		wide = true;
		return true;
	}
	value = stored;
	return true;
}

} // namespace

bool StackFrame::InvokeLambda(int lambda_id, lpMethod call_method,
                              vector<TypeDesc> &call_args)
{
	std::map<int, LambdaInfo>::iterator it = g_lambdas.find(lambda_id);
	if (it == g_lambdas.end())
	{
		return false;
	}
	LambdaInfo &info = it->second;
	if (!info.target_method || !info.target_method->methodDesc)
	{
		return false;
	}

	std::vector<TypeDesc> targs;
	TypeDesc tret;
	if (!parse_method_desc_type(info.target_method->methodDesc->GetName(), targs, tret))
	{
		return false;
	}
	std::vector<TypeDesc> sam_args;
	TypeDesc sam_ret;
	if (!parse_method_desc_type(call_method->methodDesc->GetName(), sam_args, sam_ret))
	{
		return false;
	}

	int call_slots = 0;
	for (size_t i = 0; i < call_args.size(); i++)
	{
		call_slots += (call_args[i].data_type == DT_LONG ||
		               call_args[i].data_type == DT_DOUBLE) ? 2 : 1;
	}
	Slot receiver;
	if (!op_stack->top(call_slots, receiver))
	{
		return false;
	}
	LPOBJECT_REF self = receiver.heapPtr;
	GCRootScope root_self(VMContext::object_heap, &self);

	std::vector<Slot> vars;
	size_t target_index = 0;
	size_t cap_start = 0;
	bool is_static = info.target_method->isStatic();
	bool is_constructor = (info.kind == 8);
	bool receiver_in_args = false;
	LPOBJECT_REF ctor_obj = NULL;
	GCRootScope root_ctor(VMContext::object_heap, &ctor_obj);
	if (is_constructor)
	{
		// Constructor reference (REF_newInvokeSpecial): allocate the target
		// object, pass it as local 0 and invoke <init> on it.  The object is
		// the call-site result; <init> itself returns void.
		if (!info.target_class ||
		    !VMContext::object_heap->NewObject(info.target_class, ctor_obj) ||
		    !ctor_obj)
		{
			return false;
		}
		Slot this_slot;
		this_slot.heapPtr = ctor_obj;
		vars.push_back(this_slot);
	}
	else if (!is_static)
	{
		if (info.captures.empty())
		{
			// Unbound instance method reference: the first SAM argument is
			// the receiver and becomes local variable 0.
			receiver_in_args = true;
		}
		else
		{
			vars.push_back(info.captures[0].low);
			cap_start = 1;
		}
	}
	for (size_t ci = cap_start; ci < info.captures.size(); ci++)
	{
		Slot low = info.captures[ci].low;
		if (target_index < targs.size() && info.captures[ci].type == DT_REF &&
		    targs[target_index].data_type != DT_REF && !targs[target_index].check_array)
		{
			Slot unboxed;
			bool wide = false;
			if (UnboxWrapper(low.heapPtr, targs[target_index].data_type, unboxed, wide))
			{
				low = unboxed;
				if (wide)
				{
					WByteValue bv;
					bv.lValue = info.captures[ci].low.lValue;
					Slot hi;
					hi.set(bv.bytes + 4, 4);
					vars.push_back(low);
					vars.push_back(hi);
					target_index++;
					continue;
				}
			}
		}
		vars.push_back(low);
		if (info.captures[ci].wide)
		{
			vars.push_back(info.captures[ci].high);
		}
		target_index++;
	}

	int base = op_stack->size() - call_slots;
	int slot_index = base;
	for (size_t ai = 0; ai < call_args.size(); ai++)
	{
		Slot low, high;
		if (!op_stack->get(slot_index, low))
		{
			return false;
		}
		bool wide = (call_args[ai].data_type == DT_LONG || call_args[ai].data_type == DT_DOUBLE);
		if (wide && !op_stack->get(slot_index + 1, high))
		{
			return false;
		}
		// The first SAM argument of an unbound instance method reference is
		// the receiver, not a declared parameter.
		bool is_receiver = receiver_in_args && ai == 0;
		// Unbox Integer/Long/... when the implementation expects a primitive.
		if (!is_receiver && target_index < targs.size() && call_args[ai].data_type == DT_REF &&
		    targs[target_index].data_type != DT_REF && !targs[target_index].check_array)
		{
			Slot unboxed;
			bool uwide = false;
			if (UnboxWrapper(low.heapPtr, targs[target_index].data_type, unboxed, uwide))
			{
				low = unboxed;
				wide = uwide;
				if (wide)
				{
					WByteValue bv;
					bv.lValue = unboxed.lValue;
					Slot hi;
					hi.set(bv.bytes + 4, 4);
					high = hi;
				}
			}
		}
		vars.push_back(low);
		if (wide)
		{
			vars.push_back(high);
		}
		slot_index += wide ? 2 : 1;
		if (!is_receiver)
		{
			target_index++;
		}
	}

	if (info.target_method->isNative())
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/UnsupportedOperationException",
			"lambda implementation method is native");
		return false;
	}

	// Call-site arguments and the receiver may now be discarded; the values
	// live in vars (and captured refs remain reachable through the lambda).
	for (int i = 0; i < call_slots + 1; i++)
	{
		Slot ignored;
		op_stack->pop(ignored);
	}

	Slot result;
	bool result_wide = false;
	if (!InvokeJavaSync(info.target_class, info.target_method, vars, result, result_wide))
	{
		return false;
	}

	if (is_constructor)
	{
		Slot out;
		out.heapPtr = ctor_obj;
		op_stack->push(out);
		return true;
	}

	DATA_TYPE ret = tret.data_type;
	if (tret.check_array)
	{
		ret = DT_ARRAY;
	}
	DATA_TYPE sam = sam_ret.data_type;
	if (sam_ret.check_array)
	{
		sam = DT_ARRAY;
	}
	if (sam == DT_VOID || ret == DT_VOID)
	{
		return true;
	}
	if (ret == DT_REF || ret == DT_ARRAY)
	{
		Slot out = result;
		if ((sam != DT_REF && sam != DT_ARRAY) && out.heapPtr)
		{
			Slot unboxed;
			bool wide = false;
			if (!UnboxWrapper(out.heapPtr, sam, unboxed, wide))
			{
				return false;
			}
			out = unboxed;
			if (wide)
			{
				WByteValue bv;
				bv.lValue = result.lValue;
				Slot hi;
				hi.set(bv.bytes + 4, 4);
				op_stack->push(out);
				op_stack->push(hi);
				return true;
			}
		}
		op_stack->push(out);
		return true;
	}
	if (result_wide)
	{
		Slot low = result;
		Slot high = thread_stack->return_value_hi;
		if ((sam == DT_REF || sam == DT_ARRAY))
		{
			// Box the primitive so it can satisfy an Object-returning SAM.
			const char *wrapper = NULL;
			const char *method = NULL;
			if (ret == DT_INT) { wrapper = "java/lang/Integer"; method = "valueOf:(I)Ljava/lang/Integer;"; }
			else if (ret == DT_LONG) { wrapper = "java/lang/Long"; method = "valueOf:(J)Ljava/lang/Long;"; }
			else if (ret == DT_DOUBLE) { wrapper = "java/lang/Double"; method = "valueOf:(D)Ljava/lang/Double;"; }
			if (!wrapper)
			{
				return false;
			}
			lpClass wc = NULL;
			if (!class_loader->FindClass(wrapper, wc))
			{
				return false;
			}
			lpMethod wm = NULL;
			if (!wc->FindMethod((LPSTR)method, wm) || !wm)
			{
				return false;
			}
			std::vector<Slot> wvars;
			wvars.push_back(low);
			wvars.push_back(high);
			Slot boxed;
			bool bwide = false;
			if (!InvokeJavaSync(wc, wm, wvars, boxed, bwide))
			{
				return false;
			}
			op_stack->push(boxed);
			return true;
		}
		op_stack->push(low);
		op_stack->push(high);
		return true;
	}
	if (sam == DT_REF || sam == DT_ARRAY)
	{
		const char *wrapper = NULL;
		const char *method = NULL;
		switch (ret)
		{
		case DT_BOOL: wrapper = "java/lang/Boolean"; method = "valueOf:(Z)Ljava/lang/Boolean;"; break;
		case DT_CHAR: wrapper = "java/lang/Character"; method = "valueOf:(C)Ljava/lang/Character;"; break;
		case DT_FLOAT: wrapper = "java/lang/Double"; method = "valueOf:(D)Ljava/lang/Double;"; break;
		case DT_INT: wrapper = "java/lang/Integer"; method = "valueOf:(I)Ljava/lang/Integer;"; break;
		default: return false;
		}
		lpClass wc = NULL;
		if (!class_loader->FindClass(wrapper, wc))
		{
			return false;
		}
		lpMethod wm = NULL;
		if (!wc->FindMethod((LPSTR)method, wm) || !wm)
		{
			return false;
		}
		std::vector<Slot> wvars;
		if (ret == DT_FLOAT)
		{
			double d = static_cast<double>(result.fValue);
			WByteValue bv;
			bv.dValue = d;
			Slot lo, hi;
			lo.set(bv.bytes, 4);
			hi.set(bv.bytes + 4, 4);
			wvars.push_back(lo);
			wvars.push_back(hi);
		}
		else
		{
			wvars.push_back(result);
		}
		Slot boxed;
		bool bwide = false;
		if (!InvokeJavaSync(wc, wm, wvars, boxed, bwide))
		{
			return false;
		}
		op_stack->push(boxed);
		return true;
	}
	op_stack->push(result);
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
	char atype = 0;
	switch (index)
	{
	case 4:  atype = 'Z'; break; // boolean
	case 5:  atype = 'C'; break; // char
	case 6:  atype = 'F'; break; // float
	case 7:  atype = 'D'; break; // double
	case 8:  atype = 'B'; break; // byte
	case 9:  atype = 'S'; break; // short
	case 10: atype = 'I'; break; // int
	case 11: atype = 'J'; break; // long
	default:
		throw new VMExecutorException("Invalid newarray type.");
	}
	char name[3] = { '[', atype, '\0' };
	lpClass array_class = NULL;
	if (!FindClassOrError(class_loader, name, array_class))
	{
		return false;
	}
	Slot slot;
	op_stack->top(0, slot);
	if (slot.iValue < 0)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NegativeArraySizeException", "negative array size");
		return false;
	}
	op_stack->pop(slot);
	LPOBJECT_REF obj_ref = NULL;
	if (!VMContext::object_heap->NewArrayObject(slot.iValue, array_class, obj_ref))
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/OutOfMemoryError", "cannot allocate array");
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
	// anewarray names the *component* type; the new object has the array
	// class "[" + component (needed for checkcast/instanceof on arrays).
	std::string component(cc->utf8->bytes);
	std::string array_name;
	if (!component.empty() && component[0] == '[')
	{
		array_name = std::string("[") + component;
	}
	else
	{
		array_name = "[L" + component + ";";
	}
	lpClass array_class = NULL;
	if (!FindClassOrError(class_loader, array_name.c_str(), array_class))
	{
		return false;
	}

	Slot slot;
	op_stack->top(0, slot);
	if (slot.iValue < 0)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NegativeArraySizeException", "negative array size");
		return false;
	}
	op_stack->pop(slot);
	LPOBJECT_REF obj_ref = NULL;
	if (!VMContext::object_heap->NewObjectArrayObject(slot.iValue, array_class, obj_ref))
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/OutOfMemoryError", "cannot allocate array");
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
	lpClass array_class = NULL;
	if (!FindClassOrError(class_loader, cc->utf8->bytes, array_class))
	{
		return false;
	}

	LPINT32 nums = new INT32[count];
	Slot slot;
	for (int i = 0; i < count; i++)
	{
		op_stack->top(i, slot);
		nums[i] = slot.iValue;
	}
	for (int i = 0; i < count; i++)
	{
		if (nums[i] < 0)
		{
			delete[] nums;
			VMContext::pending_exception = CreateJavaException(
				"java/lang/NegativeArraySizeException", "negative array size");
			return false;
		}
	}
	for (int i = 0; i < count; i++)
	{
		op_stack->pop(slot);
	}
	LPOBJECT_REF obj_ref = NULL;
	bool ok = VMContext::object_heap->NewMultiArrayObject(nums, count, array_class, obj_ref);
	delete[] nums;
	if (!ok)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/OutOfMemoryError", "cannot allocate multi-dimensional array");
		return false;
	}
	op_stack->pushObject(obj_ref);
	return true;
}

namespace {

lpClass LoadedComponentClass(lpClassLoader loader, const char *internal_name)
{
	if (!loader || !internal_name || !internal_name[0])
	{
		return NULL;
	}
	lpClass clazz = NULL;
	loader->GetLoadedClass(internal_name, clazz);
	return clazz;
}

// True when a value of class src may be stored into an array whose element
// class is target.  Only already-loaded classes are consulted: the aastore
// check must not trigger class loading (and therefore a GC) while the
// receiver/value live only in operand-stack slots.
bool ArrayElementAccepts(lpClassLoader loader, lpClass target, lpClass src)
{
	if (!target || !src)
	{
		return true;
	}
	if (ClassIsAssignable(target, src))
	{
		return true;
	}
	const char *tn = (target->thisClass && target->thisClass->GetName())
		? target->thisClass->GetName() : NULL;
	const char *sn = (src->thisClass && src->thisClass->GetName())
		? src->thisClass->GetName() : NULL;
	if (!tn || !sn)
	{
		return true;
	}
	if (sn[0] == '[')
	{
		// Arrays are assignable to Object, Cloneable and java.io.Serializable.
		if (strcmp(tn, "java/lang/Object") == 0 ||
		    strcmp(tn, "java/lang/Cloneable") == 0 ||
		    strcmp(tn, "java/io/Serializable") == 0)
		{
			return true;
		}
		if (tn[0] == '[')
		{
			const char *tc = tn + 1;
			const char *sc = sn + 1;
			if (tc[0] == 'L' && sc[0] == 'L')
			{
				std::string tcn(tc + 1, strlen(tc) - 2);
				std::string scn(sc + 1, strlen(sc) - 2);
				return ArrayElementAccepts(loader,
					LoadedComponentClass(loader, tcn.c_str()),
					LoadedComponentClass(loader, scn.c_str()));
			}
			if (tc[0] == '[' && sc[0] == '[')
			{
				return ArrayElementAccepts(loader,
					LoadedComponentClass(loader, tc),
					LoadedComponentClass(loader, sc));
			}
			return strcmp(tc, sc) == 0;
		}
	}
	return false;
}

// Returns the element class of an array object, or NULL for primitive arrays
// or classes that are not loaded.
lpClass ArrayElementClass(lpClassLoader loader, lpObject array)
{
	if (!array || !array->obj_class || !array->obj_class->thisClass)
	{
		return NULL;
	}
	const char *name = array->obj_class->thisClass->GetName();
	if (!name || name[0] != '[')
	{
		return NULL;
	}
	const char *comp = name + 1;
	if (comp[0] == 'L' && comp[strlen(comp) - 1] == ';')
	{
		std::string internal(comp + 1, strlen(comp) - 2);
		return LoadedComponentClass(loader, internal.c_str());
	}
	if (comp[0] == '[')
	{
		return LoadedComponentClass(loader, comp);
	}
	return NULL;
}

bool SetArrayException(const char *class_name, const std::string &message)
{
	VMContext::pending_exception = CreateJavaException(class_name, message.c_str());
	return false;
}

} // namespace

bool StackFrame::SetArrayIndex(int value_slots)
{
	VMLock lk;
	Slot value_low;
	Slot value_high;
	Slot index_slot;
	Slot ary;
	if (value_slots == 2)
	{
		op_stack->top(0, value_high);
		op_stack->top(1, value_low);
	}
	else
	{
		op_stack->top(0, value_low);
		value_high = Slot();
	}
	op_stack->top(value_slots, index_slot);
	op_stack->top(value_slots + 1, ary);

	LPOBJECT_REF obj_ref = ary.heapPtr;
	if (!obj_ref)
	{
		return SetArrayException("java/lang/NullPointerException",
			"array reference is null");
	}
	lpObject obj = NULL;
	if (!VMContext::object_heap->GetObject(obj_ref, obj) || !obj)
	{
		return SetArrayException("java/lang/NullPointerException",
			"array reference is invalid");
	}
	if (!obj->array_var)
	{
		return SetArrayException("java/lang/ClassCastException",
			"value is not an array");
	}
	int index = index_slot.iValue;
	if (index < 0 || index >= obj->array_length)
	{
		char msg[128];
		snprintf(msg, sizeof(msg), "Index %d out of bounds for length %d",
			index, obj->array_length);
		return SetArrayException("java/lang/ArrayIndexOutOfBoundsException", msg);
	}

	Slot value = value_low;
	if (value_slots == 2)
	{
		WByteValue wb(value_low.bytes, value_high.bytes);
		value.dValue = wb.dValue;
	}

	if (obj->array_data_type == DT_REF || obj->array_data_type == DT_ARRAY)
	{
		LPOBJECT_REF value_ref = value.heapPtr;
		if (value_ref)
		{
			lpClass element = ArrayElementClass(class_loader, obj);
			lpObject value_obj = NULL;
			if (element && VMContext::object_heap->GetObject(value_ref, value_obj) && value_obj)
			{
				if (!ArrayElementAccepts(class_loader, element, value_obj->obj_class))
				{
					char msg[256];
					snprintf(msg, sizeof(msg),
						"cannot store %s into array of %s",
						ObjectClassName(value_obj), ObjectClassName(obj));
					return SetArrayException("java/lang/ArrayStoreException", msg);
				}
			}
		}
	}

	obj->SetArrayIndex(index, value);
	for (int i = 0; i < value_slots + 2; i++)
	{
		Slot discarded;
		op_stack->pop(discarded);
	}
	return true;
}

bool StackFrame::LoadArray(DATA_TYPE data_type)
{
	VMLock lk;
	Slot ary, index_slot;
	op_stack->top(0, index_slot);
	op_stack->top(1, ary);
	LPOBJECT_REF obj_ref = ary.heapPtr;
	if (!obj_ref)
	{
		return SetArrayException("java/lang/NullPointerException",
			"array reference is null");
	}
	lpObject obj = NULL;
	if (!VMContext::object_heap->GetObject(obj_ref, obj) || !obj)
	{
		return SetArrayException("java/lang/NullPointerException",
			"array reference is invalid");
	}
	if (!obj->array_var)
	{
		return SetArrayException("java/lang/ClassCastException",
			"value is not an array");
	}
	int index = index_slot.iValue;
	if (index < 0 || index >= obj->array_length)
	{
		char msg[128];
		snprintf(msg, sizeof(msg), "Index %d out of bounds for length %d",
			index, obj->array_length);
		return SetArrayException("java/lang/ArrayIndexOutOfBoundsException", msg);
	}
	Slot ret;
	switch (data_type)
	{
	case DT_BYTE:
		{
			jbyte *bytes = (jbyte*)obj->array_var;
			ret.bValue = bytes[index];
			break;
		}
	case DT_CHAR:
		{
			jchar *vals = (jchar*)obj->array_var;
			ret.cValue = vals[index];
			break;
		}
	case DT_DOUBLE:
		{
			jdouble *vals = (jdouble*)obj->array_var;
			ret.dValue = vals[index];
			break;
		}
	case DT_FLOAT:
		{
			jfloat *vals = (jfloat*)obj->array_var;
			ret.fValue = vals[index];
			break;
		}
	case DT_INT:
		{
			jint *vals = (jint*)obj->array_var;
			ret.iValue = vals[index];
			break;
		}
	case DT_LONG:
		{
			jlong *vals = (jlong*)obj->array_var;
			ret.lValue = vals[index];
			break;
		}
	case DT_SHORT:
		{
			jshort *vals = (jshort*)obj->array_var;
			ret.sValue = vals[index];
			break;
		}
	case DT_ARRAY:
	case DT_REF:
		{
			LPOBJECT_REF* refs = (LPOBJECT_REF*)obj->array_var;
			ret.heapPtr = refs[index];
			break;
		}
	}
	Slot discarded;
	op_stack->pop(discarded);
	op_stack->pop(discarded);
	if (data_type == DT_LONG)
	{
		op_stack->push(ret.lValue);
	}
	else if (data_type == DT_DOUBLE)
	{
		op_stack->push(ret.dValue);
	}
	else
	{
		op_stack->push(ret);
	}
	return true;
}

bool StackFrame::ArrayLength()
{
	VMLock lk;
	Slot slot;
	op_stack->top(0, slot);
	LPOBJECT_REF obj_ref = slot.heapPtr;
	if (!obj_ref)
	{
		return SetArrayException("java/lang/NullPointerException",
			"array reference is null");
	}
	lpObject obj = NULL;
	if (!VMContext::object_heap->GetObject(obj_ref, obj) || !obj)
	{
		return SetArrayException("java/lang/NullPointerException",
			"array reference is invalid");
	}
	if (!obj->array_var)
	{
		return SetArrayException("java/lang/ClassCastException",
			"value is not an array");
	}
	op_stack->pop(slot);
	op_stack->push(obj->array_length);
	return true;
}

bool StackFrame::PutStatic(U2 index)
{
	VMLock lk;
	FieldRefConstant *frc = (FieldRefConstant*) clazz->const_pool->GetConstant(index);
	if (!frc)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NoSuchFieldError", "invalid field constant index");
		return false;
	}
	lpField field = NULL;
	lpFieldValue fv = NULL;
	if (!ResolveStaticField(class_loader, frc, field, fv))
	{
		return false;
	}
	LPSTR desc = (frc->nameType && frc->nameType->descUtf8)
		? frc->nameType->descUtf8->GetName() : NULL;
	TypeDesc type_desc;
	if (!parse_desc_type(desc, type_desc, desc ? strlen(desc) : 0))
	{
		return false;
	}
	// An array descriptor such as [J keeps its element type in data_type; the
	// field itself is always a single reference slot.
	if (type_desc.check_array)
	{
		type_desc.data_type = DT_ARRAY;
	}
	switch (type_desc.data_type)
	{
	case DT_LONG:
		fv->field_value.lValue = popLong();
		break;
	case DT_DOUBLE:
		fv->field_value.dValue = popDouble();
		break;
	default:
		op_stack->pop(fv->field_value);
		break;
	}
	return true;
}

bool StackFrame::GetStatic(U2 index)
{
	VMLock lk;
	FieldRefConstant *frc = (FieldRefConstant*) clazz->const_pool->GetConstant(index);
	if (!frc)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NoSuchFieldError", "invalid field constant index");
		return false;
	}
	lpField field = NULL;
	lpFieldValue fv = NULL;
	if (!ResolveStaticField(class_loader, frc, field, fv))
	{
		return false;
	}
	LPSTR desc = (frc->nameType && frc->nameType->descUtf8)
		? frc->nameType->descUtf8->GetName() : NULL;
	TypeDesc type_desc;
	if (!parse_desc_type(desc, type_desc, desc ? strlen(desc) : 0))
	{
		return false;
	}
	if (type_desc.check_array)
	{
		type_desc.data_type = DT_ARRAY;
	}
	switch (type_desc.data_type)
	{
	case DT_LONG:
		op_stack->push(fv->field_value.lValue);
		break;
	case DT_DOUBLE:
		op_stack->push(fv->field_value.dValue);
		break;
	default:
		op_stack->push(fv->field_value);
		break;
	}
	return true;
}

bool StackFrame::GetField(U2 index)
{
	VMLock lk;
	FieldRefConstant *frc = (FieldRefConstant*) clazz->const_pool->GetConstant(index);
	if (!frc)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NoSuchFieldError", "invalid field constant index");
		return false;
	}
	// Resolve before popping the receiver: resolution can load classes and
	// therefore collect, and the receiver must stay rooted on the operand
	// stack until the field is known.
	lpField field = NULL;
	if (!ResolveInstanceField(class_loader, frc, field))
	{
		return false;
	}
	Slot slot_obj;
	op_stack->pop(slot_obj);
	LPOBJECT_REF obj_ref = slot_obj.heapPtr;
	if (!obj_ref)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NullPointerException", "null object reference");
		return false;
	}
	lpObject obj = NULL;
	if (!VMContext::object_heap->GetObject(obj_ref, obj) || !obj)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NullPointerException", "invalid object reference");
		return false;
	}
	lpFieldValue fv = obj->GetFieldValue(field);
	if (!fv)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NoSuchFieldError", "field storage is missing");
		return false;
	}
	LPSTR desc = (frc->nameType && frc->nameType->descUtf8)
		? frc->nameType->descUtf8->GetName() : NULL;
	TypeDesc type_desc;
	if (!parse_desc_type(desc, type_desc, desc ? strlen(desc) : 0))
	{
		return false;
	}
	if (type_desc.check_array)
	{
		type_desc.data_type = DT_ARRAY;
	}
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
	return true;
}

bool StackFrame::PutField(U2 index)
{
	VMLock lk;
	FieldRefConstant *frc = (FieldRefConstant*) clazz->const_pool->GetConstant(index);
	if (!frc)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NoSuchFieldError", "invalid field constant index");
		return false;
	}
	lpField field = NULL;
	if (!ResolveInstanceField(class_loader, frc, field))
	{
		return false;
	}
	LPSTR desc = (frc->nameType && frc->nameType->descUtf8)
		? frc->nameType->descUtf8->GetName() : NULL;
	TypeDesc type_desc;
	if (!parse_desc_type(desc, type_desc, desc ? strlen(desc) : 0))
	{
		return false;
	}
	if (type_desc.check_array)
	{
		type_desc.data_type = DT_ARRAY;
	}

	Slot slot_val;
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
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NullPointerException", "null object reference");
		return false;
	}
	lpObject obj = NULL;
	if (!VMContext::object_heap->GetObject(obj_ref, obj) || !obj)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NullPointerException", "invalid object reference");
		return false;
	}
	lpFieldValue fv = obj->GetFieldValue(field);
	if (!fv)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/NoSuchFieldError", "field storage is missing");
		return false;
	}
	fv->field_value = slot_val;
	return true;
}

void StackFrame::TableSwitch()
{
	// The offsets following the opcode are 4-byte aligned relative to the
	// beginning of the method code, so skip the padding bytes first.
	int pad = (4 - (pc % 4)) % 4;
	pc += pad;
	int default_offset = ReadInt();
	int low = ReadInt();
	int high = ReadInt();
	if (high < low)
	{
		Slot slot;
		op_stack->pop(slot);
		pc = last_pc + default_offset;
		return;
	}
	LONG64 n64 = (LONG64)high - (LONG64)low + 1;
	if (n64 <= 0 || n64 > (LONG64)code_length)
	{
		throw new VMExecutorException("Invalid tableswitch size.");
	}
	int n = (int)n64;
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
	// Same 4-byte alignment rule as tableswitch (the "match" entries follow
	// an int default and an int count).
	int pad = (4 - (pc % 4)) % 4;
	pc += pad;
	int default_offset = ReadInt();
	int n = ReadInt();
	if (n < 0 || (LONG64)n * 8 > (LONG64)code_length)
	{
		throw new VMExecutorException("Invalid lookupswitch size.");
	}
	Slot slot;
	op_stack->pop(slot);
	int val = slot.iValue;
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