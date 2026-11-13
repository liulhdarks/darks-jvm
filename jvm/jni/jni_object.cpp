// JNI class, object, method and field functions.

#include "stdafx.h"
#include "jni_impl.h"

#include "vm/executor/exec_common.h"
#include "vm/executor/stackframe.h"
#include "vm/classfiles/parseclass.h"

#include <cstdarg>
#include <cstring>
#include <map>
#include <string>
#include <vector>

namespace {

lpThreadStack JniStack(JNIEnv *env)
{
	return env ? reinterpret_cast<lpThreadStack>(env->thread_stack) : NULL;
}

std::string JniInternalName(const char *name)
{
	std::string result(name ? name : "");
	for (size_t i = 0; i < result.size(); i++)
	{
		if (result[i] == '.')
		{
			result[i] = '/';
		}
	}
	return result;
}

lpClass DeclaringClass(lpClass clazz, lpField field)
{
	for (lpClass c = clazz; c; c = c->super_class)
	{
		for (U2 i = 0; i < c->fieldCount; i++)
		{
			if (c->fields && c->fields[i] == field)
			{
				return c;
			}
		}
	}
	return clazz;
}

jvalue DefaultValue(DATA_TYPE type)
{
	jvalue value;
	memset(&value, 0, sizeof(value));
	(void)type;
	return value;
}

} // namespace

// ---------------------------------------------------------------------------
// Class lookup
// ---------------------------------------------------------------------------

lpClass JniFindClass(JNIEnv *env, const char *name)
{
	(void)env;
	if (!name || !VMContext::bootstrap_classloader)
	{
		return NULL;
	}
	std::string internal = JniInternalName(name);
	lpClass clazz = NULL;
	try
	{
		if (!VMContext::bootstrap_classloader->FindClass(internal.c_str(), clazz))
		{
			return NULL;
		}
	}
	catch (VMException *error)
	{
		// FindClass throws heap-allocated VMException objects for missing
		// classes; JNI reports the failure by returning NULL.
		delete error;
		return NULL;
	}
	catch (...)
	{
		return NULL;
	}
	return clazz;
}

lpMethod JniFindMethod(lpClass clazz, const char *name, const char *sig, bool isStatic)
{
	if (!clazz || !name || !sig)
	{
		return NULL;
	}
	std::string key = std::string(name) + ":" + sig;
	for (lpClass c = clazz; c; c = c->super_class)
	{
		lpMethod cached = MethodArea::GetMethod(c, const_cast<char *>(key.c_str()));
		if (cached && cached->isStatic() == isStatic)
		{
			return cached;
		}
		for (U2 i = 0; i < c->methodCount; i++)
		{
			lpMethod m = c->methods ? c->methods[i] : NULL;
			if (!m || !m->methodName || !m->methodDesc)
			{
				continue;
			}
			if (strcmp(m->methodName->GetName(), name) == 0 &&
			    strcmp(m->methodDesc->GetName(), sig) == 0 &&
			    m->isStatic() == isStatic)
			{
				return m;
			}
		}
	}
	return NULL;
}

lpField JniFindField(lpClass clazz, const char *name, const char *sig, bool isStatic)
{
	if (!clazz || !name || !sig)
	{
		return NULL;
	}
	std::string key = std::string(name) + ":" + sig;
	for (lpClass c = clazz; c; c = c->super_class)
	{
		std::map<std::string, lpField>::iterator it = c->fields_map.find(key);
		if (it != c->fields_map.end() && it->second)
		{
			bool field_static = (it->second->accFlag & ACF_STATIC) != 0;
			if (field_static == isStatic)
			{
				return it->second;
			}
		}
		for (U2 i = 0; i < c->fieldCount; i++)
		{
			lpField f = c->fields ? c->fields[i] : NULL;
			if (!f || !f->fieldName || !f->fieldDesc)
			{
				continue;
			}
			if (strcmp(f->fieldName->GetName(), name) == 0 &&
			    strcmp(f->fieldDesc->GetName(), sig) == 0 &&
			    (((f->accFlag & ACF_STATIC) != 0) == isStatic))
			{
				return f;
			}
		}
	}
	return NULL;
}

lpFieldValue JniStaticFieldValue(lpClass clazz, lpField field, bool create)
{
	if (!field)
	{
		return NULL;
	}
	// Static storage belongs to the class that declares the field, so a
	// subclass access and a superclass access share the same slot.
	lpClass declaring = DeclaringClass(clazz, field);
	if (!declaring)
	{
		declaring = clazz;
	}
	if (!declaring)
	{
		return NULL;
	}
	if (!declaring->static_layout_ready.load(std::memory_order_acquire))
	{
		if (!create)
		{
			return NULL;
		}
		declaring->EnsureStaticLayout();
	}
	int slot = field->static_slot;
	if (slot < 0)
	{
		// Defensive fallback for a Field whose layout was built by another
		// Class object (should not happen).
		slot = declaring->FindStaticSlot(std::string(field->GetSimpleName()));
	}
	if (slot < 0 || slot >= declaring->static_field_total || !declaring->static_slots)
	{
		return NULL;
	}
	return &declaring->static_slots[slot];
}

lpFieldValue JniInstanceFieldValue(jobject obj, jfieldID field)
{
	lpObject object = JniResolveObject(obj);
	lpField f = JniResolveField(field);
	if (!object || !f)
	{
		return NULL;
	}
	return object->GetFieldValue(f);
}

// ---------------------------------------------------------------------------
// Invocation
// ---------------------------------------------------------------------------

namespace {

bool MethodIsAbstract(lpMethod method)
{
	return (method->accFlag & ACM_ABSTRACT) != 0;
}

void PushValue(std::vector<Slot> &vars, DATA_TYPE type, const jvalue &value)
{
	Slot slot;
	slot.dValue = 0;
	switch (type)
	{
	case DT_BOOL:
		slot.iValue = value.z ? 1 : 0;
		vars.push_back(slot);
		break;
	case DT_BYTE:
		slot.iValue = value.b;
		vars.push_back(slot);
		break;
	case DT_CHAR:
		slot.cValue = static_cast<uint16_t>(value.c);
		vars.push_back(slot);
		break;
	case DT_SHORT:
		slot.sValue = value.s;
		vars.push_back(slot);
		break;
	case DT_INT:
		slot.iValue = value.i;
		vars.push_back(slot);
		break;
	case DT_FLOAT:
		slot.fValue = value.f;
		vars.push_back(slot);
		break;
	case DT_LONG:
		{
			WByteValue wide;
			wide.lValue = value.j;
			Slot low, high;
			low.set(wide.bytes, 4);
			high.set(wide.bytes + 4, 4);
			vars.push_back(low);
			vars.push_back(high);
			break;
		}
	case DT_DOUBLE:
		{
			WByteValue wide;
			wide.dValue = value.d;
			Slot low, high;
			low.set(wide.bytes, 4);
			high.set(wide.bytes + 4, 4);
			vars.push_back(low);
			vars.push_back(high);
			break;
		}
	case DT_ARRAY:
	case DT_REF:
		slot.heapPtr = JniRef(value.l);
		vars.push_back(slot);
		break;
	default:
		break;
	}
}

jvalue ResultToValue(lpThreadStack stack, DATA_TYPE type)
{
	jvalue value;
	memset(&value, 0, sizeof(value));
	if (!stack || !stack->has_return)
	{
		return value;
	}
	switch (type)
	{
	case DT_BOOL:
		value.z = stack->return_value.iValue ? JNI_TRUE : JNI_FALSE;
		break;
	case DT_BYTE:
		value.b = static_cast<jbyte>(stack->return_value.iValue);
		break;
	case DT_CHAR:
		value.c = static_cast<jchar>(stack->return_value.cValue);
		break;
	case DT_SHORT:
		value.s = static_cast<jshort>(stack->return_value.iValue);
		break;
	case DT_INT:
		value.i = stack->return_value.iValue;
		break;
	case DT_FLOAT:
		value.f = stack->return_value.fValue;
		break;
	case DT_LONG:
		{
			WByteValue wide;
			Slot low = stack->return_value;
			Slot high = stack->return_value_hi;
			for (int i = 0; i < 4; i++)
			{
				wide.bytes[i] = low.bytes[i];
				wide.bytes[4 + i] = high.bytes[i];
			}
			value.j = wide.lValue;
			break;
		}
	case DT_DOUBLE:
		{
			WByteValue wide;
			Slot low = stack->return_value;
			Slot high = stack->return_value_hi;
			for (int i = 0; i < 4; i++)
			{
				wide.bytes[i] = low.bytes[i];
				wide.bytes[4 + i] = high.bytes[i];
			}
			value.d = wide.dValue;
			break;
		}
	case DT_ARRAY:
	case DT_REF:
		value.l = JniObject(stack->return_value.heapPtr);
		break;
	default:
		break;
	}
	return value;
}

} // namespace

jvalue JniInvokeMethod(JNIEnv *env, jobject receiver, jclass clazz,
                       jmethodID method, const jvalue *args,
                       bool isStatic, bool nonvirtual)
{
	lpThreadStack stack = JniStack(env);
	lpMethod target = JniResolveMethod(method);
	jvalue result = DefaultValue(DT_VOID);
	if (!stack || !target)
	{
		return result;
	}

	lpClass declaring = target->clazz;
	if (!isStatic && !nonvirtual && receiver)
	{
		lpObject object = JniResolveObject(receiver);
		if (!object)
		{
			VMContext::pending_exception = CreateJavaException(
				"java/lang/NullPointerException", "null object reference");
			return result;
		}
		lpMethod override = NULL;
		if (object->obj_class &&
		    object->obj_class->FindMethod(target->fullName, override) && override)
		{
			target = override;
			declaring = override->clazz;
		}
	}
	if (isStatic && clazz)
	{
		declaring = JniResolveClass(clazz);
	}
	if (!declaring && target->clazz)
	{
		declaring = target->clazz;
	}

	if (MethodIsAbstract(target) || !target->code_attr)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/AbstractMethodError",
			target->fullName ? target->fullName : "abstract method");
		return result;
	}

	std::vector<TypeDesc> type_desc;
	TypeDesc result_desc;
	if (!parse_method_desc_type(target->methodDesc->GetName(), type_desc, result_desc))
	{
		return result;
	}

	std::vector<Slot> vars;
	if (!isStatic)
	{
		Slot this_slot;
		this_slot.heapPtr = JniRef(receiver);
		vars.push_back(this_slot);
	}
	for (size_t i = 0; i < type_desc.size(); i++)
	{
		DATA_TYPE type = type_desc[i].data_type;
		if (type_desc[i].check_array)
		{
			type = DT_ARRAY;
		}
		PushValue(vars, type, args ? args[i] : DefaultValue(type));
	}

	EventNotify ev = stack->execute(declaring, target, vars.empty() ? NULL : &vars[0],
	                                static_cast<int>(vars.size()));
	if (ev == EN_EXCEPTION || VMContext::pending_exception)
	{
		return DefaultValue(result_desc.data_type);
	}
	result = ResultToValue(stack, result_desc.data_type);
	if (result_desc.check_array || result_desc.data_type == DT_REF)
	{
		DarksVM_AddLocalRef(env, result.l);
	}
	return result;
}

namespace {

std::vector<jvalue> ArgsFromVaList(jmethodID method, va_list ap)
{
	std::vector<jvalue> values;
	lpMethod target = JniResolveMethod(method);
	if (!target || !target->methodDesc)
	{
		return values;
	}
	std::vector<TypeDesc> type_desc;
	TypeDesc result_desc;
	if (!parse_method_desc_type(target->methodDesc->GetName(), type_desc, result_desc))
	{
		return values;
	}
	for (size_t i = 0; i < type_desc.size(); i++)
	{
		jvalue value;
		memset(&value, 0, sizeof(value));
		DATA_TYPE type = type_desc[i].data_type;
		if (type_desc[i].check_array)
		{
			type = DT_ARRAY;
		}
		switch (type)
		{
		case DT_BOOL:
			value.z = static_cast<jboolean>(va_arg(ap, jint));
			break;
		case DT_BYTE:
			value.b = static_cast<jbyte>(va_arg(ap, jint));
			break;
		case DT_CHAR:
			value.c = static_cast<jchar>(va_arg(ap, jint));
			break;
		case DT_SHORT:
			value.s = static_cast<jshort>(va_arg(ap, jint));
			break;
		case DT_INT:
			value.i = va_arg(ap, jint);
			break;
		case DT_FLOAT:
			// Default argument promotion: float travels as double.
			value.f = static_cast<jfloat>(va_arg(ap, jdouble));
			break;
		case DT_LONG:
			value.j = va_arg(ap, jlong);
			break;
		case DT_DOUBLE:
			value.d = va_arg(ap, jdouble);
			break;
		case DT_ARRAY:
		case DT_REF:
			value.l = va_arg(ap, jobject);
			break;
		default:
			break;
		}
		values.push_back(value);
	}
	return values;
}

} // namespace

// ---------------------------------------------------------------------------
// DefineClass / FindClass
// ---------------------------------------------------------------------------

extern "C" JNIEXPORT jclass JNICALL JNI_DefineClass(JNIEnv *env, const char *name,
	jobject loader, const jbyte *buf, jsize len)
{
	(void)loader;
	if (!buf || len <= 0 || !VMContext::bootstrap_classloader)
	{
		return NULL;
	}
	BYTE *bytes = new BYTE[static_cast<size_t>(len)];
	memcpy(bytes, buf, static_cast<size_t>(len));
	lpClass clazz = NULL;
	bool ok = false;
	try
	{
		ok = parse_class_bytes(bytes, static_cast<int>(len), clazz);
	}
	catch (...)
	{
		ok = false;
	}
	if (!ok || !clazz)
	{
		return NULL;
	}
	if (name && *name)
	{
		// parse_class_bytes already filled thisClass from the constant pool;
		// the name argument is informational.
	}
	VMContext::bootstrap_classloader->StoreClass(clazz);
	clazz->classloader = VMContext::bootstrap_classloader;
	clazz->initialize();
	(void)env;
	return reinterpret_cast<jclass>(clazz);
}

extern "C" JNIEXPORT jclass JNICALL JNI_FindClass(JNIEnv *env, const char *name)
{
	return reinterpret_cast<jclass>(JniFindClass(env, name));
}

extern "C" JNIEXPORT jclass JNICALL JNI_GetSuperclass(JNIEnv *env, jclass sub)
{
	(void)env;
	lpClass clazz = JniResolveClass(sub);
	if (!clazz || !clazz->super_class)
	{
		return NULL;
	}
	return reinterpret_cast<jclass>(clazz->super_class);
}

extern "C" JNIEXPORT jboolean JNICALL JNI_IsAssignableFrom(JNIEnv *env, jclass sub, jclass sup)
{
	(void)env;
	lpClass s = JniResolveClass(sub);
	lpClass t = JniResolveClass(sup);
	if (!s || !t)
	{
		return JNI_FALSE;
	}
	return ClassIsAssignable(t, s) ? JNI_TRUE : JNI_FALSE;
}

// ---------------------------------------------------------------------------
// Object creation
// ---------------------------------------------------------------------------

extern "C" JNIEXPORT jclass JNICALL JNI_GetObjectClass(JNIEnv *env, jobject obj)
{
	(void)env;
	lpObject object = JniResolveObject(obj);
	if (!object || !object->obj_class)
	{
		return NULL;
	}
	return reinterpret_cast<jclass>(object->obj_class);
}

extern "C" JNIEXPORT jboolean JNICALL JNI_IsInstanceOf(JNIEnv *env, jobject obj, jclass clazz)
{
	(void)env;
	lpObject object = JniResolveObject(obj);
	lpClass target = JniResolveClass(clazz);
	if (!object || !target)
	{
		return JNI_FALSE;
	}
	return IsInstanceOfClass(object, target) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jobject JNICALL JNI_AllocObject(JNIEnv *env, jclass clazz)
{
	lpClass c = JniResolveClass(clazz);
	if (!c || !VMContext::object_heap || c->isInterface())
	{
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewObject(c, ref) || !ref)
	{
		return NULL;
	}
	jobject result = JniObject(ref);
	JniAddLocalRef(env, result);
	return result;
}

extern "C" JNIEXPORT jobject JNICALL JNI_NewObject(JNIEnv *env, jclass clazz,
	jmethodID methodID, ...)
{
	va_list ap;
	va_start(ap, methodID);
	std::vector<jvalue> args = ArgsFromVaList(methodID, ap);
	va_end(ap);
	jobject obj = JNI_AllocObject(env, clazz);
	if (!obj)
	{
		return NULL;
	}
	jvalue ignored = JniInvokeMethod(env, obj, clazz, methodID,
		args.empty() ? NULL : &args[0], false, true);
	(void)ignored;
	if (VMContext::pending_exception)
	{
		return NULL;
	}
	return obj;
}

extern "C" JNIEXPORT jobject JNICALL JNI_NewObjectV(JNIEnv *env, jclass clazz,
	jmethodID methodID, va_list args)
{
	jobject obj = JNI_AllocObject(env, clazz);
	if (!obj)
	{
		return NULL;
	}
	std::vector<jvalue> values = ArgsFromVaList(methodID, args);
	JniInvokeMethod(env, obj, clazz, methodID,
		values.empty() ? NULL : &values[0], false, true);
	if (VMContext::pending_exception)
	{
		return NULL;
	}
	return obj;
}

extern "C" JNIEXPORT jobject JNICALL JNI_NewObjectA(JNIEnv *env, jclass clazz,
	jmethodID methodID, const jvalue *args)
{
	jobject obj = JNI_AllocObject(env, clazz);
	if (!obj)
	{
		return NULL;
	}
	JniInvokeMethod(env, obj, clazz, methodID, args, false, true);
	if (VMContext::pending_exception)
	{
		return NULL;
	}
	return obj;
}

// ---------------------------------------------------------------------------
// Method IDs
// ---------------------------------------------------------------------------

extern "C" JNIEXPORT jmethodID JNICALL JNI_GetMethodID(JNIEnv *env, jclass clazz,
	const char *name, const char *sig)
{
	(void)env;
	return reinterpret_cast<jmethodID>(JniFindMethod(JniResolveClass(clazz), name, sig, false));
}

extern "C" JNIEXPORT jmethodID JNICALL JNI_GetStaticMethodID(JNIEnv *env, jclass clazz,
	const char *name, const char *sig)
{
	(void)env;
	lpClass c = JniResolveClass(clazz);
	lpMethod method = JniFindMethod(c, name, sig, true);
	if (!method)
	{
		return NULL;
	}
	DarksVM_EnsureInitialized(c);
	return reinterpret_cast<jmethodID>(method);
}

// ---------------------------------------------------------------------------
// Call*Method wrappers
// ---------------------------------------------------------------------------

#define JNI_CALL_PRIMITIVE(NAME, TYPE, FIELD, DEFAULT_TYPE, IS_STATIC, NONVIRTUAL) \
extern "C" JNIEXPORT TYPE JNICALL NAME(JNIEnv *env, jobject obj, jmethodID mid, ...) \
{ \
	va_list ap; \
	va_start(ap, mid); \
	std::vector<jvalue> args = ArgsFromVaList(mid, ap); \
	va_end(ap); \
	jvalue r = JniInvokeMethod(env, obj, NULL, mid, args.empty() ? NULL : &args[0], \
	                           IS_STATIC, NONVIRTUAL); \
	return r.FIELD; \
}

#define JNI_CALL_PRIMITIVE_A(NAME, TYPE, FIELD, IS_STATIC, NONVIRTUAL) \
extern "C" JNIEXPORT TYPE JNICALL NAME(JNIEnv *env, jobject obj, jmethodID mid, \
	const jvalue *args) \
{ \
	jvalue r = JniInvokeMethod(env, obj, NULL, mid, args, IS_STATIC, NONVIRTUAL); \
	return r.FIELD; \
}

#define JNI_CALL_PRIMITIVE_V(NAME, TYPE, FIELD, IS_STATIC, NONVIRTUAL) \
extern "C" JNIEXPORT TYPE JNICALL NAME(JNIEnv *env, jobject obj, jmethodID mid, \
	va_list args) \
{ \
	std::vector<jvalue> values = ArgsFromVaList(mid, args); \
	jvalue r = JniInvokeMethod(env, obj, NULL, mid, values.empty() ? NULL : &values[0], \
	                           IS_STATIC, NONVIRTUAL); \
	return r.FIELD; \
}

// Object
extern "C" JNIEXPORT jobject JNICALL JNI_CallObjectMethod(JNIEnv *env, jobject obj,
	jmethodID mid, ...)
{
	va_list ap;
	va_start(ap, mid);
	std::vector<jvalue> args = ArgsFromVaList(mid, ap);
	va_end(ap);
	return JniInvokeMethod(env, obj, NULL, mid, args.empty() ? NULL : &args[0], false, false).l;
}
extern "C" JNIEXPORT jobject JNICALL JNI_CallObjectMethodV(JNIEnv *env, jobject obj,
	jmethodID mid, va_list args)
{
	std::vector<jvalue> v = ArgsFromVaList(mid, args);
	return JniInvokeMethod(env, obj, NULL, mid, v.empty() ? NULL : &v[0], false, false).l;
}
extern "C" JNIEXPORT jobject JNICALL JNI_CallObjectMethodA(JNIEnv *env, jobject obj,
	jmethodID mid, const jvalue *args)
{
	return JniInvokeMethod(env, obj, NULL, mid, args, false, false).l;
}

JNI_CALL_PRIMITIVE(JNI_CallBooleanMethod, jboolean, z, DT_BOOL, false, false)
JNI_CALL_PRIMITIVE_A(JNI_CallBooleanMethodA, jboolean, z, false, false)
JNI_CALL_PRIMITIVE_V(JNI_CallBooleanMethodV, jboolean, z, false, false)
JNI_CALL_PRIMITIVE(JNI_CallByteMethod, jbyte, b, DT_BYTE, false, false)
JNI_CALL_PRIMITIVE_A(JNI_CallByteMethodA, jbyte, b, false, false)
JNI_CALL_PRIMITIVE_V(JNI_CallByteMethodV, jbyte, b, false, false)
JNI_CALL_PRIMITIVE(JNI_CallCharMethod, jchar, c, DT_CHAR, false, false)
JNI_CALL_PRIMITIVE_A(JNI_CallCharMethodA, jchar, c, false, false)
JNI_CALL_PRIMITIVE_V(JNI_CallCharMethodV, jchar, c, false, false)
JNI_CALL_PRIMITIVE(JNI_CallShortMethod, jshort, s, DT_SHORT, false, false)
JNI_CALL_PRIMITIVE_A(JNI_CallShortMethodA, jshort, s, false, false)
JNI_CALL_PRIMITIVE_V(JNI_CallShortMethodV, jshort, s, false, false)
JNI_CALL_PRIMITIVE(JNI_CallIntMethod, jint, i, DT_INT, false, false)
JNI_CALL_PRIMITIVE_A(JNI_CallIntMethodA, jint, i, false, false)
JNI_CALL_PRIMITIVE_V(JNI_CallIntMethodV, jint, i, false, false)
JNI_CALL_PRIMITIVE(JNI_CallLongMethod, jlong, j, DT_LONG, false, false)
JNI_CALL_PRIMITIVE_A(JNI_CallLongMethodA, jlong, j, false, false)
JNI_CALL_PRIMITIVE_V(JNI_CallLongMethodV, jlong, j, false, false)
JNI_CALL_PRIMITIVE(JNI_CallFloatMethod, jfloat, f, DT_FLOAT, false, false)
JNI_CALL_PRIMITIVE_A(JNI_CallFloatMethodA, jfloat, f, false, false)
JNI_CALL_PRIMITIVE_V(JNI_CallFloatMethodV, jfloat, f, false, false)
JNI_CALL_PRIMITIVE(JNI_CallDoubleMethod, jdouble, d, DT_DOUBLE, false, false)
JNI_CALL_PRIMITIVE_A(JNI_CallDoubleMethodA, jdouble, d, false, false)
JNI_CALL_PRIMITIVE_V(JNI_CallDoubleMethodV, jdouble, d, false, false)

extern "C" JNIEXPORT void JNICALL JNI_CallVoidMethod(JNIEnv *env, jobject obj,
	jmethodID mid, ...)
{
	va_list ap;
	va_start(ap, mid);
	std::vector<jvalue> args = ArgsFromVaList(mid, ap);
	va_end(ap);
	JniInvokeMethod(env, obj, NULL, mid, args.empty() ? NULL : &args[0], false, false);
}
extern "C" JNIEXPORT void JNICALL JNI_CallVoidMethodV(JNIEnv *env, jobject obj,
	jmethodID mid, va_list args)
{
	std::vector<jvalue> v = ArgsFromVaList(mid, args);
	JniInvokeMethod(env, obj, NULL, mid, v.empty() ? NULL : &v[0], false, false);
}
extern "C" JNIEXPORT void JNICALL JNI_CallVoidMethodA(JNIEnv *env, jobject obj,
	jmethodID mid, const jvalue *args)
{
	JniInvokeMethod(env, obj, NULL, mid, args, false, false);
}

// Nonvirtual variants. The receiver also determines the object, but the
// method is not selected virtually.
#define JNI_NONVIRTUAL_OBJECT(NAME) \
extern "C" JNIEXPORT jobject JNICALL NAME(JNIEnv *env, jobject obj, jclass clazz, \
	jmethodID mid, ...) \
{ \
	va_list ap; va_start(ap, mid); \
	std::vector<jvalue> args = ArgsFromVaList(mid, ap); va_end(ap); \
	return JniInvokeMethod(env, obj, clazz, mid, args.empty() ? NULL : &args[0], false, true).l; \
}

JNI_NONVIRTUAL_OBJECT(JNI_CallNonvirtualObjectMethod)

extern "C" JNIEXPORT jobject JNICALL JNI_CallNonvirtualObjectMethodV(JNIEnv *env,
	jobject obj, jclass clazz, jmethodID mid, va_list args)
{
	std::vector<jvalue> v = ArgsFromVaList(mid, args);
	return JniInvokeMethod(env, obj, clazz, mid, v.empty() ? NULL : &v[0], false, true).l;
}
extern "C" JNIEXPORT jobject JNICALL JNI_CallNonvirtualObjectMethodA(JNIEnv *env,
	jobject obj, jclass clazz, jmethodID mid, const jvalue *args)
{
	return JniInvokeMethod(env, obj, clazz, mid, args, false, true).l;
}

#define JNI_NONVIRTUAL_PRIMITIVE(NAME, TYPE, FIELD) \
extern "C" JNIEXPORT TYPE JNICALL NAME(JNIEnv *env, jobject obj, jclass clazz, \
	jmethodID mid, ...) \
{ \
	va_list ap; va_start(ap, mid); \
	std::vector<jvalue> args = ArgsFromVaList(mid, ap); va_end(ap); \
	return JniInvokeMethod(env, obj, clazz, mid, args.empty() ? NULL : &args[0], false, true).FIELD; \
}

#define JNI_NONVIRTUAL_PRIMITIVE_A(NAME, TYPE, FIELD) \
extern "C" JNIEXPORT TYPE JNICALL NAME(JNIEnv *env, jobject obj, jclass clazz, \
	jmethodID mid, const jvalue *args) \
{ \
	return JniInvokeMethod(env, obj, clazz, mid, args, false, true).FIELD; \
}

#define JNI_NONVIRTUAL_PRIMITIVE_V(NAME, TYPE, FIELD) \
extern "C" JNIEXPORT TYPE JNICALL NAME(JNIEnv *env, jobject obj, jclass clazz, \
	jmethodID mid, va_list args) \
{ \
	std::vector<jvalue> v = ArgsFromVaList(mid, args); \
	return JniInvokeMethod(env, obj, clazz, mid, v.empty() ? NULL : &v[0], false, true).FIELD; \
}

JNI_NONVIRTUAL_PRIMITIVE(JNI_CallNonvirtualBooleanMethod, jboolean, z)
JNI_NONVIRTUAL_PRIMITIVE_A(JNI_CallNonvirtualBooleanMethodA, jboolean, z)
JNI_NONVIRTUAL_PRIMITIVE_V(JNI_CallNonvirtualBooleanMethodV, jboolean, z)
JNI_NONVIRTUAL_PRIMITIVE(JNI_CallNonvirtualByteMethod, jbyte, b)
JNI_NONVIRTUAL_PRIMITIVE_A(JNI_CallNonvirtualByteMethodA, jbyte, b)
JNI_NONVIRTUAL_PRIMITIVE_V(JNI_CallNonvirtualByteMethodV, jbyte, b)
JNI_NONVIRTUAL_PRIMITIVE(JNI_CallNonvirtualCharMethod, jchar, c)
JNI_NONVIRTUAL_PRIMITIVE_A(JNI_CallNonvirtualCharMethodA, jchar, c)
JNI_NONVIRTUAL_PRIMITIVE_V(JNI_CallNonvirtualCharMethodV, jchar, c)
JNI_NONVIRTUAL_PRIMITIVE(JNI_CallNonvirtualShortMethod, jshort, s)
JNI_NONVIRTUAL_PRIMITIVE_A(JNI_CallNonvirtualShortMethodA, jshort, s)
JNI_NONVIRTUAL_PRIMITIVE_V(JNI_CallNonvirtualShortMethodV, jshort, s)
JNI_NONVIRTUAL_PRIMITIVE(JNI_CallNonvirtualIntMethod, jint, i)
JNI_NONVIRTUAL_PRIMITIVE_A(JNI_CallNonvirtualIntMethodA, jint, i)
JNI_NONVIRTUAL_PRIMITIVE_V(JNI_CallNonvirtualIntMethodV, jint, i)
JNI_NONVIRTUAL_PRIMITIVE(JNI_CallNonvirtualLongMethod, jlong, j)
JNI_NONVIRTUAL_PRIMITIVE_A(JNI_CallNonvirtualLongMethodA, jlong, j)
JNI_NONVIRTUAL_PRIMITIVE_V(JNI_CallNonvirtualLongMethodV, jlong, j)
JNI_NONVIRTUAL_PRIMITIVE(JNI_CallNonvirtualFloatMethod, jfloat, f)
JNI_NONVIRTUAL_PRIMITIVE_A(JNI_CallNonvirtualFloatMethodA, jfloat, f)
JNI_NONVIRTUAL_PRIMITIVE_V(JNI_CallNonvirtualFloatMethodV, jfloat, f)
JNI_NONVIRTUAL_PRIMITIVE(JNI_CallNonvirtualDoubleMethod, jdouble, d)
JNI_NONVIRTUAL_PRIMITIVE_A(JNI_CallNonvirtualDoubleMethodA, jdouble, d)
JNI_NONVIRTUAL_PRIMITIVE_V(JNI_CallNonvirtualDoubleMethodV, jdouble, d)

extern "C" JNIEXPORT void JNICALL JNI_CallNonvirtualVoidMethod(JNIEnv *env,
	jobject obj, jclass clazz, jmethodID mid, ...)
{
	va_list ap;
	va_start(ap, mid);
	std::vector<jvalue> args = ArgsFromVaList(mid, ap);
	va_end(ap);
	JniInvokeMethod(env, obj, clazz, mid, args.empty() ? NULL : &args[0], false, true);
}
extern "C" JNIEXPORT void JNICALL JNI_CallNonvirtualVoidMethodV(JNIEnv *env,
	jobject obj, jclass clazz, jmethodID mid, va_list args)
{
	std::vector<jvalue> v = ArgsFromVaList(mid, args);
	JniInvokeMethod(env, obj, clazz, mid, v.empty() ? NULL : &v[0], false, true);
}
extern "C" JNIEXPORT void JNICALL JNI_CallNonvirtualVoidMethodA(JNIEnv *env,
	jobject obj, jclass clazz, jmethodID mid, const jvalue *args)
{
	JniInvokeMethod(env, obj, clazz, mid, args, false, true);
}

// Static call wrappers. The "obj" parameter is actually the jclass.
#define JNI_STATIC_OBJECT(NAME) \
extern "C" JNIEXPORT jobject JNICALL NAME(JNIEnv *env, jclass clazz, jmethodID mid, ...) \
{ \
	va_list ap; va_start(ap, mid); \
	std::vector<jvalue> args = ArgsFromVaList(mid, ap); va_end(ap); \
	return JniInvokeMethod(env, NULL, clazz, mid, args.empty() ? NULL : &args[0], true, true).l; \
}

JNI_STATIC_OBJECT(JNI_CallStaticObjectMethod)

extern "C" JNIEXPORT jobject JNICALL JNI_CallStaticObjectMethodV(JNIEnv *env,
	jclass clazz, jmethodID mid, va_list args)
{
	std::vector<jvalue> v = ArgsFromVaList(mid, args);
	return JniInvokeMethod(env, NULL, clazz, mid, v.empty() ? NULL : &v[0], true, true).l;
}
extern "C" JNIEXPORT jobject JNICALL JNI_CallStaticObjectMethodA(JNIEnv *env,
	jclass clazz, jmethodID mid, const jvalue *args)
{
	return JniInvokeMethod(env, NULL, clazz, mid, args, true, true).l;
}

#define JNI_STATIC_PRIMITIVE(NAME, TYPE, FIELD) \
extern "C" JNIEXPORT TYPE JNICALL NAME(JNIEnv *env, jclass clazz, jmethodID mid, ...) \
{ \
	va_list ap; va_start(ap, mid); \
	std::vector<jvalue> args = ArgsFromVaList(mid, ap); va_end(ap); \
	return JniInvokeMethod(env, NULL, clazz, mid, args.empty() ? NULL : &args[0], true, true).FIELD; \
}

#define JNI_STATIC_PRIMITIVE_A(NAME, TYPE, FIELD) \
extern "C" JNIEXPORT TYPE JNICALL NAME(JNIEnv *env, jclass clazz, jmethodID mid, \
	const jvalue *args) \
{ \
	return JniInvokeMethod(env, NULL, clazz, mid, args, true, true).FIELD; \
}

#define JNI_STATIC_PRIMITIVE_V(NAME, TYPE, FIELD) \
extern "C" JNIEXPORT TYPE JNICALL NAME(JNIEnv *env, jclass clazz, jmethodID mid, \
	va_list args) \
{ \
	std::vector<jvalue> v = ArgsFromVaList(mid, args); \
	return JniInvokeMethod(env, NULL, clazz, mid, v.empty() ? NULL : &v[0], true, true).FIELD; \
}

JNI_STATIC_PRIMITIVE(JNI_CallStaticBooleanMethod, jboolean, z)
JNI_STATIC_PRIMITIVE_A(JNI_CallStaticBooleanMethodA, jboolean, z)
JNI_STATIC_PRIMITIVE_V(JNI_CallStaticBooleanMethodV, jboolean, z)
JNI_STATIC_PRIMITIVE(JNI_CallStaticByteMethod, jbyte, b)
JNI_STATIC_PRIMITIVE_A(JNI_CallStaticByteMethodA, jbyte, b)
JNI_STATIC_PRIMITIVE_V(JNI_CallStaticByteMethodV, jbyte, b)
JNI_STATIC_PRIMITIVE(JNI_CallStaticCharMethod, jchar, c)
JNI_STATIC_PRIMITIVE_A(JNI_CallStaticCharMethodA, jchar, c)
JNI_STATIC_PRIMITIVE_V(JNI_CallStaticCharMethodV, jchar, c)
JNI_STATIC_PRIMITIVE(JNI_CallStaticShortMethod, jshort, s)
JNI_STATIC_PRIMITIVE_A(JNI_CallStaticShortMethodA, jshort, s)
JNI_STATIC_PRIMITIVE_V(JNI_CallStaticShortMethodV, jshort, s)
JNI_STATIC_PRIMITIVE(JNI_CallStaticIntMethod, jint, i)
JNI_STATIC_PRIMITIVE_A(JNI_CallStaticIntMethodA, jint, i)
JNI_STATIC_PRIMITIVE_V(JNI_CallStaticIntMethodV, jint, i)
JNI_STATIC_PRIMITIVE(JNI_CallStaticLongMethod, jlong, j)
JNI_STATIC_PRIMITIVE_A(JNI_CallStaticLongMethodA, jlong, j)
JNI_STATIC_PRIMITIVE_V(JNI_CallStaticLongMethodV, jlong, j)
JNI_STATIC_PRIMITIVE(JNI_CallStaticFloatMethod, jfloat, f)
JNI_STATIC_PRIMITIVE_A(JNI_CallStaticFloatMethodA, jfloat, f)
JNI_STATIC_PRIMITIVE_V(JNI_CallStaticFloatMethodV, jfloat, f)
JNI_STATIC_PRIMITIVE(JNI_CallStaticDoubleMethod, jdouble, d)
JNI_STATIC_PRIMITIVE_A(JNI_CallStaticDoubleMethodA, jdouble, d)
JNI_STATIC_PRIMITIVE_V(JNI_CallStaticDoubleMethodV, jdouble, d)

extern "C" JNIEXPORT void JNICALL JNI_CallStaticVoidMethod(JNIEnv *env, jclass clazz,
	jmethodID mid, ...)
{
	va_list ap;
	va_start(ap, mid);
	std::vector<jvalue> args = ArgsFromVaList(mid, ap);
	va_end(ap);
	JniInvokeMethod(env, NULL, clazz, mid, args.empty() ? NULL : &args[0], true, true);
}
extern "C" JNIEXPORT void JNICALL JNI_CallStaticVoidMethodV(JNIEnv *env, jclass clazz,
	jmethodID mid, va_list args)
{
	std::vector<jvalue> v = ArgsFromVaList(mid, args);
	JniInvokeMethod(env, NULL, clazz, mid, v.empty() ? NULL : &v[0], true, true);
}
extern "C" JNIEXPORT void JNICALL JNI_CallStaticVoidMethodA(JNIEnv *env, jclass clazz,
	jmethodID mid, const jvalue *args)
{
	JniInvokeMethod(env, NULL, clazz, mid, args, true, true);
}

// ---------------------------------------------------------------------------
// Fields
// ---------------------------------------------------------------------------

extern "C" JNIEXPORT jfieldID JNICALL JNI_GetFieldID(JNIEnv *env, jclass clazz,
	const char *name, const char *sig)
{
	(void)env;
	return reinterpret_cast<jfieldID>(JniFindField(JniResolveClass(clazz), name, sig, false));
}

extern "C" JNIEXPORT jfieldID JNICALL JNI_GetStaticFieldID(JNIEnv *env, jclass clazz,
	const char *name, const char *sig)
{
	(void)env;
	lpClass c = JniResolveClass(clazz);
	lpField field = JniFindField(c, name, sig, true);
	if (!field)
	{
		return NULL;
	}
	DarksVM_EnsureInitialized(c);
	JniStaticFieldValue(c, field, true);
	return reinterpret_cast<jfieldID>(field);
}

#define JNI_GET_FIELD(NAME, TYPE, MEMBER) \
extern "C" JNIEXPORT TYPE JNICALL NAME(JNIEnv *env, jobject obj, jfieldID fid) \
{ \
	(void)env; \
	lpFieldValue fv = JniInstanceFieldValue(obj, fid); \
	if (!fv) return 0; \
	Variant v = fv->LoadValue(); \
	return static_cast<TYPE>(v.MEMBER); \
}

#define JNI_SET_FIELD(NAME, TYPE, MEMBER) \
extern "C" JNIEXPORT void JNICALL NAME(JNIEnv *env, jobject obj, jfieldID fid, TYPE value) \
{ \
	(void)env; \
	lpFieldValue fv = JniInstanceFieldValue(obj, fid); \
	if (!fv) return; \
	Variant v; \
	v.MEMBER = value; \
	fv->StoreValue(v); \
}

JNI_GET_FIELD(JNI_GetBooleanField, jboolean, blValue)
JNI_GET_FIELD(JNI_GetByteField, jbyte, bValue)
JNI_GET_FIELD(JNI_GetCharField, jchar, cValue)
JNI_GET_FIELD(JNI_GetShortField, jshort, sValue)
JNI_GET_FIELD(JNI_GetIntField, jint, iValue)
JNI_GET_FIELD(JNI_GetLongField, jlong, lValue)
JNI_GET_FIELD(JNI_GetFloatField, jfloat, fValue)
JNI_GET_FIELD(JNI_GetDoubleField, jdouble, dValue)
JNI_SET_FIELD(JNI_SetBooleanField, jboolean, blValue)
JNI_SET_FIELD(JNI_SetByteField, jbyte, bValue)
JNI_SET_FIELD(JNI_SetCharField, jchar, cValue)
JNI_SET_FIELD(JNI_SetShortField, jshort, sValue)
JNI_SET_FIELD(JNI_SetIntField, jint, iValue)
JNI_SET_FIELD(JNI_SetLongField, jlong, lValue)
JNI_SET_FIELD(JNI_SetFloatField, jfloat, fValue)
JNI_SET_FIELD(JNI_SetDoubleField, jdouble, dValue)

extern "C" JNIEXPORT jobject JNICALL JNI_GetObjectField(JNIEnv *env, jobject obj,
	jfieldID fid)
{
	lpFieldValue fv = JniInstanceFieldValue(obj, fid);
	if (!fv)
	{
		return NULL;
	}
	Variant v = fv->LoadValue();
	jobject ref = JniObject(v.heapPtr);
	JniAddLocalRef(env, ref);
	return ref;
}

extern "C" JNIEXPORT void JNICALL JNI_SetObjectField(JNIEnv *env, jobject obj,
	jfieldID fid, jobject value)
{
	(void)env;
	lpFieldValue fv = JniInstanceFieldValue(obj, fid);
	if (!fv)
	{
		return;
	}
	Variant v;
	v.heapPtr = JniRef(value);
	fv->StoreValue(v);
}

#define JNI_GET_STATIC_FIELD(NAME, TYPE, MEMBER) \
extern "C" JNIEXPORT TYPE JNICALL NAME(JNIEnv *env, jclass clazz, jfieldID fid) \
{ \
	(void)env; \
	lpFieldValue fv = JniStaticFieldValue(JniResolveClass(clazz), \
		JniResolveField(fid), true); \
	if (!fv) return 0; \
	Variant v = fv->LoadValue(); \
	return static_cast<TYPE>(v.MEMBER); \
}

#define JNI_SET_STATIC_FIELD(NAME, TYPE, MEMBER) \
extern "C" JNIEXPORT void JNICALL NAME(JNIEnv *env, jclass clazz, jfieldID fid, TYPE value) \
{ \
	(void)env; \
	lpFieldValue fv = JniStaticFieldValue(JniResolveClass(clazz), \
		JniResolveField(fid), true); \
	if (!fv) return; \
	Variant v; \
	v.MEMBER = value; \
	fv->StoreValue(v); \
}

JNI_GET_STATIC_FIELD(JNI_GetStaticBooleanField, jboolean, blValue)
JNI_GET_STATIC_FIELD(JNI_GetStaticByteField, jbyte, bValue)
JNI_GET_STATIC_FIELD(JNI_GetStaticCharField, jchar, cValue)
JNI_GET_STATIC_FIELD(JNI_GetStaticShortField, jshort, sValue)
JNI_GET_STATIC_FIELD(JNI_GetStaticIntField, jint, iValue)
JNI_GET_STATIC_FIELD(JNI_GetStaticLongField, jlong, lValue)
JNI_GET_STATIC_FIELD(JNI_GetStaticFloatField, jfloat, fValue)
JNI_GET_STATIC_FIELD(JNI_GetStaticDoubleField, jdouble, dValue)
JNI_SET_STATIC_FIELD(JNI_SetStaticBooleanField, jboolean, blValue)
JNI_SET_STATIC_FIELD(JNI_SetStaticByteField, jbyte, bValue)
JNI_SET_STATIC_FIELD(JNI_SetStaticCharField, jchar, cValue)
JNI_SET_STATIC_FIELD(JNI_SetStaticShortField, jshort, sValue)
JNI_SET_STATIC_FIELD(JNI_SetStaticIntField, jint, iValue)
JNI_SET_STATIC_FIELD(JNI_SetStaticLongField, jlong, lValue)
JNI_SET_STATIC_FIELD(JNI_SetStaticFloatField, jfloat, fValue)
JNI_SET_STATIC_FIELD(JNI_SetStaticDoubleField, jdouble, dValue)

extern "C" JNIEXPORT jobject JNICALL JNI_GetStaticObjectField(JNIEnv *env, jclass clazz,
	jfieldID fid)
{
	lpFieldValue fv = JniStaticFieldValue(JniResolveClass(clazz), JniResolveField(fid), true);
	if (!fv)
	{
		return NULL;
	}
	Variant v = fv->LoadValue();
	jobject ref = JniObject(v.heapPtr);
	JniAddLocalRef(env, ref);
	return ref;
}

extern "C" JNIEXPORT void JNICALL JNI_SetStaticObjectField(JNIEnv *env, jclass clazz,
	jfieldID fid, jobject value)
{
	(void)env;
	lpFieldValue fv = JniStaticFieldValue(JniResolveClass(clazz), JniResolveField(fid), true);
	if (!fv)
	{
		return;
	}
	Variant v;
	v.heapPtr = JniRef(value);
	fv->StoreValue(v);
}

// ---------------------------------------------------------------------------
// Reflection bridging (minimal)
// ---------------------------------------------------------------------------

extern "C" JNIEXPORT jmethodID JNICALL JNI_FromReflectedMethod(JNIEnv *env, jobject method)
{
	(void)env;
	(void)method;
	return NULL;
}

extern "C" JNIEXPORT jfieldID JNICALL JNI_FromReflectedField(JNIEnv *env, jobject field)
{
	(void)env;
	(void)field;
	return NULL;
}

extern "C" JNIEXPORT jobject JNICALL JNI_ToReflectedMethod(JNIEnv *env, jclass cls,
	jmethodID methodID, jboolean isStatic)
{
	(void)env;
	(void)cls;
	(void)methodID;
	(void)isStatic;
	return NULL;
}

extern "C" JNIEXPORT jobject JNICALL JNI_ToReflectedField(JNIEnv *env, jclass cls,
	jfieldID fieldID, jboolean isStatic)
{
	(void)env;
	(void)cls;
	(void)fieldID;
	(void)isStatic;
	return NULL;
}

// ---------------------------------------------------------------------------
// Class mirrors and instance field setters (used by the runtime natives)
// ---------------------------------------------------------------------------

namespace {

std::map<lpClass, LPOBJECT_REF> &ClassMirrors()
{
	static std::map<lpClass, LPOBJECT_REF> mirrors;
	return mirrors;
}

bool SetFieldValue(lpObject object, const char *name, Variant &value)
{
	if (!object || !object->obj_class || !name)
	{
		return false;
	}
	for (lpClass c = object->obj_class; c; c = c->super_class)
	{
		for (U2 i = 0; i < c->fieldCount; i++)
		{
			lpField f = c->fields ? c->fields[i] : NULL;
			if (!f || !f->fieldName)
			{
				continue;
			}
			if (strcmp(f->fieldName->GetName(), name) == 0)
			{
				lpFieldValue fv = object->GetFieldValue(f);
				if (!fv)
				{
					return false;
				}
				fv->StoreValue(value);
				return true;
			}
		}
	}
	return false;
}

} // namespace

bool JniSetObjectFieldByName(jobject obj, const char *name, jobject value_ref)
{
	lpObject object = JniResolveObject(obj);
	if (!object)
	{
		return false;
	}
	Variant v;
	v.heapPtr = JniRef(value_ref);
	return SetFieldValue(object, name, v);
}

bool JniSetLongFieldByName(jobject obj, const char *name, jlong value)
{
	lpObject object = JniResolveObject(obj);
	if (!object)
	{
		return false;
	}
	Variant v;
	v.lValue = value;
	return SetFieldValue(object, name, v);
}

jlong JniGetLongFieldByName(jobject obj, const char *name)
{
	lpObject object = JniResolveObject(obj);
	if (!object || !object->obj_class || !name)
	{
		return 0;
	}
	for (lpClass c = object->obj_class; c; c = c->super_class)
	{
		for (U2 i = 0; i < c->fieldCount; i++)
		{
			lpField f = c->fields ? c->fields[i] : NULL;
			if (!f || !f->fieldName)
			{
				continue;
			}
			if (strcmp(f->fieldName->GetName(), name) == 0)
			{
				lpFieldValue fv = object->GetFieldValue(f);
				return fv ? fv->LoadValue().lValue : 0;
			}
		}
	}
	return 0;
}

jobject JniClassMirror(jclass clazz)
{
	lpClass c = JniResolveClass(clazz);
	if (!c || !VMContext::object_heap)
	{
		return NULL;
	}
	std::map<lpClass, LPOBJECT_REF>::iterator it = ClassMirrors().find(c);
	if (it != ClassMirrors().end())
	{
		return JniObject(it->second);
	}
	lpClass mirror_class = JniFindClass(NULL, "java/lang/Class");
	if (!mirror_class)
	{
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewObject(mirror_class, ref))
	{
		return NULL;
	}
	jobject mirror = JniObject(ref);
	// Dotted class name, as java.lang.Class.getName() reports it.
	std::string name = c->thisClass && c->thisClass->GetName() ? c->thisClass->GetName() : "";
	for (size_t i = 0; i < name.size(); i++)
	{
		if (name[i] == '/')
		{
			name[i] = '.';
		}
	}
	LPOBJECT_REF name_ref = NULL;
	if (VMContext::object_heap->NewStringObject(const_cast<char *>(name.c_str()),
			static_cast<int>(name.size()), name_ref))
	{
		JniSetObjectFieldByName(mirror, "name", JniObject(name_ref));
	}
	JniSetLongFieldByName(mirror, "vmClass", reinterpret_cast<jlong>(c));
	VMContext::object_heap->AddGlobalRef(JniRef(mirror));
	ClassMirrors()[c] = JniRef(mirror);
	return mirror;
}
