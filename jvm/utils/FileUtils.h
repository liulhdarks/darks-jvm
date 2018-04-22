#pragma once

#include "common.h"
#include <stdio.h>

class FileUtils
{

public:

	static bool ReadFile(LPSTR filename, LPBYTE &bytes, int &length)
	{
		FILE *fp = fopen(filename, "rb+");
		if (!fp)
		{
			return false;
		}
		length = GetFileLen(fp);
		bytes = new BYTE[length];
		fread(bytes, sizeof(BYTE), length, fp);
		fclose(fp);
		return true;
	}

	static int GetFileLen(FILE *fp)
	{
		int flag = ftell(fp);
		fseek(fp, 0, SEEK_END);
		int len = ftell(fp);
		fseek(fp, 0, flag);
		return len;
	}
};