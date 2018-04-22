#pragma once

#include "common.h"
#include "vm/types.h"
#include "vm/executor/jclass.h"

C_JVM_API bool parse_class_bytes(LPBYTE bytes, int length, lpClass &clazz);