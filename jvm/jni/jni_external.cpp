#include "stdafx.h"
#include "jni_external.h"

LPOBJECT_REF GetVMObjectRef(jobject obj)
{
	return (LPOBJECT_REF)obj;
}

lpObject GetVMObject(jobject obj)
{
	lpObject object = NULL;
	LPOBJECT_REF obj_ref = GetVMObjectRef(obj);
	if (!VMContext::object_heap->GetObject(obj_ref, object))
	{
		return NULL;
	}
	return object;
}

jobject GetJniObject(lpObject obj)
{
	LPOBJECT_REF obj_ref = &obj->index;
	return GetJniObject(obj_ref);
}

jobject GetJniObject(LPOBJECT_REF obj_ref)
{
	return (jobject)obj_ref;
}

jobject GetJniObject(lpFieldValue fv)
{
	return GetJniObject(fv->field_value.heapPtr);
}


lpClass GetVMClass(jclass clazz)
{
	return (lpClass) clazz;
}


jclass GetJniClass(lpClass clazz)
{
	return (jclass)clazz;
}

LPSTR GetFieldName(const char *name, const char *sig)
{
	int len = strlen(name) + strlen(sig) + 2;
	LPSTR ret = new char[len];
	sprintf(ret, "%s:%s", name, sig);
	return ret;
}

lpField GetVMField(jfieldID fieldId)
{
	return (lpField)fieldId;
}

jfieldID GetJniField(lpField field)
{
	return (jfieldID)field;
}

lpFieldValue GetVMFieldValue(jobject obj, jfieldID fieldID)
{
	lpField field = GetVMField(fieldID);
	if (!field)
	{
		return NULL;
	}

	lpObject object = GetVMObject(obj);
	if (!object)
	{
		return NULL;
	}
	return object->GetFieldValue(field);
}