#pragma once

#include "vm/types.h"
#include "vm/executor/descriptor.h"
#include "jvm/jni/jni.h"

#include <string>
#include <vector>

// Synthetic proxy classes: ordinary VM Class objects that implement one or
// more interfaces and carry a "$proxy:handler" entry in the instance varant
// map.  The interpreter dispatches every interface method of such an object
// to the InvocationHandler stored there (see StackFrame::InvokeProxy).

JVM_API lpClass DarksVM_FindProxyClass(const std::vector<std::string> &interfaces,
                                       lpClassLoader loader);

JVM_API bool DarksVM_StoreProxyHandler(lpObject object, LPOBJECT_REF handler);
JVM_API bool DarksVM_LoadProxyHandler(lpObject object, LPOBJECT_REF &handler);

// Builds a java.lang.reflect.Method mirror for a VM method.
JVM_API jobject DarksVM_NewMethodMirror(lpMethod method);

// Boxes a primitive value into its wrapper class (java.lang.Integer, ...).
JVM_API jobject DarksVM_BoxValue(DATA_TYPE type, const jvalue &value);

// Creates an annotation proxy: a synthetic object implementing the annotation
// interface whose AnnotationInvocationHandler answers element methods.
JVM_API jobject DarksVM_CreateAnnotationProxy(JNIEnv *env, lpClass annotationClass,
                                              const std::vector<std::string> &names,
                                              const std::vector<jobject> &values);

JVM_API lpClass DarksVM_ProxyAnnotationClass(jobject annotation);
JVM_API jobject DarksVM_ProxyAnnotationValue(jobject annotation, const char *name);

// Invokes handler.invoke(proxy, method, args) and converts the result to the
// declared return type.  Used by reflection on proxy objects.
JVM_API bool DarksVM_InvokeProxyHandler(JNIEnv *env, jobject proxy,
                                        lpMethod target,
                                        const std::vector<jvalue> &args,
                                        DATA_TYPE returnType, jvalue &out);
