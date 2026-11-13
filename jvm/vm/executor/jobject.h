#pragma once

#include "exec_common.h"
#include "jclass.h"
#include "descriptor.h"
#include <string>
#include <vector>
using namespace std;

class JObject
{

public:
	// VM-synthetic entries ("$lambda:id", "ann:<element>", ...).  Real
	// instance fields live in the slot array; only objects that need extra
	// name-keyed values pay for this vector.
	struct ExtraField
	{
		std::string key;
		lpFieldValue value;

		ExtraField(const std::string &key, lpFieldValue value)
			: key(key), value(value)
		{
		}
	};

	// Contiguous, fixed-length instance field storage.  The length comes from
	// the class layout (Class::EnsureInstanceLayout) and never changes after
	// construction, so slot addresses stay valid for the object's lifetime.
	FieldValue *slots;
	int slot_count;
	std::vector<ExtraField> *extras;

	lpClass obj_class;
	// Heap references point at this member; it must never move.
	OBJECT_REF index;

	JArray array_var;
	int array_length;
	DATA_TYPE array_data_type;

private:
	void DeleteArray();

public:
	JObject()
	{
		slots = NULL;
		slot_count = 0;
		extras = NULL;
		obj_class = NULL;
		array_length = 0;
		array_var = NULL;
		array_data_type = DT_VOID;
		index = 0;
	}

	JObject(lpClass obj_class)
	{
		slots = NULL;
		slot_count = 0;
		extras = NULL;
		this->obj_class = obj_class;
		array_length = 0;
		array_var = NULL;
		array_data_type = DT_VOID;
		index = 0;
	}

	~JObject();

	// Hot path: a resolved Field carries its slot index, so no name lookup
	// and no per-field allocation is involved.
	lpFieldValue GetFieldValue(lpField field)
	{
		if (!field)
		{
			return NULL;
		}
		int slot = field->slot;
		if (slot >= 0 && slot < slot_count)
		{
			return &slots[slot];
		}
		return GetFieldValueSlow(field);
	}

	lpFieldValue SlotAt(int slot)
	{
		return (slot >= 0 && slot < slot_count) ? &slots[slot] : NULL;
	}

	// Name-keyed access for JNI/reflection/runtime helpers.  Checks synthetic
	// entries first (they shadow a real field of the same key, matching the
	// old map overwrite), then the class slot index table.
	lpFieldValue GetFieldValueByName(const string &key);
	// Accepts either "<name>:<descriptor>" or a bare field name.
	lpFieldValue GetFieldValueByNamePrefix(const string &key);
	bool HasFieldValue(const string &key)
	{
		return GetFieldValueByName(key) != NULL;
	}

	// Takes ownership of a FieldValue created for a VM-synthetic entry and
	// publishes it in the extras list.
	void AdoptFieldValue(const string &key, lpFieldValue value);

	// Releases all field storage (used by the destructor and newInstance).
	void ReleaseFields();

	bool newInstance()
	{
		return newInstance(obj_class);
	}

	bool newInstance(lpClass obj_class);

	bool newArrayInstance(int size);

	bool newArrayInstance(DATA_TYPE data_type, int size);

	// Takes ownership of a buffer allocated with new[].
	void SetData(DATA_TYPE data_type, LPVOID data, int size);

	void SetArrayIndex(int index, Slot &slot);

private:
	lpFieldValue GetFieldValueSlow(lpField field);
};
