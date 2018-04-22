#pragma once

#include "jni.h"
#include "vm/executor/exec_common.h"
#include "vm/executor/jclass.h"
#include "vm/executor/jfield.h"
#include "vm/executor/jmethod.h"
#include "vm/executor/jobject.h"
#include "vm/executor/context.h"

LPOBJECT_REF GetVMObjectRef(jobject obj);

lpObject GetVMObject(jobject obj);

jobject GetJniObject(lpObject obj);

jobject GetJniObject(LPOBJECT_REF obj_ref);

jobject GetJniObject(lpFieldValue fv);

lpClass GetVMClass(jclass clazz);

jclass GetJniClass(lpClass clazz);

LPSTR GetFieldName(const char *name, const char *sig);

lpField GetVMField(jfieldID fieldId);

jfieldID GetJniField(lpField field);

lpFieldValue GetVMFieldValue(jobject obj, jfieldID fieldID);