// java.lang.reflect natives: Class member enumeration, Method.invoke,
// Field get/set, Constructor.newInstance and runtime-visible annotation
// materialisation.
//
// Annotation values are stored on a synthetic instance of the annotation
// interface under "ann:<element>" keys.  The interpreter special-cases calls
// to annotation element methods on such instances (see ExecuteVirtual).

#include "stdafx.h"
#include "jni_impl.h"
#include "vm/executor/javaexception.h"
#include "vm/executor/proxy.h"
#include "vm/executor/threadstack.h"
#include "vm/executor/gc/gcbarrier.h"

#include <cstring>
#include <string>
#include <vector>

namespace {

lpClass MirrorClass(jobject mirror)
{
	return reinterpret_cast<lpClass>(static_cast<intptr_t>(
		JniGetLongFieldByName(mirror, "vmClass")));
}

// Annotation machinery may reference application classes (annotation types,
// enum constants, class literals) that are invisible to the bootstrap loader.
lpClass FindClassAnywhere(JNIEnv *env, const char *name)
{
	lpClass clazz = NULL;
	if (name && DarksVM_LoadClassWithoutInitializing(std::string(name), clazz) && clazz)
	{
		return clazz;
	}
	return JniFindClass(env, name);
}

lpFieldValue FindNamedField(lpObject object, const char *name)
{
	if (!object || !name)
	{
		return NULL;
	}
	for (lpClass c = object->obj_class; c; c = c->super_class)
	{
		for (U2 i = 0; i < c->fieldCount; i++)
		{
			lpField f = c->fields ? c->fields[i] : NULL;
			if (!f || !f->fieldName || (f->accFlag & ACF_STATIC) != 0)
			{
				continue;
			}
			if (strcmp(f->fieldName->GetName(), name) == 0)
			{
				return object->GetFieldValue(f);
			}
		}
	}
	return NULL;
}

std::string ObjectFieldString(JNIEnv *env, lpObject object, const char *name)
{
	lpFieldValue fv = FindNamedField(object, name);
	if (!fv || !fv->field_value.heapPtr)
	{
		return std::string();
	}
	jobject str = JniObject(fv->field_value.heapPtr);
	const char *chars = env->GetStringUTFChars(reinterpret_cast<jstring>(str), NULL);
	if (!chars)
	{
		return std::string();
	}
	std::string result(chars);
	env->ReleaseStringUTFChars(reinterpret_cast<jstring>(str), chars);
	return result;
}

jobject ObjectFieldRef(lpObject object, const char *name)
{
	lpFieldValue fv = FindNamedField(object, name);
	return (fv && fv->field_value.heapPtr) ? JniObject(fv->field_value.heapPtr) : NULL;
}

jobject NewHeapObject(JNIEnv *env, lpClass clazz)
{
	if (!clazz || !VMContext::object_heap)
	{
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewObject(clazz, ref))
	{
		return NULL;
	}
	jobject result = JniObject(ref);
	JniAddLocalRef(env, result);
	return result;
}

bool SetRefField(jobject object, const char *name, jobject value)
{
	return JniSetObjectFieldByName(object, name, value);
}

jobject NewString(JNIEnv *env, const std::string &text)
{
	jstring str = env->NewStringUTF(text.c_str());
	if (str)
	{
		JniAddLocalRef(env, reinterpret_cast<jobject>(str));
	}
	return reinterpret_cast<jobject>(str);
}

jobject MakeMember(JNIEnv *env, const char *member_class, lpClass declaring,
                   const char *name, const char *desc, int modifiers)
{
	lpClass clazz = JniFindClass(env, member_class);
	jobject member = NewHeapObject(env, clazz);
	if (!member)
	{
		return NULL;
	}
	jobject mirror = JniClassMirror(reinterpret_cast<jclass>(declaring));
	if (mirror)
	{
		JniAddLocalRef(env, mirror);
		SetRefField(member, "declaringClass", mirror);
	}
	if (name)
	{
		SetRefField(member, "name", NewString(env, name));
	}
	if (desc)
	{
		SetRefField(member, "descriptor", NewString(env, desc));
	}
	JniSetLongFieldByName(member, "modifiers", modifiers);
	return member;
}

jobject MakeObjectArray(JNIEnv *env, const char *array_class_name,
                       const std::vector<jobject> &items)
{
	lpClass array_class = JniFindClass(env, array_class_name);
	if (!array_class)
	{
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewObjectArrayObject(
			static_cast<int>(items.size()), array_class, ref))
	{
		return NULL;
	}
	jobject result = JniObject(ref);
	JniAddLocalRef(env, result);
	lpObject array = NULL;
	if (VMContext::object_heap->GetObject(ref, array) && array && array->array_var)
	{
		jobject *elements = static_cast<jobject *>(array->array_var);
		for (size_t i = 0; i < items.size(); i++)
		{
			elements[i] = items[i];
		}
	}
	return result;
}

bool ReadAccessibleFlag(jobject member)
{
	lpObject object = JniResolveObject(member);
	if (!object)
	{
		return false;
	}
	for (lpClass c = object->obj_class; c; c = c->super_class)
	{
		for (U2 i = 0; i < c->fieldCount; i++)
		{
			lpField f = c->fields ? c->fields[i] : NULL;
			if (!f || !f->fieldName || (f->accFlag & ACF_STATIC) != 0)
			{
				continue;
			}
			if (strcmp(f->fieldName->GetName(), "accessible") == 0)
			{
				lpFieldValue fv = object->GetFieldValue(f);
				return fv && fv->field_value.blValue != 0;
			}
		}
	}
	return false;
}

// Access checks follow java.lang.reflect.Reflection.verifyMemberAccess: the
// caller class is the innermost frame outside java.lang.reflect/java.lang.Class
// (see ThreadStack::ReflectionCallerClass).  When no Java frame is available
// (calls made directly from VM/native code) only public members are allowed.
std::string InternalClassName(lpClass clazz)
{
	if (!clazz || !clazz->thisClass || !clazz->thisClass->GetName())
	{
		return std::string();
	}
	return clazz->thisClass->GetName();
}

std::string PackageName(const std::string &internal_name)
{
	size_t slash = internal_name.rfind('/');
	return slash == std::string::npos
		? std::string() : internal_name.substr(0, slash);
}

bool SameRuntimePackage(lpClass first, lpClass second)
{
	if (!first || !second || first->classloader != second->classloader)
	{
		return false;
	}
	return PackageName(InternalClassName(first)) ==
		PackageName(InternalClassName(second));
}

// Nest host of a class: its own name when no NestHost attribute was present.
std::string NestHostName(lpClass clazz)
{
	if (!clazz)
	{
		return std::string();
	}
	if (!clazz->nest_host.empty())
	{
		return clazz->nest_host;
	}
	return InternalClassName(clazz);
}

bool SameNest(lpClass first, lpClass second)
{
	if (!first || !second || first->classloader != second->classloader)
	{
		return false;
	}
	return NestHostName(first) == NestHostName(second);
}

bool IsSubclassOf(lpClass clazz, lpClass ancestor)
{
	for (lpClass c = clazz; c; c = c->super_class)
	{
		if (c == ancestor)
		{
			return true;
		}
	}
	return false;
}

bool MemberAccessAllowed(jobject member, int modifiers, lpClass declaring,
	lpClass caller)
{
	if (ReadAccessibleFlag(member))
	{
		return true;
	}
	if (!caller)
	{
		return declaring && (modifiers & ACF_PUBLIC) != 0 &&
			(declaring->accFlag & ACF_PUBLIC) != 0;
	}
	if (caller == declaring)
	{
		return true;
	}
	bool same_package = SameRuntimePackage(caller, declaring);
	bool declaring_public = declaring &&
		(declaring->accFlag & ACF_PUBLIC) != 0;
	if (!declaring_public && !same_package)
	{
		return false;
	}
	if ((modifiers & ACF_PUBLIC) != 0)
	{
		// A public member of a public class is visible across packages; a
		// non-public declaring class additionally requires package access.
		return declaring_public || same_package;
	}
	if ((modifiers & ACF_PROTECTED) != 0)
	{
		return same_package || IsSubclassOf(caller, declaring);
	}
	if ((modifiers & ACF_PRIVATE) != 0)
	{
		return SameNest(caller, declaring);
	}
	return same_package;
}

lpClass CurrentReflectionCaller()
{
	ThreadStack *stack = DarksVM_ActiveThreadStack();
	return stack ? stack->ReflectionCallerClass() : NULL;
}

void ThrowIllegalAccess(JNIEnv *env, const char *message)
{
	jclass error = reinterpret_cast<jclass>(
		JniFindClass(env, "java/lang/IllegalAccessException"));
	if (error)
	{
		env->ThrowNew(error, message ? message : "member is not accessible");
	}
}

void ThrowNullPointer(JNIEnv *env, const char *message)
{
	jclass error = reinterpret_cast<jclass>(
		JniFindClass(env, "java/lang/NullPointerException"));
	if (error)
	{
		env->ThrowNew(error, message ? message : "null object");
	}
}

jobject MirrorForDescriptor(JNIEnv *env, const char *desc, int length)
{
	if (!desc || length <= 0)
	{
		return NULL;
	}
	lpClass clazz = NULL;
	if (desc[0] == 'L' && length >= 2 && desc[length - 1] == ';')
	{
		std::string name(desc + 1, desc + length - 1);
		clazz = FindClassAnywhere(env, name.c_str());
	}
	else if (desc[0] == '[')
	{
		std::string name(desc, desc + length);
		clazz = JniFindClass(env, name.c_str());
	}
	else if (length == 1)
	{
		const char *primitive = NULL;
		switch (desc[0])
		{
		case 'Z': primitive = "boolean"; break;
		case 'B': primitive = "byte"; break;
		case 'C': primitive = "char"; break;
		case 'S': primitive = "short"; break;
		case 'I': primitive = "int"; break;
		case 'J': primitive = "long"; break;
		case 'F': primitive = "float"; break;
		case 'D': primitive = "double"; break;
		case 'V': primitive = "void"; break;
		default: break;
		}
		if (!primitive)
		{
			return NULL;
		}
		clazz = DarksVM_GetPrimitiveClass(primitive);
	}
	if (!clazz)
	{
		return NULL;
	}
	jobject mirror = JniClassMirror(reinterpret_cast<jclass>(clazz));
	if (mirror)
	{
		JniAddLocalRef(env, mirror);
	}
	return mirror;
}

// Consumes one field/method type descriptor starting at *pos.
jobject ParseDescriptorType(JNIEnv *env, const char *desc, int &pos, int length)
{
	if (!desc || pos < 0 || pos >= length)
	{
		return NULL;
	}
	char tag = desc[pos];
	if (tag == 'L')
	{
		int end = pos + 1;
		while (end < length && desc[end] != ';')
		{
			end++;
		}
		if (end >= length)
		{
			return NULL;
		}
		jobject result = MirrorForDescriptor(env, desc + pos, end - pos + 1);
		pos = end + 1;
		return result;
	}
	if (tag == '[')
	{
		int end = pos;
		while (end < length && desc[end] == '[')
		{
			end++;
		}
		if (end >= length)
		{
			return NULL;
		}
		if (desc[end] == 'L')
		{
			while (end < length && desc[end] != ';')
			{
				end++;
			}
			if (end >= length)
			{
				return NULL;
			}
		}
		end++;
		jobject result = MirrorForDescriptor(env, desc + pos, end - pos);
		pos = end;
		return result;
	}
	jobject result = MirrorForDescriptor(env, desc + pos, 1);
	pos += 1;
	return result;
}

jobject ParameterTypesFromDescriptor(JNIEnv *env, const char *desc)
{
	std::vector<jobject> items;
	if (desc && desc[0] == '(')
	{
		int length = static_cast<int>(strlen(desc));
		int pos = 1;
		while (pos < length && desc[pos] != ')')
		{
			jobject type = ParseDescriptorType(env, desc, pos, length);
			if (!type)
			{
				break;
			}
			items.push_back(type);
		}
	}
	return MakeObjectArray(env, "[Ljava/lang/Class;", items);
}

jobject ReturnTypeFromDescriptor(JNIEnv *env, const char *desc)
{
	if (!desc)
	{
		return NULL;
	}
	int length = static_cast<int>(strlen(desc));
	int pos = 0;
	while (pos < length && desc[pos] != ')')
	{
		pos++;
	}
	if (pos < length)
	{
		pos++;
	}
	return ParseDescriptorType(env, desc, pos, length);
}

jobject ClassArrayFromExceptionAttribute(JNIEnv *env, lpMethod method)
{
	std::vector<jobject> items;
	if (method && method->exception_attr && method->exception_attr->exception_table)
	{
		for (U2 i = 0; i < method->exception_attr->exception_count; i++)
		{
			lpClassConstant constant = method->exception_attr->exception_table[i];
			if (!constant || !constant->GetName())
			{
				continue;
			}
			lpClass clazz = FindClassAnywhere(env, constant->GetName());
			if (!clazz)
			{
				continue;
			}
			jobject mirror = JniClassMirror(reinterpret_cast<jclass>(clazz));
			if (mirror)
			{
				JniAddLocalRef(env, mirror);
				items.push_back(mirror);
			}
		}
	}
	return MakeObjectArray(env, "[Ljava/lang/Class;", items);
}

// Converts an exception raised by a reflected call into
// InvocationTargetException, matching the JDK contract.
jobject WrapInvocationTarget(JNIEnv *env, LPOBJECT_REF target)
{
	if (!target)
	{
		return NULL;
	}
	GCRootScope root(VMContext::object_heap, &target);
	PrintUncaughtException(target);
	VMContext::SetPendingException(NULL);
	lpClass wrapper_class = JniFindClass(env,
		"java/lang/reflect/InvocationTargetException");
	if (!wrapper_class)
	{
		VMContext::SetPendingException(target);
		return NULL;
	}
	LPOBJECT_REF wrapper = NULL;
	if (!VMContext::object_heap->NewObject(wrapper_class, wrapper))
	{
		VMContext::SetPendingException(target);
		return NULL;
	}
	jobject wrapper_obj = JniObject(wrapper);
	JniAddLocalRef(env, wrapper_obj);
	JniSetObjectFieldByName(wrapper_obj, "target", JniObject(target));
	VMContext::SetPendingException(wrapper);
	return NULL;
}

std::string InternalName(jobject mirror)
{
	lpClass clazz = MirrorClass(mirror);
	if (!clazz || !clazz->thisClass || !clazz->thisClass->GetName())
	{
		return std::string();
	}
	return std::string(clazz->thisClass->GetName());
}

std::string ClassToDescriptor(jobject mirror)
{
	std::string name = InternalName(mirror);
	if (name.empty())
	{
		return std::string();
	}
	// Primitive Class mirrors (int.class, Integer.TYPE) describe a value
	// type, not an object type.
	if (name == "boolean") return "Z";
	if (name == "byte") return "B";
	if (name == "char") return "C";
	if (name == "short") return "S";
	if (name == "int") return "I";
	if (name == "long") return "J";
	if (name == "float") return "F";
	if (name == "double") return "D";
	if (name == "void") return "V";
	if (name[0] == '[')
	{
		return name;
	}
	return "L" + name + ";";
}

lpMethod FindMethodByDescriptor(lpClass clazz, const char *name, const char *desc,
                                bool declared_only)
{
	if (!clazz || !name || !desc)
	{
		return NULL;
	}
	for (lpClass c = clazz; c; c = c->super_class)
	{
		for (U2 i = 0; i < c->methodCount; i++)
		{
			lpMethod m = c->methods ? c->methods[i] : NULL;
			if (!m || !m->methodName || !m->methodDesc)
			{
				continue;
			}
			if (strcmp(m->methodName->GetName(), name) == 0 &&
			    strcmp(m->methodDesc->GetName(), desc) == 0)
			{
				return m;
			}
		}
		if (declared_only)
		{
			break;
		}
	}
	return NULL;
}

lpMethod FindMethodByParams(lpClass clazz, const char *name, const char *params,
                            bool declared_only)
{
	if (!clazz || !name || !params)
	{
		return NULL;
	}
	size_t params_length = strlen(params);
	for (lpClass c = clazz; c; c = c->super_class)
	{
		for (U2 i = 0; i < c->methodCount; i++)
		{
			lpMethod m = c->methods ? c->methods[i] : NULL;
			if (!m || !m->methodName || !m->methodDesc)
			{
				continue;
			}
			const char *desc = m->methodDesc->GetName();
			if (desc && strcmp(m->methodName->GetName(), name) == 0 &&
			    strncmp(desc, params, params_length) == 0)
			{
				return m;
			}
		}
		if (declared_only)
		{
			break;
		}
	}
	return NULL;
}

lpField FindFieldByName(lpClass clazz, const char *name, bool declared_only)
{
	if (!clazz || !name)
	{
		return NULL;
	}
	for (lpClass c = clazz; c; c = c->super_class)
	{
		for (U2 i = 0; i < c->fieldCount; i++)
		{
			lpField f = c->fields ? c->fields[i] : NULL;
			if (f && f->fieldName && strcmp(f->fieldName->GetName(), name) == 0)
			{
				return f;
			}
		}
		if (declared_only)
		{
			break;
		}
	}
	return NULL;
}

// ---------------------------------------------------------------------------
// Annotation decoding
// ---------------------------------------------------------------------------

struct ByteReader
{
	const unsigned char *data;
	int length;
	int pos;
	bool ok;

	ByteReader(const unsigned char *d, int len) : data(d), length(len), pos(0), ok(true) {}

	unsigned char U1()
	{
		if (!ok || pos + 1 > length)
		{
			ok = false;
			return 0;
		}
		return data[pos++];
	}

	unsigned short U2()
	{
		if (!ok || pos + 2 > length)
		{
			ok = false;
			return 0;
		}
		unsigned short value = static_cast<unsigned short>(
			(data[pos] << 8) | data[pos + 1]);
		pos += 2;
		return value;
	}
};

std::string Utf8At(ConstantPool *pool, U2 index)
{
	if (!pool)
	{
		return std::string();
	}
	lpConstant c = pool->GetConstant(index);
	if (!c || c->tag != UTF8_CONSTANT)
	{
		return std::string();
	}
	lpUtf8Constant u = static_cast<lpUtf8Constant>(c);
	if (!u->bytes)
	{
		return std::string();
	}
	return std::string(u->bytes, u->length);
}

bool ReadElementValue(JNIEnv *env, ConstantPool *pool, ByteReader &r,
                      const char *expected_desc, Variant &out);

jobject BoxValue(JNIEnv *env, DATA_TYPE type, const Variant &value);

bool method_desc_has_no_args(const char *desc);

std::string AnnotationElementDescriptor(lpMethod method)
{
	if (!method || !method->methodDesc || !method->methodDesc->GetName())
	{
		return std::string();
	}
	const char *desc = method->methodDesc->GetName();
	const char *close = strchr(desc, ')');
	if (!close)
	{
		return std::string();
	}
	return std::string(close + 1);
}

lpMethod FindAnnotationElement(lpClass annotation_class, const std::string &name)
{
	if (!annotation_class)
	{
		return NULL;
	}
	for (U2 i = 0; i < annotation_class->methodCount; i++)
	{
		lpMethod method = annotation_class->methods ? annotation_class->methods[i] : NULL;
		if (!method || !method->methodName || !method->methodDesc)
		{
			continue;
		}
		const char *method_name = method->methodName->GetName();
		if (!method_name || strcmp(method_name, name.c_str()) != 0)
		{
			continue;
		}
		if (method_desc_has_no_args(method->methodDesc->GetName()))
		{
			return method;
		}
	}
	return NULL;
}

bool method_desc_has_no_args(const char *desc)
{
	return desc && strncmp(desc, "()", 2) == 0;
}

jobject AnnotationValueObject(JNIEnv *env, const Variant &value, const char *desc)
{
	if (!desc || !desc[0])
	{
		if (value.heapPtr)
		{
			jobject ref = JniObject(value.heapPtr);
			JniAddLocalRef(env, ref);
			return ref;
		}
		return NULL;
	}
	if (desc[0] == 'L' || desc[0] == '[')
	{
		if (!value.heapPtr)
		{
			return NULL;
		}
		jobject ref = JniObject(value.heapPtr);
		JniAddLocalRef(env, ref);
		return ref;
	}
	DATA_TYPE type = DT_INT;
	switch (desc[0])
	{
	case 'Z': type = DT_BOOL; break;
	case 'B': type = DT_BYTE; break;
	case 'C': type = DT_CHAR; break;
	case 'S': type = DT_SHORT; break;
	case 'I': type = DT_INT; break;
	case 'J': type = DT_LONG; break;
	case 'F': type = DT_FLOAT; break;
	case 'D': type = DT_DOUBLE; break;
	default: break;
	}
	jobject boxed = BoxValue(env, type, value);
	if (boxed)
	{
		JniAddLocalRef(env, boxed);
	}
	return boxed;
}

jobject ReadAnnotation(JNIEnv *env, ConstantPool *pool, ByteReader &r)
{
	U2 type_index = r.U2();
	std::string descriptor = Utf8At(pool, type_index);
	U2 pair_count = r.U2();
	bool valid_type = descriptor.size() >= 3 && descriptor[0] == 'L' &&
		descriptor[descriptor.size() - 1] == ';';
	std::string class_name;
	lpClass annotation_class = NULL;
	if (valid_type)
	{
		class_name = descriptor.substr(1, descriptor.size() - 2);
		annotation_class = FindClassAnywhere(env, class_name.c_str());
	}
	if (!annotation_class)
	{
		// Skip the element values so the surrounding annotation array stays
		// aligned even when one annotation type cannot be resolved.
		for (U2 i = 0; i < pair_count && r.ok; i++)
		{
			r.U2();
			Variant ignored;
			ReadElementValue(env, pool, r, "", ignored);
		}
		return NULL;
	}
	std::map<std::string, jobject> provided;
	for (U2 i = 0; i < pair_count && r.ok; i++)
	{
		U2 name_index = r.U2();
		std::string element_name = Utf8At(pool, name_index);
		lpMethod element = FindAnnotationElement(annotation_class, element_name);
		std::string element_desc = AnnotationElementDescriptor(element);
		Variant value;
		if (!ReadElementValue(env, pool, r, element_desc.c_str(), value))
		{
			break;
		}
		jobject object = AnnotationValueObject(env, value, element_desc.c_str());
		if (object)
		{
			provided[element_name] = object;
		}
	}
	std::vector<std::string> names;
	std::vector<jobject> values;
	for (U2 m = 0; m < annotation_class->methodCount; m++)
	{
		lpMethod method = annotation_class->methods ? annotation_class->methods[m] : NULL;
		if (!method || !method->methodName || !method->methodDesc)
		{
			continue;
		}
		std::string element_name = method->methodName->GetName();
		if (element_name.empty() || element_name[0] == '<' ||
		    !method_desc_has_no_args(method->methodDesc->GetName()))
		{
			continue;
		}
		std::string element_desc = AnnotationElementDescriptor(method);
		jobject object = NULL;
		std::map<std::string, jobject>::iterator it = provided.find(element_name);
		if (it != provided.end())
		{
			object = it->second;
		}
		else if (method->annotation_default && method->annotation_default->bytes)
		{
			ByteReader defaults(method->annotation_default->bytes,
			                    method->annotation_default->attr_length);
			Variant value;
			if (ReadElementValue(env, annotation_class->const_pool, defaults,
			                     element_desc.c_str(), value))
			{
				object = AnnotationValueObject(env, value, element_desc.c_str());
			}
		}
		if (!object)
		{
			continue;
		}
		names.push_back(element_name);
		values.push_back(object);
	}
	jobject proxy = DarksVM_CreateAnnotationProxy(env, annotation_class, names, values);
	return proxy;
}

bool ReadElementValue(JNIEnv *env, ConstantPool *pool, ByteReader &r,
                      const char *expected_desc, Variant &out)
{
	char tag = static_cast<char>(r.U1());
	switch (tag)
	{
	case 'B':
	case 'C':
	case 'S':
	case 'Z':
	case 'I':
		{
			U2 index = r.U2();
			lpConstant c = pool ? pool->GetConstant(index) : NULL;
			if (!c || c->tag != INTEGER_CONSTANT)
			{
				return false;
			}
			out.iValue = static_cast<int>(static_cast<IntegerConstant *>(c)->bytes);
			if (tag == 'Z')
			{
				out.blValue = out.iValue != 0 ? 1 : 0;
			}
			else if (tag == 'C')
			{
				out.cValue = static_cast<uint16_t>(out.iValue);
			}
			else if (tag == 'S')
			{
				out.sValue = static_cast<short>(out.iValue);
			}
			else if (tag == 'B')
			{
				out.bValue = static_cast<signed char>(out.iValue);
			}
			return true;
		}
	case 'J':
		{
			U2 index = r.U2();
			lpConstant c = pool ? pool->GetConstant(index) : NULL;
			if (!c || c->tag != LONG_CONSTANT)
			{
				return false;
			}
			LongConstant *lc = static_cast<LongConstant *>(c);
			WByteValue wv(lc->bytes1, lc->bytes2);
			out.lValue = wv.lValue;
			return true;
		}
	case 'F':
		{
			U2 index = r.U2();
			lpConstant c = pool ? pool->GetConstant(index) : NULL;
			if (!c || c->tag != FLOAT_CONSTANT)
			{
				return false;
			}
			FloatConstant *fc = static_cast<FloatConstant *>(c);
			out.set(fc->bytes, 4);
			return true;
		}
	case 'D':
		{
			U2 index = r.U2();
			lpConstant c = pool ? pool->GetConstant(index) : NULL;
			if (!c || c->tag != DOUBLE_CONSTANT)
			{
				return false;
			}
			DoubleConstant *dc = static_cast<DoubleConstant *>(c);
			WByteValue wv(dc->bytes1, dc->bytes2);
			out.dValue = wv.dValue;
			return true;
		}
	case 's':
		{
			U2 index = r.U2();
			std::string text = Utf8At(pool, index);
			out.heapPtr = JniRef(NewString(env, text));
			return true;
		}
	case 'c':
		{
			U2 index = r.U2();
			std::string desc = Utf8At(pool, index);
			jobject mirror = MirrorForDescriptor(env, desc.c_str(),
			                                     static_cast<int>(desc.size()));
			if (!mirror)
			{
				return false;
			}
			out.heapPtr = JniRef(mirror);
			return true;
		}
	case 'e':
		{
			U2 type_index = r.U2();
			U2 name_index = r.U2();
			std::string enum_desc = Utf8At(pool, type_index);
			std::string constant = Utf8At(pool, name_index);
			std::string enum_name = (enum_desc.size() >= 3 && enum_desc[0] == 'L')
				? enum_desc.substr(1, enum_desc.size() - 2) : enum_desc;
			lpClass enum_class = FindClassAnywhere(env, enum_name.c_str());
			if (enum_class)
			{
				DarksVM_EnsureInitialized(enum_class);
				std::string field_desc = "L" + enum_name + ";";
				lpField field = JniFindField(enum_class, constant.c_str(),
				                             field_desc.c_str(), true);
				lpFieldValue fv = JniStaticFieldValue(enum_class, field, false);
				out.heapPtr = fv ? fv->field_value.heapPtr : NULL;
			}
			else
			{
				out.heapPtr = NULL;
			}
			return true;
		}
	case '@':
		{
			jobject nested = ReadAnnotation(env, pool, r);
			out.heapPtr = JniRef(nested);
			return true;
		}
	case '[':
		{
			U2 count = r.U2();
			std::string component;
			if (expected_desc && expected_desc[0] == '[')
			{
				component = std::string(expected_desc + 1);
			}
			std::string array_name;
			if (!component.empty())
			{
				array_name = "[" + component;
			}
			else
			{
				array_name = "[Ljava/lang/Object;";
			}
			lpClass array_class = FindClassAnywhere(env, array_name.c_str());
			if (!array_class || !VMContext::object_heap)
			{
				return false;
			}
			char component_tag = component.empty() ? 'L' : component[0];
			bool primitive = component.size() == 1 &&
				(component_tag == 'B' || component_tag == 'C' ||
				 component_tag == 'S' || component_tag == 'Z' ||
				 component_tag == 'I' || component_tag == 'J' ||
				 component_tag == 'F' || component_tag == 'D');
			LPOBJECT_REF ref = NULL;
			if (primitive)
			{
				if (!VMContext::object_heap->NewArrayObject(count, array_class, ref))
				{
					return false;
				}
			}
			else
			{
				if (!VMContext::object_heap->NewObjectArrayObject(count, array_class, ref))
				{
					return false;
				}
			}
			jobject array = JniObject(ref);
			JniAddLocalRef(env, array);
			lpObject array_obj = NULL;
			VMContext::object_heap->GetObject(ref, array_obj);
			if (!array_obj)
			{
				return false;
			}
			if (primitive)
			{
				for (U2 i = 0; i < count && r.ok; i++)
				{
					Variant element;
					if (!ReadElementValue(env, pool, r, component.c_str(), element))
					{
						break;
					}
					if (!array_obj->array_var)
					{
						continue;
					}
					switch (component_tag)
					{
					case 'B':
						static_cast<jbyte *>(array_obj->array_var)[i] = element.bValue; break;
					case 'C':
						static_cast<jchar *>(array_obj->array_var)[i] = element.cValue; break;
					case 'S':
						static_cast<jshort *>(array_obj->array_var)[i] = element.sValue; break;
					case 'Z':
						static_cast<jboolean *>(array_obj->array_var)[i] = element.blValue; break;
					case 'I':
						static_cast<jint *>(array_obj->array_var)[i] = element.iValue; break;
					case 'J':
						static_cast<jlong *>(array_obj->array_var)[i] = element.lValue; break;
					case 'F':
						static_cast<jfloat *>(array_obj->array_var)[i] = element.fValue; break;
					case 'D':
						static_cast<jdouble *>(array_obj->array_var)[i] = element.dValue; break;
					default:
						break;
					}
				}
			}
			else
			{
				jobject *elements = array_obj->array_var
					? static_cast<jobject *>(array_obj->array_var) : NULL;
				for (U2 i = 0; i < count && r.ok; i++)
				{
					Variant element;
					if (!ReadElementValue(env, pool, r, component.c_str(), element))
					{
						break;
					}
					if (elements)
					{
						elements[i] = JniObject(element.heapPtr);
					}
				}
			}
			out.heapPtr = JniRef(array);
			return true;
		}
	default:
		return false;
	}
}

jobject AnnotationsToArray(JNIEnv *env, lpClass owner, lpRuntimeVisibleAnnotations attr)
{
	std::vector<jobject> results;
	if (owner && owner->const_pool && attr && attr->bytes && attr->attr_length >= 2)
	{
		ByteReader reader(attr->bytes, attr->attr_length);
		U2 count = reader.U2();
		for (U2 i = 0; i < count && reader.ok; i++)
		{
			jobject annotation = ReadAnnotation(env, owner->const_pool, reader);
			if (annotation)
			{
				results.push_back(annotation);
			}
		}
	}
	return MakeObjectArray(env, "[Ljava/lang/annotation/Annotation;", results);
}

jobject ParameterAnnotationsToArray(JNIEnv *env, lpClass owner,
                                   lpRuntimeVisibleParameterAnnotations attr,
                                   int parameter_count)
{
	std::vector<jobject> rows;
	if (owner && owner->const_pool && attr && attr->bytes && attr->attr_length >= 1)
	{
		ByteReader reader(attr->bytes, attr->attr_length);
		int count = reader.U1();
		for (int p = 0; p < count && reader.ok; p++)
		{
			U2 annotation_count = reader.U2();
			std::vector<jobject> annotations;
			for (U2 a = 0; a < annotation_count && reader.ok; a++)
			{
				jobject annotation = ReadAnnotation(env, owner->const_pool, reader);
				if (annotation)
				{
					annotations.push_back(annotation);
				}
			}
			rows.push_back(MakeObjectArray(env,
				"[Ljava/lang/annotation/Annotation;", annotations));
		}
	}
	// Synthetic parameters are not represented in the attribute; the JDK pads
	// the row table to the reflective parameter count.
	while (static_cast<int>(rows.size()) < parameter_count)
	{
		rows.push_back(MakeObjectArray(env,
			"[Ljava/lang/annotation/Annotation;", std::vector<jobject>()));
	}
	return MakeObjectArray(env, "[[Ljava/lang/annotation/Annotation;", rows);
}

int CountMethodParameters(const char *desc)
{
	if (!desc)
	{
		return 0;
	}
	const char *p = strchr(desc, '(');
	if (!p)
	{
		return 0;
	}
	p++;
	int count = 0;
	while (*p && *p != ')')
	{
		while (*p == '[')
		{
			p++;
		}
		if (*p == 'L')
		{
			p = strchr(p, ';');
			if (!p)
			{
				break;
			}
			p++;
		}
		else if (*p)
		{
			p++;
		}
		count++;
	}
	return count;
}

// Finds the container annotation declared by @Repeatable on annotation_class.
lpClass FindRepeatableContainer(JNIEnv *env, lpClass annotation_class)
{
	if (!annotation_class || !annotation_class->const_pool ||
	    !annotation_class->rt_visible_attr || !annotation_class->rt_visible_attr->bytes)
	{
		return NULL;
	}
	lpRuntimeVisibleAnnotations attr = annotation_class->rt_visible_attr;
	if (attr->attr_length < 2)
	{
		return NULL;
	}
	ByteReader reader(attr->bytes, attr->attr_length);
	U2 count = reader.U2();
	for (U2 i = 0; i < count && reader.ok; i++)
	{
		int start = reader.pos;
		U2 type_index = reader.U2();
		std::string descriptor = Utf8At(annotation_class->const_pool, type_index);
		if (descriptor == "Ljava/lang/annotation/Repeatable;")
		{
			U2 pair_count = reader.U2();
			for (U2 p = 0; p < pair_count && reader.ok; p++)
			{
				U2 name_index = reader.U2();
				std::string name = Utf8At(annotation_class->const_pool, name_index);
				Variant value;
				if (!ReadElementValue(env, annotation_class->const_pool, reader,
				                      "Ljava/lang/Class;", value))
				{
					return NULL;
				}
				if (name == "value" && value.heapPtr)
				{
					return MirrorClass(JniObject(value.heapPtr));
				}
			}
			return NULL;
		}
		// Not the Repeatable annotation: skip its element values.
		reader.pos = start;
		if (!ReadAnnotation(env, annotation_class->const_pool, reader))
		{
			return NULL;
		}
	}
	return NULL;
}

jobject ExpandAnnotations(JNIEnv *env, jobjectArray annotations, lpClass target)
{
	std::vector<jobject> results;
	if (!target)
	{
		return MakeObjectArray(env, "[Ljava/lang/annotation/Annotation;", results);
	}
	lpClass container = FindRepeatableContainer(env, target);
	lpObject array = JniResolveObject(reinterpret_cast<jobject>(annotations));
	if (array && array->array_var)
	{
		jobject *elements = static_cast<jobject *>(array->array_var);
		for (int i = 0; i < array->array_length; i++)
		{
			jobject annotation = elements[i];
			if (!annotation)
			{
				continue;
			}
			lpClass annotation_class = DarksVM_ProxyAnnotationClass(annotation);
			if (annotation_class == target)
			{
				results.push_back(annotation);
			}
			else if (container && annotation_class == container)
			{
				jobject value = DarksVM_ProxyAnnotationValue(annotation, "value");
				lpObject values = JniResolveObject(value);
				if (values && values->array_var)
				{
					jobject *items = static_cast<jobject *>(values->array_var);
					for (int k = 0; k < values->array_length; k++)
					{
						if (items[k] &&
						    DarksVM_ProxyAnnotationClass(items[k]) == target)
						{
							results.push_back(items[k]);
						}
					}
				}
			}
		}
	}
	std::string array_name = "[L" + std::string(target->thisClass->GetName()) + ";";
	return MakeObjectArray(env, array_name.c_str(), results);
}

// Box/unbox helpers shared by Method.invoke and Field access.

jobject BoxValue(JNIEnv *env, DATA_TYPE type, const Variant &value)
{
	const char *wrapper = NULL;
	switch (type)
	{
	case DT_BOOL: wrapper = "java/lang/Boolean"; break;
	case DT_BYTE: wrapper = "java/lang/Byte"; break;
	case DT_CHAR: wrapper = "java/lang/Character"; break;
	case DT_SHORT: wrapper = "java/lang/Short"; break;
	case DT_INT: wrapper = "java/lang/Integer"; break;
	case DT_LONG: wrapper = "java/lang/Long"; break;
	case DT_FLOAT: wrapper = "java/lang/Float"; break;
	case DT_DOUBLE: wrapper = "java/lang/Double"; break;
	default: return NULL;
	}
	lpClass clazz = JniFindClass(env, wrapper);
	jobject boxed = NewHeapObject(env, clazz);
	if (!boxed)
	{
		return NULL;
	}
	lpObject object = JniResolveObject(boxed);
	lpFieldValue fv = FindNamedField(object, "value");
	if (fv)
	{
		fv->field_value = value;
	}
	return boxed;
}

bool UnboxValue(jobject boxed, DATA_TYPE type, Variant &out)
{
	if (!boxed)
	{
		return false;
	}
	lpObject object = JniResolveObject(boxed);
	if (!object || !object->obj_class || !object->obj_class->thisClass)
	{
		return false;
	}
	const char *wrapper = NULL;
	switch (type)
	{
	case DT_BOOL: wrapper = "java/lang/Boolean"; break;
	case DT_BYTE: wrapper = "java/lang/Byte"; break;
	case DT_CHAR: wrapper = "java/lang/Character"; break;
	case DT_SHORT: wrapper = "java/lang/Short"; break;
	case DT_INT: wrapper = "java/lang/Integer"; break;
	case DT_LONG: wrapper = "java/lang/Long"; break;
	case DT_FLOAT: wrapper = "java/lang/Float"; break;
	case DT_DOUBLE: wrapper = "java/lang/Double"; break;
	default: return false;
	}
	const char *actual = object->obj_class->thisClass->GetName();
	if (!actual || strcmp(actual, wrapper) != 0)
	{
		return false;
	}
	lpFieldValue fv = FindNamedField(object, "value");
	if (!fv)
	{
		return false;
	}
	switch (type)
	{
	case DT_BOOL:  out.blValue = fv->field_value.blValue; break;
	case DT_BYTE:  out.bValue = fv->field_value.bValue; break;
	case DT_CHAR:  out.cValue = fv->field_value.cValue; break;
	case DT_SHORT: out.sValue = fv->field_value.sValue; break;
	case DT_INT:   out.iValue = fv->field_value.iValue; break;
	case DT_LONG:  out.lValue = fv->field_value.lValue; break;
	case DT_FLOAT: out.fValue = fv->field_value.fValue; break;
	case DT_DOUBLE: out.dValue = fv->field_value.dValue; break;
	default: return false;
	}
	return true;
}

bool PrimitiveArrayType(const char *name, DATA_TYPE &type, char &tag)
{
	if (!name)
	{
		return false;
	}
	struct PrimitiveName
	{
		const char *name;
		DATA_TYPE type;
		char tag;
	};
	static const PrimitiveName names[] = {
		{ "boolean", DT_BOOL, 'Z' },
		{ "byte", DT_BYTE, 'B' },
		{ "char", DT_CHAR, 'C' },
		{ "short", DT_SHORT, 'S' },
		{ "int", DT_INT, 'I' },
		{ "long", DT_LONG, 'J' },
		{ "float", DT_FLOAT, 'F' },
		{ "double", DT_DOUBLE, 'D' },
		{ NULL, DT_VOID, 0 }
	};
	for (int i = 0; names[i].name; i++)
	{
		if (strcmp(name, names[i].name) == 0)
		{
			type = names[i].type;
			tag = names[i].tag;
			return true;
		}
	}
	return false;
}

void ThrowArrayIllegalArgument(JNIEnv *env, const char *message)
{
	jclass error = reinterpret_cast<jclass>(
		JniFindClass(env, "java/lang/IllegalArgumentException"));
	if (error)
	{
		env->ThrowNew(error, message ? message : "not an array");
	}
}

void ThrowArrayIndex(JNIEnv *env, int index)
{
	jclass error = reinterpret_cast<jclass>(
		JniFindClass(env, "java/lang/ArrayIndexOutOfBoundsException"));
	if (error)
	{
		std::string message = "index " + std::to_string(index);
		env->ThrowNew(error, message.c_str());
	}
}

void ThrowArrayStore(JNIEnv *env)
{
	jclass error = reinterpret_cast<jclass>(
		JniFindClass(env, "java/lang/ArrayStoreException"));
	if (error)
	{
		env->ThrowNew(error, "array store");
	}
}

// Resolves a java.lang.reflect.Array receiver, checking that it is an array
// object and that the index is inside it.
lpObject ResolveArray(JNIEnv *env, jobject array, int index, bool check_index)
{
	if (!array)
	{
		jclass error = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/NullPointerException"));
		if (error)
		{
			env->ThrowNew(error, "array");
		}
		return NULL;
	}
	lpObject object = JniResolveObject(array);
	if (!object || !object->obj_class || !object->obj_class->thisClass ||
	    !object->obj_class->thisClass->GetName() ||
	    object->obj_class->thisClass->GetName()[0] != '[')
	{
		ThrowArrayIllegalArgument(env, "not an array");
		return NULL;
	}
	if (check_index && (index < 0 || index >= object->array_length))
	{
		ThrowArrayIndex(env, index);
		return NULL;
	}
	return object;
}

bool ArrayElementMatches(lpObject object, DATA_TYPE expected)
{
	if (expected == DT_VOID)
	{
		return true;
	}
	DATA_TYPE actual = object->array_data_type;
	if (actual == DT_ARRAY)
	{
		actual = DT_REF;
	}
	if (expected == DT_ARRAY)
	{
		expected = DT_REF;
	}
	return actual == expected;
}

bool ArrayStoreAllowed(JNIEnv *env, lpObject array, jobject value)
{
	if (!value || array->array_data_type != DT_REF)
	{
		return true;
	}
	lpObject source = JniResolveObject(value);
	if (!source)
	{
		return true;
	}
	const char *name = array->obj_class && array->obj_class->thisClass
		? array->obj_class->thisClass->GetName() : NULL;
	if (!name || name[0] != '[' || name[1] != 'L')
	{
		return true;
	}
	size_t length = strlen(name);
	if (length < 3)
	{
		return true;
	}
	std::string component(name + 2, length - 3);
	lpClass target = JniFindClass(env, component.c_str());
	if (!target)
	{
		return true;
	}
	return IsInstanceOfClass(source, target);
}

jobject BoxArrayElement(JNIEnv *env, lpObject array, int index, DATA_TYPE expected)
{
	if (!ArrayElementMatches(array, expected))
	{
		ThrowArrayIllegalArgument(env, "array type mismatch");
		return NULL;
	}
	if (array->array_data_type == DT_REF || array->array_data_type == DT_ARRAY)
	{
		jobject *refs = static_cast<jobject *>(array->array_var);
		jobject value = refs ? refs[index] : NULL;
		if (value)
		{
			JniAddLocalRef(env, value);
		}
		return value;
	}
	Variant value;
	switch (array->array_data_type)
	{
	case DT_BOOL: value.blValue = static_cast<jboolean *>(array->array_var)[index]; break;
	case DT_BYTE: value.bValue = static_cast<jbyte *>(array->array_var)[index]; break;
	case DT_CHAR: value.cValue = static_cast<jchar *>(array->array_var)[index]; break;
	case DT_SHORT: value.sValue = static_cast<jshort *>(array->array_var)[index]; break;
	case DT_INT: value.iValue = static_cast<jint *>(array->array_var)[index]; break;
	case DT_LONG: value.lValue = static_cast<jlong *>(array->array_var)[index]; break;
	case DT_FLOAT: value.fValue = static_cast<jfloat *>(array->array_var)[index]; break;
	case DT_DOUBLE: value.dValue = static_cast<jdouble *>(array->array_var)[index]; break;
	default: return NULL;
	}
	return BoxValue(env, array->array_data_type, value);
}

bool StoreArrayElement(JNIEnv *env, lpObject array, int index, jobject value,
                       DATA_TYPE expected)
{
	if (!ArrayElementMatches(array, expected))
	{
		ThrowArrayIllegalArgument(env, "array type mismatch");
		return false;
	}
	if (array->array_data_type == DT_REF || array->array_data_type == DT_ARRAY)
	{
		if (!ArrayStoreAllowed(env, array, value))
		{
			ThrowArrayIllegalArgument(env, "value cannot be converted");
			return false;
		}
		jobject *elements = static_cast<jobject *>(array->array_var);
		LPOBJECT_REF new_ref = JniRef(value);
		LPOBJECT_REF old_ref = reinterpret_cast<LPOBJECT_REF>(elements[index]);
		elements[index] = reinterpret_cast<jobject>(new_ref);
		GCBarrierWrite(old_ref, new_ref, GCBarrier::REFLECT);
		return true;
	}
	Variant raw;
	if (!UnboxValue(value, array->array_data_type, raw))
	{
		ThrowArrayIllegalArgument(env, "value type mismatch");
		return false;
	}
	switch (array->array_data_type)
	{
	case DT_BOOL: static_cast<jboolean *>(array->array_var)[index] = raw.blValue; break;
	case DT_BYTE: static_cast<jbyte *>(array->array_var)[index] = raw.bValue; break;
	case DT_CHAR: static_cast<jchar *>(array->array_var)[index] = raw.cValue; break;
	case DT_SHORT: static_cast<jshort *>(array->array_var)[index] = raw.sValue; break;
	case DT_INT: static_cast<jint *>(array->array_var)[index] = raw.iValue; break;
	case DT_LONG: static_cast<jlong *>(array->array_var)[index] = raw.lValue; break;
	case DT_FLOAT: static_cast<jfloat *>(array->array_var)[index] = raw.fValue; break;
	case DT_DOUBLE: static_cast<jdouble *>(array->array_var)[index] = raw.dValue; break;
	default: break;
	}
	return true;
}

bool BuildArguments(lpObject args_array, const std::vector<TypeDesc> &types,
                    std::vector<jvalue> &out)
{
	int length = args_array ? args_array->array_length : 0;
	jobject *elements = (args_array && args_array->array_var)
		? static_cast<jobject *>(args_array->array_var) : NULL;
	for (size_t i = 0; i < types.size(); i++)
	{
		jvalue value;
		memset(&value, 0, sizeof(value));
		DATA_TYPE type = types[i].check_array ? DT_ARRAY : types[i].data_type;
		jobject arg = (static_cast<int>(i) < length && elements) ? elements[i] : NULL;
		if (type == DT_REF || type == DT_ARRAY)
		{
			value.l = reinterpret_cast<jobject>(JniRef(arg));
		}
		else
		{
			Variant raw;
			if (arg && !UnboxValue(arg, type, raw))
			{
				return false;
			}
			switch (type)
			{
			case DT_BOOL: value.z = raw.blValue; break;
			case DT_BYTE: value.b = raw.bValue; break;
			case DT_CHAR: value.c = raw.cValue; break;
			case DT_SHORT: value.s = raw.sValue; break;
			case DT_INT: value.i = raw.iValue; break;
			case DT_LONG: value.j = raw.lValue; break;
			case DT_FLOAT: value.f = raw.fValue; break;
			case DT_DOUBLE: value.d = raw.dValue; break;
			default: break;
			}
		}
		out.push_back(value);
	}
	return true;
}

Variant JValueToVariant(const jvalue &value, DATA_TYPE type)
{
	Variant out;
	switch (type)
	{
	case DT_BOOL: out.blValue = value.z ? 1 : 0; break;
	case DT_BYTE: out.bValue = value.b; break;
	case DT_CHAR: out.cValue = value.c; break;
	case DT_SHORT: out.sValue = value.s; break;
	case DT_INT: out.iValue = value.i; break;
	case DT_LONG: out.lValue = value.j; break;
	case DT_FLOAT: out.fValue = value.f; break;
	case DT_DOUBLE: out.dValue = value.d; break;
	case DT_REF:
	case DT_ARRAY: out.heapPtr = JniRef(value.l); break;
	default: break;
	}
	return out;
}

jobject VariantToObject(JNIEnv *env, DATA_TYPE type, const Variant &value)
{
	if (type == DT_REF || type == DT_ARRAY)
	{
		jobject ref = JniObject(value.heapPtr);
		if (ref)
		{
			JniAddLocalRef(env, ref);
		}
		return ref;
	}
	if (type == DT_VOID)
	{
		return NULL;
	}
	return BoxValue(env, type, value);
}

std::vector<lpMethod> CollectMethods(lpClass clazz, bool public_only,
                                     bool declared_only)
{
	std::vector<lpMethod> result;
	std::vector<lpClass> classes;
	// getMethods() on an interface must not report java.lang.Object's public
	// methods: interfaces have Object as their class-file superclass, but the
	// JDK only exposes the interface and its superinterfaces.  Callers such as
	// Byte Buddy's Invoker generator assume every reported method belongs to
	// the interface and fail on zero-argument Object methods.
	bool interface_owner = clazz && clazz->isInterface();
	for (lpClass c = clazz; c; c = c->super_class)
	{
		if (interface_owner && c != clazz && c->thisClass &&
		    c->thisClass->GetName() &&
		    strcmp(c->thisClass->GetName(), "java/lang/Object") == 0)
		{
			break;
		}
		classes.push_back(c);
		if (declared_only)
		{
			break;
		}
	}
	if (!declared_only)
	{
		// Append directly implemented interfaces (no transitive walk needed
		// for the common single-level case).
		for (size_t i = 0; i < classes.size(); i++)
		{
			for (U2 k = 0; k < classes[i]->interfaceCount; k++)
			{
				lpClassConstant ic = classes[i]->interfaces ? classes[i]->interfaces[k] : NULL;
				if (!ic || !ic->GetName() || !classes[i]->classloader)
				{
					continue;
				}
				lpClass iface = NULL;
				if (classes[i]->classloader->FindClass(ic->GetName(), iface) && iface)
				{
					classes.push_back(iface);
				}
			}
		}
	}
	for (size_t i = 0; i < classes.size(); i++)
	{
		lpClass c = classes[i];
		for (U2 m = 0; m < c->methodCount; m++)
		{
			lpMethod method = c->methods ? c->methods[m] : NULL;
			if (!method || !method->methodName || !method->methodDesc)
			{
				continue;
			}
			const char *name = method->methodName->GetName();
			if (strcmp(name, "<init>") == 0 || strcmp(name, "<clinit>") == 0)
			{
				continue;
			}
			if (public_only && (method->accFlag & ACM_PUBLIC) == 0)
			{
				continue;
			}
			bool duplicate = false;
			for (size_t k = 0; k < result.size(); k++)
			{
				if (strcmp(result[k]->methodName->GetName(), name) == 0 &&
				    strcmp(result[k]->methodDesc->GetName(),
				           method->methodDesc->GetName()) == 0)
				{
					duplicate = true;
					break;
				}
			}
			if (!duplicate)
			{
				result.push_back(method);
			}
		}
	}
	return result;
}

std::vector<lpField> CollectFields(lpClass clazz, bool public_only,
                                   bool declared_only)
{
	std::vector<lpField> result;
	for (lpClass c = clazz; c; c = c->super_class)
	{
		for (U2 i = 0; i < c->fieldCount; i++)
		{
			lpField field = c->fields ? c->fields[i] : NULL;
			if (!field || !field->fieldName)
			{
				continue;
			}
			if (public_only && (field->accFlag & ACF_PUBLIC) == 0)
			{
				continue;
			}
			bool duplicate = false;
			for (size_t k = 0; k < result.size(); k++)
			{
				if (strcmp(result[k]->fieldName->GetName(),
				           field->fieldName->GetName()) == 0)
				{
					duplicate = true;
					break;
				}
			}
			if (!duplicate)
			{
				result.push_back(field);
			}
		}
		if (declared_only)
		{
			break;
		}
	}
	return result;
}

jobject MakeMethod(JNIEnv *env, lpMethod method)
{
	if (!method || !method->clazz)
	{
		return NULL;
	}
	return MakeMember(env, "java/lang/reflect/Method", method->clazz,
	                  method->methodName->GetName(),
	                  method->methodDesc->GetName(), method->accFlag);
}

jobject MakeField(JNIEnv *env, lpClass owner, lpField field)
{
	if (!field)
	{
		return NULL;
	}
	return MakeMember(env, "java/lang/reflect/Field", owner,
	                  field->fieldName->GetName(),
	                  field->fieldDesc->GetName(), field->accFlag);
}

jobject MakeConstructor(JNIEnv *env, lpMethod method)
{
	if (!method || !method->clazz)
	{
		return NULL;
	}
	return MakeMember(env, "java/lang/reflect/Constructor", method->clazz,
	                  NULL, method->methodDesc->GetName(), method->accFlag);
}

} // namespace

// ---------------------------------------------------------------------------
// java.lang.Class: member enumeration
// ---------------------------------------------------------------------------

extern "C" {

JNIEXPORT jint JNICALL Java_java_lang_Class_getModifiers0(JNIEnv *env, jobject thiz)
{
	(void)env;
	lpClass clazz = MirrorClass(thiz);
	return clazz ? static_cast<jint>(clazz->accFlag) : 0;
}

JNIEXPORT jboolean JNICALL Java_java_lang_Class_isAssignableFrom(JNIEnv *env,
	jobject thiz, jobject other)
{
	(void)env;
	lpClass target = MirrorClass(thiz);
	lpClass src = MirrorClass(other);
	return ClassIsAssignable(target, src) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getInterfaces0(JNIEnv *env,
	jobject thiz)
{
	lpClass clazz = MirrorClass(thiz);
	std::vector<jobject> items;
	if (clazz && clazz->interfaces)
	{
		for (U2 i = 0; i < clazz->interfaceCount; i++)
		{
			lpClassConstant ic = clazz->interfaces[i];
			if (!ic || !ic->GetName())
			{
				continue;
			}
			lpClass iface = NULL;
			try
			{
				if (clazz->classloader)
				{
					clazz->classloader->FindClass(ic->GetName(), iface);
				}
			}
			catch (...)
			{
				iface = NULL;
			}
			if (iface)
			{
				jobject mirror = JniClassMirror(reinterpret_cast<jclass>(iface));
				if (mirror)
				{
					JniAddLocalRef(env, mirror);
					items.push_back(mirror);
				}
			}
		}
	}
	return MakeObjectArray(env, "[Ljava/lang/Class;", items);
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getDeclaredClasses0(JNIEnv *env,
	jobject thiz)
{
	lpClass clazz = MirrorClass(thiz);
	std::vector<jobject> items;
	if (clazz && clazz->const_pool && clazz->inner_classes &&
	    clazz->inner_classes->inner_classes && clazz->thisClass)
	{
		const char *owner = clazz->thisClass->GetName();
		for (U2 i = 0; i < clazz->inner_classes->number_of_classes; i++)
		{
			const InnerClassesInfo &info = clazz->inner_classes->inner_classes[i];
			if (!owner || info.outer_class_info_index == 0)
			{
				continue;
			}
			lpClassConstant inner = (lpClassConstant)
				clazz->const_pool->GetConstant(info.inner_class_info_index);
			lpClassConstant outer = (lpClassConstant)
				clazz->const_pool->GetConstant(info.outer_class_info_index);
			if (!inner || !outer || !inner->GetName() || !outer->GetName() ||
			    strcmp(outer->GetName(), owner) != 0)
			{
				continue;
			}
			lpClass member = FindClassAnywhere(env, inner->GetName());
			if (!member)
			{
				continue;
			}
			jobject mirror = JniClassMirror(reinterpret_cast<jclass>(member));
			if (mirror)
			{
				JniAddLocalRef(env, mirror);
				items.push_back(mirror);
			}
		}
	}
	return MakeObjectArray(env, "[Ljava/lang/Class;", items);
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getDeclaredMethods0(JNIEnv *env,
	jobject thiz)
{
	lpClass clazz = MirrorClass(thiz);
	std::vector<jobject> items;
	std::vector<lpMethod> methods = CollectMethods(clazz, false, true);
	for (size_t i = 0; i < methods.size(); i++)
	{
		jobject method = MakeMethod(env, methods[i]);
		if (method)
		{
			items.push_back(method);
		}
	}
	return MakeObjectArray(env, "[Ljava/lang/reflect/Method;", items);
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getMethods0(JNIEnv *env,
	jobject thiz)
{
	lpClass clazz = MirrorClass(thiz);
	std::vector<jobject> items;
	std::vector<lpMethod> methods = CollectMethods(clazz, true, false);
	for (size_t i = 0; i < methods.size(); i++)
	{
		jobject method = MakeMethod(env, methods[i]);
		if (method)
		{
			items.push_back(method);
		}
	}
	return MakeObjectArray(env, "[Ljava/lang/reflect/Method;", items);
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getDeclaredMethod0(JNIEnv *env,
	jobject thiz, jstring name, jobjectArray parameter_types)
{
	lpClass clazz = MirrorClass(thiz);
	if (!clazz || !name)
	{
		return NULL;
	}
	const char *method_name = env->GetStringUTFChars(name, NULL);
	if (!method_name)
	{
		return NULL;
	}
	std::string descriptor = "(";
	if (parameter_types)
	{
		lpObject types = JniResolveObject(reinterpret_cast<jobject>(parameter_types));
		if (types && types->array_var)
		{
			jobject *elements = static_cast<jobject *>(types->array_var);
			for (int i = 0; i < types->array_length; i++)
			{
				descriptor += ClassToDescriptor(elements[i]);
			}
		}
	}
	descriptor += ")";
	lpMethod method = FindMethodByParams(clazz, method_name, descriptor.c_str(), true);
	env->ReleaseStringUTFChars(name, method_name);
	if (!method)
	{
		jclass error = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/NoSuchMethodException"));
		if (error)
		{
			env->ThrowNew(error, "method not found");
		}
		return NULL;
	}
	jobject result = MakeMethod(env, method);
	if (result)
	{
		JniAddLocalRef(env, result);
	}
	return result;
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getMethod0(JNIEnv *env,
	jobject thiz, jstring name, jobjectArray parameter_types)
{
	lpClass clazz = MirrorClass(thiz);
	if (!clazz || !name)
	{
		return NULL;
	}
	const char *method_name = env->GetStringUTFChars(name, NULL);
	if (!method_name)
	{
		return NULL;
	}
	std::string descriptor = "(";
	if (parameter_types)
	{
		lpObject types = JniResolveObject(reinterpret_cast<jobject>(parameter_types));
		if (types && types->array_var)
		{
			jobject *elements = static_cast<jobject *>(types->array_var);
			for (int i = 0; i < types->array_length; i++)
			{
				descriptor += ClassToDescriptor(elements[i]);
			}
		}
	}
	descriptor += ")";
	lpMethod method = FindMethodByParams(clazz, method_name, descriptor.c_str(), false);
	env->ReleaseStringUTFChars(name, method_name);
	if (!method)
	{
		jclass error = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/NoSuchMethodException"));
		if (error)
		{
			env->ThrowNew(error, "method not found");
		}
		return NULL;
	}
	jobject result = MakeMethod(env, method);
	if (result)
	{
		JniAddLocalRef(env, result);
	}
	return result;
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getDeclaredFields0(JNIEnv *env,
	jobject thiz)
{
	lpClass clazz = MirrorClass(thiz);
	std::vector<jobject> items;
	std::vector<lpField> fields = CollectFields(clazz, false, true);
	for (size_t i = 0; i < fields.size(); i++)
	{
		jobject field = MakeField(env, clazz, fields[i]);
		if (field)
		{
			items.push_back(field);
		}
	}
	return MakeObjectArray(env, "[Ljava/lang/reflect/Field;", items);
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getFields0(JNIEnv *env,
	jobject thiz)
{
	lpClass clazz = MirrorClass(thiz);
	std::vector<jobject> items;
	std::vector<lpField> fields = CollectFields(clazz, true, false);
	for (size_t i = 0; i < fields.size(); i++)
	{
		jobject field = MakeField(env, clazz, fields[i]);
		if (field)
		{
			items.push_back(field);
		}
	}
	return MakeObjectArray(env, "[Ljava/lang/reflect/Field;", items);
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getDeclaredField0(JNIEnv *env,
	jobject thiz, jstring name)
{
	lpClass clazz = MirrorClass(thiz);
	if (!clazz || !name)
	{
		return NULL;
	}
	const char *field_name = env->GetStringUTFChars(name, NULL);
	lpField field = FindFieldByName(clazz, field_name, true);
	env->ReleaseStringUTFChars(name, field_name);
	if (!field)
	{
		jclass error = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/NoSuchFieldException"));
		if (error)
		{
			env->ThrowNew(error, "field not found");
		}
		return NULL;
	}
	jobject result = MakeField(env, clazz, field);
	if (result)
	{
		JniAddLocalRef(env, result);
	}
	return result;
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getField0(JNIEnv *env,
	jobject thiz, jstring name)
{
	lpClass clazz = MirrorClass(thiz);
	if (!clazz || !name)
	{
		return NULL;
	}
	const char *field_name = env->GetStringUTFChars(name, NULL);
	lpField field = FindFieldByName(clazz, field_name, false);
	env->ReleaseStringUTFChars(name, field_name);
	if (!field)
	{
		jclass error = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/NoSuchFieldException"));
		if (error)
		{
			env->ThrowNew(error, "field not found");
		}
		return NULL;
	}
	jobject result = MakeField(env, clazz, field);
	if (result)
	{
		JniAddLocalRef(env, result);
	}
	return result;
}

static jobject ClassConstructors(JNIEnv *env, lpClass clazz, bool public_only)
{
	std::vector<jobject> items;
	if (clazz)
	{
		for (U2 i = 0; i < clazz->methodCount; i++)
		{
			lpMethod method = clazz->methods ? clazz->methods[i] : NULL;
			if (!method || !method->methodName ||
			    strcmp(method->methodName->GetName(), "<init>") != 0)
			{
				continue;
			}
			if (public_only && (method->accFlag & ACM_PUBLIC) == 0)
			{
				continue;
			}
			jobject ctor = MakeConstructor(env, method);
			if (ctor)
			{
				items.push_back(ctor);
			}
		}
	}
	return MakeObjectArray(env, "[Ljava/lang/reflect/Constructor;", items);
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getDeclaredConstructors0(JNIEnv *env,
	jobject thiz)
{
	return ClassConstructors(env, MirrorClass(thiz), false);
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getConstructors0(JNIEnv *env,
	jobject thiz)
{
	return ClassConstructors(env, MirrorClass(thiz), true);
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getDeclaredAnnotations0(JNIEnv *env,
	jobject thiz)
{
	lpClass clazz = MirrorClass(thiz);
	return AnnotationsToArray(env, clazz,
		clazz ? clazz->rt_visible_attr : NULL);
}

JNIEXPORT jobject JNICALL Java_java_lang_Class_getEnclosingClass0(JNIEnv *env,
	jobject thiz)
{
	lpClass clazz = MirrorClass(thiz);
	if (!clazz || !clazz->const_pool || !clazz->thisClass)
	{
		return NULL;
	}
	std::string self = clazz->thisClass->GetName();
	if (clazz->inner_classes)
	{
		for (U2 i = 0; i < clazz->inner_classes->number_of_classes; i++)
		{
			InnerClassesInfo &info = clazz->inner_classes->inner_classes[i];
			if (info.outer_class_info_index == 0)
			{
				continue;
			}
			lpClassConstant outer = static_cast<lpClassConstant>(
				clazz->const_pool->GetConstant(info.outer_class_info_index));
			lpClassConstant inner = static_cast<lpClassConstant>(
				clazz->const_pool->GetConstant(info.inner_class_info_index));
			if (!outer || !inner || outer->tag != CLASS_CONSTANT ||
			    inner->tag != CLASS_CONSTANT || !outer->utf8 ||
			    !outer->utf8->bytes || !inner->utf8 || !inner->utf8->bytes)
			{
				continue;
			}
			if (self != inner->utf8->bytes)
			{
				continue;
			}
			lpClass outer_class = JniFindClass(env, outer->utf8->bytes);
			if (!outer_class)
			{
				return NULL;
			}
			jobject mirror = JniClassMirror(reinterpret_cast<jclass>(outer_class));
			if (mirror)
			{
				JniAddLocalRef(env, mirror);
			}
			return mirror;
		}
	}
	if (clazz->enclose_method_attr)
	{
		lpClassConstant owner = static_cast<lpClassConstant>(
			clazz->const_pool->GetConstant(clazz->enclose_method_attr->class_index));
		if (owner && owner->tag == CLASS_CONSTANT && owner->utf8 && owner->utf8->bytes)
		{
			lpClass owner_class = JniFindClass(env, owner->utf8->bytes);
			if (owner_class)
			{
				jobject mirror = JniClassMirror(reinterpret_cast<jclass>(owner_class));
				if (mirror)
				{
					JniAddLocalRef(env, mirror);
				}
				return mirror;
			}
		}
	}
	return NULL;
}

// ---------------------------------------------------------------------------
// java.lang.reflect.Method
// ---------------------------------------------------------------------------

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Method_invoke0(JNIEnv *env,
	jobject thiz, jobject receiver, jobjectArray args)
{
	lpObject method_obj = JniResolveObject(thiz);
	if (!method_obj)
	{
		return NULL;
	}
	jobject declaring_mirror = ObjectFieldRef(method_obj, "declaringClass");
	lpClass declaring = MirrorClass(declaring_mirror);
	std::string name = ObjectFieldString(env, method_obj, "name");
	std::string desc = ObjectFieldString(env, method_obj, "descriptor");
	lpMethod target = JniFindMethod(declaring, name.c_str(), desc.c_str(), false);
	if (!target)
	{
		target = JniFindMethod(declaring, name.c_str(), desc.c_str(), true);
	}
	if (!target)
	{
		jclass error = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/NoSuchMethodException"));
		if (error)
		{
			env->ThrowNew(error, "method not found");
		}
		return NULL;
	}
	if (!MemberAccessAllowed(thiz, target->accFlag, declaring,
			CurrentReflectionCaller()))
	{
		ThrowIllegalAccess(env, "method is not accessible");
		return NULL;
	}
	if (!target->isStatic() && !receiver)
	{
		ThrowNullPointer(env, "null receiver for non-static method");
		return NULL;
	}
	std::vector<TypeDesc> types;
	TypeDesc result_desc;
	parse_method_desc_type(target->methodDesc->GetName(), types, result_desc);
	lpObject args_obj = JniResolveObject(reinterpret_cast<jobject>(args));
	std::vector<jvalue> values;
	if (!BuildArguments(args_obj, types, values))
	{
		jclass error = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/IllegalArgumentException"));
		if (error)
		{
			env->ThrowNew(error, "argument type mismatch");
		}
		return NULL;
	}
	LPOBJECT_REF proxy_handler = NULL;
	lpObject receiver_obj = JniResolveObject(receiver);
	if (receiver_obj && DarksVM_LoadProxyHandler(receiver_obj, proxy_handler))
	{
		DATA_TYPE return_type = result_desc.data_type;
		if (result_desc.check_array)
		{
			return_type = DT_ARRAY;
		}
		jvalue proxy_result;
		if (!DarksVM_InvokeProxyHandler(env, receiver, target, values,
		                                return_type, proxy_result))
		{
			if (VMContext::PendingException())
			{
				return WrapInvocationTarget(env, VMContext::PendingException());
			}
			return NULL;
		}
		return VariantToObject(env, return_type,
			JValueToVariant(proxy_result, return_type));
	}
	if (target->isStatic())
	{
		bool initialized = false;
		try
		{
			initialized = declaring->initialize();
		}
		catch (...)
		{
			initialized = false;
		}
		if (!initialized)
		{
			// A failing <clinit> is reported while the method is being
			// entered, so the pending ExceptionInInitializerError or
			// NoClassDefFoundError is not wrapped in InvocationTargetException.
			return NULL;
		}
	}
	jvalue result = JniInvokeMethod(env, receiver,
		reinterpret_cast<jclass>(declaring), reinterpret_cast<jmethodID>(target),
		values.empty() ? NULL : &values[0], target->isStatic(), false);
	if (VMContext::PendingException())
	{
		return WrapInvocationTarget(env, VMContext::PendingException());
	}
	return VariantToObject(env, result_desc.data_type,
		JValueToVariant(result, result_desc.data_type));
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Method_getDeclaredAnnotations0(JNIEnv *env,
	jobject thiz)
{
	lpObject method_obj = JniResolveObject(thiz);
	lpClass declaring = MirrorClass(ObjectFieldRef(method_obj, "declaringClass"));
	std::string name = ObjectFieldString(env, method_obj, "name");
	std::string desc = ObjectFieldString(env, method_obj, "descriptor");
	lpMethod method = FindMethodByDescriptor(declaring, name.c_str(), desc.c_str(), false);
	return AnnotationsToArray(env, declaring, method ? method->rt_visible_attr : NULL);
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Method_getParameterAnnotations0(JNIEnv *env,
	jobject thiz)
{
	lpObject method_obj = JniResolveObject(thiz);
	if (!method_obj)
	{
		return NULL;
	}
	lpClass declaring = MirrorClass(ObjectFieldRef(method_obj, "declaringClass"));
	std::string name = ObjectFieldString(env, method_obj, "name");
	std::string desc = ObjectFieldString(env, method_obj, "descriptor");
	lpMethod method = FindMethodByDescriptor(declaring, name.c_str(), desc.c_str(), false);
	return ParameterAnnotationsToArray(env, declaring,
		method ? method->rt_visible_param_attr : NULL,
		CountMethodParameters(desc.c_str()));
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Method_getDefaultValue(JNIEnv *env,
	jobject thiz)
{
	lpObject method_obj = JniResolveObject(thiz);
	if (!method_obj)
	{
		return NULL;
	}
	lpClass declaring = MirrorClass(ObjectFieldRef(method_obj, "declaringClass"));
	std::string name = ObjectFieldString(env, method_obj, "name");
	std::string desc = ObjectFieldString(env, method_obj, "descriptor");
	lpMethod method = FindMethodByDescriptor(declaring, name.c_str(), desc.c_str(), false);
	if (!method || !method->annotation_default || !method->annotation_default->bytes ||
	    method->annotation_default->attr_length <= 0)
	{
		return NULL;
	}
	lpClass owner = method->clazz ? method->clazz : declaring;
	if (!owner || !owner->const_pool)
	{
		return NULL;
	}
	std::string return_desc = AnnotationElementDescriptor(method);
	if (return_desc.empty())
	{
		return NULL;
	}
	ByteReader reader(method->annotation_default->bytes,
	                  method->annotation_default->attr_length);
	Variant value;
	if (!ReadElementValue(env, owner->const_pool, reader, return_desc.c_str(), value))
	{
		return NULL;
	}
	return AnnotationValueObject(env, value, return_desc.c_str());
}

// ---------------------------------------------------------------------------
// java.lang.reflect.Field
// ---------------------------------------------------------------------------

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Field_get0(JNIEnv *env,
	jobject thiz, jobject obj)
{
	lpObject field_obj = JniResolveObject(thiz);
	lpClass declaring = MirrorClass(ObjectFieldRef(field_obj, "declaringClass"));
	std::string name = ObjectFieldString(env, field_obj, "name");
	std::string desc = ObjectFieldString(env, field_obj, "descriptor");
	lpField field = JniFindField(declaring, name.c_str(), desc.c_str(),
	                             false);
	if (!field)
	{
		field = JniFindField(declaring, name.c_str(), desc.c_str(), true);
	}
	if (!field)
	{
		jclass error = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/NoSuchFieldException"));
		if (error)
		{
			env->ThrowNew(error, "field not found");
		}
		return NULL;
	}
	if (!MemberAccessAllowed(thiz, field->accFlag, declaring,
			CurrentReflectionCaller()))
	{
		ThrowIllegalAccess(env, "field is not accessible");
		return NULL;
	}
	Variant value;
	if ((field->accFlag & ACF_STATIC) != 0)
	{
		DarksVM_EnsureInitialized(declaring);
		lpFieldValue fv = JniStaticFieldValue(declaring, field, true);
		if (!fv)
		{
			return NULL;
		}
		value = fv->field_value;
	}
	else
	{
		if (!obj)
		{
			ThrowNullPointer(env, "null object");
			return NULL;
		}
		lpFieldValue fv = JniInstanceFieldValue(obj, reinterpret_cast<jfieldID>(field));
		if (!fv)
		{
			ThrowNullPointer(env, "null object");
			return NULL;
		}
		value = fv->field_value;
	}
	TypeDesc type;
	parse_desc_type(desc.c_str(), type, static_cast<int>(desc.size()));
	return VariantToObject(env, type.check_array ? DT_ARRAY : type.data_type, value);
}

JNIEXPORT void JNICALL Java_java_lang_reflect_Field_set0(JNIEnv *env,
	jobject thiz, jobject obj, jobject new_value)
{
	lpObject field_obj = JniResolveObject(thiz);
	lpClass declaring = MirrorClass(ObjectFieldRef(field_obj, "declaringClass"));
	std::string name = ObjectFieldString(env, field_obj, "name");
	std::string desc = ObjectFieldString(env, field_obj, "descriptor");
	lpField field = JniFindField(declaring, name.c_str(), desc.c_str(), false);
	if (!field)
	{
		field = JniFindField(declaring, name.c_str(), desc.c_str(), true);
	}
	if (!field)
	{
		jclass error = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/NoSuchFieldException"));
		if (error)
		{
			env->ThrowNew(error, "field not found");
		}
		return;
	}
	if (!MemberAccessAllowed(thiz, field->accFlag, declaring,
			CurrentReflectionCaller()))
	{
		ThrowIllegalAccess(env, "field is not accessible");
		return;
	}
	TypeDesc type;
	parse_desc_type(desc.c_str(), type, static_cast<int>(desc.size()));
	Variant value;
	if (type.data_type == DT_REF || type.data_type == DT_ARRAY || type.check_array)
	{
		value.heapPtr = JniRef(new_value);
	}
	else if (!UnboxValue(new_value, type.data_type, value))
	{
		jclass error = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/IllegalArgumentException"));
		if (error)
		{
			env->ThrowNew(error, "value type mismatch");
		}
		return;
	}
	if ((field->accFlag & ACF_STATIC) == 0 && !obj)
	{
		ThrowNullPointer(env, "null object");
		return;
	}
	lpFieldValue fv = NULL;
	if ((field->accFlag & ACF_STATIC) != 0)
	{
		DarksVM_EnsureInitialized(declaring);
		fv = JniStaticFieldValue(declaring, field, true);
	}
	else
	{
		fv = JniInstanceFieldValue(obj, reinterpret_cast<jfieldID>(field));
	}
	if (fv)
	{
		if (type.data_type == DT_REF || type.data_type == DT_ARRAY ||
		    type.check_array)
		{
			LPOBJECT_REF old_ref = fv->field_value.heapPtr;
			fv->field_value = value;
			GCBarrierWrite(old_ref, value.heapPtr, GCBarrier::REFLECT);
		}
		else
		{
			fv->field_value = value;
		}
	}
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Field_getDeclaredAnnotations0(JNIEnv *env,
	jobject thiz)
{
	lpObject field_obj = JniResolveObject(thiz);
	lpClass declaring = MirrorClass(ObjectFieldRef(field_obj, "declaringClass"));
	std::string name = ObjectFieldString(env, field_obj, "name");
	std::string desc = ObjectFieldString(env, field_obj, "descriptor");
	lpField field = FindFieldByName(declaring, name.c_str(), false);
	return AnnotationsToArray(env, declaring, field ? field->rt_visible_attr : NULL);
}

// ---------------------------------------------------------------------------
// java.lang.reflect.Constructor
// ---------------------------------------------------------------------------

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Constructor_newInstance0(JNIEnv *env,
	jobject thiz, jobjectArray args)
{
	lpObject ctor_obj = JniResolveObject(thiz);
	lpClass declaring = MirrorClass(ObjectFieldRef(ctor_obj, "declaringClass"));
	std::string desc = ObjectFieldString(env, ctor_obj, "descriptor");
	if (!declaring)
	{
		return NULL;
	}
	lpMethod init = FindMethodByDescriptor(declaring, "<init>", desc.c_str(), true);
	if (!init)
	{
		return NULL;
	}
	if (!MemberAccessAllowed(thiz, init->accFlag, declaring,
			CurrentReflectionCaller()))
	{
		ThrowIllegalAccess(env, "constructor is not accessible");
		return NULL;
	}
	if ((declaring->accFlag & 0x0400) != 0 || declaring->isInterface())
	{
		jclass error = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/InstantiationException"));
		if (error)
		{
			env->ThrowNew(error, "cannot instantiate an abstract class");
		}
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewObject(declaring, ref))
	{
		return NULL;
	}
	jobject object = JniObject(ref);
	JniAddLocalRef(env, object);
	std::vector<TypeDesc> types;
	TypeDesc result_desc;
	parse_method_desc_type(init->methodDesc->GetName(), types, result_desc);
	lpObject args_obj = JniResolveObject(reinterpret_cast<jobject>(args));
	std::vector<jvalue> values;
	if (!BuildArguments(args_obj, types, values))
	{
		jclass error = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/IllegalArgumentException"));
		if (error)
		{
			env->ThrowNew(error, "argument type mismatch");
		}
		return NULL;
	}
	JniInvokeMethod(env, object, reinterpret_cast<jclass>(declaring),
		reinterpret_cast<jmethodID>(init), values.empty() ? NULL : &values[0],
		false, true);
	if (VMContext::PendingException())
	{
		return WrapInvocationTarget(env, VMContext::PendingException());
	}
	return object;
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Constructor_getDeclaredAnnotations0(JNIEnv *env,
	jobject thiz)
{
	lpObject ctor_obj = JniResolveObject(thiz);
	lpClass declaring = MirrorClass(ObjectFieldRef(ctor_obj, "declaringClass"));
	std::string desc = ObjectFieldString(env, ctor_obj, "descriptor");
	lpMethod init = FindMethodByDescriptor(declaring, "<init>", desc.c_str(), true);
	return AnnotationsToArray(env, declaring, init ? init->rt_visible_attr : NULL);
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Constructor_getParameterAnnotations0(JNIEnv *env,
	jobject thiz)
{
	lpObject ctor_obj = JniResolveObject(thiz);
	if (!ctor_obj)
	{
		return NULL;
	}
	lpClass declaring = MirrorClass(ObjectFieldRef(ctor_obj, "declaringClass"));
	std::string desc = ObjectFieldString(env, ctor_obj, "descriptor");
	lpMethod init = FindMethodByDescriptor(declaring, "<init>", desc.c_str(), true);
	return ParameterAnnotationsToArray(env, declaring,
		init ? init->rt_visible_param_attr : NULL,
		CountMethodParameters(desc.c_str()));
}

// ---------------------------------------------------------------------------
// Class component type and member type descriptors
// ---------------------------------------------------------------------------

JNIEXPORT jobject JNICALL Java_java_lang_Class_getComponentType(JNIEnv *env,
	jobject thiz)
{
	lpClass clazz = MirrorClass(thiz);
	if (!clazz || !clazz->thisClass || !clazz->thisClass->GetName())
	{
		return NULL;
	}
	const char *name = clazz->thisClass->GetName();
	if (name[0] != '[')
	{
		return NULL;
	}
	// The component class must be resolved through the loader that defined the
	// array: application classes are not visible to the bootstrap loader.
	const char *desc = name + 1;
	int length = static_cast<int>(strlen(desc));
	lpClass component = clazz->component_class;
	if (!component && desc[0] == 'L' && length >= 2 && desc[length - 1] == ';')
	{
		std::string component_name(desc + 1, length - 2);
		if (!DarksVM_LoadClassWithoutInitializing(component_name, component))
		{
			component = JniFindClass(env, component_name.c_str());
		}
	}
	else if (desc[0] == '[')
	{
		if (!DarksVM_LoadClassWithoutInitializing(std::string(desc, length), component))
		{
			component = JniFindClass(env, desc);
		}
	}
	else if (length == 1)
	{
		const char *primitive = NULL;
		switch (desc[0])
		{
		case 'Z': primitive = "boolean"; break;
		case 'B': primitive = "byte"; break;
		case 'C': primitive = "char"; break;
		case 'S': primitive = "short"; break;
		case 'I': primitive = "int"; break;
		case 'J': primitive = "long"; break;
		case 'F': primitive = "float"; break;
		case 'D': primitive = "double"; break;
		default: break;
		}
		if (primitive)
		{
			component = DarksVM_GetPrimitiveClass(primitive);
		}
	}
	if (!component)
	{
		return NULL;
	}
	jobject mirror = JniClassMirror(reinterpret_cast<jclass>(component));
	if (mirror)
	{
		JniAddLocalRef(env, mirror);
	}
	return mirror;
}

static int PrimitiveArrayTag(const char *name)
{
	if (!name)
	{
		return 0;
	}
	if (strcmp(name, "boolean") == 0) return 'Z';
	if (strcmp(name, "byte") == 0) return 'B';
	if (strcmp(name, "char") == 0) return 'C';
	if (strcmp(name, "short") == 0) return 'S';
	if (strcmp(name, "int") == 0) return 'I';
	if (strcmp(name, "long") == 0) return 'J';
	if (strcmp(name, "float") == 0) return 'F';
	if (strcmp(name, "double") == 0) return 'D';
	return 0;
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Array_newInstance0(JNIEnv *env,
	jclass, jobject component, jint length)
{
	if (length < 0)
	{
		VMContext::SetPendingException(CreateJavaException(
			"java/lang/NegativeArraySizeException", "negative array size"));
		return NULL;
	}
	lpClass component_class = MirrorClass(component);
	if (!component_class || !component_class->thisClass ||
	    !component_class->thisClass->GetName())
	{
		return NULL;
	}
	const char *name = component_class->thisClass->GetName();
	std::string array_name;
	int tag = PrimitiveArrayTag(name);
	if (tag != 0)
	{
		array_name = std::string("[") + static_cast<char>(tag);
	}
	else if (name[0] == '[')
	{
		array_name = std::string("[") + name;
	}
	else
	{
		array_name = std::string("[L") + name + ";";
	}
	lpClass array_class = JniFindClass(env, array_name.c_str());
	if (!array_class)
	{
		return NULL;
	}
	if (!array_class->component_class)
	{
		array_class->component_class = component_class;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewObjectArrayObject(length, array_class, ref))
	{
		if (!VMContext::PendingException())
		{
			VMContext::SetPendingException(CreateJavaException(
				"java/lang/OutOfMemoryError", "cannot allocate array"));
		}
		return NULL;
	}
	jobject result = JniObject(ref);
	if (result)
	{
		JniAddLocalRef(env, result);
	}
	return result;
}

JNIEXPORT jint JNICALL Java_java_lang_reflect_Array_length0(JNIEnv *, jclass,
	jobject array)
{
	lpObject obj = JniResolveObject(array);
	if (!obj || !obj->array_var)
	{
		return 0;
	}
	return static_cast<jint>(obj->array_length);
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Field_getType(JNIEnv *env,
	jobject thiz)
{
	std::string desc = ObjectFieldString(env, JniResolveObject(thiz),
		"descriptor");
	if (desc.empty())
	{
		return NULL;
	}
	return MirrorForDescriptor(env, desc.c_str(),
		static_cast<int>(desc.size()));
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Method_getReturnType(JNIEnv *env,
	jobject thiz)
{
	std::string desc = ObjectFieldString(env, JniResolveObject(thiz),
		"descriptor");
	return ReturnTypeFromDescriptor(env, desc.c_str());
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Method_getParameterTypes(JNIEnv *env,
	jobject thiz)
{
	std::string desc = ObjectFieldString(env, JniResolveObject(thiz),
		"descriptor");
	return ParameterTypesFromDescriptor(env, desc.c_str());
}

static lpMethod ResolveMemberMethod(JNIEnv *env, jobject member)
{
	lpObject object = JniResolveObject(member);
	lpClass declaring = MirrorClass(ObjectFieldRef(object, "declaringClass"));
	std::string name = ObjectFieldString(env, object, "name");
	std::string desc = ObjectFieldString(env, object, "descriptor");
	return FindMethodByDescriptor(declaring, name.c_str(), desc.c_str(), false);
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Method_getExceptionTypes(JNIEnv *env,
	jobject thiz)
{
	return ClassArrayFromExceptionAttribute(env,
		ResolveMemberMethod(env, thiz));
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Constructor_getParameterTypes(JNIEnv *env,
	jobject thiz)
{
	std::string desc = ObjectFieldString(env, JniResolveObject(thiz),
		"descriptor");
	return ParameterTypesFromDescriptor(env, desc.c_str());
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Constructor_getExceptionTypes(JNIEnv *env,
	jobject thiz)
{
	lpObject object = JniResolveObject(thiz);
	lpClass declaring = MirrorClass(ObjectFieldRef(object, "declaringClass"));
	std::string desc = ObjectFieldString(env, object, "descriptor");
	lpMethod init = FindMethodByDescriptor(declaring, "<init>", desc.c_str(), true);
	return ClassArrayFromExceptionAttribute(env, init);
}

// ---------------------------------------------------------------------------
// Generic signatures and repeatable annotation expansion
// ---------------------------------------------------------------------------

namespace {

jobject SignatureString(JNIEnv *env, ConstantPool *pool, lpSignature signature)
{
	if (!pool || !signature)
	{
		return NULL;
	}
	std::string text = Utf8At(pool, signature->signature_index);
	if (text.empty())
	{
		return NULL;
	}
	return NewString(env, text);
}

lpMethod MemberMethod(JNIEnv *env, lpObject member, const char *fallback_name)
{
	lpClass declaring = MirrorClass(ObjectFieldRef(member, "declaringClass"));
	std::string name = ObjectFieldString(env, member, "name");
	std::string desc = ObjectFieldString(env, member, "descriptor");
	if (name.empty())
	{
		name = fallback_name ? fallback_name : "";
	}
	return FindMethodByDescriptor(declaring, name.c_str(), desc.c_str(), false);
}

} // namespace

JNIEXPORT jobject JNICALL Java_java_lang_Class_getSignature0(JNIEnv *env,
	jobject thiz)
{
	lpClass clazz = MirrorClass(thiz);
	if (!clazz)
	{
		return NULL;
	}
	return SignatureString(env, clazz->const_pool, clazz->signature_attr);
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Method_getSignature0(JNIEnv *env,
	jobject thiz)
{
	lpObject member = JniResolveObject(thiz);
	if (!member)
	{
		return NULL;
	}
	lpMethod method = MemberMethod(env, member, NULL);
	if (!method || !method->clazz)
	{
		return NULL;
	}
	return SignatureString(env, method->clazz->const_pool, method->signature_attr);
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Constructor_getSignature0(JNIEnv *env,
	jobject thiz)
{
	lpObject member = JniResolveObject(thiz);
	if (!member)
	{
		return NULL;
	}
	lpMethod method = MemberMethod(env, member, "<init>");
	if (!method || !method->clazz)
	{
		return NULL;
	}
	return SignatureString(env, method->clazz->const_pool, method->signature_attr);
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Field_getSignature0(JNIEnv *env,
	jobject thiz)
{
	lpObject member = JniResolveObject(thiz);
	if (!member)
	{
		return NULL;
	}
	lpClass declaring = MirrorClass(ObjectFieldRef(member, "declaringClass"));
	std::string name = ObjectFieldString(env, member, "name");
	std::string desc = ObjectFieldString(env, member, "descriptor");
	lpField field = JniFindField(declaring, name.c_str(), desc.c_str(), false);
	if (!field)
	{
		field = JniFindField(declaring, name.c_str(), desc.c_str(), true);
	}
	if (!field || !declaring)
	{
		return NULL;
	}
	return SignatureString(env, declaring->const_pool, field->signature_attr);
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_AnnotationSupport_expand0(
	JNIEnv *env, jclass unused, jobjectArray annotations, jobject annotationClass)
{
	(void)unused;
	return ExpandAnnotations(env, annotations, MirrorClass(annotationClass));
}

// ---------------------------------------------------------------------------
// java.lang.reflect.Array
// ---------------------------------------------------------------------------

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Array_newArray(
	JNIEnv *env, jclass unused, jobject componentType, jint length)
{
	(void)unused;
	if (length < 0)
	{
		jclass error = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/NegativeArraySizeException"));
		if (error)
		{
			env->ThrowNew(error, "negative array size");
		}
		return NULL;
	}
	lpClass component = MirrorClass(componentType);
	if (!component || !component->thisClass || !component->thisClass->GetName())
	{
		ThrowArrayIllegalArgument(env, "invalid component type");
		return NULL;
	}
	const char *name = component->thisClass->GetName();
	DATA_TYPE primitive_type = DT_VOID;
	char primitive_tag = 0;
	bool primitive = PrimitiveArrayType(name, primitive_type, primitive_tag);
	std::string array_name;
	if (primitive)
	{
		array_name = "[";
		array_name += primitive_tag;
	}
	else if (name[0] == '[')
	{
		array_name = "[";
		array_name += name;
	}
	else
	{
		if (strcmp(name, "void") == 0)
		{
			ThrowArrayIllegalArgument(env, "void component type");
			return NULL;
		}
		array_name = "[L";
		array_name += name;
		array_name += ";";
	}
	lpClass array_class = JniFindClass(env, array_name.c_str());
	if (!array_class || !VMContext::object_heap)
	{
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	bool created = primitive
		? VMContext::object_heap->NewArrayObject(length, array_class, ref)
		: VMContext::object_heap->NewObjectArrayObject(length, array_class, ref);
	if (!created)
	{
		return NULL;
	}
	jobject result = JniObject(ref);
	JniAddLocalRef(env, result);
	return result;
}

JNIEXPORT jint JNICALL Java_java_lang_reflect_Array_getLength(
	JNIEnv *env, jclass unused, jobject array)
{
	(void)unused;
	lpObject object = ResolveArray(env, array, 0, false);
	return object ? static_cast<jint>(object->array_length) : 0;
}

JNIEXPORT jobject JNICALL Java_java_lang_reflect_Array_get(
	JNIEnv *env, jclass unused, jobject array, jint index)
{
	(void)unused;
	lpObject object = ResolveArray(env, array, index, true);
	return object ? BoxArrayElement(env, object, index, DT_VOID) : NULL;
}

JNIEXPORT void JNICALL Java_java_lang_reflect_Array_set(
	JNIEnv *env, jclass unused, jobject array, jint index, jobject value)
{
	(void)unused;
	lpObject object = ResolveArray(env, array, index, true);
	if (object)
	{
		StoreArrayElement(env, object, index, value, DT_VOID);
	}
}

#define DARKS_ARRAY_GETTER(NAME, TYPE, FIELD) \
JNIEXPORT TYPE JNICALL Java_java_lang_reflect_Array_##NAME( \
	JNIEnv *env, jclass unused, jobject array, jint index) \
{ \
	(void)unused; \
	lpObject object = ResolveArray(env, array, index, true); \
	if (!object) \
	{ \
		return 0; \
	} \
	if (!ArrayElementMatches(object, FIELD)) \
	{ \
		ThrowArrayIllegalArgument(env, "array type mismatch"); \
		return 0; \
	} \
	return static_cast<TYPE *>(object->array_var)[index]; \
}

DARKS_ARRAY_GETTER(getBoolean, jboolean, DT_BOOL)
DARKS_ARRAY_GETTER(getByte, jbyte, DT_BYTE)
DARKS_ARRAY_GETTER(getChar, jchar, DT_CHAR)
DARKS_ARRAY_GETTER(getShort, jshort, DT_SHORT)
DARKS_ARRAY_GETTER(getInt, jint, DT_INT)
DARKS_ARRAY_GETTER(getLong, jlong, DT_LONG)
DARKS_ARRAY_GETTER(getFloat, jfloat, DT_FLOAT)
DARKS_ARRAY_GETTER(getDouble, jdouble, DT_DOUBLE)

#define DARKS_ARRAY_SETTER(NAME, TYPE, FIELD) \
JNIEXPORT void JNICALL Java_java_lang_reflect_Array_##NAME( \
	JNIEnv *env, jclass unused, jobject array, jint index, TYPE value) \
{ \
	(void)unused; \
	lpObject object = ResolveArray(env, array, index, true); \
	if (!object) \
	{ \
		return; \
	} \
	if (!ArrayElementMatches(object, FIELD)) \
	{ \
		ThrowArrayIllegalArgument(env, "array type mismatch"); \
		return; \
	} \
	static_cast<TYPE *>(object->array_var)[index] = value; \
}

DARKS_ARRAY_SETTER(setBoolean, jboolean, DT_BOOL)
DARKS_ARRAY_SETTER(setByte, jbyte, DT_BYTE)
DARKS_ARRAY_SETTER(setChar, jchar, DT_CHAR)
DARKS_ARRAY_SETTER(setShort, jshort, DT_SHORT)
DARKS_ARRAY_SETTER(setInt, jint, DT_INT)
DARKS_ARRAY_SETTER(setLong, jlong, DT_LONG)
DARKS_ARRAY_SETTER(setFloat, jfloat, DT_FLOAT)
DARKS_ARRAY_SETTER(setDouble, jdouble, DT_DOUBLE)

} // extern "C"

