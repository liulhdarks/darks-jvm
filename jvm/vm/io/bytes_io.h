#pragma once;

#include "common.h"
#include "vm/types.h"

class JVM_API BytesInputStream
{
private:
	LPBYTE bytes;
	int length;
	int cursor;

public:
	BytesInputStream(LPBYTE bytes, int length)
	{
		this->bytes = bytes;
		this->length = length;
		cursor = 0;
	}

	void Reset()
	{
		cursor = 0;
	}

	void SetCursor(int cursor)
	{
		this->cursor = cursor;
	}

	bool Vaildate(int offset)
	{
		int index = cursor + offset;
		if (bytes && index >=0 && index <= length)
		{
			return true;
		}
		cout<<"Bytes stream is invalid"<<endl;
		return false;
	}

	void Close()
	{
		if (bytes)
		{
			delete[] bytes;
			bytes = NULL;
			cursor = 0;
		}
	}

	U1 ReadU1();

	U2 ReadU2();

	U4 ReadU4();

	int ReadInt();

	U8 ReadU8();

	inline bool ReadBytes(int length, LPBYTE &result, bool reverse = false);

	bool ReadString(int length, LPSTR &result);

};