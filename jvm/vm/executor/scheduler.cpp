#include "stdafx.h"
#include "scheduler.h"

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
#include <thread>
#include <vector>

namespace {

int64_t NowNs()
{
	return std::chrono::duration_cast<std::chrono::nanoseconds>(
		std::chrono::steady_clock::now().time_since_epoch()).count();
}

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

thread_local lpCoroutine t_co = NULL;
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

	std::multimap<int64_t, lpCoroutine> timers;
	std::map<LPOBJECT_REF, Monitor *> monitors;
	std::map<lpClass, Monitor *> class_monitors;
	std::map<LPOBJECT_REF, lpCoroutine> by_thread;

	lpCoroutine main_co;

	bool stw_active;
	int stw_depth;
	uint64_t next_id;

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
	deadline_ns = 0;
	timer_generation = 0;
	injected = NULL;
	interrupt_flag = false;
	throw_on_resume = false;
	resume_from_wait = false;
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
	mp = EnvParallelism();
	impl->queues.resize(static_cast<size_t>(mp));
	impl->workers.assign(static_cast<size_t>(mp), static_cast<WorkerInfo *>(NULL));
	impl->running = true;
	impl->initialized = true;
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
		impl->cv.notify_all();
		return;
	}
	co->state = CO_READY;
	co->queued = true;
	int n = static_cast<int>(impl->queues.size());
	if (n < 1)
	{
		n = 1;
	}
	impl->queues[static_cast<size_t>(hint % n)].push_back(co);
	impl->cv.notify_one();
}

bool Scheduler::TakeReady(int worker_index, lpCoroutine &co)
{
	if (!impl)
	{
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
	int64_t now = NowNs();
	std::vector<lpCoroutine> expired;
	for (std::multimap<int64_t, lpCoroutine>::iterator it = impl->timers.begin();
	     it != impl->timers.end();)
	{
		if (it->first > now)
		{
			break;
		}
		lpCoroutine co = it->second;
		expired.push_back(co);
		it = impl->timers.erase(it);
	}
	for (size_t i = 0; i < expired.size(); i++)
	{
		lpCoroutine co = expired[i];
		if (co->deadline_ns == 0 || co->deadline_ns > now)
		{
			continue;
		}
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

void Scheduler::WaitForWork(int64_t)
{
	// One millisecond polling keeps timer latency low without a dedicated
	// timer thread; sleep/wait deadlines are honoured in that granularity.
	std::unique_lock<std::recursive_mutex> lk(impl->m);
	if (!impl->running)
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
		mon->owner = co;
		mon->count = 1;
		frame->monitors_held.push_back(std::make_pair(obj, static_cast<lpClass>(NULL)));
		return true;
	}
	if (mon->owner == co)
	{
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
		mon->owner = co;
		mon->count = 1;
		frame->monitors_held.push_back(std::make_pair(static_cast<LPOBJECT_REF>(NULL), clazz));
		return true;
	}
	if (mon->owner == co)
	{
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
		mon->count = next->resume_from_wait ? next->saved_count : 1;
		if (mon->count < 1) mon->count = 1;
		next->monitor_granted = !next->resume_from_wait;
		next->resume_from_wait = false;
		next->block_obj = NULL;
		next->block_class = NULL;
		next->state = CO_READY;
	}
	if (next)
	{		if (next->pin_main)
		{
			impl->cv.notify_all();
		}
		else
		{
			impl->queues[static_cast<size_t>(next->id % impl->queues.size())].push_back(next);
			next->queued = true;
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
			impl->queues[static_cast<size_t>(next->id % impl->queues.size())].push_back(next);
			next->queued = true;
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
	bool interrupted = false;
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		if (co->interrupt_flag)
		{
			co->interrupt_flag = false;
			interrupted = true;
		}
		else
		{
			if (millis <= 0 || co->injected)
			{
				return false;
			}
			co->deadline_ns = NowNs() + millis * 1000000LL;
			co->timer_generation++;
			impl->timers.insert(std::make_pair(co->deadline_ns, co));
			co->state = CO_SLEEPING;
			return true;
		}
	}
	// Build the exception outside the scheduler lock: creating Java objects
	// takes the heap mutex, and the collection path takes the locks in the
	// opposite order.
	co->injected = CreateJavaException("java/lang/InterruptedException",
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
	(void)ts;
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
		co->injected = CreateJavaException("java/lang/InterruptedException",
		                                   "join interrupted");
		return false;
	}
	co->join_target = target_co;
	target_co->joiners.push_back(co);
	co->state = CO_JOINING;
	if (millis > 0)
	{
		co->deadline_ns = NowNs() + millis * 1000000LL;
		co->timer_generation++;
		impl->timers.insert(std::make_pair(co->deadline_ns, co));
	}
	return true;
}

bool Scheduler::WaitCurrent(ThreadStack *ts, LPOBJECT_REF obj, int64_t millis)
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
	if (co->interrupt_flag)
	{
		co->interrupt_flag = false;
		lk.unlock();
		co->injected = CreateJavaException("java/lang/InterruptedException",
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
		mon->count = next->resume_from_wait ? next->saved_count : 1;
		if (mon->count < 1) mon->count = 1;
		next->monitor_granted = !next->resume_from_wait;
		next->resume_from_wait = false;
		next->block_obj = NULL;
		next->block_class = NULL;
		if (next->pin_main)
		{
			impl->cv.notify_all();
		}
		else
		{
			impl->queues[static_cast<size_t>(next->id % impl->queues.size())].push_back(next);
			next->queued = true;
			impl->cv.notify_one();
		}
	}
	if (millis > 0)
	{
		co->deadline_ns = NowNs() + millis * 1000000LL;
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
}

void Scheduler::ExitNative()
{
	WorkerInfo *w = t_worker;
	if (w)
	{
		w->in_native = false;
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
			if (joiner->pin_main)
			{
				impl->cv.notify_all();
			}
			else if (!joiner->queued)
			{
				impl->queues[static_cast<size_t>(joiner->id % impl->queues.size())].push_back(joiner);
				joiner->queued = true;
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
	ThreadStack *ts = new ThreadStack();
	lpCoroutine co = new Coroutine();
	co->ts = ts;
	co->thread_ref = thread_obj;
	co->start_method = run_method;
	co->start_class = run_method->clazz ? run_method->clazz : obj->obj_class;
	co->id = impl->next_id++;
	co->state = CO_READY;
	co->entered = false;
	AddCoroutineRoots(co);
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		impl->by_thread[thread_obj] = co;
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
		if (co->pin_main)
		{
			impl->cv.notify_all();
		}
		else if (!co->queued)
		{
			impl->queues[static_cast<size_t>(co->id % impl->queues.size())].push_back(co);
			co->queued = true;
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
	lpCoroutine prev = t_co;
	t_co = co;
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
			if (!co->queued && !co->pin_main)
			{
				impl->queues[static_cast<size_t>(co->id % impl->queues.size())].push_back(co);
				co->queued = true;
				impl->cv.notify_one();
			}
		}
		co->busy.store(false);
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
			if (!co->queued && !co->pin_main)
			{
				impl->queues[static_cast<size_t>(co->id % impl->queues.size())].push_back(co);
				co->queued = true;
				impl->cv.notify_one();
			}
		}
		co->busy.store(false);
		return;
	}
	FinishCoroutine(co, ev == EN_EXCEPTION);
}

void Scheduler::FinishCoroutine(lpCoroutine co, bool)
{
	if (VMContext::pending_exception)
	{
		PrintUncaughtException(VMContext::pending_exception);
		VMContext::pending_exception = NULL;
	}
	// Publish termination before waking joiners: a concurrent JoinCurrent
	// either finds the coroutine while it is still registered (and is then
	// woken by WakeJoiners below) or fails the lookup and returns at once.
	{
		std::lock_guard<std::recursive_mutex> lk(impl->m);
		co->state = CO_TERMINATED;
		co->finished = true;
		co->deadline_ns = 0;
		if (co->thread_ref)
		{
			impl->by_thread.erase(co->thread_ref);
		}
	}
	WakeJoiners(co);
	if (co->thread_ref)
	{
		SetBoolField(co->thread_ref, "alive", false);
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
				return;
			}
			ReapTimers();
			if (!TakeReady(0, co))
			{
				if (main_co->state == CO_READY)
				{
					main_co->state = CO_RUNNING;
					t_co = main_co;
					return;
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
		t_co = NULL;
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
		return result;
	}
	lpCoroutine co = impl->main_co;
	t_co = co;
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
		{
			std::unique_lock<std::recursive_mutex> lk(impl->m);
			if (!impl->running)
			{
				break;
			}
			ReapTimers();
			if (!TakeReady(index, co))
			{
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
