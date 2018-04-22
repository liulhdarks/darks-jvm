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
	FIELD_MAP varants;
	lpClass obj_class;
	OBJECT_REF index;

	JArray array_var;
	int array_length;
	DATA_TYPE array_data_type;

	JObject()
	{
		obj_class = NULL;
		array_length = 0;
		array_var = NULL;
		array_data_type = DT_VOID;
		index = 0;
	}

	JObject(lpClass obj_class)
	{
		this->obj_class = obj_class;
		array_length = 0;
		array_var = NULL;
		array_data_type = DT_VOID;
		index = 0;
	}

	~JObject();

	lpFieldValue GetFieldValue(lpField field);

	bool newInstance()
	{
		return newInstance(obj_class);
	}

	bool newInstance(lpClass obj_class);

	bool newArrayInstance(int size);

	bool newArrayInstance(DATA_TYPE data_type, int size);

	void SetData(DATA_TYPE data_type, LPVOID data, int size);

	void SetArrayIndex(int index, Slot &slot);
};