#pragma once

#include "common.h"
#include <vector>
using namespace std;

class JVM_API Environment
{

private:
	static vector<string> native_libs;
	static vector<string> system_libs;
	static vector<string> classpath;

public:

	static void GetClassPath(vector<string> *cp)
	{
		*cp = classpath;
	}

	static void AddClassPath(string &cp)
	{
		classpath.push_back(cp);
	}
};