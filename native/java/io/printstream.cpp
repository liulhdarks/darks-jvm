#include "stdafx.h"
#include "printstream.h"
#include "jni/jni_external.h"
#include "platform/platform.h"
#include "vm/executor/jobject.h"

#include <cstdio>
#include <string>
#include <vector>

namespace {

void write_jstring(jstring str)
{
	lpObject obj = GetVMObject((jobject)str);
	if (!obj || !obj->obj_class)
	{
		dk::DebugPrint("printstream: no string object\n");
		return;
	}
	map<string, lpField>::iterator it = obj->obj_class->fields_map.find("value:[C");
	if (it == obj->obj_class->fields_map.end())
	{
		/* no value field */
		return;
	}
	lpFieldValue fv = obj->GetFieldValue(it->second);
	if (!fv || !fv->field_value.heapPtr)
	{
		/* no field value */
		return;
	}
	lpObject chars = NULL;
	if (!VMContext::object_heap->GetObject(fv->field_value.heapPtr, chars) ||
	    !chars || !chars->array_var)
	{
		/* no char array */
		return;
	}
	const jchar *data = static_cast<const jchar*>(chars->array_var);
	std::vector<uint16_t> units(data, data + chars->array_length);
	std::string utf8 = dk::Utf16ToUtf8(units);
	if (!utf8.empty())
	{
		fwrite(utf8.data(), 1, utf8.size(), stdout);
		fflush(stdout);
	}
}

} // namespace

JNIEXPORT void JNICALL Java_java_io_PrintStream_write0
	(JNIEnv *, jobject thiz, jstring str)
{
	write_jstring(str);
}

JNIEXPORT void JNICALL Java_java_io_PrintStream_println
	(JNIEnv *, jobject, jstring str)
{
	dk::DebugPrint("printstream: println(string)\n");
	write_jstring(str);
	fputc('\n', stdout);
	fflush(stdout);
}
