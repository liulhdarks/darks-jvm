// Direct tests for the ObjectHeap mark-sweep collector.
//
// These checks drive the heap without executing Java bytecode so that
// collection and retention can be asserted deterministically. Stack-frame
// safety is covered end-to-end by tests/java/GcStress.java.
//
// Usage: gc_tests <classes-dir>
// The classes directory is needed for the string-retention check, which calls
// ObjectHeap::NewStringObject and therefore loads java/lang/String.

#include "stdafx.h"
#include "platform/platform.h"
#include "env/env.h"
#include "vm/executor/context.h"
#include "vm/executor/objectheap.h"
#include "vm/executor/jobject.h"
#include "vm/executor/jclass.h"
#include "vm/executor/jfield.h"
#include "vm/executor/methodarea.h"
#include "vm/classfiles/classloader.h"

#include <cstdio>
#include <cstring>
#include <string>

static int g_passed = 0;
static int g_failed = 0;

#define CHECK(cond) \
	do { \
		if (cond) { ++g_passed; } \
		else { ++g_failed; std::fprintf(stderr, "FAIL %s:%d: %s\n", __FILE__, __LINE__, #cond); } \
	} while (0)

static void set_ref_field(lpObject obj, const char *name, LPOBJECT_REF ref)
{
	lpFieldValue fv = new FieldValue();
	fv->field_value.heapPtr = ref;
	obj->AdoptFieldValue(std::string(name), fv);
}

static bool int_at(lpObjectHeap heap, LPOBJECT_REF ref, int index, int &value)
{
	lpObject obj = NULL;
	if (!heap->GetObject(ref, obj) || !obj || !obj->array_var ||
	    obj->array_length <= index || index < 0)
	{
		return false;
	}
	value = static_cast<int*>(obj->array_var)[index];
	return true;
}

static bool string_value_length(lpObjectHeap heap, LPOBJECT_REF ref, int &length)
{
	length = -1;
	lpObject obj = NULL;
	if (!heap->GetObject(ref, obj) || !obj || !obj->obj_class)
	{
		return false;
	}
	std::map<std::string, lpField>::iterator it = obj->obj_class->fields_map.find("value:[C");
	if (it == obj->obj_class->fields_map.end())
	{
		return false;
	}
	lpFieldValue fv = obj->GetFieldValue(it->second);
	if (!fv || !fv->field_value.heapPtr)
	{
		return false;
	}
	lpObject chars = NULL;
	if (!heap->GetObject(fv->field_value.heapPtr, chars) || !chars)
	{
		return false;
	}
	length = chars->array_length;
	return true;
}

int main(int argc, char **argv)
{
	std::string classes_dir = ".";
	if (argc > 1)
	{
		classes_dir = argv[1];
	}

	Environment::ClearClassPath();
	Environment::AddClassPath(classes_dir);

	if (!VMContext::initialize())
	{
		std::printf("gc checks: VM initialization failed\n");
		return 1;
	}
	lpObjectHeap heap = VMContext::object_heap;

	lpClass int_array_class = NULL;
	lpClass object_array_class = NULL;
	CHECK(VMContext::bootstrap_classloader->FindClass("[I", int_array_class));
	CHECK(VMContext::bootstrap_classloader->FindClass("[Ljava/lang/Object;", object_array_class));

	// Plain object metadata for the reference-chain test (no fields).
	lpClass plain_class = new Class();

	// ---------------------------------------------------------------- (a)
	// Garbage must be reclaimed and the heap occupancy must fall.
	{
		int runs_before = heap->GcRuns();
		for (int i = 0; i < 1000; i++)
		{
			LPOBJECT_REF ref = NULL;
			CHECK(heap->NewArrayObject(128, int_array_class, ref));
		}
		size_t peak = heap->LiveBytes();
		heap->gc();
		CHECK(heap->GcRuns() >= runs_before + 1);
		CHECK(heap->LiveObjectCount() == 0);
		CHECK(heap->LiveBytes() < 4096);
		CHECK(peak >= heap->LastFreedBytes());
		std::printf("gc-test: sweep peak=%zu bytes, freed_last=%zu bytes, live_after=%zu bytes\n",
			peak, heap->LastFreedBytes(), heap->LiveBytes());
	}

	// ---------------------------------------------------------------- (b1)
	// A reachable object chain (object -> field -> object -> field -> array)
	// must survive, and disappear once the root goes away.
	{
		LPOBJECT_REF a = NULL;
		GCRootScope root_a(heap, &a);
		LPOBJECT_REF b = NULL;
		LPOBJECT_REF c = NULL;
		CHECK(heap->NewObject(plain_class, a));
		CHECK(heap->NewArrayObject(8, int_array_class, b));
		CHECK(heap->NewArrayObject(16, int_array_class, c));

		lpObject ao = NULL;
		lpObject bo = NULL;
		lpObject co = NULL;
		CHECK(heap->GetObject(a, ao));
		CHECK(heap->GetObject(b, bo));
		CHECK(heap->GetObject(c, co));
		if (co)
		{
			static_cast<int*>(co->array_var)[0] = 4242;
		}
		if (ao)
		{
			set_ref_field(ao, "next:Ljava/lang/Object;", b);
		}
		if (bo)
		{
			set_ref_field(bo, "value:[I", c);
		}

		heap->gc();
		CHECK(heap->GetObject(a, ao));
		CHECK(heap->GetObject(b, bo));
		CHECK(heap->GetObject(c, co));
		int value = 0;
		CHECK(int_at(heap, c, 0, value));
		CHECK(value == 4242);
	}
	heap->gc();
	CHECK(heap->LiveObjectCount() == 0);

	// ---------------------------------------------------------------- (b2)
	// A reference array is itself a root path: only the array is rooted, the
	// element is referenced exclusively through the array.
	{
		LPOBJECT_REF refs_ref = NULL;
		GCRootScope root_refs(heap, &refs_ref);
		CHECK(heap->NewObjectArrayObject(4, object_array_class, refs_ref));

		LPOBJECT_REF element = NULL;
		CHECK(heap->NewArrayObject(4, int_array_class, element));
		lpObject element_obj = NULL;
		CHECK(heap->GetObject(element, element_obj));
		if (element_obj)
		{
			static_cast<int*>(element_obj->array_var)[0] = 777;
		}
		lpObject refs_obj = NULL;
		CHECK(heap->GetObject(refs_ref, refs_obj));
		if (refs_obj)
		{
			static_cast<jobject*>(refs_obj->array_var)[0] =
				reinterpret_cast<jobject>(element);
		}

		heap->gc();
		lpObject survivor = NULL;
		CHECK(heap->GetObject(refs_ref, refs_obj));
		if (refs_obj)
		{
			LPOBJECT_REF element_ref =
				reinterpret_cast<LPOBJECT_REF>(static_cast<jobject*>(refs_obj->array_var)[0]);
			CHECK(heap->GetObject(element_ref, survivor));
		}
		int value = 0;
		CHECK(int_at(heap, element, 0, value));
		CHECK(value == 777);
	}
	heap->gc();
	CHECK(heap->LiveObjectCount() == 0);

	// ---------------------------------------------------------------- (b3)
	// String objects (String -> char[]) stay alive while rooted.
	{
		char text[] = "gc-string";
		LPOBJECT_REF str_ref = NULL;
		GCRootScope root_str(heap, &str_ref);
		CHECK(heap->NewStringObject(text, static_cast<int>(strlen(text)), str_ref));
		heap->gc();
		lpObject str_obj = NULL;
		CHECK(heap->GetObject(str_ref, str_obj));
		int length = -1;
		CHECK(string_value_length(heap, str_ref, length));
		CHECK(length == 9);
	}
	heap->gc();
	CHECK(heap->LiveObjectCount() == 0);

	// ---------------------------------------------------------------- (b4)
	// Static fields are roots; clearing the static field makes the object
	// collectable again.
	{
		LPOBJECT_REF static_target = NULL;
		CHECK(heap->NewArrayObject(4, int_array_class, static_target));
		lpFieldValue fv = new FieldValue();
		fv->field_value.heapPtr = static_target;
		char key[] = "GcTest.saved:[I";
		MethodArea::AddStaticField(key, fv);

		heap->gc();
		lpObject obj = NULL;
		CHECK(heap->GetObject(static_target, obj));
		// Remember the index before the reference becomes dangling; a dead
		// reference must not be dereferenced after collection.
		OBJECT_REF static_index = *static_target;

		MethodArea::ClearStaticFields();
		heap->gc();
		CHECK(static_index > 0 && static_cast<size_t>(static_index) < heap->objs.size());
		CHECK(heap->objs[static_cast<size_t>(static_index)] == NULL);
		CHECK(heap->LiveObjectCount() == 0);
	}

	// ---------------------------------------------------------------- (c)
	// Repeated allocate/collect rounds with a few long-lived roots: no growth
	// and no crash, and the survivors stay intact.
	{
		LPOBJECT_REF keep_array = NULL;
		LPOBJECT_REF keep_string = NULL;
		GCRootScope root_array(heap, &keep_array);
		GCRootScope root_string(heap, &keep_string);
		CHECK(heap->NewArrayObject(4, int_array_class, keep_array));
		char text[] = "cycle";
		CHECK(heap->NewStringObject(text, static_cast<int>(strlen(text)), keep_string));

		lpObject keep_obj = NULL;
		CHECK(heap->GetObject(keep_array, keep_obj));
		if (keep_obj)
		{
			static_cast<int*>(keep_obj->array_var)[0] = 9;
		}

		bool rounds_ok = true;
		for (int round = 0; round < 20; round++)
		{
			for (int i = 0; i < 300; i++)
			{
				LPOBJECT_REF garbage = NULL;
				if (!heap->NewArrayObject(64, int_array_class, garbage))
				{
					rounds_ok = false;
				}
			}
			heap->gc();
			int value = 0;
			int length = -1;
			if (!int_at(heap, keep_array, 0, value) || value != 9)
			{
				rounds_ok = false;
			}
			if (!string_value_length(heap, keep_string, length) || length != 5)
			{
				rounds_ok = false;
			}
		}
		CHECK(rounds_ok);
		CHECK(heap->LiveObjectCount() >= 3);
		// Freed indices are reused instead of growing the slot vector forever.
		CHECK(heap->objs.size() <= 2048);
		std::printf("gc-test: %d collection rounds, live_objects=%zu, heap_slots=%zu\n",
			20, heap->LiveObjectCount(), heap->objs.size());
	}

	// Release the last roots and collect everything.
	MethodArea::ClearStaticFields();
	heap->gc();
	CHECK(heap->LiveObjectCount() == 0);

	delete plain_class;

	std::printf("gc checks: %d/%d passed\n", g_passed, g_passed + g_failed);
	std::printf("gc stats: runs=%d freed_total=%zu bytes\n",
		heap->GcRuns(), heap->TotalFreedBytes());

	VMContext::destroy();
	if (g_failed != 0)
	{
		std::printf("gc checks: %d failed\n", g_failed);
		return 1;
	}
	return 0;
}
