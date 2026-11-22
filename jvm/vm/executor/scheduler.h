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
#include <string>
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

// Thread kind as exposed to the Java API.  Both kinds currently run on the
// same coroutine scheduler; the field keeps isVirtual()/diagnostics faithful
// and gives a place to bind platform threads to dedicated carriers later.
enum CoroutineKind
{
	CO_KIND_MAIN = 0,
	CO_KIND_PLATFORM,
	CO_KIND_VIRTUAL
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
	int kind;                  // CoroutineKind
	int priority;              // Java thread priority (deterministic ordering)
	int carrier_id;            // worker index currently running it, -1 if unmounted
	int64_t deadline_ns;       // 0 => no timer
	uint64_t timer_generation;
	// Set when the coroutine is placed in a ready queue and cleared when a
	// worker claims it.  Diagnostics use it to measure dispatch latency; it is
	// only read while the scheduler lock is held.
	int64_t ready_since_ns;
	LPOBJECT_REF injected;     // InterruptedException to raise on resume (root)
	// Structured concurrency: the scope that owns this coroutine when it was
	// started by StructuredTaskScope.fork().  Registered as a GC root so the
	// Java scope stays alive for as long as a child can still report to it.
	LPOBJECT_REF scope_parent;

	bool interrupt_flag;
	bool throw_on_resume;
	bool resume_from_wait;
	// Virtual-thread diagnostics: number of nested native calls and whether a
	// pinning warning was already emitted (one warning per coroutine).
	int pinned_depth;
	bool pinning_warned;
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

// ---- deadlock / starvation diagnostics -------------------------------------
// What a parked coroutine is waiting for.  Timed waits carry their own
// deadline and are never treated as deadlock edges; they only show up as
// "timed" in a thread snapshot.
enum CoroutineWaitKind
{
	CO_WAIT_NONE = 0,
	CO_WAIT_MONITOR,       // contending for a monitor
	CO_WAIT_JOIN,          // Thread.join without a timeout
	CO_WAIT_OBJECT,        // Object.wait() without a timeout
	CO_WAIT_CHANNEL_SEND,  // parked in darks.concurrent.Channel.send
	CO_WAIT_CHANNEL_RECV,  // parked in darks.concurrent.Channel.receive
	CO_WAIT_TIMED          // sleep / timed join / timed wait
};

// One row of the diagnostic thread snapshot.  This is the interface the
// deadlock watchdog uses and that a future ThreadMXBean binding can expose.
struct DiagThread
{
	DiagThread() : id(0), kind(0), state(0), wait_kind(CO_WAIT_NONE), wait_target(0) {}

	uint64_t id;
	std::string name;
	int kind;                       // CoroutineKind
	int state;                      // CoroutineState
	int wait_kind;                  // CoroutineWaitKind
	uint64_t wait_target;           // coroutine id of the blocker (0 if none)
	std::string wait_target_name;
	std::string monitor;            // monitor / channel being waited for
	std::vector<std::string> held_monitors;
	std::string location;           // innermost Java frame
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
	// After main returns, the JVM may not exit while non-daemon Java threads
	// are still alive.  Runs them on carrier 0 until only daemons remain.
	void RunNonDaemonThreads();

	static lpCoroutine Current();
	// The Java thread an interpreter frame belongs to.  Unlike Current() this
	// keeps reporting the suspended coroutine while a nested executor (class
	// initialisation, JNI embedding) runs on that coroutine's OS thread, so
	// Thread.currentThread() never observes null.
	static lpCoroutine CurrentJavaThread();
	static ThreadStack *CurrentStack();
	static bool InCoroutine();
	// Java Thread object of the coroutine that owns this OS thread.  Unlike
	// Current(), this stays valid inside a nested executor (<clinit>), so
	// Thread.currentThread() keeps returning the enclosing Java thread.
	static LPOBJECT_REF CurrentThreadObject();

	// ---- interpreter hooks ------------------------------------------------
	// Blocking primitives implemented as native calls are intercepted before
	// libffi dispatch.  Each returns true when the call was completely handled
	// (either it returned immediately or the coroutine must suspend).
	bool SleepCurrent(ThreadStack *ts, int64_t millis);
	bool YieldCurrent(ThreadStack *ts);
	bool JoinCurrent(ThreadStack *ts, LPOBJECT_REF target, int64_t millis);
	bool WaitCurrent(ThreadStack *ts, LPOBJECT_REF obj, int64_t millis);
	bool NotifyCurrent(ThreadStack *ts, LPOBJECT_REF obj, bool all);
	// Keeps a nested invocation (JNI Call*Method) alive across a suspension:
	// runs other ready coroutines until the current one is runnable again.
	// Returns false when the scheduler stopped or the coroutine terminated.
	bool AwaitCurrentResume(ThreadStack *ts);
	// Finishes the stop-the-world handshake begun by PollSafepoint.
	bool CompleteSafepoint();

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
	bool StartVirtualThread(LPOBJECT_REF thread_obj);
	// Structured concurrency (java.util.concurrent.StructuredTaskScope).
	// Starts thread_obj as a child of scope_obj (virtual threads use the
	// virtual kind, custom factories may supply platform threads).  Fails
	// when the scope was already closed, so a late fork cannot leak past
	// shutdown.
	bool StartScopedThread(LPOBJECT_REF thread_obj, LPOBJECT_REF scope_obj);
	// Interrupts the scope's unfinished children through the regular
	// interrupt + injected_exception path.
	void ScopeShutdown(LPOBJECT_REF scope_obj);
	// Number of children that have not finished yet.
	int ScopeLiveChildren(LPOBJECT_REF scope_obj);
	// Marks the scope closed and returns the number of children still alive;
	// the registry entry goes away once the last child finishes (or
	// immediately when there is none).
	int ScopeClose(LPOBJECT_REF scope_obj);
	// Parent scope of the calling coroutine, or NULL outside a scope.
	LPOBJECT_REF CurrentScope();
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

	// ---- deadlock / starvation diagnostics -------------------------------
	// -Ddarks.diag.deadlock=1|log and -Ddarks.diag.starvation=1 install an
	// idle-point watchdog: it analyses the wait-for graph, prints a report to
	// stderr and (in "1" mode) exits with code 5 (deadlock) or 6 (starvation).
	// "log" only prints, which lets an embedder keep running.
	bool DiagnosticsEnabled() const;

	// Diagnostic request: returns a snapshot of every live coroutine.  Safe
	// to call from any coroutine; running coroutines are reported without a
	// stack location because their frames may still be mutating.
	void SnapshotThreads(std::vector<DiagThread> &out);

	// Diagnostic request: walks the wait-for graph (monitor owners, join
	// targets including StructuredTaskScope subtasks, channel send/receive
	// peers) and reports a cycle or an all-blocked state.  Returns true when
	// a deadlock was found; 'cycle' receives the involved coroutines (empty
	// for an all-blocked deadlock) and 'report' the formatted text.
	bool DetectDeadlock(std::vector<DiagThread> &cycle, std::string &report);

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
	// Deterministic mode: single ready queue ordered by (priority, seed rank,
	// enqueue sequence).  All wakeups must go through this helper so ordering
	// is consistent.
	void PushReady(lpCoroutine co, size_t hint);
	// Advances the virtual clock to the earliest pending timer.  Returns false
	// when there is nothing to advance to.
	bool AdvanceVirtualTimeLocked();
	// Stable ids for decision-log events (heap pointers differ per run).
	uint64_t DeterministicObjectId(LPOBJECT_REF obj, lpClass cls);
	void RecordMonitorGrant(lpCoroutine owner, LPOBJECT_REF obj, lpClass cls);
	void ReapTimers();
	void WaitForWork(int64_t timeout_ns);
	int64_t NextDeadline() const;
	void MarkAtSafepoint(bool at);
	void DetachCoroutine(lpCoroutine co);
	bool SetupThread(lpCoroutine co, LPOBJECT_REF thread_obj);
	bool StartThreadInternal(LPOBJECT_REF thread_obj, int kind,
	                        LPOBJECT_REF scope_obj = NULL);

	// Diagnostics.  MaybeRunDiagnostics must be called without holding the
	// scheduler lock: it acquires VMLock first and the scheduler lock second,
	// matching the existing VMLock -> scheduler lock order.
	void MaybeRunDiagnostics(bool idle);
	void ReportDeadlockAndExit(const std::string &report, bool logging_only);
	void ReportStarvationAndExit(const std::string &report, bool logging_only);

	struct Impl;
	Impl *impl;
	int mp;
};
