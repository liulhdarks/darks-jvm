#include "stdafx.h"
#include "CommonUtils.h"
#include "platform/platform.h"

#include <string>
#include <vector>

string &CommonUtils::ReplaceStr(string &str, LPCSTR src, LPCSTR dest)
{
	if (!src || !*src)
	{
		return str;
	}
	string source(src);
	string target(dest ? dest : "");
	string::size_type pos = 0;
	while ((pos = str.find(source, pos)) != string::npos)
	{
		str.replace(pos, source.size(), target);
		pos += target.size();
	}
	return str;
}

char* CommonUtils::SubString(const char *str, int start, int count)
{
	if (!str)
	{
		return NULL;
	}
	int len = static_cast<int>(strlen(str));
	if (start < 0 || start > len)
	{
		start = len;
	}
	if (count <= 0)
	{
		count = len - start;
	}
	if (start + count > len)
	{
		count = len - start;
	}
	char *dst = new char[count + 1];
	if (count > 0)
	{
		memcpy(dst, str + start, count);
	}
	dst[count] = '\0';
	return dst;
}

unsigned short* CommonUtils::Utf8ToUnicode(LPCSTR utf8, int &length)
{
	length = 0;
	if (!utf8)
	{
		return NULL;
	}
	std::vector<uint16_t> units = dk::Utf8ToUtf16(utf8);
	length = static_cast<int>(units.size());
	unsigned short *result = new unsigned short[units.size() + 1];
	for (size_t i = 0; i < units.size(); i++)
	{
		result[i] = units[i];
	}
	result[units.size()] = 0;
	return result;
}

char* CommonUtils::UnicodeToGBK(unsigned short* unicodes, int &length)
{
	length = 0;
	if (!unicodes)
	{
		return NULL;
	}
	size_t count = 0;
	while (unicodes[count] != 0)
	{
		count++;
	}
	std::vector<uint16_t> units(unicodes, unicodes + count);
	std::string utf8 = dk::Utf16ToUtf8(units);
	length = static_cast<int>(utf8.size());
	char *result = new char[utf8.size() + 1];
	memcpy(result, utf8.c_str(), utf8.size() + 1);
	return result;
}
