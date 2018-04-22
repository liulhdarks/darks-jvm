#pragma once

#include <string>
using namespace std;

class CommonUtils
{

public:

	static string &ReplaceStr(string &str, LPSTR src, LPSTR dest);

	static char* SubString(const char *str, int start = 0, int count = 0);

	static unsigned short* Utf8ToUnicode(LPCSTR utf8, int &length);

	static char* UnicodeToGBK(unsigned short* unicodes, int &length);
};