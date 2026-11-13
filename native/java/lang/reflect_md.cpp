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

#include <cstring>
#include <string>
#include <vector>

namespace {

lpClass MirrorClass(jobject mirror)
{
	return reinterpret_cast<lpClass>(static_cast<intptr_t>(
		JniGetLongFieldByName(mirror, "vmClass")));
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

// Access checks are not caller-sensitive: a non-public member is usable only
// after setAccessible(true). Public members are always accessible.
bool MemberAccessAllowed(jobject member, int modifiers)
{
	if ((modifiers & ACF_PUBLIC) != 0)
	{
		return true;
	}
	return ReadAccessibleFlag(member);
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
		clazz = JniFindClass(env, name.c_str());
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
			lpClass clazz = JniFindClass(env, constant->GetName());
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
	VMContext::pending_exception = NULL;
	lpClass wrapper_class = JniFindClass(env,
		"java/lang/reflect/InvocationTargetException");
	if (!wrapper_class)
	{
		VMContext::pending_exception = target;
		return NULL;
	}
	LPOBJECT_REF wrapper = NULL;
	if (!VMContext::object_heap->NewObject(wrapper_class, wrapper))
	{
		VMContext::pending_exception = target;
		return NULL;
	}
	jobject wrapper_obj = JniObject(wrapper);
	JniAddLocalRef(env, wrapper_obj);
	JniSetObjectFieldByName(wrapper_obj, "target", JniObject(target));
	VMContext::pending_exception = wrapper;
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

bool ReadElementValue(JNIEnv *env, ConstantPool *pool, ByteReader &r, Variant &out);

jobject ReadAnnotation(JNIEnv *env, ConstantPool *pool, ByteReader &r)
{
	U2 type_index = r.U2();
	std::string descriptor = Utf8At(pool, type_index);
	if (descriptor.size() < 3 || descriptor[0] != 'L' ||
	    descriptor[descriptor.size() - 1] != ';')
	{
		return NULL;
	}
	std::string class_name = descriptor.substr(1, descriptor.size() - 2);
	lpClass annotation_class = JniFindClass(env, class_name.c_str());
	jobject annotation = NewHeapObject(env, annotation_class);
	if (!annotation)
	{
		return NULL;
	}
	lpObject object = JniResolveObject(annotation);
	U2 pair_count = r.U2();
	for (U2 i = 0; i < pair_count && r.ok; i++)
	{
		U2 name_index = r.U2();
		std::string element_name = Utf8At(pool, name_index);
		Variant value;
		if (!ReadElementValue(env, pool, r, value))
		{
			break;
		}
		lpFieldValue fv = new FieldValue();
		fv->field_value = value;
		object->AdoptFieldValue("ann:" + element_name, fv);
	}
	// Elements absent from the annotation bytes take their declared default
	// from the annotation type's AnnotationDefault attributes.
	for (U2 m = 0; annotation_class && m < annotation_class->methodCount; m++)
	{
		lpMethod annotation_method = annotation_class->methods ? annotation_class->methods[m] : NULL;
		if (!annotation_method || !annotation_method->methodName ||
		    !annotation_method->annotation_default ||
		    !annotation_method->annotation_default->bytes)
		{
			continue;
		}
		std::string element_name = annotation_method->methodName->GetName();
		if (object->HasFieldValue("ann:" + element_name))
		{
			continue;
		}
		ByteReader defaults(annotation_method->annotation_default->bytes,
		                    annotation_method->annotation_default->attr_length);
		Variant value;
		bool default_ok = ReadElementValue(env,
			annotation_class->const_pool, defaults, value);
		if (default_ok)
		{
			lpFieldValue fv = new FieldValue();
			fv->field_value = value;
			object->AdoptFieldValue("ann:" + element_name, fv);
		}
	}
	return annotation;
}

bool ReadElementValue(JNIEnv *env, ConstantPool *pool, ByteReader &r, Variant &out)
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
			std::string name;
			if (desc.size() >= 3 && desc[0] == 'L' && desc[desc.size() - 1] == ';')
			{
				name = desc.substr(1, desc.size() - 2);
			}
			else
			{
				name = desc;
			}
			lpClass target = JniFindClass(env, name.c_str());
			jobject mirror = target ? JniClassMirror(reinterpret_cast<jclass>(target)) : NULL;
			if (mirror)
			{
				JniAddLocalRef(env, mirror);
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
			lpClass enum_class = JniFindClass(env, enum_name.c_str());
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
			if (count == 0)
			{
				jobject empty = MakeObjectArray(env, "[Ljava/lang/Object;",
				                                std::vector<jobject>());
				out.heapPtr = JniRef(empty);
				return true;
			}
			int tag_pos = r.pos;
			char component_tag = static_cast<char>(r.data[tag_pos]);
			bool primitive = component_tag == 'B' || component_tag == 'C' ||
				component_tag == 'S' || component_tag == 'Z' ||
				component_tag == 'I' || component_tag == 'J' ||
				component_tag == 'F' || component_tag == 'D';
			if (primitive)
			{
				DATA_TYPE type = DT_INT;
				switch (component_tag)
				{
				case 'B': type = DT_BYTE; break;
				case 'C': type = DT_CHAR; break;
				case 'S': type = DT_SHORT; break;
				case 'Z': type = DT_BOOL; break;
				case 'I': type = DT_INT; break;
				case 'J': type = DT_LONG; break;
				case 'F': type = DT_FLOAT; break;
				case 'D': type = DT_DOUBLE; break;
				}
				char array_name[4] = {'[', 0, 0, 0};
				switch (type)
				{
				case DT_BYTE: array_name[1] = 'B'; break;
				case DT_CHAR: array_name[1] = 'C'; break;
				case DT_SHORT: array_name[1] = 'S'; break;
				case DT_BOOL: array_name[1] = 'Z'; break;
				case DT_INT: array_name[1] = 'I'; break;
				case DT_LONG: array_name[1] = 'J'; break;
				case DT_FLOAT: array_name[1] = 'F'; break;
				case DT_DOUBLE: array_name[1] = 'D'; break;
				default: array_name[1] = 'I'; break;
				}
				lpClass array_class = JniFindClass(env, array_name);
				LPOBJECT_REF ref = NULL;
				if (!VMContext::object_heap->NewArrayObject(count, array_class, ref))
				{
					return false;
				}
				jobject array = JniObject(ref);
				JniAddLocalRef(env, array);
				lpObject array_obj = NULL;
				VMContext::object_heap->GetObject(ref, array_obj);
				for (U2 i = 0; i < count && r.ok; i++)
				{
					Variant element;
					if (!ReadElementValue(env, pool, r, element))
					{
						break;
					}
					if (array_obj && array_obj->array_var)
					{
						switch (type)
						{
						case DT_BYTE:
							static_cast<jbyte *>(array_obj->array_var)[i] = element.bValue; break;
						case DT_CHAR:
							static_cast<jchar *>(array_obj->array_var)[i] = element.cValue; break;
						case DT_SHORT:
							static_cast<jshort *>(array_obj->array_var)[i] = element.sValue; break;
						case DT_BOOL:
							static_cast<jboolean *>(array_obj->array_var)[i] = element.blValue; break;
						case DT_INT:
							static_cast<jint *>(array_obj->array_var)[i] = element.iValue; break;
						case DT_LONG:
							static_cast<jlong *>(array_obj->array_var)[i] = element.lValue; break;
						case DT_FLOAT:
							static_cast<jfloat *>(array_obj->array_var)[i] = element.fValue; break;
						case DT_DOUBLE:
							static_cast<jdouble *>(array_obj->array_var)[i] = element.dValue; break;
						default:
							break;
						}
					}
				}
				out.heapPtr = JniRef(array);
				return true;
			}
			std::vector<jobject> elements;
			for (U2 i = 0; i < count && r.ok; i++)
			{
				Variant element;
				if (!ReadElementValue(env, pool, r, element))
				{
					break;
				}
				elements.push_back(JniObject(element.heapPtr));
			}
			jobject array = MakeObjectArray(env, "[Ljava/lang/Object;", elements);
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
	for (lpClass c = clazz; c; c = c->super_class)
	{
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
	if (!MemberAccessAllowed(thiz, target->accFlag))
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
	jvalue result = JniInvokeMethod(env, receiver,
		reinterpret_cast<jclass>(declaring), reinterpret_cast<jmethodID>(target),
		values.empty() ? NULL : &values[0], target->isStatic(), false);
	if (VMContext::pending_exception)
	{
		return WrapInvocationTarget(env, VMContext::pending_exception);
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
	if (!MemberAccessAllowed(thiz, field->accFlag))
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
	if (!MemberAccessAllowed(thiz, field->accFlag))
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
		fv->field_value = value;
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
	if (!MemberAccessAllowed(thiz, init->accFlag))
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
	if (VMContext::pending_exception)
	{
		return WrapInvocationTarget(env, VMContext::pending_exception);
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
	return MirrorForDescriptor(env, name + 1,
		static_cast<int>(strlen(name + 1)));
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

} // extern "C"
