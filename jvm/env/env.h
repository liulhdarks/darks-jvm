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

	static void AddClassPath(const string &cp)
	{
		if (cp.empty())
		{
			return;
		}
		classpath.push_back(cp);
	}

	static void AddClassPath(LPCSTR cp)
	{
		if (cp && *cp)
		{
			classpath.push_back(string(cp));
		}
	}

	static void ClearClassPath()
	{
		classpath.clear();
	}
};
