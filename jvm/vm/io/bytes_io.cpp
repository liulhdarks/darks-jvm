#include "stdafx.h"
#include "bytes_io.h"

U1 BytesInputStream::ReadU1()
{
	if (!Vaildate(1))
	{
		return 0;
	}
	return bytes[cursor++];
}

U2 BytesInputStream::ReadU2()
{
	if (!Vaildate(2))
	{
		return 0;
	}
	U1 u1 = bytes[cursor++];
	U1 u2 = bytes[cursor++];
	return (u1 << 8) + (u2 << 0);
}

U4 BytesInputStream::ReadU4()
{
	if (!Vaildate(4))
	{
		return 0;
	}
	U1 u1 = bytes[cursor++];
	U1 u2 = bytes[cursor++];
	U1 u3 = bytes[cursor++];
	U1 u4 = bytes[cursor++];
	return (u1 << 24) + (u2 << 16) + (u3 << 8) + (u4 << 0);
}

int BytesInputStream::ReadInt()
{
	if (!Vaildate(4))
	{
		return 0;
	}
	U1 u1 = bytes[cursor++];
	U1 u2 = bytes[cursor++];
	U1 u3 = bytes[cursor++];
	U1 u4 = bytes[cursor++];
	return ((u1 & 0xFF) << 24) + ((u2 & 0xFF) << 16) + ((u3 & 0xFF) << 8) + ((u4 & 0xFF) << 0);
}

U8 BytesInputStream::ReadU8()
{
	if (!Vaildate(8))
	{
		return 0;
	}
	U1 u1 = bytes[cursor++];
	U1 u2 = bytes[cursor++];
	U1 u3 = bytes[cursor++];
	U1 u4 = bytes[cursor++];
	U1 u5 = bytes[cursor++];
	U1 u6 = bytes[cursor++];
	U1 u7 = bytes[cursor++];
	U1 u8 = bytes[cursor++];
	return (((long)u1 << 56) +
		((long)(u2 & 255) << 48) +
		((long)(u3 & 255) << 40) +
		((long)(u4 & 255) << 32) +
		((long)(u5 & 255) << 24) +
		((u6 & 255) << 16) +
		((u7 & 255) <<  8) +
		((u8 & 255) <<  0));
}

bool BytesInputStream::ReadBytes(int length, LPBYTE &result, bool reverse)
{
	if (!Vaildate(length))
	{
		return false;
	}
	result = new BYTE[length];
	if (!reverse)
	{
		for (int i = 0; i < length; i++)
		{
			result[i] = bytes[cursor++];
		}
	}
	else
	{
		for (int i = length - 1; i >= 0; i--)
		{
			result[i] = bytes[cursor++];
		}
	}
	return true;
}

bool BytesInputStream::ReadString(int length, LPSTR &result)
{
	if (length == 0)
	{
		result = new CHAR[1];
		result[0] = '\0';
		return true;
	}
	if (!Vaildate(length))
	{
		return false;
	}
	LPBYTE tmp = new BYTE[length + 1];
	for (int i = 0; i < length; i++)
	{
		tmp[i] = bytes[cursor++];
	}
	tmp[length] = '\0';
	result = (LPSTR) tmp;
	return true;
}