#include "stdafx.h"
#include "jobject.h"
#include "jni/jni.h"

void JObject::DeleteArray()
{
	if (!array_var)
	{
		return;
	}
	// Deleting through a void* is undefined behaviour, so dispatch on the
	// element type. Every allocation below uses new[], so use delete[].
	switch (array_data_type)
	{
	case DT_INT:
		delete[] static_cast<jint*>(array_var);
		break;
	case DT_SHORT:
		delete[] static_cast<jshort*>(array_var);
		break;
	case DT_CHAR:
		delete[] static_cast<jchar*>(array_var);
		break;
	case DT_BYTE:
	case DT_BOOL:
		delete[] static_cast<jbyte*>(array_var);
		break;
	case DT_LONG:
		delete[] static_cast<jlong*>(array_var);
		break;
	case DT_FLOAT:
		delete[] static_cast<jfloat*>(array_var);
		break;
	case DT_DOUBLE:
		delete[] static_cast<jdouble*>(array_var);
		break;
	case DT_ARRAY:
	case DT_REF:
		delete[] static_cast<jobject*>(array_var);
		break;
	default:
		break;
	}
	array_var = NULL;
}

void JObject::ReleaseFields()
{
	delete[] slots;
	slots = NULL;
	slot_count = 0;
	if (extras)
	{
		for (size_t i = 0; i < extras->size(); i++)
		{
			delete (*extras)[i].value;
		}
		delete extras;
		extras = NULL;
	}
}

JObject::~JObject()
{
	ReleaseFields();
	obj_class = NULL;
	DeleteArray();
}

bool JObject::newInstance(lpClass obj_class)
{
	this->obj_class = obj_class;
	ReleaseFields();
	if (!obj_class)
	{
		return false;
	}
	// One contiguous allocation for every slot in the superclass chain.  A
	// hidden superclass field keeps its own slot, like the JVM layout.
	int total = obj_class->EnsureInstanceLayout();
	if (total > 0)
	{
		slots = new FieldValue[static_cast<size_t>(total)];
		slot_count = total;
	}
	for (lpClass c = obj_class; c; c = c->super_class)
	{
		for (int i = 0; i < c->fieldCount; i++)
		{
			lpField field = c->fields[i];
			if (!field)
			{
				continue;
			}
			if ((field->accFlag & ACF_STATIC) != 0)
			{
				continue;
			}
			if (field->slot < 0 || field->slot >= slot_count)
			{
				continue;
			}
			FieldValue *fv = &slots[field->slot];
			fv->field = field;
			fv->initialize();
		}
	}
	return true;
}

void JObject::AdoptFieldValue(const string &key, lpFieldValue value)
{
	if (!value)
	{
		return;
	}
	if (!extras)
	{
		extras = new std::vector<ExtraField>();
	}
	extras->push_back(ExtraField(key, value));
}

lpFieldValue JObject::GetFieldValueSlow(lpField field)
{
	if (!field)
	{
		return NULL;
	}
	// Layouts are normally computed when the object is created.  A field
	// resolved through reflection on a class whose layout was not ready yet
	// gets one more chance here; the object's slot array is still the
	// authority for bounds.
	if (obj_class)
	{
		obj_class->EnsureInstanceLayout();
		int slot = field->slot;
		if (slot >= 0 && slot < slot_count)
		{
			return &slots[slot];
		}
	}
	// Fall back to the name index (covers objects whose class metadata did
	// not assign this particular field a slot).
	return GetFieldValueByName(std::string(field->GetSimpleName()));
}

lpFieldValue JObject::GetFieldValueByName(const string &key)
{
	if (extras)
	{
		// Later adoptions win, mirroring map assignment semantics.
		for (size_t i = extras->size(); i > 0; i--)
		{
			if ((*extras)[i - 1].key == key)
			{
				return (*extras)[i - 1].value;
			}
		}
	}
	if (obj_class)
	{
		int slot = obj_class->FindInstanceSlot(key);
		if (slot >= 0 && slot < slot_count)
		{
			return &slots[slot];
		}
	}
	return NULL;
}

lpFieldValue JObject::GetFieldValueByNamePrefix(const string &key)
{
	lpFieldValue exact = GetFieldValueByName(key);
	if (exact)
	{
		return exact;
	}
	if (obj_class)
	{
		int slot = obj_class->FindInstanceSlotByName(key.c_str());
		if (slot >= 0 && slot < slot_count)
		{
			return &slots[slot];
		}
	}
	return NULL;
}

bool JObject::newArrayInstance(int size)
{
	TypeDesc type_desc;
	LPSTR desc = obj_class && obj_class->thisClass ? obj_class->thisClass->GetName() : NULL;
	if (!desc)
	{
		return false;
	}
	parse_desc_type(desc, type_desc, static_cast<int>(strlen(desc)));
	return newArrayInstance(type_desc.data_type, size);
}

bool JObject::newArrayInstance(DATA_TYPE data_type, int size)
{
	if (size < 0)
	{
		return false;
	}
	DeleteArray();
	this->array_data_type = data_type;
	this->array_length = size;
	int alloc = size > 0 ? size : 1;
	switch (data_type)
	{
	case DT_INT:
		array_var = (JArray) (new jint[alloc]);
		memset(array_var, 0 , sizeof(jint) * alloc);
		break;
	case DT_SHORT:
		array_var = (JArray) (new jshort[alloc]);
		memset(array_var, 0 , sizeof(jshort) * alloc);
		break;
	case DT_CHAR:
		array_var = (JArray) (new jchar[alloc]);
		memset(array_var, 0 , sizeof(jchar) * alloc);
		break;
	case DT_BYTE:
		array_var = (JArray) (new jbyte[alloc]);
		memset(array_var, 0 , sizeof(jbyte) * alloc);
		break;
	case DT_LONG:
		array_var = (JArray) (new jlong[alloc]);
		memset(array_var, 0 , sizeof(jlong) * alloc);
		break;
	case DT_FLOAT:
		array_var = (JArray) (new jfloat[alloc]);
		memset(array_var, 0 , sizeof(jfloat) * alloc);
		break;
	case DT_DOUBLE:
		array_var = (JArray) (new jdouble[alloc]);
		memset(array_var, 0 , sizeof(jdouble) * alloc);
		break;
	case DT_BOOL:
		array_var = (JArray) (new jboolean[alloc]);
		memset(array_var, 0 , sizeof(jboolean) * alloc);
		break;
	case DT_ARRAY:
	case DT_REF:
		array_var = (JArray) (new jobject[alloc]);
		memset(array_var, 0 , sizeof(jobject) * alloc);
		break;
	default:
		return false;
	}
	if (!array_var)
	{
		return false;
	}
	return true;
}

void JObject::SetData(DATA_TYPE data_type, LPVOID data, int size)
{
	DeleteArray();
	this->array_data_type = data_type;
	this->array_length = size;
	this->array_var = (JArray)data;
}

void JObject::SetArrayIndex(int index, Slot &slot)
{
	if (!array_var || index < 0 || index >= array_length)
	{
		throw new VMExecutorException((LPSTR)"Array index out of bounds.");
	}
	switch (array_data_type)
	{
	case DT_INT:
		((jint*)array_var)[index] = slot.iValue;
		break;
	case DT_SHORT:
		((jshort*)array_var)[index] = slot.sValue;
		break;
	case DT_CHAR:
		((jchar*)array_var)[index] = slot.cValue;
		break;
	case DT_BYTE:
		((jbyte*)array_var)[index] = slot.bValue;
		break;
	case DT_LONG:
		((jlong*)array_var)[index] = slot.lValue;
		break;
	case DT_FLOAT:
		((jfloat*)array_var)[index] = slot.fValue;
		break;
	case DT_DOUBLE:
		((jdouble*)array_var)[index] = slot.dValue;
		break;
	case DT_BOOL:
		((jboolean*)array_var)[index] = slot.blValue;
		break;
	case DT_ARRAY:
	case DT_REF:
		((jobject*)array_var)[index] = (jobject)slot.heapPtr;
		break;
	default:
		break;
	}
}
