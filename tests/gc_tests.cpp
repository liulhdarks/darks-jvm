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

static unsigned next_random(unsigned &state)
{
	state = state * 1103515245u + 12345u;
	return state >> 8;
}

static bool string_value_length(lpObjectHeap heap, LPOBJECT_REF ref, int &length)
{
	length = -1;
	lpObject obj = NULL;
	if (!heap->GetObject(ref, obj) || !obj || !obj->obj_class)
	{
		return false;
	}
	std::map<std::string, lpField>::iterator it = obj->obj_class->fields_map.find("value:[B");
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
	int coder = 0;
	std::map<std::string, lpField>::iterator coder_it =
		obj->obj_class->fields_map.find("coder:B");
	if (coder_it != obj->obj_class->fields_map.end())
	{
		lpFieldValue coder_fv = obj->GetFieldValue(coder_it->second);
		if (coder_fv)
		{
			coder = coder_fv->field_value.bValue & 1;
		}
	}
	length = coder ? chars->array_length / 2 : chars->array_length;
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

	// The sliced-marking check needs an automatic collection.  Pin the
	// allocation threshold so it does not depend on the host's -Xms
	// ergonomics; an explicit setting from the test harness wins.
#if defined(_WIN32)
	_putenv_s("DARKS_JVM_GC_THRESHOLD", "4194304");
#else
	setenv("DARKS_JVM_GC_THRESHOLD", "4194304", 0);
#endif

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
	// String.<clinit> keeps the CASE_INSENSITIVE_ORDER comparator reachable
	// through a static field, so the first string creation leaves one object
	// that is a legitimate root.  Measure that baseline first and require the
	// string and its char[] to be collected relative to it.
	{
		char warmup[] = "gc-string-warmup";
		LPOBJECT_REF warmup_ref = NULL;
		CHECK(heap->NewStringObject(warmup, static_cast<int>(strlen(warmup)),
		                            warmup_ref));
	}
	heap->gc();
	size_t class_init_live = heap->LiveObjectCount();
	// Class mirrors (java.lang.Class objects) are permanent global
	// references, so the live count has a floor once a class literal has
	// been resolved.  b4 records that floor; later rounds must not raise it.
	size_t permanent_live = 0;
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
	CHECK(heap->LiveObjectCount() == class_init_live);

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
		permanent_live = heap->LiveObjectCount();
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
		// -Xms reserves object slots up front, so the vector starts at its
		// steady-state size; the rounds below must reuse freed indices rather
		// than grow it any further.
		const size_t slots_before_rounds = heap->objs.size();
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
		CHECK(heap->objs.size() == slots_before_rounds);
		std::printf("gc-test: %d collection rounds, live_objects=%zu, heap_slots=%zu\n",
			20, heap->LiveObjectCount(), heap->objs.size());
	}

	// ---------------------------------------------------------------- (d)
	// ref_index consistency under random mutation.  Objects, arrays and
	// strings are allocated and linked randomly, and the heap is collected
	// every round.  After each collection the index must match a rebuild from
	// the live slot vector exactly, and no repair may have been needed.  This
	// runs unchanged with DARKS_JVM_REFINDEX=full, so both maintenance modes
	// have to reach the same index for the same live set.
	{
		const int kWidth = 32;
		LPOBJECT_REF refs[kWidth];
		lpObject expected[kWidth];
		GCRootSet roots(heap);
		for (int i = 0; i < kWidth; i++)
		{
			refs[i] = NULL;
			expected[i] = NULL;
			roots.Add(&refs[i]);
		}

		unsigned state = 0x5eed1234u;
		bool objects_stable = true;
		bool index_consistent = true;
		for (int round = 0; round < 30; round++)
		{
			int count = 1 + static_cast<int>(next_random(state) % kWidth);
			for (int i = 0; i < count; i++)
			{
				LPOBJECT_REF ref = NULL;
				unsigned kind = next_random(state) % 4;
				bool created = false;
				if (kind == 0)
				{
					created = heap->NewObject(plain_class, ref);
				}
				else if (kind == 1)
				{
					created = heap->NewArrayObject(4, int_array_class, ref);
				}
				else if (kind == 2)
				{
					created = heap->NewObjectArrayObject(3, object_array_class, ref);
				}
				else
				{
					char text[] = "refindex";
					created = heap->NewStringObject(text, 8, ref);
				}
				if (!created)
				{
					continue;
				}
				refs[i] = ref;
				expected[i] = NULL;
				heap->GetObject(ref, expected[i]);
			}

			// Random reference stores: a field on an object, an element of a
			// reference array, whichever the chosen holder supports.
			for (int k = 0; k < 24; k++)
			{
				int from = static_cast<int>(next_random(state) % count);
				int to = static_cast<int>(next_random(state) % count);
				if (!refs[from] || !refs[to] || from == to)
				{
					continue;
				}
				lpObject holder = NULL;
				if (!heap->GetObject(refs[from], holder) || !holder)
				{
					continue;
				}
				if (holder->array_var &&
				    (holder->array_data_type == DT_REF ||
				     holder->array_data_type == DT_ARRAY) &&
				    holder->array_length > 0)
				{
					int slot = static_cast<int>(next_random(state) % holder->array_length);
					static_cast<jobject*>(holder->array_var)[slot] =
						reinterpret_cast<jobject>(refs[to]);
				}
				else
				{
					set_ref_field(holder, "next:Ljava/lang/Object;", refs[to]);
				}
			}

			heap->gc();
			// Lazy sweep records the dead slots and frees them in chunks
			// afterwards; drain first so this check still compares the index
			// against exactly the survivor set.
			heap->DrainPendingFreeAll();
			for (int i = 0; i < count; i++)
			{
				if (!refs[i])
				{
					continue;
				}
				lpObject now = NULL;
				if (!heap->GetObject(refs[i], now) || now != expected[i])
				{
					objects_stable = false;
				}
			}
			if (!heap->RefIndexMatchesHeap() ||
			    heap->RefIndexEntries() != heap->LiveObjectCount())
			{
				index_consistent = false;
			}
			// Drop every root so the next round allocates into freed slots;
			// the index must forget those addresses with the objects.
			for (int i = 0; i < kWidth; i++)
			{
				refs[i] = NULL;
				expected[i] = NULL;
			}
		}
		CHECK(objects_stable);
		CHECK(index_consistent);
		CHECK(heap->RefIndexRepairs() == 0);
		std::printf("gc-test: ref_index mode=%s rounds=30 consistent=%d\n",
		            heap->RefIndexIncremental() ? "incremental" : "full",
		            index_consistent ? 1 : 0);
	}

	// ---------------------------------------------------------------- (e)
	// Lazy sweep: a collection only records the dead slots, and the objects
	// are torn down in bounded chunks after the world resumes and from the
	// allocation path.  Resolving a live reference must keep working while
	// dead slots are still pending, and once the drain finishes the survivor
	// count, the byte accounting and the index must be exactly what the
	// in-pause sweep would have produced.
	{
		const int kHeartbeat = 16;
		const int kGarbage = 3000;
		LPOBJECT_REF live[kHeartbeat];
		lpObject expected[kHeartbeat];
		GCRootSet roots(heap);
		for (int i = 0; i < kHeartbeat; i++)
		{
			live[i] = NULL;
			expected[i] = NULL;
			roots.Add(&live[i]);
		}

		unsigned state = 0x1a2b3c4du;
		bool refs_stable = true;
		bool drained_ok = true;
		bool pending_seen = false;
		size_t pending_peak = 0;
		for (int round = 0; round < 16; round++)
		{
			for (int i = 0; i < kHeartbeat; i++)
			{
				if (live[i])
				{
					continue;
				}
				LPOBJECT_REF ref = NULL;
				if (!heap->NewObject(plain_class, ref))
				{
					continue;
				}
				live[i] = ref;
				expected[i] = NULL;
				heap->GetObject(ref, expected[i]);
			}
			for (int k = 0; k < kGarbage; k++)
			{
				LPOBJECT_REF garbage = NULL;
				heap->NewArrayObject(32, int_array_class, garbage);
			}
			heap->gc();
			size_t live_before = heap->LiveObjectCount();
			size_t bytes_before = heap->LiveBytes();
			size_t pending_before = heap->PendingFreeCount();
			if (pending_before > 0)
			{
				pending_seen = true;
				if (pending_before > pending_peak)
				{
					pending_peak = pending_before;
				}
			}
			for (int i = 0; i < kHeartbeat; i++)
			{
				lpObject now = NULL;
				if (live[i] &&
				    (!heap->GetObject(live[i], now) || now != expected[i]))
				{
					refs_stable = false;
				}
			}
			heap->DrainPendingFreeAll();
			if (heap->PendingFreeCount() != 0 ||
			    heap->LiveObjectCount() != live_before ||
			    heap->LiveBytes() != bytes_before ||
			    !heap->RefIndexMatchesHeap() ||
			    heap->RefIndexEntries() != heap->LiveObjectCount())
			{
				drained_ok = false;
			}
			for (int i = 0; i < kHeartbeat; i++)
			{
				if ((next_random(state) & 1u) != 0)
				{
					live[i] = NULL;
					expected[i] = NULL;
				}
			}
		}
		CHECK(refs_stable);
		CHECK(drained_ok);
		// The pause-time sweep leaves nothing pending; the lazy one must show
		// the chunked hand-over at least once.
		CHECK(!heap->LazySweepEnabled() || pending_seen);
		std::printf("gc-test: lazy sweep mode=%s pending_peak=%zu drained=%d\n",
		            heap->LazySweepEnabled() ? "lazy" : "pause", pending_peak,
		            drained_ok ? 1 : 0);
	}

	// ---------------------------------------------------------------- (f)
	// Sliced marking: with a live set larger than one slice budget an
	// automatic collection stays open while the loop keeps allocating.  The
	// objects allocated inside that window are marked as they are registered
	// (allocate black) and every reachable element must survive the cycle.
	if (heap->IncrementalMarkEnabled())
	{
		const int kLive = 40000;
		std::vector<LPOBJECT_REF> live(kLive);
		std::vector<lpObject> expected(kLive);
		LPOBJECT_REF root_refs = NULL;
		GCRootScope root(heap, &root_refs);
		CHECK(heap->NewObjectArrayObject(kLive, object_array_class, root_refs));
		lpObject root_obj = NULL;
		CHECK(heap->GetObject(root_refs, root_obj));

		bool saw_open_cycle = false;
		int constructed = 0;
		for (int i = 0; i < kLive; i++)
		{
			LPOBJECT_REF element = NULL;
			if (!root_obj || !heap->NewArrayObject(32, int_array_class, element))
			{
				break;
			}
			lpObject element_obj = NULL;
			CHECK(heap->GetObject(element, element_obj));
			if (element_obj)
			{
				static_cast<int*>(element_obj->array_var)[0] = i;
			}
			live[i] = element;
			expected[i] = element_obj;
			static_cast<jobject*>(root_obj->array_var)[i] =
				reinterpret_cast<jobject>(element);
			constructed++;
			if (heap->MarkCycleActive())
			{
				saw_open_cycle = true;
			}
		}
		CHECK(constructed == kLive);
		CHECK(saw_open_cycle);

		// Drive the remaining slices by allocating; the cycle must close.
		int guard = 0;
		while (heap->MarkCycleActive() && guard < 400000)
		{
			LPOBJECT_REF garbage = NULL;
			CHECK(heap->NewArrayObject(32, int_array_class, garbage));
			guard++;
		}
		CHECK(!heap->MarkCycleActive());
		heap->DrainPendingFreeAll();

		bool survivors_ok = true;
		for (int i = 0; i < constructed; i++)
		{
			lpObject now = NULL;
			int value = -1;
			if (!heap->GetObject(live[i], now) || now != expected[i] ||
			    !int_at(heap, live[i], 0, value) || value != i)
			{
				survivors_ok = false;
				break;
			}
		}
		CHECK(survivors_ok);
		CHECK((size_t)constructed <= heap->LiveObjectCount());
		CHECK(heap->RefIndexMatchesHeap());
		CHECK(heap->RefIndexEntries() == heap->LiveObjectCount());
		std::printf("gc-test: sliced mark open_cycle=%d constructed=%d guard=%d\n",
		            saw_open_cycle ? 1 : 0, constructed, guard);
	}

	// ---------------------------------------------------------------- (g)
	// Reference moves across an open cycle: a payload is copied from one
	// container slot into an already traced container and the source slot
	// is cleared.  The SATB journal has to keep the moved payload alive
	// until the cycle closes, otherwise a destination slot dangles.
	if (heap->IncrementalMarkEnabled())
	{
		const int kContainers = 256;
		const int kSlots = 4;
		// Ballast keeps each cycle open across many allocations, so the
		// moves below really run between marking slices.
		LPOBJECT_REF ballast_ref = NULL;
		GCRootScope ballast_root(heap, &ballast_ref);
		CHECK(heap->NewObjectArrayObject(30000, object_array_class, ballast_ref));
		lpObject ballast = NULL;
		CHECK(heap->GetObject(ballast_ref, ballast));
		for (int i = 0; i < 30000 && ballast; i++)
		{
			LPOBJECT_REF element = NULL;
			if (!heap->NewArrayObject(8, int_array_class, element))
			{
				ballast = NULL;
				break;
			}
			static_cast<jobject*>(ballast->array_var)[i] =
				reinterpret_cast<jobject>(element);
		}
		CHECK(ballast != NULL);
		LPOBJECT_REF holders_ref = NULL;
		GCRootScope holders_root(heap, &holders_ref);
		CHECK(heap->NewObjectArrayObject(kContainers, object_array_class, holders_ref));
		lpObject holders = NULL;
		CHECK(heap->GetObject(holders_ref, holders));

		std::vector<LPOBJECT_REF> containers(kContainers);
		bool built = holders != NULL;
		for (int i = 0; i < kContainers && built; i++)
		{
			LPOBJECT_REF holder = NULL;
			LPOBJECT_REF payload = NULL;
			if (!heap->NewObjectArrayObject(kSlots, object_array_class, holder) ||
			    !heap->NewArrayObject(8, int_array_class, payload))
			{
				built = false;
				break;
			}
			lpObject holder_obj = NULL;
			lpObject payload_obj = NULL;
			if (!heap->GetObject(holder, holder_obj) || !holder_obj ||
			    !heap->GetObject(payload, payload_obj) || !payload_obj)
			{
				built = false;
				break;
			}
			static_cast<int*>(payload_obj->array_var)[0] = i;
			containers[i] = holder;
			static_cast<jobject*>(holder_obj->array_var)[0] =
				reinterpret_cast<jobject>(payload);
			static_cast<jobject*>(holders->array_var)[i] =
				reinterpret_cast<jobject>(holder);
		}
		CHECK(built);

		bool survived = built;
		int moved = 0;
		size_t max_records = 0;
		for (int round = 0; round < 4 && survived; round++)
		{
			int spin = 0;
			while (!heap->MarkCycleActive() && spin < 400000)
			{
				LPOBJECT_REF garbage = NULL;
				if (!heap->NewArrayObject(8, int_array_class, garbage))
				{
					survived = false;
					break;
				}
				spin++;
			}
			for (int step = 0; step < 1200 && survived; step++)
			{
				const int a = (step * 37 + round * 11) % kContainers;
				const int b = (step * 17 + round * 23 + 7) % kContainers;
				LPOBJECT_REF fresh = NULL;
				if (!heap->NewArrayObject(8, int_array_class, fresh))
				{
					survived = false;
					break;
				}
				lpObject fresh_obj = NULL;
				if (heap->GetObject(fresh, fresh_obj) && fresh_obj)
				{
					static_cast<int*>(fresh_obj->array_var)[0] = step;
				}
				lpObject a_obj = NULL;
				lpObject b_obj = NULL;
				if (!heap->GetObject(containers[a], a_obj) || !a_obj ||
				    !heap->GetObject(containers[b], b_obj) || !b_obj)
				{
					survived = false;
					break;
				}
				// Move the source reference into the destination slot
				// (overwriting whatever it held) and refill the source
				// with the fresh payload.  SetArrayIndex is the same
				// barrier site the interpreter uses for aastore.
				const int src_slot = step % 2;
				const int dst_slot = 2 + (step % 2);
				Slot slot;
				slot.heapPtr = reinterpret_cast<LPOBJECT_REF>(
					static_cast<jobject*>(b_obj->array_var)[src_slot]);
				a_obj->SetArrayIndex(dst_slot, slot);
				slot.heapPtr = fresh;
				b_obj->SetArrayIndex(src_slot, slot);
				if (slot.heapPtr)
				{
					moved++;
				}
			}
			if (heap->LastCycleRecords() > max_records)
			{
				max_records = heap->LastCycleRecords();
			}
			int guard = 0;
			while (heap->MarkCycleActive() && guard < 400000)
			{
				LPOBJECT_REF garbage = NULL;
				if (!heap->NewArrayObject(8, int_array_class, garbage))
				{
					survived = false;
					break;
				}
				guard++;
			}
			if (heap->LastCycleRecords() > max_records)
			{
				max_records = heap->LastCycleRecords();
			}
			heap->DrainPendingFreeAll();

			// Every reference a container still holds must resolve.
			for (int i = 0; i < kContainers && survived; i++)
			{
				lpObject holder_obj = NULL;
				if (!heap->GetObject(containers[i], holder_obj) || !holder_obj)
				{
					survived = false;
					break;
				}
				jobject *slots = static_cast<jobject*>(holder_obj->array_var);
				for (int s = 0; s < kSlots; s++)
				{
					if (!slots[s])
					{
						continue;
					}
					lpObject obj = NULL;
					if (!heap->GetObject(
					        reinterpret_cast<LPOBJECT_REF>(slots[s]), obj) ||
					    !obj)
					{
						survived = false;
						break;
					}
				}
			}
		}
		CHECK(survived);
		CHECK(max_records > 0);
		CHECK(heap->RefIndexMatchesHeap());
		std::printf("gc-test: sliced mark refmove moved=%d records=%zu survived=%d\n",
		            moved, max_records, survived ? 1 : 0);
	}

	// Release the last roots and collect everything.
	MethodArea::ClearStaticFields();
	heap->gc();
	heap->DrainPendingFreeAll();
	CHECK(heap->LiveObjectCount() == permanent_live);
	// The index must be exactly the set of live objects after the final
	// collection, in either maintenance mode.
	CHECK(heap->RefIndexMatchesHeap());
	CHECK(heap->RefIndexEntries() == heap->LiveObjectCount());

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
