#pragma once

#include "common.h"
#include <map>
#include <string>
#include <vector>
using namespace std;

class JVM_API Environment
{

private:
	static vector<string> native_libs;
	static vector<string> system_libs;
	static vector<string> classpath;
	static bool assertions_enabled;

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

	// System property table.  -Dkey=value options are stored here by the
	// launcher before the VM starts; System.initProperties seeds the defaults
	// without overwriting them, then hands the whole table to Java.
	static void SetProperty(const string &key, const string &value);
	static bool GetProperty(const string &key, string &value);
	static bool HasProperty(const string &key);
	// Inserts the default only when no -D override is present.
	static void SetDefaultProperty(const string &key, const string &value);
	static bool ClearProperty(const string &key);
	static const map<string, string> &GetProperties();
	static void ClearProperties();

	// Global assertion status, controlled by -ea/-da on the command line and
	// exposed to Java through Class.desiredAssertionStatus().
	static void SetAssertionsEnabled(bool enabled)
	{
		assertions_enabled = enabled;
	}

	static bool AssertionsEnabled()
	{
		return assertions_enabled;
	}
};
