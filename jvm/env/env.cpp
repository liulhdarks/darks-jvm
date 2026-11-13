#include "stdafx.h"
#include "env/env.h"

vector<string> Environment::native_libs;
vector<string> Environment::system_libs;
vector<string> Environment::classpath;
bool Environment::assertions_enabled = false;

namespace {
// Heap allocated so that process-lifetime entries do not look like leaks to
// LeakSanitizer when the static destructor runs after the report.
map<string, string> &Properties()
{
	static map<string, string> *properties = new map<string, string>();
	return *properties;
}
}

void Environment::SetProperty(const string &key, const string &value)
{
	if (key.empty())
	{
		return;
	}
	Properties()[key] = value;
}

bool Environment::GetProperty(const string &key, string &value)
{
	map<string, string>::iterator it = Properties().find(key);
	if (it == Properties().end())
	{
		return false;
	}
	value = it->second;
	return true;
}

bool Environment::HasProperty(const string &key)
{
	return Properties().find(key) != Properties().end();
}

void Environment::SetDefaultProperty(const string &key, const string &value)
{
	if (!HasProperty(key))
	{
		Properties()[key] = value;
	}
}

bool Environment::ClearProperty(const string &key)
{
	return Properties().erase(key) > 0;
}

const map<string, string> &Environment::GetProperties()
{
	return Properties();
}

void Environment::ClearProperties()
{
	Properties().clear();
}
