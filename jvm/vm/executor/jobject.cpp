#include "stdafx.h"
#include "jobject.h"
#include "jni/jni.h"

JObject::~JObject()
{
	FIELD_MAP::iterator it = varants.begin();
	for(; it != varants.end(); it++)
	{
		if (it->second)
		{
			delete it->second;
			it->second = NULL;
		}
	}
	varants.clear();
	obj_class = NULL;
	if (array_var)
	{
		delete[] array_var;
		array_var = NULL;
	}
}

bool JObject::newInstance(lpClass obj_class)
{
	this->obj_class = obj_class;
	if (!obj_class)
	{
		return false;
	}
	if (obj_class->fieldCount <= 0)
	{
		return true;
	}

	for (int i = 0; i < obj_class->fieldCount; i++)
	{
		lpField field = obj_class->fields[i];
		lpFieldValue fv = new FieldValue(field);
		fv->initialize();
		varants[string(field->GetSimpleName())] = fv;
	}

	return true;
}

bool JObject::newArrayInstance(int size)
{
	TypeDesc type_desc;
	LPSTR desc = obj_class->thisClass->GetName();
	parse_desc_type(desc, type_desc, strlen(desc));
	return newArrayInstance(type_desc.data_type, size);
}

bool JObject::newArrayInstance(DATA_TYPE data_type, int size)
{
	this->array_data_type = data_type;
	this->array_length = size;
	switch (data_type)
	{
	case DT_INT:
		array_var = (JArray) (new jint[size]);
		memset(array_var, 0 , sizeof(jint) * size);
		break;
	case DT_SHORT:
		array_var = (JArray) (new jshort[size]);
		memset(array_var, 0 , sizeof(jshort) * size);
		break;
	case DT_CHAR:
		array_var = (JArray) (new jchar[size + 1]);
		memset(array_var, 0 , sizeof(jchar) * (size + 1));
		break;
	case DT_BYTE:
		array_var = (JArray) (new jbyte[size]);
		memset(array_var, 0 , sizeof(jbyte) * size);
		break;
	case DT_LONG:
		array_var = (JArray) (new jlong[size]);
		memset(array_var, 0 , sizeof(jlong) * size);
		break;
	case DT_FLOAT:
		array_var = (JArray) (new jfloat[size]);
		memset(array_var, 0 , sizeof(jfloat) * size);
		break;
	case DT_DOUBLE:
		array_var = (JArray) (new jdouble[size]);
		memset(array_var, 0 , sizeof(jdouble) * size);
		break;
	case DT_BOOL:
		array_var = (JArray) (new jboolean[size]);
		memset(array_var, 0 , sizeof(jboolean) * size);
		break;
	case DT_REF:
		array_var = (JArray) (new jobject[size]);
		memset(array_var, 0 , sizeof(jobject) * size);
		break;
	}
	if (!array_var)
	{
		return false;
	}
	return true;
}

void JObject::SetData(DATA_TYPE data_type, LPVOID data, int size)
{
	if (array_var)
	{
		delete[] array_var;
		array_var = NULL;
	}
	this->array_data_type = data_type;
	this->array_length = size;
	this->array_var = (JArray)data;
}

void JObject::SetArrayIndex(int index, Slot &slot)
{
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
	case DT_REF:
		((jobject*)array_var)[index] = (jobject)slot.heapPtr;
		break;
	}
}

lpFieldValue JObject::GetFieldValue(lpField field)
{
	return varants[string(field->simple_name)];
}