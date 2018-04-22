#pragma once

#include <windows.h>
#include "common.h"
#include <wchar.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
class DebugUtils
{
public:

	static void trace(LPSTR format, ...)
	{
		va_list argptr;
		va_start(argptr, format);

		char tmp[1024];
		sprintf(tmp, format, argptr);
		OutputDebugString(tmp);
		printf(format, argptr);
		va_end(argptr);

	}


};