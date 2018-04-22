#include "stdafx.h"
#include "CommonUtils.h"


string &CommonUtils::ReplaceStr(string &str, LPSTR src, LPSTR dest)
{
	string::size_type pos = 0;
	while ( (pos = str.find(src, pos)) != string::npos ) {
		str.replace( pos, strlen(src), dest);
		pos++;
	}
	return str;
}

char* CommonUtils::SubString(const char *str, int start, int count)
{
	int len = strlen(str);
	if (count == 0)
	{
		count = len - start;
	}
	LPSTR dst = new char[count];
	int end = start + count;
	int index = 0;
	for (int i = start; i < end; i++)
	{
		dst[index++] = str[i];
	}
	return dst;
}

unsigned short* CommonUtils::Utf8ToUnicode(LPCSTR utf8, int &length)
{
	length = MultiByteToWideChar(CP_UTF8, 0, utf8, -1, NULL, 0);  
	unsigned short *wszGBK = new unsigned short[length + 1];  
	memset(wszGBK, 0, length * 2 + 2);  
	MultiByteToWideChar(CP_UTF8, 0, utf8, -1, (LPWSTR)wszGBK, length);  
	return wszGBK;
}

char* CommonUtils::UnicodeToGBK(unsigned short* unicodes, int &length)
{
	length = WideCharToMultiByte(CP_ACP, 0, (LPCWSTR)unicodes, -1, NULL, 0, NULL, NULL);  
	char *szGBK = new char[length + 1];  
	memset(szGBK, 0, length + 1);  
	WideCharToMultiByte(CP_ACP,0, (LPCWSTR)unicodes, -1, szGBK, length, NULL, NULL);  
	return szGBK;
}