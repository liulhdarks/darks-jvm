#pragma once

#include "common.h"
#include "vm/types.h"

typedef enum
{
	DT_INT = 1, DT_SHORT, DT_CHAR, DT_BYTE, DT_LONG, 
	DT_FLOAT, DT_DOUBLE, DT_BOOL, DT_VOID, DT_REF,
	DT_ARRAY
} DATA_TYPE;

typedef struct TypeDesc
{
	int array_level;
	bool check_array;
	DATA_TYPE data_type;
	LPSTR class_name;

	TypeDesc(){reset();}

	void reset()
	{
		array_level = 0;
		check_array = false;
		data_type = DT_VOID;
		class_name = NULL;
	}

} TypeDesc, *lpTypeDesc;

extern "C" JVM_API bool parse_method_desc_type(LPCSTR desc, vector<TypeDesc> &type_desc, 
							TypeDesc &result_desc);

extern "C" JVM_API int parse_desc_type(LPCSTR desc, TypeDesc &type_desc, int len, int offset = 0);

extern "C" JVM_API DATA_TYPE parse_data_type(char ch_type);