#include "stdafx.h"
#include "objectheap.h"
#include "context.h"
#include "utils/CommonUtils.h"
#include "methodarea.h"
#include "executor.h"
#include "threadstack.h"
#include "stackframe.h"
#include "vm/classfiles/classloader.h"
#include "vm/classfiles/constant.h"
#include "vm/exceptions.h"
#include "jni/jni.h"
#include "scheduler.h"
#include "vmmutex.h"

#include <string>

namespace {

size_t ElementBytes(DATA_TYPE data_type)
{
	switch (data_type)
	{
	case DT_BOOL:
	case DT_BYTE:
		return 1;
	case DT_CHAR:
	case DT_SHORT:
		return 2;
	case DT_INT:
	case DT_FLOAT:
		return 4;
	case DT_LONG:
	case DT_DOUBLE:
		return 8;
	case DT_ARRAY:
	case DT_REF:
		return sizeof(jobject);
	default:
		return 0;
	}
}

} // namespace

ObjectHeap::~ObjectHeap()
{
	if (gc_log && gc_runs > 0)
	{
		fprintf(stderr, "gc-summary runs=%d freed_total=%zu bytes\n",
			gc_runs, gc_freed_total);
	}
	for (size_t i = 0; i < objs.size(); i++)
	{
		if (objs[i])
		{
			delete objs[i];
			objs[i] = NULL;
		}
	}
	objs.clear();
	sequence = 0;
}

bool ObjectHeap::initialize(int init_size)
{
	if (init_size < 1)
	{
		init_size = 1;
	}
	objs.assign(static_cast<size_t>(init_size), NULL);
	sequence = 1;
	ConfigureFromEnvironment();
	return true;
}

void ObjectHeap::ConfigureFromEnvironment()
{
	const char *gc_env = getenv("DARKS_JVM_GC");
	if (gc_env && *gc_env && strcmp(gc_env, "0") != 0 && strcmp(gc_env, "off") != 0)
	{
		gc_enabled = true;
	}
	else if (gc_env && *gc_env)
	{
		gc_enabled = false;
	}

	const char *log_env = getenv("DARKS_JVM_GC_LOG");
	gc_log = (log_env && *log_env && strcmp(log_env, "0") != 0);

	const char *threshold_env = getenv("DARKS_JVM_GC_THRESHOLD");
	if (threshold_env && *threshold_env)
	{
		unsigned long long value = strtoull(threshold_env, NULL, 10);
		if (value >= 1024)
		{
			base_threshold = static_cast<size_t>(value);
		}
	}
	threshold = base_threshold;
}

size_t ObjectHeap::ObjectBytes(lpObject obj) const
{
	if (!obj)
	{
		return 0;
	}
	// Approximate footprint: the object header, the contiguous slot array and
	// the array payload.  Synthetic extras are rare and counted coarsely.
	size_t bytes = sizeof(JObject);
	if (obj->slots && obj->slot_count > 0)
	{
		bytes += static_cast<size_t>(obj->slot_count) * sizeof(FieldValue);
	}
	if (obj->extras)
	{
		bytes += obj->extras->size() * sizeof(JObject::ExtraField);
	}
	if (obj->array_var && obj->array_length > 0)
	{
		bytes += static_cast<size_t>(obj->array_length) * ElementBytes(obj->array_data_type);
	}
	return bytes;
}

void ObjectHeap::AccountAllocation(lpObject obj)
{
	if (!obj)
	{
		return;
	}
	live_bytes += ObjectBytes(obj);
	live_objects++;
}

void ObjectHeap::MaybeGC()
{
	// Read the accounting under the heap mutex (allocations update it there),
	// then collect without it: the collector stops the world, and the
	// safepoint handshake must not run with the heap mutex already held.
	bool needed = false;
	{
		VMLock lk;
		if (!gc_enabled || in_gc || no_gc_depth > 0)
		{
			return;
		}
		needed = (live_bytes >= threshold);
	}
	if (needed)
	{
		gc();
	}
}

void ObjectHeap::AddRootSlot(LPOBJECT_REF *slot)
{
	VMLock lk;
	if (slot)
	{
		root_slots.push_back(slot);
	}
}

void ObjectHeap::RemoveRootSlot(LPOBJECT_REF *slot)
{
	VMLock lk;
	for (std::vector<LPOBJECT_REF *>::iterator it = root_slots.begin();
	     it != root_slots.end(); ++it)
	{
		if (*it == slot)
		{
			root_slots.erase(it);
			return;
		}
	}
}

void ObjectHeap::RegisterThreadStack(ThreadStack *stack)
{
	VMLock lk;
	if (!stack)
	{
		return;
	}
	for (size_t i = 0; i < thread_stacks.size(); i++)
	{
		if (thread_stacks[i] == stack)
		{
			return;
		}
	}
	thread_stacks.push_back(stack);
}

void ObjectHeap::UnregisterThreadStack(ThreadStack *stack)
{
	VMLock lk;
	for (std::vector<ThreadStack *>::iterator it = thread_stacks.begin();
	     it != thread_stacks.end(); ++it)
	{
		if (*it == stack)
		{
			thread_stacks.erase(it);
			return;
		}
	}
}

void ObjectHeap::PushNoGc()
{
	VMLock lk;
	no_gc_depth++;
}

void ObjectHeap::PopNoGc()
{
	VMLock lk;
	if (no_gc_depth > 0)
	{
		no_gc_depth--;
	}
}

void ObjectHeap::MarkObjectRef(LPOBJECT_REF ref)
{
	if (!ref)
	{
		return;
	}
	// The candidate pointer is never dereferenced: random integer/float slot
	// contents must not fault. Only pointers that are exactly the address of a
	// live object's index field are accepted, so the scan is conservative.
	std::map<const OBJECT_REF *, OBJECT_REF>::iterator it = ref_index.find(ref);
	if (it == ref_index.end())
	{
		return;
	}
	OBJECT_REF idx = it->second;
	if (idx <= 0 || static_cast<size_t>(idx) >= objs.size())
	{
		return;
	}
	if (marked[static_cast<size_t>(idx)])
	{
		return;
	}
	marked[static_cast<size_t>(idx)] = true;
	mark_stack.push_back(idx);
}

void ObjectHeap::MarkThreadStackRoots()
{
	VMLock lk;
	for (size_t i = 0; i < thread_stacks.size(); i++)
	{
		thread_stacks[i]->MarkRoots(this);
	}
}

void ObjectHeap::MarkStaticRoots()
{
	std::map<string, lpFieldValue>::iterator it = MethodArea::static_field_map.begin();
	for (; it != MethodArea::static_field_map.end(); ++it)
	{
		lpFieldValue fv = it->second;
		if (fv)
		{
			MarkObjectRef(fv->field_value.heapPtr);
		}
	}
}

void ObjectHeap::MarkConstantPoolRoots()
{
	if (!VMContext::bootstrap_classloader)
	{
		return;
	}
	const std::map<string, lpClass> &classes = VMContext::bootstrap_classloader->GetClasses();
	for (std::map<string, lpClass>::const_iterator it = classes.begin();
	     it != classes.end(); ++it)
	{
		lpClass clazz = it->second;
		if (!clazz || !clazz->const_pool)
		{
			continue;
		}
		int size = clazz->const_pool->size();
		for (int i = 1; i <= size; i++)
		{
			lpConstant constant = clazz->const_pool->GetConstant(i);
			if (constant && constant->tag == STRING_CONSTANT)
			{
				StringConstant *string_constant = static_cast<StringConstant *>(constant);
				MarkObjectRef(string_constant->string_ref);
			}
		}
	}
}

void ObjectHeap::MarkTemporaryRoots()
{
	for (size_t i = 0; i < root_slots.size(); i++)
	{
		LPOBJECT_REF *slot = root_slots[i];
		if (slot)
		{
			MarkObjectRef(*slot);
		}
	}
}

void ObjectHeap::AddGlobalRef(LPOBJECT_REF ref)
{
	VMLock lk;
	if (ref)
	{
		global_refs.push_back(ref);
	}
}

void ObjectHeap::RemoveGlobalRef(LPOBJECT_REF ref)
{
	VMLock lk;
	for (std::vector<LPOBJECT_REF>::iterator it = global_refs.begin();
	     it != global_refs.end(); ++it)
	{
		if (*it == ref)
		{
			global_refs.erase(it);
			return;
		}
	}
}

void ObjectHeap::AddWeakGlobalRef(LPOBJECT_REF ref)
{
	VMLock lk;
	if (ref)
	{
		weak_global_refs.push_back(ref);
	}
}

void ObjectHeap::RemoveWeakGlobalRef(LPOBJECT_REF ref)
{
	VMLock lk;
	for (std::vector<LPOBJECT_REF>::iterator it = weak_global_refs.begin();
	     it != weak_global_refs.end(); ++it)
	{
		if (*it == ref)
		{
			weak_global_refs.erase(it);
			return;
		}
	}
}

bool ObjectHeap::IsGlobalRef(LPOBJECT_REF ref) const
{
	VMLock lk;
	for (size_t i = 0; i < global_refs.size(); i++)
	{
		if (global_refs[i] == ref)
		{
			return true;
		}
	}
	return false;
}

bool ObjectHeap::IsWeakGlobalRef(LPOBJECT_REF ref) const
{
	VMLock lk;
	for (size_t i = 0; i < weak_global_refs.size(); i++)
	{
		if (weak_global_refs[i] == ref)
		{
			return true;
		}
	}
	return false;
}

void ObjectHeap::MarkGlobalRefs()
{
	for (size_t i = 0; i < global_refs.size(); i++)
	{
		MarkObjectRef(global_refs[i]);
	}
}

void ObjectHeap::MarkJniLocalRoots()
{
	// The JNI environment list is mutated when a coroutine finishes, possibly
	// while this collection runs on another worker.
	VMLock lk;
	for (size_t i = 0; i < VMContext::jni_envs.size(); i++)
	{
		DarksVM_MarkJniEnvRoots(VMContext::jni_envs[i], this);
	}
}

void ObjectHeap::MarkRoots()
{
	MarkThreadStackRoots();
	MarkStaticRoots();
	MarkConstantPoolRoots();
	MarkTemporaryRoots();
	MarkGlobalRefs();
	MarkJniLocalRoots();
}

void ObjectHeap::gc()
{
	// Only one collection at a time.  A native call may reach allocation from
	// another thread; in that case the collection is skipped rather than
	// waiting while holding the heap lock.
	std::unique_lock<std::mutex> gc_lock(gc_mutex, std::try_to_lock);
	if (!gc_lock.owns_lock())
	{
		return;
	}
	if (in_gc)
	{
		return;
	}
	// Hold the heap mutex before stopping the world.  A worker that is blocked
	// on this mutex is then genuinely stopped for the whole collection, which
	// makes the scheduler's "waiting for the heap lock" safepoint state stable.
	// Taking the mutex afterwards would leave a window where such a worker
	// resumes and runs Java while the heap is being traced.
	VMLock heap_lock;
	// Stop every interpreter worker at a safepoint before tracing.  Native
	// code is treated as stopped: its Java frames are at the call boundary and
	// its JNI local references are registered roots.
	Scheduler::Instance().SafepointBegin();
	in_gc = true;

	ref_index.clear();
	for (size_t i = 1; i < objs.size(); i++)
	{
		if (objs[i])
		{
			ref_index[&objs[i]->index] = static_cast<OBJECT_REF>(i);
		}
	}

	marked.assign(objs.size(), false);
	mark_stack.clear();

	MarkRoots();

	// Iterative trace: scan every marked object for outgoing references.
	while (!mark_stack.empty())
	{
		OBJECT_REF idx = mark_stack.back();
		mark_stack.pop_back();
		lpObject obj = objs[static_cast<size_t>(idx)];
		if (!obj)
		{
			continue;
		}
		// Instance field slots.  Primitive values are tolerated:
		// MarkObjectRef only accepts pointers that match a live object's
		// index address, so scanning every slot is safe.
		for (int slot = 0; slot < obj->slot_count; slot++)
		{
			MarkObjectRef(obj->slots[slot].field_value.heapPtr);
		}
		// VM-synthetic name-keyed values ("ann:", "$lambda:").
		if (obj->extras)
		{
			for (size_t e = 0; e < obj->extras->size(); e++)
			{
				lpFieldValue fv = (*obj->extras)[e].value;
				if (fv)
				{
					MarkObjectRef(fv->field_value.heapPtr);
				}
			}
		}
		// Reference arrays (object arrays and nested arrays).
		if (obj->array_var &&
		    (obj->array_data_type == DT_REF || obj->array_data_type == DT_ARRAY))
		{
			jobject *refs = static_cast<jobject *>(obj->array_var);
			for (int k = 0; k < obj->array_length; k++)
			{
				MarkObjectRef(reinterpret_cast<LPOBJECT_REF>(refs[k]));
			}
		}
	}

	size_t freed_bytes = 0;
	size_t freed_objects = 0;
	size_t live = 0;
	size_t live_count = 0;
	for (size_t i = 1; i < objs.size(); i++)
	{
		lpObject obj = objs[i];
		if (!obj)
		{
			continue;
		}
		if (!marked[i])
		{
			size_t bytes = ObjectBytes(obj);
			freed_bytes += bytes;
			freed_objects++;
			delete obj;
			objs[i] = NULL;
			// Reuse the index on the next allocation. An address-validated
			// reference can never alias the recycled slot because the new
			// object lives at a different address.
			free_indices.push_back(static_cast<OBJECT_REF>(i));
		}
		else
		{
			live += ObjectBytes(obj);
			live_count++;
		}
	}

	live_bytes = live;
	live_objects = live_count;
	gc_runs++;
	gc_freed_last = freed_bytes;
	gc_freed_total += freed_bytes;

	// Avoid collecting on every single allocation when almost everything is
	// still reachable: grow the threshold past the surviving heap.
	size_t next_threshold = base_threshold;
	if (live_bytes > next_threshold / 2)
	{
		next_threshold = live_bytes * 2;
	}
	threshold = next_threshold;

	if (gc_log)
	{
		fprintf(stderr,
			"gc-collect run=%d objects=%zu freed=%zu bytes live=%zu bytes\n",
			gc_runs, freed_objects, freed_bytes, live_bytes);
	}

	in_gc = false;
	Scheduler::Instance().SafepointEnd();
}

bool ObjectHeap::Register(lpObject obj, LPOBJECT_REF &object_ref)
{
	VMLock lk;
	if (!obj)
	{
		return false;
	}
	OBJECT_REF index;
	if (!free_indices.empty())
	{
		index = free_indices.back();
		free_indices.pop_back();
	}
	else
	{
		index = sequence++;
	}
	obj->index = index;
	if (static_cast<size_t>(index) >= objs.size())
	{
		objs.resize(static_cast<size_t>(index) + 1, NULL);
	}
	objs[static_cast<size_t>(index)] = obj;
	object_ref = &obj->index;
	return true;
}

bool ObjectHeap::NewObject(lpClass clazz, LPOBJECT_REF &object_ref)
{
	// Collect before acquiring the heap mutex: the collector stops the world
	// and must not run with the heap mutex already held (lock order).
	MaybeGC();
	VMLock lk;
	lpObject obj = new JObject(clazz);
	if (!obj)
	{
		return false;
	}
	if (!obj->newInstance())
	{
		delete obj;
		return false;
	}
	if (!Register(obj, object_ref))
	{
		delete obj;
		return false;
	}
	AccountAllocation(obj);
	return true;
}

bool ObjectHeap::NewArrayObject(int size, lpClass clazz, LPOBJECT_REF &object_ref)
{
	MaybeGC();
	VMLock lk;
	if (size < 0)
	{
		return false;
	}
	lpObject obj = new JObject(clazz);
	LPOBJECT_REF ref = NULL;
	if (!Register(obj, ref) || !obj->newArrayInstance(size))
	{
		if (obj->index > 0 && static_cast<size_t>(obj->index) < objs.size())
		{
			objs[static_cast<size_t>(obj->index)] = NULL;
			free_indices.push_back(obj->index);
		}
		delete obj;
		return false;
	}
	object_ref = ref;
	AccountAllocation(obj);
	return true;
}

bool ObjectHeap::NewObjectArrayObject(int size, lpClass clazz, LPOBJECT_REF &object_ref)
{
	MaybeGC();
	VMLock lk;
	if (size < 0)
	{
		return false;
	}
	lpObject obj = new JObject(clazz);
	LPOBJECT_REF ref = NULL;
	if (!Register(obj, ref) || !obj->newArrayInstance(DT_REF, size))
	{
		if (obj->index > 0 && static_cast<size_t>(obj->index) < objs.size())
		{
			objs[static_cast<size_t>(obj->index)] = NULL;
			free_indices.push_back(obj->index);
		}
		delete obj;
		return false;
	}
	object_ref = ref;
	AccountAllocation(obj);
	return true;
}

bool ObjectHeap::CreateArrayDimension(lpClass array_class, LPINT32 nums, int level, int count,
                                      LPOBJECT_REF &object_ref)
{
	if (!array_class || !array_class->thisClass || !nums || level < 0 || level >= count)
	{
		return false;
	}
	// The parent dimension must survive collections triggered while the child
	// dimensions are being allocated.
	GCRootScope root(this, &object_ref);
	MaybeGC();
	lpObject obj = new JObject(array_class);
	LPOBJECT_REF ref = NULL;
	if (!Register(obj, ref))
	{
		delete obj;
		return false;
	}
	object_ref = ref;

	int length = nums[level];
	if (level + 1 == count)
	{
		// Innermost dimension: the array class name is a one-dimensional
		// descriptor such as "[I"; JObject infers the element type from it.
		if (!obj->newArrayInstance(length))
		{
			objs[static_cast<size_t>(obj->index)] = NULL;
			free_indices.push_back(obj->index);
			object_ref = NULL;
			delete obj;
			return false;
		}
		AccountAllocation(obj);
		return true;
	}

	if (!obj->newArrayInstance(DT_REF, length))
	{
		objs[static_cast<size_t>(obj->index)] = NULL;
		free_indices.push_back(obj->index);
		object_ref = NULL;
		delete obj;
		return false;
	}
	AccountAllocation(obj);

	const char *name = array_class->thisClass->GetName();
	if (!name || name[0] != '[')
	{
		return false;
	}
	lpClass child_class = NULL;
	if (!VMContext::bootstrap_classloader->FindClass(name + 1, child_class))
	{
		return false;
	}
	jobject *refs = static_cast<jobject*>(obj->array_var);
	for (int i = 0; i < length; i++)
	{
		LPOBJECT_REF child_ref = NULL;
		if (!CreateArrayDimension(child_class, nums, level + 1, count, child_ref))
		{
			return false;
		}
		refs[i] = reinterpret_cast<jobject>(child_ref);
	}
	return true;
}

bool ObjectHeap::NewMultiArrayObject(LPINT32 nums, int count, lpClass clazz, LPOBJECT_REF &object_ref)
{
	VMLock lk;
	if (!nums || count <= 0 || !clazz)
	{
		return false;
	}
	for (int i = 0; i < count; i++)
	{
		if (nums[i] < 0)
		{
			return false;
		}
	}
	return CreateArrayDimension(clazz, nums, 0, count, object_ref);
}

// data is a UTF-8 byte string.
bool ObjectHeap::NewStringObject(LPSTR data, int length, LPOBJECT_REF &object_ref)
{
	VMLock lk;
	int unicode_len = 0;
	unsigned short* unicodes = CommonUtils::Utf8ToUnicode(data, unicode_len);
	if (!unicodes)
	{
		return false;
	}
	// The UTF-16 code unit count is authoritative; the UTF-8 byte length is
	// not a valid character count.
	(void)length;
	return NewStringObject(unicodes, unicode_len, object_ref);
}

// data is a UTF-16 code unit buffer; ownership is transferred to the char[]
// object. length is the number of code units.
bool ObjectHeap::NewStringObject(unsigned short *data, int length, LPOBJECT_REF &object_ref)
{
	VMLock lk;
	bool transferred = false;
	{
		// The char[] and the freshly allocated String are only held by C++
		// locals until they are linked/returned, so pin them across the
		// allocations performed while running String.<init>.
		GCRootScope root_object(this, &object_ref);
		LPOBJECT_REF chars_ref = NULL;
		GCRootScope root_chars(this, &chars_ref);
		try
		{
			lpClass data_clazz = NULL;
			if (!VMContext::bootstrap_classloader->FindClass("[C", data_clazz))
			{
				throw new VMException("Fail to load char array class.");
			}
			if (!NewArrayObject(length, data_clazz, chars_ref))
			{
				throw new VMException("Fail to create char array.");
			}
			lpObject chars_obj = NULL;
			if (!GetObject(chars_ref, chars_obj))
			{
				throw new VMException("Fail to get char array direct object.");
			}
			chars_obj->SetData(DT_CHAR, (LPVOID)data, length);
			transferred = true;

			lpClass str_class = NULL;
			if (!VMContext::bootstrap_classloader->FindClass("java/lang/String", str_class))
			{
				throw new VMException("Fail to load string class.");
			}
			if (!NewObject(str_class, object_ref))
			{
				throw new VMException("Fail to new string object.");
			}
			lpMethod method = MethodArea::GetMethod(str_class, "<init>:([C)V");
			if (!method)
			{
				throw new VMException("Fail to find string init method.");
			}
			lpVariant vars = new Variant[2];
			vars[0].heapPtr = object_ref;
			vars[1].heapPtr = chars_ref;
			lpVMExecutor exec = new VMExecutor();
			exec->Initialize();
			exec->Execute(str_class, method, vars, 2);
			delete[] vars;
			delete exec;
		}
		catch (...)
		{
			if (!transferred)
			{
				delete[] data;
			}
			throw;
		}
	}
	return true;
}

bool ObjectHeap::GetObject(OBJECT_REF obj_ref, lpObject &obj)
{
	VMLock lk;
	obj = NULL;
	if (obj_ref <= 0 || static_cast<size_t>(obj_ref) >= objs.size())
	{
		return false;
	}
	obj = objs[static_cast<size_t>(obj_ref)];
	return obj != NULL;
}
