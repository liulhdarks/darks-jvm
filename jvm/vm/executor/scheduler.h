#pragma once

// M:N user-space coroutine scheduler for java.lang.Thread.
//
// Every Java Thread runs as a coroutine.  A coroutine owns a ThreadStack whose
// interpreter frames (operand stack, locals, program counter) live on the Java
// heap, so suspending a coroutine is simply returning from the interpreter to
// the scheduler; resuming it re-enters the same interpreter loop.  Because the
// C++ call stack is only the (small) interpreter entry, no register context or
// ucontext/fiber switching is needed, and the same code compiles on Linux,
// macOS and Windows.
//
// DARKS_JVM_MP=N selects the number of OS threads.  The default is 1, which
// runs every coroutine on the launching thread (deterministic, old behaviour).
// N>1 starts N-1 worker threads and the launching thread also participates.

#include "vm/types.h"
#include "exec_common.h"

#include <stdint.h>
#include <atomic>
#include <vector>

class ThreadStack;
class StackFrame;
class Method;

struct Coroutine;

typedef Coroutine *lpCoroutine;

enum CoroutineState
{
	CO_NEW = 0,
	CO_READY,
	CO_RUNNING,
	CO_SLEEPING,
	CO_MONITOR,      // waiting to enter a monitor
	CO_WAITING,      // Object.wait
	CO_JOINING,      // Thread.join
	CO_TERMINATED
};

// One Java thread.  Fields marked "root" are registered as GC roots for their
// whole lifetime (their addresses are stable because Coroutine objects are
// heap allocated and never copied).
struct Coroutine
{
	ThreadStack *ts;
	lpClass start_class;
	lpMethod start_method;

	LPOBJECT_REF thread_ref;   // Java java.lang.Thread object (root)
	LPOBJECT_REF block_obj;    // monitor object while blocked (root)
	lpClass block_class;       // class monitor while blocked (synchronized static)
	lpCoroutine join_target;
	int saved_count;           // monitor recursion count restored after wait
	int state;
	int64_t deadline_ns;       // 0 => no timer
	uint64_t timer_generation;
	LPOBJECT_REF injected;     // InterruptedException to raise on resume (root)

	bool interrupt_flag;
	bool throw_on_resume;
	bool resume_from_wait;
	// True when ownership of a monitor was handed to this coroutine by a
	// releasing owner; the retried monitorenter must not bump the recursion
	// count a second time.
	bool monitor_granted;
	// Set while an OS worker owns (is executing) this coroutine; claimed with
	// a CAS so two workers can never run the same Java thread concurrently.
	std::atomic<bool> busy;
	bool entered;
	bool finished;
	bool pin_main;
	bool queued;
	bool in_native;
	uint64_t id;

	std::vector<lpCoroutine> joiners;

	Coroutine();
};

class Scheduler
{
public:
	static Scheduler &Instance();

	void Initialize();
	void Shutdown();
	int Parallelism() const;

	// The launching thread runs the main coroutine and doubles as worker 0.
	void AttachMain(ThreadStack *ts);
	void DetachMain();
	EventNotify RunMain(ThreadStack *ts, lpClass clazz, lpMethod method,
	                    lpVariant vars, int count);

	static lpCoroutine Current();
	static ThreadStack *CurrentStack();
	static bool InCoroutine();

	// ---- interpreter hooks ------------------------------------------------
	// Blocking primitives implemented as native calls are intercepted before
	// libffi dispatch.  Each returns true when the call was completely handled
	// (either it returned immediately or the coroutine must suspend).
	bool SleepCurrent(ThreadStack *ts, int64_t millis);
	bool YieldCurrent(ThreadStack *ts);
	bool JoinCurrent(ThreadStack *ts, LPOBJECT_REF target, int64_t millis);
	bool WaitCurrent(ThreadStack *ts, LPOBJECT_REF obj, int64_t millis);
	bool NotifyCurrent(ThreadStack *ts, LPOBJECT_REF obj, bool all);

	// Monitors.  MonitorEnter returns false when the coroutine must suspend
	// and re-execute the instruction (the caller resets pc to last_pc).
	bool MonitorEnter(StackFrame *frame, LPOBJECT_REF obj);
	bool MonitorEnterClass(StackFrame *frame, lpClass clazz);
	void MonitorExit(LPOBJECT_REF obj);
	void MonitorExitClass(lpClass clazz);
	void ReleaseFrameMonitors(StackFrame *frame);

	// Returns true when the interpreter must yield so that GC can stop the
	// world.  Kept cheap: called at every interpreter back edge.
	bool PollSafepoint(ThreadStack *ts);
	// Cooperative preemption: yields the coroutine so that other runnable
	// coroutines make progress even in tight CPU loops.
	bool FairnessYield(ThreadStack *ts);

	// ---- thread lifecycle (natives) --------------------------------------
	bool StartThread(LPOBJECT_REF thread_obj);
	void InterruptThread(LPOBJECT_REF thread_obj);
	bool IsInterrupted(LPOBJECT_REF thread_obj, bool clear);
	bool ThreadAlive(LPOBJECT_REF thread_obj);

	// Called by the interpreter around a native method so that stop-the-world
	// does not wait for a thread that is inside foreign code.
	void EnterNative();
	void ExitNative();

	// Heap hooks.
	void AddCoroutineRoots(lpCoroutine co);
	void RemoveCoroutineRoots(lpCoroutine co);
	void WakeJoiners(lpCoroutine co);

	// Used by ObjectHeap to stop the world for a collection.  Must be called
	// without holding the heap mutex.
	void SafepointBegin();
	void SafepointEnd();

	// Called by VMLock around a (possibly blocking) VMMutex acquisition so a
	// thread waiting for the VM lock still counts as a safe point.
	static void EnterVMLockWait();
	static void ExitVMLockWait();

private:
	Scheduler();
	~Scheduler();
	Scheduler(const Scheduler &);
	Scheduler &operator=(const Scheduler &);

	void WorkerMain(int index);
	void RunOne(lpCoroutine co);
	void FinishCoroutine(lpCoroutine co, bool uncaught);
	void PumpMain(lpCoroutine main_co, bool yield_to_others = false);
	bool TakeReady(int worker_index, lpCoroutine &co);
	void Enqueue(lpCoroutine co, int hint);
	void ReapTimers();
	void WaitForWork(int64_t timeout_ns);
	int64_t NextDeadline() const;
	void MarkAtSafepoint(bool at);
	void DetachCoroutine(lpCoroutine co);
	bool SetupThread(lpCoroutine co, LPOBJECT_REF thread_obj);

	struct Impl;
	Impl *impl;
	int mp;
};
