#pragma once

#include "exec_common.h"
#include "jclass.h"
#include <map>
#include "descriptor.h"
using namespace std;

typedef map<string, lpFieldValue> FIELD_MAP;

class JObject
{

public:
	// Guards JObject::FromRef: only objects created by this VM carry it, so a
	// non-reference slot value is rejected instead of being reinterpreted.
	static const uint64_t OBJECT_MAGIC = 0x6a6f626a65637431ULL;
	uint64_t magic;

	// Name-keyed view of all values (real instance fields and VM-synthetic
	// entries such as "$lambda:id").  It does not own the values.
	FIELD_MAP varants;
	// Slot-indexed instance field storage; a hidden superclass field keeps
	// its own slot.  Not owning either; ownership is in owned_values.
	vector<lpFieldValue> instance_slots;
	vector<lpFieldValue> owned_values;
	lpClass obj_class;
	OBJECT_REF index;

	JArray array_var;
	int array_length;
	DATA_TYPE array_data_type;

private:
	void DeleteArray();

public:
	JObject()
	{
		magic = OBJECT_MAGIC;
		obj_class = NULL;
		array_length = 0;
		array_var = NULL;
		array_data_type = DT_VOID;
		index = 0;
	}

	JObject(lpClass obj_class)
	{
		magic = OBJECT_MAGIC;
		this->obj_class = obj_class;
		array_length = 0;
		array_var = NULL;
		array_data_type = DT_VOID;
		index = 0;
	}

	~JObject();

	lpFieldValue GetFieldValue(lpField field);

	// Converts an interpreter reference (&object->index) back to the object
	// without the heap index map. Only valid for a live reference held in a GC
	// root (the interpreter operand stack/locals), which the collector scans.
	static lpObject FromRef(LPOBJECT_REF ref);

	// Takes ownership of a FieldValue created for a VM-synthetic entry and
	// publishes it in the name map.
	void AdoptFieldValue(const string &key, lpFieldValue value);

	lpFieldValue GetFieldValueByName(const string &key)
	{
		FIELD_MAP::iterator it = varants.find(key);
		return it == varants.end() ? NULL : it->second;
	}

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
};
