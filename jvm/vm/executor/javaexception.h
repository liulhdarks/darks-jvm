#pragma once

#include "vm/types.h"

#include <string>

// Runtime helpers for the Java exception system. They live in the VM core (not
// in JNI) so the interpreter and JNI can both use them.

// Creates an instance of className (internal name, e.g.
// "java/lang/NullPointerException") and stores message in Throwable's
// detailMessage field. Returns NULL when the class cannot be loaded.
LPOBJECT_REF CreateJavaException(const char *class_name, const char *message);

// True when an object of class src can be assigned to a variable of class
// target (class hierarchy plus the direct interface list).
bool ClassIsAssignable(lpClass target, lpClass src);

// True when obj is an instance of target (NULL obj means null -> false).
bool IsInstanceOfClass(lpObject obj, lpClass target);

// Returns the object's class name (internal form) or "<unknown>".
const char *ObjectClassName(lpObject obj);

// Extracts a java.lang.String's bytes as UTF-8. Returns false when the object
// is not a String. The result is written to out.
bool JavaStringToUtf8(LPOBJECT_REF ref, std::string &out);

// Prints "Exception in thread \"main\" ..." plus a short trace to stderr.
void PrintUncaughtException(LPOBJECT_REF exception);
