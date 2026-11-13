#pragma once

#include "vm/types.h"
#include "jclass.h"
#include "jmethod.h"
#include "localtable.h"
#include "operstack.h"
#include "exec_common.h"
#include "vm/classfiles/classloader.h"
#include "descriptor.h"
#include "jni/jni.h"

#include <utility>
#include <vector>

class ObjectHeap;

#define STATIC_METHOD 1
#define NORMAL_METHOD 2
#define SPECIAL_METHOD 3

namespace darksjit { struct Program; }

class JVM_API StackFrame
{

private:
	int pc;
	int last_pc;
	lpClassLoader class_loader;
	lpClass clazz;
	lpMethod method;
	bool inited;
	LPBYTE codes;
	U4 code_length;
	bool static_method;
	bool jit_checked;
	const darksjit::Program *jit_program;
	lpThreadStack thread_stack;
	lpStackFrame prev_frame;
public:
	lpOperStack op_stack;
	lpLocalVarTable local_var_table;
	JNIEnv *jni_env;
	// Monitors held by this frame (object monitors and class monitors).  They
	// are released when the frame is popped, which also covers exception
	// unwinding that skips monitorexit.
	std::vector<std::pair<LPOBJECT_REF, lpClass> > monitors_held;
	// Monitor acquired for a synchronized call but not yet attached to a
	// callee frame (native methods have no frame).
	LPOBJECT_REF pending_sync_obj;
	lpClass pending_sync_class;
	bool pending_sync_held;

public:
	StackFrame();

	StackFrame(lpThreadStack thread_stack, lpClass clazz, lpMethod method, lpClassLoader class_loader);

	~StackFrame();

	EventNotify Execute();

	void init(lpVariant vars = NULL, int count = 0);

	// Conservative root scan: every local variable and live operand stack slot
	// may hold an LPOBJECT_REF.
	void MarkRoots(ObjectHeap *heap);

	// Searches this frame's exception table at last_pc and, on a match,
	// replaces the operand stack with the exception and jumps to the handler.
	bool HandleException();

	// After a raise: returns EN_NOOP when the current frame caught it, else
	// EN_EXCEPTION so the caller unwinds.
	EventNotify ExceptionEvent();

	bool HasPendingException() const;

private:

	EventNotify ExecuteCode();

	// Prologue shared by the interpreter and the JIT loop (safepoints,
	// coroutine events, last_pc bookkeeping).
	EventNotify BeginInstruction();

	EventNotify Interpret();

	EventNotify ExecuteJit(const darksjit::Program *program);

	U2 ReadU2();

	short ReadShort();
	int ReadInt();

	bool LoadConstant(int index, Slot &slot);

	bool LoadConstantW(int index, Slot &slot1, Slot &slot2);
	bool ExecuteNewObject(U2 index);
	bool ExecuteNewArray(U1 index);
	bool ExecuteNewObjectArray(U2 index);
	bool ExecuteNewMultiArray(U2 index, int count);
	bool ExecuteSpecial(int index);
	bool ExecuteStatic(int index);
	bool ExecuteVirtual(int index, int type);
	bool ExecuteInvokeDynamic(U2 index);
	bool CloneArrayReceiver();
	bool InvokeLambda(int lambda_id, lpMethod call_method,
	                  vector<TypeDesc> &call_args);

	// Runs a Java method on a nested ThreadStack frame and returns its value.
	// The caller's frame stays suspended (and therefore stays a GC root), so
	// this is only safe from inside ExecuteCode.
	bool InvokeJavaSync(lpClass target, lpMethod method,
	                    const std::vector<Slot> &vars, Slot &result,
	                    bool &wide);

	// Appends the Java string representation of a value to a UTF-16 buffer.
	bool AppendValueUnits(const TypeDesc &desc, Slot *slots,
	                      std::vector<uint16_t> &out);
	bool AppendStringRefUnits(LPOBJECT_REF ref, std::vector<uint16_t> &out);

	bool ExecuteNormalMethod(lpClass clazz, lpMethod method, vector<TypeDesc> &type_desc);
	bool ExecuteNativeMethod(lpClass clazz, lpMethod method, vector<TypeDesc> &type_desc, TypeDesc &result_desc);

	// Operand-stack based array access.  The value/index/arrayref are peeked
	// (not popped) until validation succeeds, so a GC triggered while the
	// type check runs still traces the array and value through the frame.
	bool SetArrayIndex(int value_slots);
	bool LoadArray(DATA_TYPE data_type);
	bool ArrayLength();

	bool PutStatic(U2 index);
	bool GetStatic(U2 index);
	bool GetField(U2 index);
	bool PutField(U2 index);
	bool TryInline(lpClass declaring, lpMethod method,
	               vector<TypeDesc> &type_desc, TypeDesc &result_desc);
	void TableSwitch();
	void LookupSwitch();
private:

	double popDouble();
	LONG64 popLong();
	bool StackToLocal(lpStackFrame frame, vector<TypeDesc> &type_desc, bool this_var = true);
};
