#pragma once

#include "common.h"
#include "platform/platform.h"

#include <cstdarg>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>

// Debug output is routed through the platform layer: it is a no-op unless
// DARKS_JVM_DEBUG is enabled, so normal runs stay quiet on every platform.
class DebugUtils
{
public:

	static void trace(LPCSTR format, ...)
	{
		if (!dk::DebugEnabled())
		{
			return;
		}
		char tmp[1024];
		va_list argptr;
		va_start(argptr, format);
		vsnprintf(tmp, sizeof(tmp), format, argptr);
		va_end(argptr);
		dk::DebugPrint(tmp);
	}

	static void print(int num, ...)
	{
		if (!dk::DebugEnabled())
		{
			return;
		}
		va_list argptr;
		va_start(argptr, num);

		std::string line;
		for (int i = 0; i < num; i++)
		{
			LPCSTR s = va_arg(argptr, LPCSTR);
			if (s)
			{
				line += s;
			}
		}
		line += "\n";
		dk::DebugPrint(line.c_str());
		va_end(argptr);
	}

	static void print(LPCSTR msg, bool newline = true)
	{
		if (!dk::DebugEnabled())
		{
			return;
		}
		dk::DebugPrint(msg ? msg : "");
		if (newline)
		{
			dk::DebugPrint("\n");
		}
	}

	static void print(LPBYTE msg, int len)
	{
		// Hex dumps are only useful interactively; keep them disabled.
		(void)msg;
		(void)len;
	}
};
