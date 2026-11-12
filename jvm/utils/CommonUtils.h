#pragma once

#include "common.h"

#include <string>
using namespace std;

class CommonUtils
{

public:

	static string &ReplaceStr(string &str, LPCSTR src, LPCSTR dest);

	// Returns a heap allocated, NUL terminated copy of the requested range.
	static char* SubString(const char *str, int start = 0, int count = 0);

	// UTF-8 -> UTF-16. "length" receives the number of 16-bit code units.
	static unsigned short* Utf8ToUnicode(LPCSTR utf8, int &length);

	// UTF-16 -> UTF-8 (kept under the historical name; the original only
	// produced GBK on Windows). "length" receives the byte count.
	static char* UnicodeToGBK(unsigned short* unicodes, int &length);
};
