#include "stdafx.h"
#include "scheduler.h"
#include "deterministic.h"

#include "threadstack.h"
#include "stackframe.h"
#include "context.h"
#include "objectheap.h"
#include "jobject.h"
#include "jclass.h"
#include "jmethod.h"
#include "jni_impl.h"
#include "javaexception.h"
#include "vm/classfiles/classloader.h"
#include "vmmutex.h"
#include "env/env.h"
#include "platform/platform.h"

#include <algorithm>
#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <deque>
#include <map>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

namespace {

int64_t NowNs()
{
	return std::chrono::duration_cast<std::chrono::nanoseconds>(
		std::chrono::steady_clock::now().time_since_epoch()).count();
}

// Deterministic mode substitutes the virtual clock for every scheduler
// deadline computation; the default path is byte-for-byte unchanged.
int64_t SchedulerNowNs()
{
	DarksDeterministic &det = DarksDeterministic::Instance();
	if (det.Enabled())
	{
		return det.NowNs();
	}
	return NowNs();
}

// Ready-queue key in deterministic mode: Java priority first, then the
// seed-derived rank, then the enqueue sequence (FIFO for equal keys).
struct ReadyKey
{
	ReadyKey() : priority(0), rank(0), seq(0) {}
	ReadyKey(int p, uint64_t r, uint64_t s) : priority(p), rank(r), seq(s) {}
	int priority;
	uint64_t rank;
	uint64_t seq;

	bool operator<(const ReadyKey &other) const
	{
		if (priority != other.priority)
		{
			// Java priorities count up: 10 outranks 1.
			return priority > other.priority;
		}
		if (rank != other.rank)
		{
			return rank < other.rank;
		}
		return seq < other.seq;
	}
};

struct Monitor
{
	Monitor()
	{
		owner = NULL;
		count = 0;
	}
	lpCoroutine owner;
	int count;
	std::vector<lpCoroutine> entry;    // waiting to acquire (retry or wait-resume)
	std::vector<lpCoroutine> waiters;  // Object.wait
};

struct WorkerInfo
{
	WorkerInfo()
	{
		at_safepoint = false;
		in_native = false;
		waiting_lock = false;
		index = 0;
	}
	std::atomic<bool> at_safepoint;
	std::atomic<bool> in_native;
	std::atomic<bool> waiting_lock;
	int index;
};

// Structured concurrency registry: one entry per open StructuredTaskScope.
// Children are coroutines, pruned as they terminate; the entry itself is
// removed on close once the last child is gone.
struct ScopeInfo
{
	ScopeInfo()
	{
		closed = false;
	}
	std::vector<lpCoroutine> children;
	bool closed;
};

thread_local lpCoroutine t_co = NULL;
// The coroutine whose OS thread is borrowed by a nested executor (class
// initialisation, string construction).  Thread.currentThread() must keep
// reporting that Java thread while the nested stack runs.
thread_local lpCoroutine t_host_co = NULL;
thread_local WorkerInfo *t_worker = NULL;

std::atomic<bool> g_stw_requested(false);

int EnvParallelism()
{
	const char *value = getenv("DARKS_JVM_MP");
	if (!value || !*value)
	{
		return 1;
	}
	long n = strtol(value, NULL, 10);
	if (n < 1)
	{
		return 1;
	}
	if (n > 64)
	{
		n = 64;
	}
	return static_cast<int>(n);
}

// Virtual-thread pinning diagnostics are opt-in: DARKS_VTHREAD_PINNING=warn
// prints one warning per virtual thread when it enters a native call while
// holding a monitor.  They are off by default because many short natives
// (identityHashCode, currentTimeMillis) are harmless.
bool PinningWarnEnabled()
{
	static int enabled = -1;
	if (enabled < 0)
	{
		const char *value = getenv("DARKS_VTHREAD_PINNING");
		enabled = (value && strcmp(value, "warn") == 0) ? 1 : 0;
	}
	return enabled != 0;
}

// Object layouts store instance fields under "<name>:<descriptor>" (see
// Field::GetSimpleName).  The helpers below accept the bare field name and
// resolve the descriptor-qualified entry.
lpFieldValue FindInstanceField(lpObject obj, const char *name)
{
	if (!obj || !name)
	{
		return NULL;
	}
	std::string key(name);
	FIELD_MAP::iterator it = obj->varants.find(key);
	if (it != obj->varants.end())
	{
		return it->second;
	}
	key += ':';
	size_t key_len = key.size();
	for (it = obj->varants.begin(); it != obj->varants.end(); ++it)
	{
		if (it->first.size() >= key_len && it->first.compare(0, key_len, key) == 0)
		{
			return it->second;
		}
	}
	return NULL;
}

bool GetBoolField(LPOBJECT_REF ref, const char *name, bool &out)
{
	VMLock lk;
	lpObject obj = NULL;
	if (!ref || !VMContext::object_heap || !VMContext::object_heap->GetObject(ref, obj) || !obj)
	{
		return false;
	}
	lpFieldValue fv = FindInstanceField(obj, name);
	if (!fv)
	{
		return false;
	}
	out = fv->field_value.iValue != 0;
	return true;
}

bool GetIntField(LPOBJECT_REF ref, const char *name, int &out)
{
	VMLock lk;
	lpObject obj = NULL;
	if (!ref || !VMContext::object_heap || !VMContext::object_heap->GetObject(ref, obj) || !obj)
	{
		return false;
	}
	lpFieldValue fv = FindInstanceField(obj, name);
	if (!fv)
	{
		return false;
	}
	out = fv->field_value.iValue;
	return true;
}

bool SetInstanceField(LPOBJECT_REF ref, const char *name, const Variant &value)
{
	VMLock lk;
	lpObject obj = NULL;
	if (!ref || !VMContext::object_heap || !VMContext::object_heap->GetObject(ref, obj) || !obj)
	{
		return false;
	}
	lpFieldValue fv = FindInstanceField(obj, name);
	if (!fv)
	{
		return false;
	}
	fv->field_value = value;
	return true;
}

bool SetBoolField(LPOBJECT_REF ref, const char *name, bool value)
{
	Variant v;
	v.iValue = value ? 1 : 0;
	return SetInstanceField(ref, name, v);
}

// ---------------------------------------------------------------------------
// Deadlock / starvation diagnostic helpers.
//
// Everything below assumes VMLock is held (heap is stable) and, unless stated
// otherwise, the scheduler lock.  See Scheduler::MaybeRunDiagnostics for the
// lock order.
// ---------------------------------------------------------------------------
const int kDiagDeadlockExit = 5;
const int kDiagStarvationExit = 6;

const char *DiagStateName(int state)
{
	switch (state)
	{
	case CO_NEW: return "NEW";
	case CO_READY: return "READY";
	case CO_RUNNING: return "RUNNING";
	case CO_SLEEPING: return "TIMED_WAITING";
	case CO_MONITOR: return "BLOCKED";
	case CO_WAITING: return "WAITING";
	case CO_JOINING: return "WAITING_JOIN";
	case CO_TERMINATED: return "TERMINATED";
	default: return "UNKNOWN";
	}
}

// One coroutine in the wait graph.
struct DiagNode
{
	DiagNode()
		: co(NULL), id(0), state(0), wait_kind(CO_WAIT_NONE), target(NULL),
		  target_id(0), timed(false), channel(0), channel_kind(-1)
	{
	}

	lpCoroutine co;
	uint64_t id;
	std::string name;
	int state;
	int wait_kind;
	lpCoroutine target;            // coroutine that has to act for progress
	uint64_t target_id;
	std::string target_name;
	std::string monitor;           // monitor / channel being waited for
	std::vector<std::string> held;
	std::string location;
	bool timed;
	OBJECT_REF channel;            // channel object index (channel waits)
	int channel_kind;
};

struct ChannelWaiterInfo
{
	ChannelWaiterInfo() : channel(0), kind(-1) {}
	OBJECT_REF channel;
	int kind;
};

std::string DiagThreadName(LPOBJECT_REF thread_ref)
{
	if (!thread_ref)
	{
		return "<main>";
	}
	lpObject thread = NULL;
	if (!VMContext::object_heap || !VMContext::object_heap->GetObject(thread_ref, thread) || !thread)
	{
		return "<unknown>";
	}
	lpFieldValue name = FindInstanceField(thread, "name");
	if (!name || !name->field_value.heapPtr)
	{
		return "<unnamed>";
	}
	std::string out;
	if (JavaStringToUtf8(name->field_value.heapPtr, out) && !out.empty())
	{
		return out;
	}
	return "<unnamed>";
}

std::string DiagMonitorText(LPOBJECT_REF ref, lpClass cls)
{
	if (cls)
	{
		std::string text("class ");
		text += (cls->thisClass && cls->thisClass->GetName()) ? cls->thisClass->GetName() : "?";
		return text;
	}
	if (!ref)
	{
		return "<unknown monitor>";
	}
	std::string name("object");
	lpObject obj = NULL;
	if (VMContext::object_heap && VMContext::object_heap->GetObject(ref, obj) && obj &&
	    obj->obj_class && obj->obj_class->thisClass)
	{
		name = obj->obj_class->thisClass->GetName();
	}
	char buf[48];
	snprintf(buf, sizeof(buf), "#%lld", static_cast<long long>(*ref));
	return name + buf;
}

std::string DiagChannelText(OBJECT_REF channel)
{
	long long id = channel;
	long long capacity = 0;
	long long count = 0;
	lpObject obj = NULL;
	if (VMContext::object_heap && channel > 0 &&
	    static_cast<size_t>(channel) < VMContext::object_heap->objs.size())
	{
		obj = VMContext::object_heap->objs[static_cast<size_t>(channel)];
	}
	if (obj)
	{
		lpFieldValue f = FindInstanceField(obj, "id");
		if (f)
		{
			id = f->field_value.lValue;
		}
		f = FindInstanceField(obj, "capacity");
		if (f)
		{
			capacity = f->field_value.iValue;
		}
		f = FindInstanceField(obj, "count");
		if (f)
		{
			count = f->field_value.iValue;
		}
	}
	char buf[96];
	snprintf(buf, sizeof(buf), "channel#%lld(capacity=%lld,count=%lld)", id, capacity, count);
	return buf;
}

// darks.concurrent.Channel blocks on a per-waiter Parker; the Waiter object
// links that parker to the channel and to the operation kind.  One scan of the
// heap builds the parker -> channel map used to classify channel waiters.
void CollectChannelWaiters(std::map<OBJECT_REF, std::vector<ChannelWaiterInfo> > &out)
{
	out.clear();
	if (!VMContext::object_heap)
	{
		return;
	}
	std::vector<lpObject> &objs = VMContext::object_heap->objs;
	for (size_t i = 0; i < objs.size(); i++)
	{
		lpObject obj = objs[i];
		if (!obj || !obj->obj_class || !obj->obj_class->thisClass)
		{
			continue;
		}
		const char *class_name = obj->obj_class->thisClass->GetName();
		if (!class_name || strcmp(class_name, "darks/concurrent/Waiter") != 0)
		{
			continue;
		}
		lpFieldValue parker = FindInstanceField(obj, "parker");
		lpFieldValue channel = FindInstanceField(obj, "channel");
		lpFieldValue kind = FindInstanceField(obj, "kind");
		if (!parker || !channel || !kind)
		{
			continue;
		}
		LPOBJECT_REF parker_ref = parker->field_value.heapPtr;
		LPOBJECT_REF channel_ref = channel->field_value.heapPtr;
		if (!parker_ref || !channel_ref)
		{
			continue;
		}
		ChannelWaiterInfo info;
		info.channel = *channel_ref;
		info.kind = kind->field_value.iValue;
		out[*parker_ref].push_back(info);
	}
}

std::string JoinStrings(const std::vector<std::string> &items, const char *sep)
{
	std::string out;
	for (size_t i = 0; i < items.size(); i++)
	{
		if (i)
		{
			out += sep;
		}
		out += items[i];
	}
	return out;
}

} // namespace

// ---------------------------------------------------------------------------
// Scheduler::Impl
// ---------------------------------------------------------------------------
struct Scheduler::Impl
{
	mutable std::recursive_mutex m;
	std::condition_variable_any cv;
	std::condition_variable_any stw_cv;

	bool running;
	bool initialized;
	int registered;
	// Number of detached worker threads that have been created but not yet
	// finished.  Shutdown must wait for this to reach zero before destroying
	// the scheduler state (a worker still parked on cv/m would otherwise
	// touch freed memory).
	int spawned;

	std::vector<std::deque<lpCoroutine> > queues;
	std::vector<WorkerInfo *> workers;

	// Deterministic mode: a single ready queue ordered by ReadyKey, plus
	// stable object ids for decision-log events.
	bool deterministic;
	std::multimap<ReadyKey, lpCoroutine> ready;
	uint64_t ready_seq;
	std::map<LPOBJECT_REF, uint64_t> object_ids;
	std::map<lpClass, uint64_t> class_ids;
	uint64_t next_object_id;

	std::multimap<int64_t, lpCoroutine> timers;
	std::map<LPOBJECT_REF, Monitor *> monitors;
	std::map<lpClass, Monitor *> class_monitors;
	std::map<LPOBJECT_REF, lpCoroutine> by_thread;
	std::map<LPOBJECT_REF, ScopeInfo *> scopes;

	lpCoroutine main_co;

	bool stw_active;
	int stw_depth;
	uint64_t next_id;

	// Every coroutine that has been created and not yet destroyed.  Used by
	// the diagnostics to enumerate threads without walking the heap.
	std::vector<lpCoroutine> live;

	// Deadlock / starvation watchdog state.
	bool diag_any;
	bool diag_deadlock;
	bool diag_starvation;
	bool diag_exit_on_deadlock;
	bool diag_exit_on_starvation;
	int64_t diag_starvation_ns;
	int64_t diag_confirm_ns;
	std::atomic<int64_t> diag_next_ns;
	std::string diag_pending_sig;
	int64_t diag_pending_since_ns;
	uint64_t monitor_barges;

	Impl()
	{
		running = false;
		initialized = false;
		registered = 0;
		spawned = 0;
		main_co = NULL;
		stw_active = false;
		stw_depth = 0;
		next_id = 1;
		deterministic = false;
		ready_seq = 1;
		next_object_id = 1;
		diag_any = false;
		diag_deadlock = false;
		diag_starvation = false;
		diag_exit_on_deadlock = true;
		diag_exit_on_starvation = true;
		diag_starvation_ns = 1000 * 1000000LL;
		diag_confirm_ns = 150 * 1000000LL;
		diag_next_ns.store(0, std::memory_order_relaxed);
		diag_pending_since_ns = 0;
		monitor_barges = 0;
	}
};

Coroutine::Coroutine()
{
	ts = NULL;
	start_class = NULL;
	start_method = NULL;
	thread_ref = NULL;
	block_obj = NULL;
	block_class = NULL;
	join_target = NULL;
	saved_count = 0;
	state = CO_NEW;
	kind = CO_KIND_PLATFORM;
	priority = 5;
	carrier_id = -1;
	deadline_ns = 0;
	timer_generation = 0;
	ready_since_ns = 0;
	injected = NULL;
	scope_parent = NULL;
	interrupt_flag = false;
	throw_on_resume = false;
	resume_from_wait = false;
	pinned_depth = 0;
	pinning_warned = false;
	monitor_granted = false;
	busy.store(false);
	entered = false;
	finished = false;
	pin_main = false;
	queued = false;
	in_native = false;
	id = 0;
}

// ---------------------------------------------------------------------------
// Lifecycle
// ---------------------------------------------------------------------------
Scheduler::Scheduler()
{
	impl = NULL;
	mp = 1;
}

Scheduler::~Scheduler()
{
	Shutdown();
}

Scheduler &Scheduler::Instance()
{
	static Scheduler *instance = new Scheduler();
	return *instance;
}

void Scheduler::Initialize()
{
	if (impl)
	{
		return;
	}
	impl = new Impl();
	DarksDeterministic &det = DarksDeterministic::Instance();
	det.Initialize();
	// Deterministic mode is single-worker by construction: one ready queue,
	// one dispatcher, no wall-clock races.  DARKS_JVM_MP is ignored so a test
	// harness cannot accidentally reintroduce M:N nondeterminism.
	impl->deterministic = det.Enabled();
	mp = impl->deterministic ? 1 : EnvParallelism();
	impl->queues.resize(static_cast<size_t>(mp));
	impl->workers.assign(static_cast<size_t>(mp), static_cast<WorkerInfo *>(NULL));
	impl->running = true;
	impl->initialized = true;

	// -Ddarks.diag.deadlock / -Ddarks.diag.starvation install the watchdogs.
	// "log" keeps reporting without terminating the process.
	std::string value;
	impl->diag_deadlock = Environment::GetProperty("darks.diag.deadlock", value) &&
		(value == "1" || value == "true" || value == "log");
	impl->diag_exit_on_deadlock = !(impl->diag_deadlock && value == "log");
	impl->diag_starvation = Environment::GetProperty("darks.diag.starvation", value) &&
		(value == "1" || value == "true" || value == "log");
	impl->diag_exit_on_starvation = !(impl->diag_starvation && value == "log");
	if (Environment::GetProperty("darks.diag.starvation.ms", value))
	{
		long ms = strtol(value.c_str(), NULL, 10);
		if (ms > 0)
		{
			impl->diag_starvation_ns = static_cast<int64_t>(ms) * 1000000LL;
		}
	}
	if (Environment::GetProperty("darks.diag.confirm.ms", value))
	{
		long ms = strtol(value.c_str(), NULL, 10);
		if (ms >= 0)
		{
			impl->diag_confirm_ns = static_cast<int64_t>(ms) * 1000000LL;
		}
	}
	impl->diag_any = impl->diag_deadlock || impl->diag_starvation;
}

int Scheduler::Parallelism() const
{
	return mp;
}

void Scheduler::AttachMain(ThreadStack *ts)
{
	if (!impl)
	{
		return;
	}
	if (impl->main_co)
	{
		// A nested VMExecutor (class initialisation, JNI embedding) must not
		// replace the process main coroutine, and it must not disturb the
		// coroutine that is currently running.
		return;
	}
	lpCoroutine co = new Coroutine();
	co->ts = ts;
	co->pin_main = true;
	co->kind = CO_KIND_MAIN;
	co->id = impl->next_id++;
	co->state = CO_READY;
	co->entered = false;
	AddCoroutineRoots(co);

	WorkerInfo *w = new WorkerInfo();
	w->index = 0;
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		impl->workers[0] = w;
		impl->registered++;
		impl->main_co = co;
		impl->live.push_back(co);
	}
	t_worker = w;
	t_co = co;
	if (VMContext::object_heap)
	{
		VMLock lk;
		VMContext::object_heap->AddRootSlot(&VMContext::pending_exception);
	}

	// Start the remaining OS threads lazily; worker 0 is the invoking thread.
	for (int i = 1; i < mp; i++)
	{
		{
			std::lock_guard<std::recursive_mutex> lk(impl->m);
			impl->spawned++;
		}
		std::thread th(&Scheduler::WorkerMain, this, i);
		th.detach();
	}
}

void Scheduler::DetachMain()
{
	if (!impl || !impl->main_co)
	{
		return;
	}
	lpCoroutine co = impl->main_co;
	WorkerInfo *w = t_worker;
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		impl->main_co = NULL;
		if (w)
		{
			impl->workers[0] = NULL;
			impl->registered--;
		}
		impl->live.erase(std::remove(impl->live.begin(), impl->live.end(), co),
		                 impl->live.end());
	}
	t_co = NULL;
	t_worker = NULL;
	if (w)
	{
		delete w;
	}
	if (VMContext::object_heap)
	{
		VMLock lk;
		VMContext::object_heap->RemoveRootSlot(&VMContext::pending_exception);
	}
	RemoveCoroutineRoots(co);
	delete co;
}

void Scheduler::Shutdown()
{
	if (!impl)
	{
		return;
	}
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		impl->running = false;
		impl->cv.notify_all();
		impl->stw_cv.notify_all();
	}
	// Workers exit when they observe running == false; wait until every
	// spawned thread has actually returned before freeing the state they use.
	for (int i = 0; i < 5000; i++)
	{
		bool done;
		{
			std::lock_guard<std::recursive_mutex> lk(impl->m);
			done = impl->spawned <= 0;
		}
		if (done)
		{
			break;
		}
		std::this_thread::sleep_for(std::chrono::milliseconds(1));
	}
	// Reap any coroutine that did not terminate (daemon threads at exit).
	std::vector<lpCoroutine> leftover;
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		for (std::map<LPOBJECT_REF, lpCoroutine>::iterator it = impl->by_thread.begin();
		     it != impl->by_thread.end(); ++it)
		{
			leftover.push_back(it->second);
		}
		impl->by_thread.clear();
		for (std::map<LPOBJECT_REF, Monitor *>::iterator mi = impl->monitors.begin();
		     mi != impl->monitors.end(); ++mi)
		{
			delete mi->second;
		}
		impl->monitors.clear();
		for (std::map<lpClass, Monitor *>::iterator ci = impl->class_monitors.begin();
		     ci != impl->class_monitors.end(); ++ci)
		{
			delete ci->second;
		}
		impl->class_monitors.clear();
		impl->timers.clear();
		for (std::map<LPOBJECT_REF, ScopeInfo *>::iterator si = impl->scopes.begin();
		     si != impl->scopes.end(); ++si)
		{
			delete si->second;
		}
		impl->scopes.clear();
	}
	for (size_t i = 0; i < leftover.size(); i++)
	{
		lpCoroutine co = leftover[i];
		if (co->ts)
		{
			delete co->ts;
			co->ts = NULL;
		}
		RemoveCoroutineRoots(co);
		delete co;
	}
	if (impl->main_co && impl->main_co->thread_ref)
	{
		SetBoolField(impl->main_co->thread_ref, "alive", false);
	}
	if (impl->main_co)
	{
		lpCoroutine co = impl->main_co;
		impl->main_co = NULL;
		RemoveCoroutineRoots(co);
		delete co;
	}
	impl->live.clear();
	if (!impl->workers.empty() && impl->workers[0])
	{
		delete impl->workers[0];
		impl->workers[0] = NULL;
	}
	delete impl;
	impl = NULL;
}

// ---------------------------------------------------------------------------
// Ready queues / timers
// ---------------------------------------------------------------------------
void Scheduler::Enqueue(lpCoroutine co, int hint)
{
	if (!impl || !co)
	{
		return;
	}
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	if (co->queued || co->state == CO_TERMINATED)
	{
		return;
	}
	if (co->pin_main)
	{
		co->state = CO_READY;
		co->ready_since_ns = NowNs();
		impl->cv.notify_all();
		return;
	}
	co->state = CO_READY;
	co->ready_since_ns = NowNs();
	co->queued = true;
	PushReady(co, hint < 0 ? 0 : static_cast<size_t>(hint));
	impl->cv.notify_one();
}

// Inserts a runnable coroutine.  Deterministic mode uses one globally ordered
// queue so the dispatch order depends only on priority, the seed-derived rank
// and the enqueue sequence.
void Scheduler::PushReady(lpCoroutine co, size_t hint)
{
	if (!impl || !co)
	{
		return;
	}
	if (impl->deterministic)
	{
		ReadyKey key(co->priority,
		             DarksDeterministic::Instance().OrderFor(co->id),
		             impl->ready_seq++);
		impl->ready.insert(std::make_pair(key, co));
		co->queued = true;
		return;
	}
	size_t n = impl->queues.size();
	if (n < 1)
	{
		n = 1;
	}
	impl->queues[hint % n].push_back(co);
	co->queued = true;
}

bool Scheduler::TakeReady(int worker_index, lpCoroutine &co)
{
	if (!impl)
	{
		return false;
	}
	if (impl->deterministic)
	{
		while (!impl->ready.empty())
		{
			std::multimap<ReadyKey, lpCoroutine>::iterator it = impl->ready.begin();
			lpCoroutine c = it->second;
			impl->ready.erase(it);
			bool expected = false;
			if (!c->busy.compare_exchange_strong(expected, true))
			{
				continue;
			}
			c->queued = false;
			co = c;
			DarksDeterministic::Instance().Event('S', c->id, 0);
			return true;
		}
		return false;
	}
	int n = static_cast<int>(impl->queues.size());
	if (n < 1)
	{
		return false;
	}
	int idx = worker_index % n;
	for (int k = 0; k < n; k++)
	{
		int j = (idx + k) % n;
		std::deque<lpCoroutine> &dq = impl->queues[static_cast<size_t>(j)];
		if (dq.empty())
		{
			continue;
		}
		// Prefer the local queue's front; steal from another worker's back.
		lpCoroutine c = (k == 0) ? dq.front() : dq.back();
		// A coroutine may have been made runnable (monitor handoff, interrupt,
		// yield) while its previous worker is still unwinding RunOne.  Claim it
		// atomically so two OS threads never execute the same Java thread.
		bool expected = false;
		if (!c->busy.compare_exchange_strong(expected, true))
		{
			continue;
		}
		if (k == 0)
		{
			dq.pop_front();
		}
		else
		{
			dq.pop_back();
		}
		c->queued = false;
		c->ready_since_ns = 0;
		co = c;
		return true;
	}
	return false;
}

void Scheduler::ReapTimers()
{
	if (!impl)
	{
		return;
	}
	int64_t now = SchedulerNowNs();
	std::vector<lpCoroutine> expired;
	std::vector<int64_t> expired_deadline;
	for (std::multimap<int64_t, lpCoroutine>::iterator it = impl->timers.begin();
	     it != impl->timers.end();)
	{
		if (it->first > now)
		{
			break;
		}
		lpCoroutine co = it->second;
		expired.push_back(co);
		expired_deadline.push_back(it->first);
		it = impl->timers.erase(it);
	}
	for (size_t i = 0; i < expired.size(); i++)
	{
		lpCoroutine co = expired[i];
		if (co->deadline_ns == 0 || co->deadline_ns > now)
		{
			continue;
		}
		DarksDeterministic::Instance().Event('T', co->id,
		                                     static_cast<uint64_t>(expired_deadline[i]));
		co->deadline_ns = 0;
		if (co->state == CO_SLEEPING)
		{
			Enqueue(co, static_cast<int>(co->id));
		}
		else if (co->state == CO_JOINING)
		{
			// Timed join expired: detach from the target's joiner list so a
			// later termination does not enqueue this coroutine twice.
			if (co->join_target)
			{
				std::vector<lpCoroutine> &js = co->join_target->joiners;
				js.erase(std::remove(js.begin(), js.end(), co), js.end());
				co->join_target = NULL;
			}
			Enqueue(co, static_cast<int>(co->id));
		}
		else if (co->state == CO_WAITING)
		{
			Monitor *mon = NULL;
			std::map<LPOBJECT_REF, Monitor *>::iterator mi =
				impl->monitors.find(co->block_obj);
			if (mi != impl->monitors.end())
			{
				mon = mi->second;
			}
			if (mon)
			{
				mon->waiters.erase(std::remove(mon->waiters.begin(), mon->waiters.end(), co),
				                   mon->waiters.end());
				co->resume_from_wait = true;
				mon->entry.push_back(co);
				if (!mon->owner)
				{
					// grant immediately
					lpCoroutine next = mon->entry.front();
					mon->entry.erase(mon->entry.begin());
					mon->owner = next;
					RecordMonitorGrant(next, co->block_obj, NULL);
					mon->count = next->resume_from_wait ? next->saved_count : 1;
					// A wait-resumer has no monitorenter left to retry, so it
					// must not skip the recursion count on its next entry.
					next->monitor_granted = !next->resume_from_wait;
					next->resume_from_wait = false;
					Enqueue(next, static_cast<int>(next->id));
				}
			}
			else
			{
				Enqueue(co, static_cast<int>(co->id));
			}
		}
	}
}

// Deterministic mode never waits for a wall-clock timer: when nothing is
// runnable the virtual clock jumps to the earliest deadline, so
// Thread.sleep(1h) completes immediately.
bool Scheduler::AdvanceVirtualTimeLocked()
{
	if (!impl || !impl->deterministic || impl->timers.empty())
	{
		return false;
	}
	DarksDeterministic &det = DarksDeterministic::Instance();
	int64_t next = impl->timers.begin()->first;
	if (next <= det.NowNs())
	{
		return false;
	}
	det.AdvanceTo(next);
	return true;
}

uint64_t Scheduler::DeterministicObjectId(LPOBJECT_REF obj, lpClass cls)
{
	if (!impl)
	{
		return 0;
	}
	if (cls)
	{
		std::map<lpClass, uint64_t>::iterator it = impl->class_ids.find(cls);
		if (it != impl->class_ids.end())
		{
			return it->second;
		}
		uint64_t id = impl->next_object_id++;
		impl->class_ids[cls] = id;
		return id;
	}
	if (!obj)
	{
		return 0;
	}
	std::map<LPOBJECT_REF, uint64_t>::iterator it = impl->object_ids.find(obj);
	if (it != impl->object_ids.end())
	{
		return it->second;
	}
	uint64_t id = impl->next_object_id++;
	impl->object_ids[obj] = id;
	return id;
}

void Scheduler::RecordMonitorGrant(lpCoroutine owner, LPOBJECT_REF obj, lpClass cls)
{
	if (!impl || !owner)
	{
		return;
	}
	uint64_t id = DeterministicObjectId(obj, cls);
	DarksDeterministic::Instance().Event('M', id, owner->id);
}

void Scheduler::WaitForWork(int64_t)
{
	// One millisecond polling keeps timer latency low without a dedicated
	// timer thread; sleep/wait deadlines are honoured in that granularity.
	std::unique_lock<std::recursive_mutex> lk(impl->m);
	if (!impl->running)
	{
		return;
	}
	ReapTimers();
	if (AdvanceVirtualTimeLocked())
	{
		return;
	}
	if (t_worker)
	{
		t_worker->at_safepoint = true;
		impl->stw_cv.notify_all();
	}
	impl->cv.wait_for(lk, std::chrono::milliseconds(1));
	// Keep reporting "at safepoint" until a collection that is already in
	// progress has finished: clearing the flag early would let this worker
	// touch the heap while background marking/sweeping runs.
	while (impl->stw_active && impl->running)
	{
		impl->stw_cv.wait_for(lk, std::chrono::milliseconds(2));
	}
	if (t_worker)
	{
		t_worker->at_safepoint = false;
	}
}

// ---------------------------------------------------------------------------
// Monitor helpers (all called with impl->m held)
// ---------------------------------------------------------------------------
namespace {
Monitor *GetMonitor(std::map<LPOBJECT_REF, Monitor *> &map, LPOBJECT_REF key,
                    std::map<lpClass, Monitor *> &class_map, lpClass cls)
{
	if (cls)
	{
		std::map<lpClass, Monitor *>::iterator it = class_map.find(cls);
		if (it != class_map.end())
		{
			return it->second;
		}
		Monitor *mon = new Monitor();
		class_map[cls] = mon;
		return mon;
	}
	std::map<LPOBJECT_REF, Monitor *>::iterator it = map.find(key);
	if (it != map.end())
	{
		return it->second;
	}
	Monitor *mon = new Monitor();
	map[key] = mon;
	return mon;
}

bool MonitorEmpty(Monitor *mon)
{
	return mon && !mon->owner && mon->entry.empty() && mon->waiters.empty();
}

void EraseMonitor(std::map<LPOBJECT_REF, Monitor *> &map,
                  std::map<lpClass, Monitor *> &class_map, LPOBJECT_REF key,
                  lpClass cls, Monitor *mon)
{
	if (!MonitorEmpty(mon))
	{
		return;
	}
	if (cls)
	{
		std::map<lpClass, Monitor *>::iterator it = class_map.find(cls);
		if (it != class_map.end() && it->second == mon)
		{
			class_map.erase(it);
			delete mon;
		}
		return;
	}
	std::map<LPOBJECT_REF, Monitor *>::iterator it = map.find(key);
	if (it != map.end() && it->second == mon)
	{
		map.erase(it);
		delete mon;
	}
}
} // namespace

// Public wrappers; the scheduler lock is recursive so nested helper calls are
// safe.  Granting is implemented inline in the functions below.
bool Scheduler::MonitorEnter(StackFrame *frame, LPOBJECT_REF obj)
{
	if (!impl || !frame || !obj)
	{
		return true;
	}
	lpCoroutine co = Current();
	if (!co)
	{
		return true;
	}
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	Monitor *mon = GetMonitor(impl->monitors, obj, impl->class_monitors, NULL);
	if (!mon->owner)
	{
		// Barging: the monitor was free while coroutines were queued for it.
		// The handoff code normally prevents this, but a recursive re-entry or
		// a lookup race can still bypass the queue.
		if (!mon->entry.empty())
		{
			impl->monitor_barges++;
		}
		mon->owner = co;
		mon->count = 1;
		frame->monitors_held.push_back(std::make_pair(obj, static_cast<lpClass>(NULL)));
		return true;
	}
	if (mon->owner == co)
	{
		if (!mon->entry.empty())
		{
			impl->monitor_barges++;
		}
		if (co->monitor_granted)
		{
			// Ownership was handed off by the releasing thread; its recursion
			// count is already recorded.
			co->monitor_granted = false;
		}
		else
		{
			mon->count++;
		}
		frame->monitors_held.push_back(std::make_pair(obj, static_cast<lpClass>(NULL)));
		return true;
	}
	co->state = CO_MONITOR;
	co->block_obj = obj;
	co->block_class = NULL;
	mon->entry.push_back(co);
	return false;
}

bool Scheduler::MonitorEnterClass(StackFrame *frame, lpClass clazz)
{
	if (!impl || !frame || !clazz)
	{
		return true;
	}
	lpCoroutine co = Current();
	if (!co)
	{
		return true;
	}
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	Monitor *mon = GetMonitor(impl->monitors, NULL, impl->class_monitors, clazz);
	if (!mon->owner)
	{
		if (!mon->entry.empty())
		{
			impl->monitor_barges++;
		}
		mon->owner = co;
		mon->count = 1;
		frame->monitors_held.push_back(std::make_pair(static_cast<LPOBJECT_REF>(NULL), clazz));
		return true;
	}
	if (mon->owner == co)
	{
		if (!mon->entry.empty())
		{
			impl->monitor_barges++;
		}
		if (co->monitor_granted)
		{
			co->monitor_granted = false;
		}
		else
		{
			mon->count++;
		}
		frame->monitors_held.push_back(std::make_pair(static_cast<LPOBJECT_REF>(NULL), clazz));
		return true;
	}
	co->state = CO_MONITOR;
	co->block_obj = NULL;
	co->block_class = clazz;
	mon->entry.push_back(co);
	return false;
}

void Scheduler::MonitorExit(LPOBJECT_REF obj)
{
	if (!impl || !obj)
	{
		return;
	}
	lpCoroutine co = Current();
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	std::map<LPOBJECT_REF, Monitor *>::iterator it = impl->monitors.find(obj);
	if (it == impl->monitors.end())
	{
		return;
	}
	Monitor *mon = it->second;
	if (!co || mon->owner != co)
	{
		return;
	}
	if (mon->count > 1)
	{
		mon->count--;
		return;
	}
	mon->count = 0;
	mon->owner = NULL;
	lpCoroutine next = NULL;
	if (!mon->entry.empty())
	{
		next = mon->entry.front();
		mon->entry.erase(mon->entry.begin());
		mon->owner = next;
		RecordMonitorGrant(next, obj, NULL);
		mon->count = next->resume_from_wait ? next->saved_count : 1;
		if (mon->count < 1) mon->count = 1;
		next->monitor_granted = !next->resume_from_wait;
		next->resume_from_wait = false;
		next->block_obj = NULL;
		next->block_class = NULL;
		next->state = CO_READY;
		next->ready_since_ns = NowNs();
	}
	if (next)
	{		if (next->pin_main)
		{
			impl->cv.notify_all();
		}
		else
		{
			PushReady(next, static_cast<size_t>(next->id));
			impl->cv.notify_one();
		}
	}
	EraseMonitor(impl->monitors, impl->class_monitors, obj, NULL, mon);
}

void Scheduler::MonitorExitClass(lpClass clazz)
{
	if (!impl || !clazz)
	{
		return;
	}
	lpCoroutine co = Current();
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	std::map<lpClass, Monitor *>::iterator it = impl->class_monitors.find(clazz);
	if (it == impl->class_monitors.end())
	{
		return;
	}
	Monitor *mon = it->second;
	if (!co || mon->owner != co)
	{
		return;
	}
	if (mon->count > 1)
	{
		mon->count--;
		return;
	}
	mon->count = 0;
	mon->owner = NULL;
	if (!mon->entry.empty())
	{
		lpCoroutine next = mon->entry.front();
		mon->entry.erase(mon->entry.begin());
		mon->owner = next;
		RecordMonitorGrant(next, NULL, clazz);
		mon->count = next->resume_from_wait ? next->saved_count : 1;
		if (mon->count < 1) mon->count = 1;
		next->monitor_granted = !next->resume_from_wait;
		next->resume_from_wait = false;
		next->block_obj = NULL;
		next->block_class = NULL;
		next->state = CO_READY;
		if (next->pin_main)
		{
			impl->cv.notify_all();
		}
		else
		{
			PushReady(next, static_cast<size_t>(next->id));
			impl->cv.notify_one();
		}
	}
	EraseMonitor(impl->monitors, impl->class_monitors, NULL, clazz, mon);
}

void Scheduler::ReleaseFrameMonitors(StackFrame *frame)
{
	if (!frame)
	{
		return;
	}
	while (!frame->monitors_held.empty())
	{
		std::pair<LPOBJECT_REF, lpClass> key = frame->monitors_held.back();
		frame->monitors_held.pop_back();
		if (key.second)
		{
			MonitorExitClass(key.second);
		}
		else
		{
			MonitorExit(key.first);
		}
	}
}

// ---------------------------------------------------------------------------
// Blocking primitives
// ---------------------------------------------------------------------------
bool Scheduler::SleepCurrent(ThreadStack *ts, int64_t millis)
{
	lpCoroutine co = Current();
	if (!co)
	{
		if (millis > 0)
		{
			std::this_thread::sleep_for(std::chrono::milliseconds(millis));
		}
		return false;
	}
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		if (co->interrupt_flag)
		{
			co->interrupt_flag = false;
		}
		else
		{
			if (millis <= 0 || co->injected)
			{
				return false;
			}
			co->deadline_ns = SchedulerNowNs() + millis * 1000000LL;
			co->timer_generation++;
			impl->timers.insert(std::make_pair(co->deadline_ns, co));
			co->state = CO_SLEEPING;
			return true;
		}
	}
	// Build the exception outside the scheduler lock: creating Java objects
	// takes the heap mutex, and the collection path takes the locks in the
	// opposite order.
	// The interrupt arrived while this coroutine was runnable, so it must be
	// raised at the sleep call site rather than parked for the next resume.
	ts->injected_exception = CreateJavaException("java/lang/InterruptedException",
	                                             "sleep interrupted");
	return false;
}

bool Scheduler::YieldCurrent(ThreadStack *)
{
	lpCoroutine co = Current();
	if (!co)
	{
		return false;
	}
	if (!impl)
	{
		return false;
	}
	// Leave the state as CO_RUNNING: RunOne requeues the coroutine only after
	// it has actually stopped executing, which keeps it from being picked up
	// concurrently by another worker.
	return true;
}

bool Scheduler::JoinCurrent(ThreadStack *ts, LPOBJECT_REF target, int64_t millis)
{
	lpCoroutine co = Current();
	if (!co || !impl)
	{
		return false;
	}
	std::unique_lock<std::recursive_mutex> lk(impl->m);
	std::map<LPOBJECT_REF, lpCoroutine>::iterator it = impl->by_thread.find(target);
	if (it == impl->by_thread.end())
	{
		return false;
	}
	lpCoroutine target_co = it->second;
	if (target_co == co || target_co->state == CO_TERMINATED || target_co->finished)
	{
		return false;
	}
	if (co->interrupt_flag)
	{
		co->interrupt_flag = false;
		lk.unlock();
		// Raise at the join call site: the interrupt was recorded while this
		// coroutine was still runnable.
		ts->injected_exception = CreateJavaException("java/lang/InterruptedException",
		                                             "join interrupted");
		return false;
	}
	co->join_target = target_co;
	target_co->joiners.push_back(co);
	co->state = CO_JOINING;
	if (millis > 0)
	{
		co->deadline_ns = SchedulerNowNs() + millis * 1000000LL;
		co->timer_generation++;
		impl->timers.insert(std::make_pair(co->deadline_ns, co));
	}
	return true;
}

bool Scheduler::WaitCurrent(ThreadStack *ts, LPOBJECT_REF obj, int64_t millis)
{
	lpCoroutine co = Current();
	if (!co || !impl)
	{
		return false;
	}
	std::unique_lock<std::recursive_mutex> lk(impl->m);
	std::map<LPOBJECT_REF, Monitor *>::iterator it = impl->monitors.find(obj);
	Monitor *mon = it == impl->monitors.end() ? NULL : it->second;
	if (!mon || mon->owner != co)
	{
		lk.unlock();
		VMContext::pending_exception = CreateJavaException(
			"java/lang/IllegalMonitorStateException", "object not locked by thread");
		return false;
	}
	if (co->interrupt_flag)
	{
		co->interrupt_flag = false;
		lk.unlock();
		// Raise at the wait call site: the interrupt was recorded while this
		// coroutine was still runnable.
		ts->injected_exception = CreateJavaException("java/lang/InterruptedException",
		                                             "wait interrupted");
		return false;
	}
	co->saved_count = mon->count;
	mon->owner = NULL;
	mon->count = 0;
	co->resume_from_wait = true;
	co->block_obj = obj;
	co->state = CO_WAITING;
	mon->waiters.push_back(co);
	// Let a coroutine blocked on monitorenter proceed while we wait.
	if (!mon->entry.empty())
	{
		lpCoroutine next = mon->entry.front();
		mon->entry.erase(mon->entry.begin());
		mon->owner = next;
		RecordMonitorGrant(next, obj, NULL);
		mon->count = next->resume_from_wait ? next->saved_count : 1;
		if (mon->count < 1) mon->count = 1;
		next->monitor_granted = !next->resume_from_wait;
		next->resume_from_wait = false;
		next->block_obj = NULL;
		next->block_class = NULL;
		// Publish the wakeup before making the coroutine runnable.  The main
		// coroutine is resumed by PumpMain only when its state is CO_READY, so
		// omitting this left main parked in CO_MONITOR forever when it
		// happened to be the next entry-queue owner at a wait() handoff.
		next->state = CO_READY;
		next->ready_since_ns = NowNs();
		if (next->pin_main)
		{
			impl->cv.notify_all();
		}
		else
		{
			PushReady(next, static_cast<size_t>(next->id));
			impl->cv.notify_one();
		}
	}
	if (millis > 0)
	{
		co->deadline_ns = SchedulerNowNs() + millis * 1000000LL;
		co->timer_generation++;
		impl->timers.insert(std::make_pair(co->deadline_ns, co));
	}
	return true;
}

bool Scheduler::NotifyCurrent(ThreadStack *ts, LPOBJECT_REF obj, bool all)
{
	(void)ts;
	lpCoroutine co = Current();
	if (!co || !impl)
	{
		return false;
	}
	std::unique_lock<std::recursive_mutex> lk(impl->m);
	std::map<LPOBJECT_REF, Monitor *>::iterator it = impl->monitors.find(obj);
	Monitor *mon = it == impl->monitors.end() ? NULL : it->second;
	if (!mon || mon->owner != co)
	{
		lk.unlock();
		VMContext::pending_exception = CreateJavaException(
			"java/lang/IllegalMonitorStateException", "object not locked by thread");
		return false;
	}
	if (mon->waiters.empty())
	{
		return true;
	}
	size_t count = all ? mon->waiters.size() : 1;
	for (size_t i = 0; i < count; i++)
	{
		lpCoroutine w = mon->waiters.front();
		mon->waiters.erase(mon->waiters.begin());
		w->deadline_ns = 0;
		w->resume_from_wait = true;
		mon->entry.push_back(w);
	}
	return true;
}

// ---------------------------------------------------------------------------
// Interpreter notifications
// ---------------------------------------------------------------------------
bool Scheduler::PollSafepoint(ThreadStack *)
{
	if (!g_stw_requested.load(std::memory_order_acquire))
	{
		return false;
	}
	if (!impl || !t_worker)
	{
		return false;
	}
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	if (!impl->stw_active)
	{
		return false;
	}
	t_worker->at_safepoint = true;
	impl->stw_cv.notify_all();
	return true;
}

bool Scheduler::FairnessYield(ThreadStack *)
{
	if (!impl)
	{
		return false;
	}
	lpCoroutine co = Current();
	if (!co || co->pin_main)
	{
		return false;
	}
	// See YieldCurrent: the owning worker requeues after suspending.
	return true;
}

void Scheduler::EnterNative()
{
	// Only atomics are touched here: a native call may be made while the
	// caller already holds the heap mutex, so taking the scheduler lock would
	// invert the lock order.  The stop-the-world loop polls the flag.
	WorkerInfo *w = t_worker;
	if (w)
	{
		w->in_native = true;
		if (impl)
		{
			impl->stw_cv.notify_all();
		}
	}
	lpCoroutine co = t_co;
	if (co && co->kind == CO_KIND_VIRTUAL)
	{
		co->pinned_depth++;
		if (PinningWarnEnabled() && !co->pinning_warned && co->ts &&
		    co->ts->HoldsMonitor())
		{
			fprintf(stderr,
			        "darks-jvm: virtual thread %llu is pinned in a native call "
			        "while holding a monitor\n",
			        static_cast<unsigned long long>(co->id));
			co->pinning_warned = true;
		}
	}
}

void Scheduler::ExitNative()
{
	WorkerInfo *w = t_worker;
	if (w)
	{
		w->in_native = false;
	}
	lpCoroutine co = t_co;
	if (co && co->kind == CO_KIND_VIRTUAL && co->pinned_depth > 0)
	{
		co->pinned_depth--;
	}
}

// ---------------------------------------------------------------------------
// Stop-the-world
// ---------------------------------------------------------------------------
void Scheduler::SafepointBegin()
{
	if (!impl || !t_worker)
	{
		return;
	}
	std::unique_lock<std::recursive_mutex> lk(impl->m);
	if (impl->stw_active)
	{
		impl->stw_depth++;
		return;
	}
	impl->stw_active = true;
	impl->stw_depth++;
	g_stw_requested.store(true, std::memory_order_release);
	// Fixed GC point: record it so two runs can be compared on where the
	// collector stopped the world.  With one deterministic worker the
	// handshake below completes without any wall-clock wait.
	DarksDeterministic::Instance().Event('G', 1, 0);
	t_worker->at_safepoint = true;
	impl->stw_cv.notify_all();
	while (impl->running)
	{
		bool all = true;
		for (size_t i = 0; i < impl->workers.size(); i++)
		{
			WorkerInfo *w = impl->workers[i];
			if (!w)
			{
				continue;
			}
			if (!w->at_safepoint.load(std::memory_order_acquire) &&
			    !w->in_native.load(std::memory_order_acquire) &&
			    !w->waiting_lock.load(std::memory_order_acquire))
			{
				all = false;
				break;
			}
		}
		if (all)
		{
			break;
		}
		impl->stw_cv.wait_for(lk, std::chrono::milliseconds(2));
	}
}

void Scheduler::SafepointEnd()
{
	if (!impl)
	{
		return;
	}
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	if (!impl->stw_active)
	{
		return;
	}
	g_stw_requested.store(false, std::memory_order_release);
	if (impl->stw_depth > 0)
	{
		impl->stw_depth--;
	}
	if (impl->stw_depth == 0)
	{
		impl->stw_active = false;
		if (t_worker)
		{
			t_worker->at_safepoint = false;
		}
	}
	impl->stw_cv.notify_all();
	impl->cv.notify_all();
}

void Scheduler::EnterVMLockWait()
{
	WorkerInfo *w = t_worker;
	if (w)
	{
		w->waiting_lock.store(true, std::memory_order_release);
	}
}

void Scheduler::ExitVMLockWait()
{
	WorkerInfo *w = t_worker;
	if (w)
	{
		w->waiting_lock.store(false, std::memory_order_release);
	}
}

// ---------------------------------------------------------------------------
// Coroutine roots
// ---------------------------------------------------------------------------
void Scheduler::AddCoroutineRoots(lpCoroutine co)
{
	if (!co || !VMContext::object_heap)
	{
		return;
	}
	VMLock lk;
	VMContext::object_heap->AddRootSlot(&co->thread_ref);
	VMContext::object_heap->AddRootSlot(&co->block_obj);
	VMContext::object_heap->AddRootSlot(&co->injected);
	VMContext::object_heap->AddRootSlot(&co->scope_parent);
}

void Scheduler::RemoveCoroutineRoots(lpCoroutine co)
{
	if (!co || !VMContext::object_heap)
	{
		return;
	}
	VMLock lk;
	VMContext::object_heap->RemoveRootSlot(&co->thread_ref);
	VMContext::object_heap->RemoveRootSlot(&co->block_obj);
	VMContext::object_heap->RemoveRootSlot(&co->injected);
	VMContext::object_heap->RemoveRootSlot(&co->scope_parent);
}

void Scheduler::WakeJoiners(lpCoroutine co)
{
	if (!impl || !co)
	{
		return;
	}
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	for (size_t i = 0; i < co->joiners.size(); i++)
	{
		lpCoroutine joiner = co->joiners[i];
		if (joiner && joiner->join_target == co)
		{
			joiner->join_target = NULL;
			joiner->deadline_ns = 0;
			joiner->state = CO_READY;
			joiner->ready_since_ns = NowNs();
			if (joiner->pin_main)
			{
				impl->cv.notify_all();
			}
			else if (!joiner->queued)
			{
				PushReady(joiner, static_cast<size_t>(joiner->id));
				impl->cv.notify_one();
			}
		}
	}
	co->joiners.clear();
}

// ---------------------------------------------------------------------------
// Thread lifecycle
// ---------------------------------------------------------------------------
bool Scheduler::StartThread(LPOBJECT_REF thread_obj)
{
	return StartThreadInternal(thread_obj, CO_KIND_PLATFORM);
}

bool Scheduler::StartVirtualThread(LPOBJECT_REF thread_obj)
{
	return StartThreadInternal(thread_obj, CO_KIND_VIRTUAL);
}

bool Scheduler::StartScopedThread(LPOBJECT_REF thread_obj, LPOBJECT_REF scope_obj)
{
	if (!scope_obj)
	{
		return false;
	}
	bool virtual_thread = true;
	GetBoolField(thread_obj, "virtual", virtual_thread);
	return StartThreadInternal(thread_obj, virtual_thread ? CO_KIND_VIRTUAL : CO_KIND_PLATFORM,
	                           scope_obj);
}

void Scheduler::ScopeShutdown(LPOBJECT_REF scope_obj)
{
	if (!impl || !scope_obj)
	{
		return;
	}
	// Collect targets under the scheduler lock, then interrupt each one via
	// InterruptThread: it allocates the InterruptedException outside the lock
	// and installs it through the injected_exception path, so a parked child
	// resumes by throwing at its blocking call site.
	std::vector<LPOBJECT_REF> targets;
	lpCoroutine self = Current();
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		std::map<LPOBJECT_REF, ScopeInfo *>::iterator si = impl->scopes.find(scope_obj);
		if (si == impl->scopes.end())
		{
			return;
		}
		for (size_t i = 0; i < si->second->children.size(); i++)
		{
			lpCoroutine child = si->second->children[i];
			if (!child || child == self || child->finished ||
			    child->state == CO_TERMINATED || !child->thread_ref)
			{
				continue;
			}
			targets.push_back(child->thread_ref);
		}
	}
	for (size_t i = 0; i < targets.size(); i++)
	{
		InterruptThread(targets[i]);
	}
}

int Scheduler::ScopeLiveChildren(LPOBJECT_REF scope_obj)
{
	if (!impl || !scope_obj)
	{
		return 0;
	}
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	std::map<LPOBJECT_REF, ScopeInfo *>::iterator si = impl->scopes.find(scope_obj);
	if (si == impl->scopes.end())
	{
		return 0;
	}
	int live = 0;
	for (size_t i = 0; i < si->second->children.size(); i++)
	{
		lpCoroutine child = si->second->children[i];
		if (child && !child->finished && child->state != CO_TERMINATED)
		{
			live++;
		}
	}
	return live;
}

int Scheduler::ScopeClose(LPOBJECT_REF scope_obj)
{
	if (!impl || !scope_obj)
	{
		return 0;
	}
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	std::map<LPOBJECT_REF, ScopeInfo *>::iterator si = impl->scopes.find(scope_obj);
	if (si == impl->scopes.end())
	{
		return 0;
	}
	si->second->closed = true;
	int live = static_cast<int>(si->second->children.size());
	if (live == 0)
	{
		delete si->second;
		impl->scopes.erase(si);
	}
	return live;
}

LPOBJECT_REF Scheduler::CurrentScope()
{
	lpCoroutine co = Current();
	return co ? co->scope_parent : NULL;
}

bool Scheduler::StartThreadInternal(LPOBJECT_REF thread_obj, int kind,
                                    LPOBJECT_REF scope_obj)
{
	if (!impl || !thread_obj || !VMContext::object_heap)
	{
		return false;
	}
	lpObject obj = NULL;
	if (!VMContext::object_heap->GetObject(thread_obj, obj) || !obj || !obj->obj_class)
	{
		return false;
	}
	lpMethod run_method = NULL;
	if (!obj->obj_class->FindMethod(const_cast<char *>("run:()V"), run_method) ||
	    !run_method)
	{
		return false;
	}
	if ((run_method->accFlag & ACM_ABSTRACT) != 0 || !run_method->code_attr)
	{
		return false;
	}
	int priority = 5;
	GetIntField(thread_obj, "priority", priority);
	ThreadStack *ts = new ThreadStack();
	lpCoroutine co = new Coroutine();
	co->ts = ts;
	co->thread_ref = thread_obj;
	co->start_method = run_method;
	co->start_class = run_method->clazz ? run_method->clazz : obj->obj_class;
	co->kind = kind;
	co->priority = priority;
	co->id = impl->next_id++;
	co->state = CO_READY;
	co->entered = false;
	co->scope_parent = scope_obj;
	AddCoroutineRoots(co);
	bool rejected = false;
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		if (scope_obj)
		{
			std::map<LPOBJECT_REF, ScopeInfo *>::iterator si =
				impl->scopes.find(scope_obj);
			if (si == impl->scopes.end())
			{
				ScopeInfo *info = new ScopeInfo();
				impl->scopes[scope_obj] = info;
				si = impl->scopes.find(scope_obj);
			}
			if (si->second->closed)
			{
				rejected = true;
			}
			else
			{
				si->second->children.push_back(co);
			}
		}
		if (!rejected)
		{
			impl->by_thread[thread_obj] = co;
			impl->live.push_back(co);
		}
	}
	if (rejected)
	{
		RemoveCoroutineRoots(co);
		delete ts;
		delete co;
		return false;
	}
	SetBoolField(thread_obj, "alive", true);
	SetBoolField(thread_obj, "started", true);
	Enqueue(co, static_cast<int>(co->id));
	return true;
}

void Scheduler::InterruptThread(LPOBJECT_REF thread_obj)
{
	if (!impl || !thread_obj)
	{
		return;
	}
	// Prepare the exception before taking the scheduler lock: allocating Java
	// objects takes the heap mutex, which must not be acquired under it.
	LPOBJECT_REF exc = CreateJavaException("java/lang/InterruptedException",
	                                       "interrupted");
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	lpCoroutine co = NULL;
	std::map<LPOBJECT_REF, lpCoroutine>::iterator it = impl->by_thread.find(thread_obj);
	if (it != impl->by_thread.end())
	{
		co = it->second;
	}
	else if (impl->main_co && impl->main_co->thread_ref == thread_obj)
	{
		// The process main coroutine is not in by_thread.
		co = impl->main_co;
	}
	if (!co)
	{
		return;
	}
	co->interrupt_flag = true;
	if (co->state == CO_SLEEPING || co->state == CO_JOINING)
	{
		co->interrupt_flag = false;
		co->deadline_ns = 0;
		co->throw_on_resume = true;
		if (co->state == CO_JOINING && co->join_target)
		{
			// Stop waiting on the target; WakeJoiners must not enqueue us
			// again after the InterruptedException has been raised.
			std::vector<lpCoroutine> &js = co->join_target->joiners;
			js.erase(std::remove(js.begin(), js.end(), co), js.end());
			co->join_target = NULL;
		}
		co->injected = exc;
		co->state = CO_READY;
		co->ready_since_ns = NowNs();
		if (co->pin_main)
		{
			impl->cv.notify_all();
		}
		else if (!co->queued)
		{
			PushReady(co, static_cast<size_t>(co->id));
			impl->cv.notify_one();
		}
	}
	else if (co->state == CO_WAITING)
	{
		co->interrupt_flag = false;
		std::map<LPOBJECT_REF, Monitor *>::iterator mi = impl->monitors.find(co->block_obj);
		if (mi != impl->monitors.end())
		{
			Monitor *mon = mi->second;
			mon->waiters.erase(std::remove(mon->waiters.begin(), mon->waiters.end(), co),
			                   mon->waiters.end());
			co->deadline_ns = 0;
			co->resume_from_wait = true;
			co->throw_on_resume = true;
			co->injected = exc;
			mon->entry.push_back(co);
			if (!mon->owner)
			{
				// Nobody else holds the monitor, so grant it now; otherwise the
				// interrupted waiter would sit in the entry queue forever.
				lpCoroutine next = mon->entry.front();
				mon->entry.erase(mon->entry.begin());
				mon->owner = next;
				RecordMonitorGrant(next, co->block_obj, NULL);
				mon->count = next->resume_from_wait ? next->saved_count : 1;
				if (mon->count < 1)
				{
					mon->count = 1;
				}
				next->monitor_granted = !next->resume_from_wait;
				next->resume_from_wait = false;
				next->block_obj = NULL;
				next->block_class = NULL;
				next->state = CO_READY;
				Enqueue(next, static_cast<int>(next->id));
			}
		}
	}
}

bool Scheduler::IsInterrupted(LPOBJECT_REF thread_obj, bool clear)
{
	if (!impl || !thread_obj)
	{
		return false;
	}
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	lpCoroutine co = Current();
	if (co && co->thread_ref == thread_obj)
	{
		bool value = co->interrupt_flag;
		if (clear)
		{
			co->interrupt_flag = false;
		}
		return value;
	}
	std::map<LPOBJECT_REF, lpCoroutine>::iterator it = impl->by_thread.find(thread_obj);
	lpCoroutine target_co = NULL;
	if (it != impl->by_thread.end())
	{
		target_co = it->second;
	}
	else if (impl->main_co && impl->main_co->thread_ref == thread_obj)
	{
		target_co = impl->main_co;
	}
	if (!target_co)
	{
		return false;
	}
	bool value = target_co->interrupt_flag;
	if (clear)
	{
		target_co->interrupt_flag = false;
	}
	return value;
}

bool Scheduler::ThreadAlive(LPOBJECT_REF thread_obj)
{
	bool alive = false;
	if (GetBoolField(thread_obj, "alive", alive))
	{
		return alive;
	}
	return false;
}

// ---------------------------------------------------------------------------
// Execution
// ---------------------------------------------------------------------------
lpCoroutine Scheduler::Current()
{
	return t_co;
}

lpCoroutine Scheduler::CurrentJavaThread()
{
	return t_co ? t_co : t_host_co;
}

ThreadStack *Scheduler::CurrentStack()
{
	return t_co ? t_co->ts : NULL;
}

bool Scheduler::InCoroutine()
{
	return t_co != NULL;
}

void Scheduler::RunOne(lpCoroutine co)
{
	if (!co || !co->ts)
	{
		return;
	}
	// Dispatch-latency samples are taken here so the starvation watchdog sees
	// ready-queue waits even when the scheduler never goes idle.
	MaybeRunDiagnostics(false);
	lpCoroutine prev = t_co;
	t_co = co;
	co->carrier_id = t_worker ? t_worker->index : -1;
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		co->state = CO_RUNNING;
	}
	if (co->injected)
	{
		// Deliver a pending InterruptedException at the resumed call site.
		co->ts->injected_exception = co->injected;
		co->injected = NULL;
	}
	EventNotify ev;
	if (!co->entered)
	{
		Variant self;
		self.heapPtr = co->thread_ref;
		ev = co->ts->execute(co->start_class, co->start_method, &self, 1);
		co->entered = true;
	}
	else
	{
		ev = co->ts->start();
	}
	t_co = prev;

	if (ev == EN_YIELD)
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		if (co->state == CO_RUNNING)
		{
			// The callee returned EN_YIELD without parking (defensive).
			co->state = CO_READY;
			co->ready_since_ns = NowNs();
			if (!co->queued && !co->pin_main)
			{
				PushReady(co, static_cast<size_t>(co->id));
				impl->cv.notify_one();
			}
		}
		co->busy.store(false);
		co->carrier_id = -1;
		return;
	}
	if (ev == EN_SAFEPOINT)
	{
		{
			std::unique_lock<std::recursive_mutex> lk(impl->m);
			if (t_worker)
			{
				t_worker->at_safepoint = true;
				impl->stw_cv.notify_all();
			}
			while (impl->stw_active)
			{
				impl->stw_cv.wait_for(lk, std::chrono::milliseconds(2));
			}
			if (t_worker)
			{
				t_worker->at_safepoint = false;
			}
		}
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		if (co->state == CO_RUNNING)
		{
			co->state = CO_READY;
			co->ready_since_ns = NowNs();
			if (!co->queued && !co->pin_main)
			{
				PushReady(co, static_cast<size_t>(co->id));
				impl->cv.notify_one();
			}
		}
		co->busy.store(false);
		co->carrier_id = -1;
		return;
	}
	FinishCoroutine(co, ev == EN_EXCEPTION);
}

void Scheduler::FinishCoroutine(lpCoroutine co, bool)
{
	co->carrier_id = -1;
	if (VMContext::pending_exception)
	{
		PrintUncaughtException(VMContext::pending_exception);
		VMContext::pending_exception = NULL;
	}
	// Clear isAlive() before publishing termination to joiners.  JoinCurrent
	// treats "not in by_thread" as already finished and returns immediately;
	// clearing the flag afterwards left a window where join() returned while
	// isAlive() still observed true, so callers thought a finished worker was
	// hung.  SetBoolField takes the VM lock, so it is called before acquiring
	// the scheduler lock (and released again) instead of nesting the two.
	if (co->thread_ref)
	{
		SetBoolField(co->thread_ref, "alive", false);
	}
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		// Drop pending timer entries before the Coroutine is destroyed: an
		// interrupted sleep leaves its deadline entry behind, and ReapTimers
		// would otherwise dereference the freed coroutine.
		for (std::multimap<int64_t, lpCoroutine>::iterator ti = impl->timers.begin();
		     ti != impl->timers.end();)
		{
			if (ti->second == co)
			{
				ti = impl->timers.erase(ti);
			}
			else
			{
				++ti;
			}
		}
		co->state = CO_TERMINATED;
		co->finished = true;
		co->deadline_ns = 0;
		impl->live.erase(std::remove(impl->live.begin(), impl->live.end(), co),
		                 impl->live.end());
		if (co->thread_ref)
		{
			impl->by_thread.erase(co->thread_ref);
		}
	}
	WakeJoiners(co);
	if (co->scope_parent)
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		std::map<LPOBJECT_REF, ScopeInfo *>::iterator si =
			impl->scopes.find(co->scope_parent);
		if (si != impl->scopes.end())
		{
			std::vector<lpCoroutine> &children = si->second->children;
			children.erase(std::remove(children.begin(), children.end(), co),
			               children.end());
			if (children.empty() && si->second->closed)
			{
				delete si->second;
				impl->scopes.erase(si);
			}
		}
		co->scope_parent = NULL;
	}
	if (co->ts)
	{
		delete co->ts;
		co->ts = NULL;
	}
	RemoveCoroutineRoots(co);
	delete co;
}

void Scheduler::PumpMain(lpCoroutine main_co, bool yield_to_others)
{
	while (impl && impl->running)
	{
		lpCoroutine co = NULL;
		bool idle = false;
		{
			std::unique_lock<std::recursive_mutex> lk(impl->m);
			if (main_co->state == CO_TERMINATED)
			{
				return;
			}
			// After a plain Thread.yield() let another runnable coroutine run
			// first; without this a yielding main loop would starve them.
			if (!yield_to_others && main_co->state == CO_READY)
			{
				main_co->state = CO_RUNNING;
				t_co = main_co;
				DarksDeterministic::Instance().Event('S', main_co->id, 1);
				return;
			}
			ReapTimers();
			if (!TakeReady(0, co))
			{
				if (main_co->state == CO_READY)
				{
					main_co->state = CO_RUNNING;
					t_co = main_co;
					DarksDeterministic::Instance().Event('S', main_co->id, 1);
					return;
				}
				// Nothing runnable: in deterministic mode the virtual clock
				// jumps to the next timer instead of sleeping on the wall
				// clock, so sleeping coroutines make immediate progress.
				if (AdvanceVirtualTimeLocked())
				{
					continue;
				}
				idle = true;
			}
		}
		if (idle)
		{
			// Run the diagnostic watchdog outside the scheduler lock: it takes
			// VMLock before the scheduler lock and must not be called with the
			// latter already held.
			MaybeRunDiagnostics(true);
			std::unique_lock<std::recursive_mutex> lk(impl->m);
			if (!impl->running || main_co->state == CO_TERMINATED)
			{
				return;
			}
			if (!TakeReady(0, co))
			{
				if (main_co->state == CO_READY)
				{
					main_co->state = CO_RUNNING;
					t_co = main_co;
					DarksDeterministic::Instance().Event('S', main_co->id, 1);
					return;
				}
				if (AdvanceVirtualTimeLocked())
				{
					continue;
				}
				if (t_worker)
				{
					t_worker->at_safepoint = true;
					impl->stw_cv.notify_all();
				}
				impl->cv.wait_for(lk, std::chrono::milliseconds(1));
				while (impl->stw_active && impl->running)
				{
					impl->stw_cv.wait_for(lk, std::chrono::milliseconds(2));
				}
				if (t_worker)
				{
					t_worker->at_safepoint = false;
				}
				continue;
			}
		}
		RunOne(co);
		t_co = main_co;
		// Only the first iteration of a yield needs to defer to other work.
		yield_to_others = false;
	}
}

EventNotify Scheduler::RunMain(ThreadStack *ts, lpClass clazz, lpMethod method,
                               lpVariant vars, int count)
{
	if (!impl || !impl->main_co || impl->main_co->ts != ts)
	{
		// Scheduler not attached (unit tests / embedded use / nested executor
		// for class initialisation): behave exactly like the original
		// single-threaded interpreter.  Clear t_co so blocking primitives on
		// this nested stack take their OS-level fallback instead of parking the
		// coroutine that happens to be running on this OS thread.
		lpCoroutine saved = t_co;
		lpCoroutine saved_host = t_host_co;
		t_co = NULL;
		if (saved)
		{
			t_host_co = saved;
		}
		EventNotify result;
		if (vars)
		{
			result = ts->execute(clazz, method, vars, count);
		}
		else
		{
			result = ts->execute(clazz, method);
		}
		t_co = saved;
		t_host_co = saved_host;
		return result;
	}
	lpCoroutine co = impl->main_co;
	t_co = co;
	co->carrier_id = t_worker ? t_worker->index : -1;
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		co->state = CO_RUNNING;
	}
	EventNotify ev;
	if (vars)
	{
		ev = ts->execute(clazz, method, vars, count);
	}
	else
	{
		ev = ts->execute(clazz, method);
	}
	co->entered = true;
	while (ev == EN_YIELD || ev == EN_SAFEPOINT)
	{
		if (ev == EN_SAFEPOINT)
		{
			std::unique_lock<std::recursive_mutex> lk(impl->m);
			if (t_worker)
			{
				t_worker->at_safepoint = true;
				impl->stw_cv.notify_all();
			}
			while (impl->stw_active)
			{
				impl->stw_cv.wait_for(lk, std::chrono::milliseconds(2));
			}
			if (t_worker)
			{
				t_worker->at_safepoint = false;
			}
			co->state = CO_RUNNING;
		}
		else
		{
			bool plain_yield = false;
			{
				// A plain yield (Thread.yield / fairness preemption) leaves the
				// state as CO_RUNNING; mark the main coroutine runnable and let
				// another ready coroutine run before resuming it.
				std::lock_guard<std::recursive_mutex> lk(impl->m);
				if (co->state == CO_RUNNING)
				{
					co->state = CO_READY;
					plain_yield = true;
				}
			}
			PumpMain(co, plain_yield);
			if (co->state == CO_TERMINATED || !impl->running)
			{
				break;
			}
		}
		t_co = co;
		if (co->injected)
		{
			// The main coroutine is resumed directly (not through RunOne), so
			// deliver a pending InterruptedException here as well.
			ts->injected_exception = co->injected;
			co->injected = NULL;
		}
		ev = ts->start();
	}
	t_co = co;
	return ev;
}

// ---------------------------------------------------------------------------
// Deadlock / starvation diagnostics
// ---------------------------------------------------------------------------
namespace {

// Builds one DiagNode per live coroutine.  Requires VMLock (heap reads) and
// the scheduler lock (state is consistent and a READY coroutine cannot be
// claimed while we look at its frames).
void BuildDiagNodes(const std::vector<lpCoroutine> &live,
                    const std::map<LPOBJECT_REF, Monitor *> &monitors,
                    const std::map<lpClass, Monitor *> &class_monitors,
                    std::vector<DiagNode> &nodes)
{
	nodes.clear();
	std::map<OBJECT_REF, std::vector<ChannelWaiterInfo> > channel_waiters;
	CollectChannelWaiters(channel_waiters);

	for (size_t i = 0; i < live.size(); i++)
	{
		lpCoroutine co = live[i];
		if (!co || co->finished || co->state == CO_TERMINATED)
		{
			continue;
		}
		DiagNode node;
		node.co = co;
		node.id = co->id;
		node.state = co->state;
		node.name = DiagThreadName(co->thread_ref);
		if (co->state != CO_RUNNING && co->ts)
		{
			co->ts->DescribeTopFrame(node.location);
			std::vector<std::pair<LPOBJECT_REF, lpClass> > held;
			co->ts->CollectHeldMonitors(held);
			for (size_t h = 0; h < held.size(); h++)
			{
				std::string text = DiagMonitorText(held[h].first, held[h].second);
				if (node.held.empty() || node.held.back() != text)
				{
					node.held.push_back(text);
				}
			}
		}

		switch (co->state)
		{
		case CO_MONITOR:
		{
			node.wait_kind = CO_WAIT_MONITOR;
			Monitor *mon = NULL;
			if (co->block_class)
			{
				std::map<lpClass, Monitor *>::const_iterator it =
					class_monitors.find(co->block_class);
				if (it != class_monitors.end())
				{
					mon = it->second;
				}
			}
			else if (co->block_obj)
			{
				std::map<LPOBJECT_REF, Monitor *>::const_iterator it =
					monitors.find(co->block_obj);
				if (it != monitors.end())
				{
					mon = it->second;
				}
			}
			node.monitor = DiagMonitorText(co->block_obj, co->block_class);
			if (mon && mon->owner && mon->owner != co)
			{
				node.target = mon->owner;
				node.target_id = mon->owner->id;
			}
			break;
		}
		case CO_JOINING:
			if (co->deadline_ns != 0)
			{
				node.wait_kind = CO_WAIT_TIMED;
				node.timed = true;
			}
			else
			{
				node.wait_kind = CO_WAIT_JOIN;
				if (co->join_target && !co->join_target->finished)
				{
					node.target = co->join_target;
					node.target_id = co->join_target->id;
				}
			}
			break;
		case CO_WAITING:
			if (co->deadline_ns != 0)
			{
				node.wait_kind = CO_WAIT_TIMED;
				node.timed = true;
				break;
			}
			if (co->block_obj)
			{
				std::map<OBJECT_REF, std::vector<ChannelWaiterInfo> >::const_iterator it =
					channel_waiters.find(*co->block_obj);
				if (it != channel_waiters.end() && !it->second.empty())
				{
					const ChannelWaiterInfo &info = it->second.front();
					node.channel = info.channel;
					node.channel_kind = info.kind;
					node.wait_kind = (info.kind == 0) ? CO_WAIT_CHANNEL_SEND
					                                  : CO_WAIT_CHANNEL_RECV;
					node.monitor = DiagChannelText(info.channel);
					break;
				}
			}
			node.wait_kind = CO_WAIT_OBJECT;
			node.monitor = DiagMonitorText(co->block_obj, NULL);
			break;
		case CO_SLEEPING:
			node.wait_kind = CO_WAIT_TIMED;
			node.timed = true;
			break;
		default:
			break;
		}
		nodes.push_back(node);
	}

	// Channel edges: a parked sender and receiver on the same channel are
	// peers.  (When both exist the handoff usually completes one of them
	// immediately, so this mostly documents select registrations.)
	for (size_t i = 0; i < nodes.size(); i++)
	{
		DiagNode &node = nodes[i];
		if (node.channel <= 0 ||
		    (node.wait_kind != CO_WAIT_CHANNEL_SEND && node.wait_kind != CO_WAIT_CHANNEL_RECV))
		{
			continue;
		}
		int other = (node.wait_kind == CO_WAIT_CHANNEL_SEND) ? CO_WAIT_CHANNEL_RECV
		                                                    : CO_WAIT_CHANNEL_SEND;
		for (size_t j = 0; j < nodes.size(); j++)
		{
			if (j == i || nodes[j].channel != node.channel || nodes[j].wait_kind != other)
			{
				continue;
			}
			node.target = nodes[j].co;
			node.target_id = nodes[j].id;
			break;
		}
	}

	for (size_t i = 0; i < nodes.size(); i++)
	{
		if (!nodes[i].target)
		{
			continue;
		}
		for (size_t j = 0; j < nodes.size(); j++)
		{
			if (nodes[j].co == nodes[i].target)
			{
				nodes[i].target_name = nodes[j].name;
				break;
			}
		}
	}
}

// Follows the wait-for edges looking for a cycle.  all_blocked reports that
// no coroutine is runnable and no parked coroutine owns a deadline.
bool AnalyzeWaitGraph(std::vector<DiagNode> &nodes, std::vector<int> &cycle,
                      bool &all_blocked)
{
	cycle.clear();
	bool active = false;
	bool timed = false;
	int blocked = 0;
	for (size_t i = 0; i < nodes.size(); i++)
	{
		switch (nodes[i].state)
		{
		case CO_RUNNING:
		case CO_READY:
		case CO_NEW:
			active = true;
			break;
		case CO_TERMINATED:
			break;
		default:
			blocked++;
			if (nodes[i].timed)
			{
				timed = true;
			}
			break;
		}
	}
	all_blocked = !active && !timed && blocked > 0;

	std::map<lpCoroutine, int> index;
	for (size_t i = 0; i < nodes.size(); i++)
	{
		index[nodes[i].co] = static_cast<int>(i);
	}

	for (size_t start = 0; start < nodes.size(); start++)
	{
		std::map<int, int> seen;
		std::vector<int> path;
		int cur = static_cast<int>(start);
		while (cur >= 0 && seen.find(cur) == seen.end())
		{
			seen[cur] = static_cast<int>(path.size());
			path.push_back(cur);
			DiagNode &node = nodes[static_cast<size_t>(cur)];
			if (!node.target)
			{
				cur = -1;
				break;
			}
			std::map<lpCoroutine, int>::iterator it = index.find(node.target);
			cur = (it == index.end()) ? -1 : it->second;
		}
		if (cur >= 0 && seen.find(cur) != seen.end())
		{
			for (size_t k = static_cast<size_t>(seen[cur]); k < path.size(); k++)
			{
				cycle.push_back(path[k]);
			}
			return true;
		}
	}
	return false;
}

void AppendDiagNode(std::string &out, const DiagNode &node)
{
	char buf[320];
	snprintf(buf, sizeof(buf), "  coroutine #%llu \"%s\" state=%s",
	         static_cast<unsigned long long>(node.id), node.name.c_str(),
	         DiagStateName(node.state));
	out += buf;
	switch (node.wait_kind)
	{
	case CO_WAIT_MONITOR:
		out += " blocked on monitor ";
		out += node.monitor;
		if (node.target)
		{
			snprintf(buf, sizeof(buf), " held by #%llu \"%s\"",
			         static_cast<unsigned long long>(node.target_id),
			         node.target_name.c_str());
			out += buf;
		}
		break;
	case CO_WAIT_JOIN:
		out += " joining";
		if (node.target)
		{
			snprintf(buf, sizeof(buf), " coroutine #%llu \"%s\"",
			         static_cast<unsigned long long>(node.target_id),
			         node.target_name.c_str());
			out += buf;
		}
		break;
	case CO_WAIT_OBJECT:
		out += " in Object.wait on ";
		out += node.monitor;
		break;
	case CO_WAIT_CHANNEL_SEND:
	case CO_WAIT_CHANNEL_RECV:
		out += (node.wait_kind == CO_WAIT_CHANNEL_SEND) ? " blocked sending to "
		                                               : " blocked receiving from ";
		out += node.monitor;
		break;
	case CO_WAIT_TIMED:
		out += " in a timed wait (not a deadlock edge)";
		break;
	default:
		break;
	}
	out += "\n";
	if (!node.held.empty())
	{
		out += "      holds: ";
		out += JoinStrings(node.held, ", ");
		out += "\n";
	}
	if (!node.location.empty())
	{
		out += "      at ";
		out += node.location;
		out += "\n";
	}
}

std::string FormatDeadlockReport(const std::vector<DiagNode> &nodes,
                                 const std::vector<int> &cycle, uint64_t barges)
{
	std::string out;
	char buf[256];
	if (!cycle.empty())
	{
		snprintf(buf, sizeof(buf),
		         "darks-jvm: deadlock detected: %d coroutine(s) in a wait-for cycle\n",
		         static_cast<int>(cycle.size()));
		out += buf;
		for (size_t i = 0; i < cycle.size(); i++)
		{
			AppendDiagNode(out, nodes[static_cast<size_t>(cycle[i])]);
		}
	}
	else
	{
		snprintf(buf, sizeof(buf),
		         "darks-jvm: deadlock detected: all %d live coroutine(s) are blocked "
		         "and no timed wait can make progress\n",
		         static_cast<int>(nodes.size()));
		out += buf;
		for (size_t i = 0; i < nodes.size(); i++)
		{
			AppendDiagNode(out, nodes[i]);
		}
	}
	snprintf(buf, sizeof(buf), "  monitor barging events=%llu\n",
	         static_cast<unsigned long long>(barges));
	out += buf;
	return out;
}

DiagThread ToDiagThread(const DiagNode &node)
{
	DiagThread thread;
	thread.id = node.id;
	thread.name = node.name;
	thread.kind = node.co ? node.co->kind : 0;
	thread.state = node.state;
	thread.wait_kind = node.wait_kind;
	thread.wait_target = node.target_id;
	thread.wait_target_name = node.target_name;
	thread.monitor = node.monitor;
	thread.held_monitors = node.held;
	thread.location = node.location;
	return thread;
}

} // namespace

bool Scheduler::DiagnosticsEnabled() const
{
	return impl && impl->diag_any;
}

void Scheduler::SnapshotThreads(std::vector<DiagThread> &out)
{
	out.clear();
	if (!impl)
	{
		return;
	}
	VMLock vmlk;
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	std::vector<DiagNode> nodes;
	BuildDiagNodes(impl->live, impl->monitors, impl->class_monitors, nodes);
	for (size_t i = 0; i < nodes.size(); i++)
	{
		out.push_back(ToDiagThread(nodes[i]));
	}
}

bool Scheduler::DetectDeadlock(std::vector<DiagThread> &cycle, std::string &report)
{
	cycle.clear();
	report.clear();
	if (!impl)
	{
		return false;
	}
	VMLock vmlk;
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	std::vector<DiagNode> nodes;
	BuildDiagNodes(impl->live, impl->monitors, impl->class_monitors, nodes);
	std::vector<int> indices;
	bool all_blocked = false;
	bool found = AnalyzeWaitGraph(nodes, indices, all_blocked);
	bool quiescent = all_blocked && impl->timers.empty();
	if (quiescent)
	{
		for (size_t q = 0; q < impl->queues.size(); q++)
		{
			if (!impl->queues[q].empty())
			{
				quiescent = false;
				break;
			}
		}
	}
	if (!found && !quiescent)
	{
		return false;
	}
	report = FormatDeadlockReport(nodes, indices, impl->monitor_barges);
	if (!indices.empty())
	{
		for (size_t i = 0; i < indices.size(); i++)
		{
			cycle.push_back(ToDiagThread(nodes[static_cast<size_t>(indices[i])]));
		}
	}
	else
	{
		for (size_t i = 0; i < nodes.size(); i++)
		{
			cycle.push_back(ToDiagThread(nodes[i]));
		}
	}
	return true;
}

void Scheduler::ReportDeadlockAndExit(const std::string &report, bool logging_only)
{
	fprintf(stderr, "%s", report.c_str());
	fflush(stderr);
	fflush(stdout);
	if (!logging_only)
	{
		std::_Exit(kDiagDeadlockExit);
	}
}

void Scheduler::ReportStarvationAndExit(const std::string &report, bool logging_only)
{
	fprintf(stderr, "%s", report.c_str());
	fflush(stderr);
	fflush(stdout);
	if (!logging_only)
	{
		std::_Exit(kDiagStarvationExit);
	}
}

void Scheduler::MaybeRunDiagnostics(bool idle)
{
	if (!impl || !impl->diag_any || !impl->running)
	{
		return;
	}
	int64_t now = NowNs();
	if (now < impl->diag_next_ns.load(std::memory_order_relaxed))
	{
		return;
	}
	// Lock order: VMLock first, then the scheduler lock.  NewStringObject
	// establishes the same order by running Java with VMLock held.
	VMLock vmlk;
	std::lock_guard<std::recursive_mutex> lk(impl->m);
	if (!impl->running)
	{
		return;
	}
	now = NowNs();
	if (now < impl->diag_next_ns.load(std::memory_order_relaxed))
	{
		return;
	}
	impl->diag_next_ns.store(now + 50 * 1000000LL, std::memory_order_relaxed);

	if (impl->diag_starvation)
	{
		int starved = 0;
		int64_t worst_wait = 0;
		lpCoroutine worst = NULL;
		for (size_t q = 0; q < impl->queues.size(); q++)
		{
			std::deque<lpCoroutine> &queue = impl->queues[q];
			for (size_t i = 0; i < queue.size(); i++)
			{
				lpCoroutine co = queue[i];
				if (!co || co->ready_since_ns <= 0)
				{
					continue;
				}
				int64_t waited = now - co->ready_since_ns;
				if (waited > impl->diag_starvation_ns)
				{
					starved++;
					if (waited > worst_wait)
					{
						worst_wait = waited;
						worst = co;
					}
				}
			}
		}
		if (starved > 0)
		{
			std::string name = worst ? DiagThreadName(worst->thread_ref) : "<unknown>";
			char buf[420];
			snprintf(buf, sizeof(buf),
			         "darks-jvm: starvation detected: %d runnable coroutine(s) waited "
			         "longer than %lld ms in the ready queue (worst #%llu \"%s\" %lld ms)\n"
			         "  monitor barging events=%llu\n",
			         starved, static_cast<long long>(impl->diag_starvation_ns / 1000000LL),
			         static_cast<unsigned long long>(worst ? worst->id : 0), name.c_str(),
			         static_cast<long long>(worst_wait / 1000000LL),
			         static_cast<unsigned long long>(impl->monitor_barges));
			bool logging_only = !impl->diag_exit_on_starvation;
			ReportStarvationAndExit(std::string(buf), logging_only);
			if (logging_only)
			{
				impl->diag_next_ns.store(now + 1000000000LL, std::memory_order_relaxed);
			}
			return;
		}
	}

	if (!idle || !impl->diag_deadlock)
	{
		return;
	}

	std::vector<DiagNode> nodes;
	BuildDiagNodes(impl->live, impl->monitors, impl->class_monitors, nodes);
	std::vector<int> cycle;
	bool all_blocked = false;
	bool found = AnalyzeWaitGraph(nodes, cycle, all_blocked);
	bool quiescent = all_blocked && impl->timers.empty();
	if (quiescent)
	{
		for (size_t q = 0; q < impl->queues.size(); q++)
		{
			if (!impl->queues[q].empty())
			{
				quiescent = false;
				break;
			}
		}
	}
	if (!found && !quiescent)
	{
		impl->diag_pending_sig.clear();
		return;
	}

	std::string signature;
	if (!cycle.empty())
	{
		for (size_t i = 0; i < cycle.size(); i++)
		{
			char buf[64];
			snprintf(buf, sizeof(buf), "#%llu:%d;",
			         static_cast<unsigned long long>(nodes[static_cast<size_t>(cycle[i])].id),
			         nodes[static_cast<size_t>(cycle[i])].wait_kind);
			signature += buf;
		}
	}
	else
	{
		for (size_t i = 0; i < nodes.size(); i++)
		{
			char buf[64];
			snprintf(buf, sizeof(buf), "#%llu:%d;",
			         static_cast<unsigned long long>(nodes[i].id), nodes[i].wait_kind);
			signature += buf;
		}
	}
	if (signature != impl->diag_pending_sig)
	{
		impl->diag_pending_sig = signature;
		impl->diag_pending_since_ns = now;
		return;
	}
	if (now - impl->diag_pending_since_ns < impl->diag_confirm_ns)
	{
		return;
	}

	std::string report = FormatDeadlockReport(nodes, cycle, impl->monitor_barges);
	bool logging_only = !impl->diag_exit_on_deadlock;
	impl->diag_pending_sig.clear();
	ReportDeadlockAndExit(report, logging_only);
	if (logging_only)
	{
		impl->diag_next_ns.store(now + 1000000000LL, std::memory_order_relaxed);
	}
}

void Scheduler::WorkerMain(int index)
{
	WorkerInfo *w = new WorkerInfo();
	w->index = index;
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		impl->workers[static_cast<size_t>(index)] = w;
		impl->registered++;
	}
	t_worker = w;
	if (VMContext::object_heap)
	{
		VMLock lk;
		VMContext::object_heap->AddRootSlot(&VMContext::pending_exception);
	}
	while (true)
	{
		lpCoroutine co = NULL;
		bool idle = false;
		{
			std::unique_lock<std::recursive_mutex> lk(impl->m);
			if (!impl->running)
			{
				break;
			}
			ReapTimers();
			if (!TakeReady(index, co))
			{
				if (AdvanceVirtualTimeLocked())
				{
					continue;
				}
				idle = true;
			}
		}
		if (idle)
		{
			// See PumpMain: diagnostics take VMLock before the scheduler lock.
			MaybeRunDiagnostics(true);
			std::unique_lock<std::recursive_mutex> lk(impl->m);
			if (!impl->running)
			{
				break;
			}
			if (!TakeReady(index, co))
			{
				if (AdvanceVirtualTimeLocked())
				{
					continue;
				}
				w->at_safepoint = true;
				impl->stw_cv.notify_all();
				impl->cv.wait_for(lk, std::chrono::milliseconds(1));
				while (impl->stw_active && impl->running)
				{
					impl->stw_cv.wait_for(lk, std::chrono::milliseconds(2));
				}
				w->at_safepoint = false;
				continue;
			}
		}
		RunOne(co);
		if (!impl->running)
		{
			break;
		}
	}
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		impl->workers[static_cast<size_t>(index)] = NULL;
		impl->registered--;
		impl->spawned--;
	}
	if (VMContext::object_heap)
	{
		VMLock lk;
		VMContext::object_heap->RemoveRootSlot(&VMContext::pending_exception);
	}
	t_worker = NULL;
	delete w;
}
