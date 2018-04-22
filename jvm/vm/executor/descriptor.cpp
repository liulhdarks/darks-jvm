#include "stdafx.h"
#include "descriptor.h"

bool parse_method_desc_type(LPCSTR desc, vector<TypeDesc> &type_desc, 
					TypeDesc &result_desc)
{
	int len = strlen(desc);
	int index = 0;
	while (index < len)
	{
		char ch = desc[index++];
		if (ch == '(' || ch == ' ')continue;
		if (ch == ')')
		{
			index = parse_desc_type(desc, result_desc, len, index);
			break;
		}
		index--;
		TypeDesc tdesc;
		index = parse_desc_type(desc, tdesc, len, index);
		type_desc.push_back(tdesc);
	}
	return true;
}

int parse_desc_type(LPCSTR desc, TypeDesc &type_desc, int len, int offset)
{
	type_desc.reset();
	int index = offset;
	while (index < len)
	{
		char ch = desc[index++];
		if (ch == '(' || ch == ' ')continue;
		if (ch == ')')
		{
			break;
		}
		type_desc.data_type = parse_data_type(ch);
		if (type_desc.data_type == DT_ARRAY)
		{
			type_desc.check_array = true;
			type_desc.array_level++;
		}
		else if (type_desc.data_type == DT_REF)
		{
			type_desc.class_name = new char[256];
			int i = 0;
			while (index < len)
			{
				ch = desc[index++];
				if (ch == ';' || ch == ')')
				{
					break;
				}
				type_desc.class_name[i++] = ch;
			}
			type_desc.class_name[i] = '\0';
			break;
		}
		else
		{
			break;
		}
	}
	return index;
}

DATA_TYPE parse_data_type(char ch_type)
{
	switch(ch_type)
	{
	case 'B':
		return DT_BYTE;
	case 'C':
		return DT_CHAR;
	case 'D':
		return DT_CHAR;
	case 'F':
		return DT_FLOAT;
	case 'I':
		return DT_INT;
	case 'J':
		return DT_LONG;
	case 'S':
		return DT_SHORT;
	case 'Z':
		return DT_BOOL;
	case 'V':
		return DT_VOID;
	case 'L':
		return DT_REF;
	case '[':
		return DT_ARRAY;
	default:
		return DT_VOID;
	}
}