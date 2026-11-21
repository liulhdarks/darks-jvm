#include "stdafx.h"
#include "proxy.h"
#include "jni/jni_impl.h"
#include "vm/executor/objectheap.h"
#include "vm/executor/javaexception.h"

#include <cstring>
#include <string>
#include <vector>

namespace {

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

LPOBJECT_REF NewStringRef(const std::string &text)
{
	if (!VMContext::object_heap || text.empty())
	{
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewStringObject(const_cast<char *>(text.c_str()),
	                                            static_cast<int>(text.size()), ref))
	{
		return NULL;
	}
	return ref;
}

LPOBJECT_REF NewObjectArray(const char *array_class_name, int count)
{
	lpClass array_class = JniFindClass(NULL, array_class_name);
	if (!array_class || !VMContext::object_heap)
	{
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewObjectArrayObject(count, array_class, ref))
	{
		return NULL;
	}
	return ref;
}

bool UnboxValue(jobject boxed, DATA_TYPE type, jvalue &out)
{
	lpObject object = JniResolveObject(boxed);
	if (!object)
	{
		return false;
	}
	lpFieldValue fv = FindNamedField(object, "value");
	if (!fv)
	{
		return false;
	}
	memset(&out, 0, sizeof(out));
	switch (type)
	{
	case DT_BOOL: out.z = fv->field_value.blValue ? JNI_TRUE : JNI_FALSE; break;
	case DT_BYTE: out.b = fv->field_value.bValue; break;
	case DT_CHAR: out.c = fv->field_value.cValue; break;
	case DT_SHORT: out.s = fv->field_value.sValue; break;
	case DT_INT: out.i = fv->field_value.iValue; break;
	case DT_LONG: out.j = fv->field_value.lValue; break;
	case DT_FLOAT: out.f = fv->field_value.fValue; break;
	case DT_DOUBLE: out.d = fv->field_value.dValue; break;
	default: return false;
	}
	return true;
}

std::map<std::string, lpClass> &ProxyClassCache()
{
	static std::map<std::string, lpClass> *cache = new std::map<std::string, lpClass>();
	return *cache;
}

int &ProxyClassCounter()
{
	static int counter = 0;
	return counter;
}

} // namespace

jobject DarksVM_BoxValue(DATA_TYPE type, const jvalue &value)
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
	lpClass clazz = JniFindClass(NULL, wrapper);
	if (!clazz || !VMContext::object_heap)
	{
		return NULL;
	}
	LPOBJECT_REF ref = NULL;
	if (!VMContext::object_heap->NewObject(clazz, ref))
	{
		return NULL;
	}
	jobject boxed = JniObject(ref);
	lpObject object = NULL;
	if (!VMContext::object_heap->GetObject(ref, object) || !object)
	{
		return NULL;
	}
	lpFieldValue fv = FindNamedField(object, "value");
	if (!fv)
	{
		return NULL;
	}
	switch (type)
	{
	case DT_BOOL: fv->field_value.blValue = value.z ? 1 : 0; break;
	case DT_BYTE: fv->field_value.bValue = value.b; break;
	case DT_CHAR: fv->field_value.cValue = value.c; break;
	case DT_SHORT: fv->field_value.sValue = value.s; break;
	case DT_INT: fv->field_value.iValue = value.i; break;
	case DT_LONG: fv->field_value.lValue = value.j; break;
	case DT_FLOAT: fv->field_value.fValue = value.f; break;
	case DT_DOUBLE: fv->field_value.dValue = value.d; break;
	default: break;
	}
	return boxed;
}

lpClass DarksVM_FindProxyClass(const std::vector<std::string> &interfaces,
                               lpClassLoader loader)
{
	std::string key;
	for (size_t i = 0; i < interfaces.size(); i++)
	{
		if (i)
		{
			key += "|";
		}
		key += interfaces[i];
	}
	std::map<std::string, lpClass> &cache = ProxyClassCache();
	std::map<std::string, lpClass>::iterator it = cache.find(key);
	if (it != cache.end())
	{
		return it->second;
	}
	lpClass synth = new Class(CT_OBJECT);
	char name[64];
	snprintf(name, sizeof(name), "darks/Proxy$%d", ProxyClassCounter()++);
	synth->thisClass = new ClassConstant();
	synth->thisClass->name = new CHAR[strlen(name) + 1];
	memcpy(synth->thisClass->name, name, strlen(name) + 1);
	synth->superClass = new ClassConstant();
	const char *object_name = "java/lang/Object";
	synth->superClass->name = new CHAR[strlen(object_name) + 1];
	memcpy(synth->superClass->name, object_name, strlen(object_name) + 1);
	synth->classloader = loader;
	lpClass object_class = NULL;
	if (loader)
	{
		loader->FindClass("java/lang/Object", object_class);
	}
	synth->super_class = object_class;
	synth->interfaceCount = static_cast<U2>(interfaces.size());
	synth->interfaces = new lpClassConstant[interfaces.size()];
	for (size_t i = 0; i < interfaces.size(); i++)
	{
		synth->interfaces[i] = new ClassConstant();
		synth->interfaces[i]->name = new CHAR[interfaces[i].size() + 1];
		memcpy(synth->interfaces[i]->name, interfaces[i].c_str(),
		       interfaces[i].size() + 1);
	}
	// The synthetic constants are owned by the constant pool so destroying the
	// proxy Class releases them; otherwise every annotation proxy leaks one
	// ClassConstant per interface.
	synth->const_pool->init(static_cast<int>(2 + interfaces.size()));
	synth->const_pool->AddConstant(synth->thisClass);
	synth->const_pool->AddConstant(synth->superClass);
	for (size_t i = 0; i < interfaces.size(); i++)
	{
		synth->const_pool->AddConstant(synth->interfaces[i]);
	}
	// ACC_PUBLIC | ACC_FINAL
	synth->accFlag = 0x0001 | 0x0010;
	synth->major_version = 52;
	synth->initialized_state = true;
	if (loader)
	{
		loader->StoreClass(synth);
	}
	cache[key] = synth;
	return synth;
}

bool DarksVM_StoreProxyHandler(lpObject object, LPOBJECT_REF handler)
{
	if (!object)
	{
		return false;
	}
	lpFieldValue fv = new FieldValue();
	fv->field_value.heapPtr = handler;
	object->AdoptFieldValue("$proxy:handler", fv);
	return true;
}

bool DarksVM_LoadProxyHandler(lpObject object, LPOBJECT_REF &handler)
{
	if (!object)
	{
		return false;
	}
	FIELD_MAP::iterator it = object->varants.find("$proxy:handler");
	if (it == object->varants.end() || !it->second || !it->second->field_value.heapPtr)
	{
		return false;
	}
	handler = it->second->field_value.heapPtr;
	return true;
}

jobject DarksVM_NewMethodMirror(lpMethod method)
{
	if (!method || !method->clazz || !VMContext::object_heap)
	{
		return NULL;
	}
	GCNativeScope no_gc(VMContext::object_heap);
	lpClass mirror_class = JniFindClass(NULL, "java/lang/reflect/Method");
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
	jobject declaring = JniClassMirror(reinterpret_cast<jclass>(method->clazz));
	if (declaring)
	{
		JniSetObjectFieldByName(mirror, "declaringClass", declaring);
	}
	if (method->methodName)
	{
		LPOBJECT_REF name_ref = NewStringRef(method->methodName->GetName());
		if (name_ref)
		{
			JniSetObjectFieldByName(mirror, "name", JniObject(name_ref));
		}
	}
	if (method->methodDesc)
	{
		LPOBJECT_REF desc_ref = NewStringRef(method->methodDesc->GetName());
		if (desc_ref)
		{
			JniSetObjectFieldByName(mirror, "descriptor", JniObject(desc_ref));
		}
	}
	JniSetLongFieldByName(mirror, "modifiers", method->accFlag);
	return mirror;
}

jobject DarksVM_CreateAnnotationProxy(JNIEnv *env, lpClass annotationClass,
                                      const std::vector<std::string> &names,
                                      const std::vector<jobject> &values)
{
	if (!annotationClass || !annotationClass->thisClass || !VMContext::object_heap)
	{
		return NULL;
	}
	GCNativeScope no_gc(VMContext::object_heap);
	std::vector<std::string> interfaces;
	interfaces.push_back(annotationClass->thisClass->GetName());
	if (interfaces[0] != "java/lang/annotation/Annotation")
	{
		interfaces.push_back("java/lang/annotation/Annotation");
	}
	lpClass proxy_class = DarksVM_FindProxyClass(interfaces,
		VMContext::bootstrap_classloader);
	lpClass handler_class = JniFindClass(env,
		"java/lang/reflect/AnnotationInvocationHandler");
	if (!proxy_class || !handler_class)
	{
		return NULL;
	}
	LPOBJECT_REF proxy_ref = NULL;
	if (!VMContext::object_heap->NewObject(proxy_class, proxy_ref))
	{
		return NULL;
	}
	LPOBJECT_REF handler_ref = NULL;
	if (!VMContext::object_heap->NewObject(handler_class, handler_ref))
	{
		return NULL;
	}
	jobject handler = JniObject(handler_ref);
	jobject type_mirror = JniClassMirror(reinterpret_cast<jclass>(annotationClass));
	if (type_mirror)
	{
		JniSetObjectFieldByName(handler, "annotationType", type_mirror);
	}
	LPOBJECT_REF names_ref = NewObjectArray("[Ljava/lang/String;",
		static_cast<int>(names.size()));
	if (names_ref)
	{
		jobject array = JniObject(names_ref);
		lpObject array_obj = NULL;
		if (VMContext::object_heap->GetObject(names_ref, array_obj) &&
		    array_obj && array_obj->array_var)
		{
			jobject *slots = static_cast<jobject *>(array_obj->array_var);
			for (size_t i = 0; i < names.size(); i++)
			{
				LPOBJECT_REF name_ref = NewStringRef(names[i]);
				slots[i] = JniObject(name_ref);
			}
		}
		JniSetObjectFieldByName(handler, "names", array);
	}
	LPOBJECT_REF values_ref = NewObjectArray("[Ljava/lang/Object;",
		static_cast<int>(values.size()));
	if (values_ref)
	{
		jobject array = JniObject(values_ref);
		lpObject array_obj = NULL;
		if (VMContext::object_heap->GetObject(values_ref, array_obj) &&
		    array_obj && array_obj->array_var)
		{
			jobject *slots = static_cast<jobject *>(array_obj->array_var);
			for (size_t i = 0; i < values.size(); i++)
			{
				slots[i] = values[i];
			}
		}
		JniSetObjectFieldByName(handler, "values", array);
	}
	DarksVM_StoreProxyHandler(JniResolveObject(JniObject(proxy_ref)), handler_ref);
	jobject proxy = JniObject(proxy_ref);
	JniAddLocalRef(env, proxy);
	return proxy;
}

lpClass DarksVM_ProxyAnnotationClass(jobject annotation)
{
	lpObject object = JniResolveObject(annotation);
	if (!object)
	{
		return NULL;
	}
	lpFieldValue fv = FindNamedField(object, "annotationType");
	lpObject handler = NULL;
	LPOBJECT_REF handler_ref = NULL;
	if (!DarksVM_LoadProxyHandler(object, handler_ref) ||
	    !VMContext::object_heap->GetObject(handler_ref, handler))
	{
		return object->obj_class;
	}
	fv = FindNamedField(handler, "annotationType");
	if (!fv || !fv->field_value.heapPtr)
	{
		return NULL;
	}
	lpObject mirror = NULL;
	if (!VMContext::object_heap->GetObject(fv->field_value.heapPtr, mirror) || !mirror)
	{
		return NULL;
	}
	lpFieldValue vm_class = FindNamedField(mirror, "vmClass");
	if (!vm_class)
	{
		return NULL;
	}
	return reinterpret_cast<lpClass>(static_cast<intptr_t>(vm_class->field_value.lValue));
}

jobject DarksVM_ProxyAnnotationValue(jobject annotation, const char *name)
{
	lpObject object = JniResolveObject(annotation);
	LPOBJECT_REF handler_ref = NULL;
	lpObject handler = NULL;
	if (!object || !name || !VMContext::object_heap ||
	    !DarksVM_LoadProxyHandler(object, handler_ref) ||
	    !VMContext::object_heap->GetObject(handler_ref, handler))
	{
		return NULL;
	}
	lpFieldValue names_value = FindNamedField(handler, "names");
	lpFieldValue values_value = FindNamedField(handler, "values");
	if (!names_value || !values_value ||
	    !names_value->field_value.heapPtr || !values_value->field_value.heapPtr)
	{
		return NULL;
	}
	lpObject names = NULL;
	lpObject values = NULL;
	if (!VMContext::object_heap->GetObject(names_value->field_value.heapPtr, names) ||
	    !VMContext::object_heap->GetObject(values_value->field_value.heapPtr, values) ||
	    !names || !values || !names->array_var || !values->array_var)
	{
		return NULL;
	}
	jobject *name_slots = static_cast<jobject *>(names->array_var);
	jobject *value_slots = static_cast<jobject *>(values->array_var);
	int count = names->array_length < values->array_length
		? names->array_length : values->array_length;
	for (int i = 0; i < count; i++)
	{
		std::string text;
		if (JavaStringToUtf8(JniRef(name_slots[i]), text) && text == name)
		{
			return value_slots[i];
		}
	}
	return NULL;
}

bool DarksVM_InvokeProxyHandler(JNIEnv *env, jobject proxy, lpMethod target,
                                const std::vector<jvalue> &args,
                                DATA_TYPE returnType, jvalue &out)
{
	memset(&out, 0, sizeof(out));
	lpObject proxy_obj = JniResolveObject(proxy);
	LPOBJECT_REF handler_ref = NULL;
	if (!proxy_obj || !target || !VMContext::object_heap ||
	    !DarksVM_LoadProxyHandler(proxy_obj, handler_ref))
	{
		return false;
	}
	lpObject handler = NULL;
	if (!VMContext::object_heap->GetObject(handler_ref, handler) || !handler)
	{
		return false;
	}
	lpClass handler_class = handler->obj_class;
	const char *invoke_desc =
		"(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;";
	lpMethod invoke = JniFindMethod(handler_class, "invoke", invoke_desc, false);
	if (!invoke)
	{
		VMContext::pending_exception = CreateJavaException(
			"java/lang/AbstractMethodError",
			"proxy invocation handler has no invoke method");
		return false;
	}
	LPOBJECT_REF method_ref = NULL;
	LPOBJECT_REF args_ref = NULL;
	GCRootSet roots(VMContext::object_heap);
	roots.Add(&method_ref);
	roots.Add(&args_ref);
	{
		GCNativeScope no_gc(VMContext::object_heap);
		jobject method = DarksVM_NewMethodMirror(target);
		method_ref = JniRef(method);
		lpClass object_array = JniFindClass(env, "[Ljava/lang/Object;");
		if (!method_ref || !object_array ||
		    !VMContext::object_heap->NewObjectArrayObject(
				static_cast<int>(args.size()), object_array, args_ref))
		{
			return false;
		}
		lpObject array = NULL;
		if (!VMContext::object_heap->GetObject(args_ref, array) ||
		    !array || !array->array_var)
		{
			return false;
		}
		jobject *slots = static_cast<jobject *>(array->array_var);
		std::vector<TypeDesc> types;
		TypeDesc result_desc;
		if (!parse_method_desc_type(target->methodDesc->GetName(), types, result_desc))
		{
			return false;
		}
		for (size_t i = 0; i < args.size() && i < types.size(); i++)
		{
			DATA_TYPE type = types[i].check_array ? DT_ARRAY : types[i].data_type;
			if (type == DT_REF || type == DT_ARRAY)
			{
				slots[i] = args[i].l;
			}
			else
			{
				slots[i] = DarksVM_BoxValue(type, args[i]);
			}
		}
	}
	jobject handler_obj = JniObject(handler_ref);
	jvalue values[4];
	memset(values, 0, sizeof(values));
	values[0].l = proxy;
	values[1].l = JniObject(method_ref);
	values[2].l = JniObject(args_ref);
	jvalue result = JniInvokeMethod(env, handler_obj, reinterpret_cast<jclass>(handler_class),
		reinterpret_cast<jmethodID>(invoke), values, false, true);
	if (VMContext::pending_exception)
	{
		return false;
	}
	if (returnType == DT_VOID)
	{
		return true;
	}
	if (returnType == DT_REF || returnType == DT_ARRAY)
	{
		out.l = result.l;
		return true;
	}
	return UnboxValue(result.l, returnType, out);
}
