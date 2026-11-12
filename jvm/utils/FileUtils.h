#pragma once

#include "common.h"
#include "platform/platform.h"

#include <cstdio>
#include <vector>

class FileUtils
{

public:

	// Opens the file in binary read-only mode. The original used "rb+", which
	// fails for read-only files (e.g. installed class files).
	static bool ReadFile(LPCSTR filename, LPBYTE &bytes, int &length)
	{
		bytes = NULL;
		length = 0;
		std::vector<uint8_t> data;
		if (!dk::ReadFileBytes(std::string(filename ? filename : ""), data))
		{
			return false;
		}
		length = static_cast<int>(data.size());
		bytes = new BYTE[data.empty() ? 1 : data.size()];
		if (!data.empty())
		{
			memcpy(bytes, data.data(), data.size());
		}
		return true;
	}

	static int GetFileLen(FILE *fp)
	{
		long flag = ftell(fp);
		fseek(fp, 0, SEEK_END);
		long len = ftell(fp);
		fseek(fp, flag, SEEK_SET);
		return static_cast<int>(len);
	}
};
