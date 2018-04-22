#pragma once

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
		//printf(format, argptr);
		va_end(argptr);

	}

	static void print(int num, ...)
	{
		va_list argptr;
		va_start(argptr, num);

		for (int i = 0; i < num; i++)
		{
			LPSTR s = va_arg(argptr, LPSTR);
			OutputDebugString(s);
			//cout<<s;
		}
		OutputDebugString("\n");
		//cout<<endl;
		va_end(argptr);

	}

	static void print(LPCSTR msg, bool newline = true)
	{
		OutputDebugString(msg);
		//cout<<msg;
		if (newline)
		{
			OutputDebugString("\n");
			//cout<<endl;
		}
	}


	static void print(LPBYTE msg, int len)
	{
		/*for (int i = 0; i < len; i++)
		{
			printf("%x ", msg[i]);
		}
		printf("\n");*/
	}

};