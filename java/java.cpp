// darks-jvm command line launcher.
//
// Supported options:
//   -cp <path> / -classpath <path>   class search path (platform separator)
//   --dump <class>                   parse a class file and print its metadata
//   --version                        print the version
//   -h / --help                      print this help
// The DARKS_JVM_CLASSPATH environment variable is used when no -cp option is
// given. Exit codes: 0 success, 2 usage error, 3 VM/runtime error,
// 4 main class not found.

#include "stdafx.h"
#include "env/env.h"
#include "platform/platform.h"
#include "utils/DebugUtils.h"
#include "vm/classfiles/parseclass.h"
#include "vm/classfiles/classloader.h"
#include "vm/io/zipreader.h"
#include "vm/executor/executor.h"
#include "vm/executor/context.h"
#include "vm/exceptions.h"
#include "vm/executor/javaexception.h"

#include <cstdio>
#include <iostream>
#include <string>
#include <vector>

using namespace std;

static const char *kVersion = "darks-jvm 1.0 (cross-platform build)";

static void print_usage()
{
	cout << "Usage: darks-jvm [options] <main-class> [args...]" << endl;
	cout << "Options:" << endl;
	cout << "  -cp <path>, -classpath <path>  class search path" << endl;
	cout << "  --dump <class>                 print class file metadata" << endl;
	cout << "  --version                      print version information" << endl;
	cout << "  -h, --help                     print this help" << endl;
}

static vector<string> split_classpath(const string &value)
{
	vector<string> result;
	string item;
	for (size_t i = 0; i < value.size(); i++)
	{
		if (value[i] == dk::PathListSeparator())
		{
			if (!item.empty())
			{
				result.push_back(item);
			}
			item.clear();
		}
		else
		{
			item.push_back(value[i]);
		}
	}
	if (!item.empty())
	{
		result.push_back(item);
	}
	return result;
}

static void set_classpath(const string &value)
{
	Environment::ClearClassPath();
	vector<string> items = split_classpath(value);
	for (size_t i = 0; i < items.size(); i++)
	{
		Environment::AddClassPath(items[i]);
	}
}

static string class_relative_path(const string &class_name)
{
	string path = class_name;
	for (size_t i = 0; i < path.size(); i++)
	{
		if (path[i] == '.')
		{
			path[i] = '/';
		}
	}
	path += ".class";
	return path;
}

static bool find_class_file(const string &class_name, string &found)
{
	vector<uint8_t> data;
	string source;
	if (!DarksVM_FindClassBytes(class_name, data, &source))
	{
		return false;
	}
	found = source;
	return true;
}

static int dump_class(const string &class_name)
{
	vector<uint8_t> data;
	string path;
	if (!DarksVM_FindClassBytes(class_name, data, &path) || data.empty())
	{
		cerr << "Error: could not find class file for " << class_name << endl;
		return 4;
	}
	uint8_t *buffer = new uint8_t[data.size()];
	memcpy(buffer, data.data(), data.size());
	lpClass clazz = NULL;
	if (!parse_class_bytes(buffer, static_cast<int>(data.size()), clazz) || !clazz)
	{
		cerr << "Error: failed to parse " << path << endl;
		return 3;
	}
	cout << "Class file: " << path << endl;
	LPCSTR this_name = (clazz->thisClass && clazz->thisClass->GetName())
		? clazz->thisClass->GetName() : "<unknown>";
	LPCSTR super_name = (clazz->superClass && clazz->superClass->GetName())
		? clazz->superClass->GetName() : "<none>";
	cout << "  this_class:  " << this_name << endl;
	cout << "  super_class: " << super_name << endl;
	cout << "  constants:   " << (clazz->const_pool ? clazz->const_pool->size() : 0) << endl;
	cout << "  fields:      " << clazz->fieldCount << endl;
	for (U2 i = 0; i < clazz->fieldCount; i++)
	{
		lpField field = clazz->fields[i];
		if (field && field->fieldName)
		{
			cout << "    " << field->fieldName->GetName()
			     << " : " << (field->fieldDesc ? field->fieldDesc->GetName() : "") << endl;
		}
	}
	cout << "  methods:     " << clazz->methodCount << endl;
	for (U2 i = 0; i < clazz->methodCount; i++)
	{
		lpMethod method = clazz->methods[i];
		if (method && method->methodName)
		{
			cout << "    " << method->methodName->GetName()
			     << (method->methodDesc ? method->methodDesc->GetName() : "") << endl;
		}
	}
	delete clazz;
	return 0;
}

static int run_class(const string &class_name, int argc, char **argv)
{
	string path;
	if (!find_class_file(class_name, path))
	{
		cerr << "Error: could not find or load main class " << class_name << endl;
		return 4;
	}

	if (!VMContext::initialize())
	{
		cerr << "Error: failed to initialize the VM." << endl;
		return 3;
	}

	int result = 0;
	lpVMExecutor exec = NULL;
	try
	{
		lpClass clazz = NULL;
		if (!VMContext::bootstrap_classloader->FindClass(class_name.c_str(), clazz) ||
		    !clazz)
		{
			throw new VMException("Failed to load main class.");
		}
		if (!clazz->mainMethod)
		{
			throw new VMException("Main method not found.");
		}

		// Build a String[] for main(String[] args).
		lpClass string_array_class = NULL;
		if (!VMContext::bootstrap_classloader->FindClass("[Ljava/lang/String;", string_array_class))
		{
			throw new VMException("Failed to load String array class.");
		}
		LPOBJECT_REF args_ref = NULL;
		// The array only lives in this C++ local until main() is entered; keep
		// it alive while the argument strings are allocated.
		GCRootScope args_root(VMContext::object_heap, &args_ref);
		if (!VMContext::object_heap->NewObjectArrayObject(argc, string_array_class, args_ref))
		{
			throw new VMException("Failed to allocate the argument array.");
		}
		lpObject args_obj = NULL;
		if (!VMContext::object_heap->GetObject(args_ref, args_obj))
		{
			throw new VMException("Failed to access the argument array.");
		}
		jobject *arg_refs = static_cast<jobject*>(args_obj->array_var);
		for (int i = 0; i < argc; i++)
		{
			LPOBJECT_REF str_ref = NULL;
			if (!VMContext::object_heap->NewStringObject(argv[i], static_cast<int>(strlen(argv[i])), str_ref))
			{
				throw new VMException("Failed to allocate argument string.");
			}
			arg_refs[i] = reinterpret_cast<jobject>(str_ref);
		}

		Variant args_var;
		args_var.heapPtr = args_ref;
		exec = new VMExecutor();
		exec->Initialize();
		exec->Execute(clazz, clazz->mainMethod, &args_var, 1);
		if (VMContext::pending_exception)
		{
			// Uncaught Java exception: print a readable trace and exit with a
			// non-zero status (3, the VM/runtime error code).
			PrintUncaughtException(VMContext::pending_exception);
			VMContext::pending_exception = NULL;
			result = 3;
		}
	}
	catch (VMException *e)
	{
		cerr << "Error: " << e->what() << endl;
		delete e;
		result = 3;
	}
	catch (const std::exception &e)
	{
		cerr << "Error: " << e.what() << endl;
		result = 3;
	}
	catch (...)
	{
		cerr << "Error: unknown VM failure." << endl;
		result = 3;
	}

	if (exec)
	{
		delete exec;
		exec = NULL;
	}
	VMContext::destroy();
	return result;
}

int main(int argc, char **argv)
{
	const char *debug = getenv("DARKS_JVM_DEBUG");
	if (debug && *debug && string(debug) != "0")
	{
		dk::SetDebugEnabled(true);
	}

	vector<string> args;
	bool classpath_set = false;
	bool jar_mode = false;
	string jar_main_class;
	for (int i = 1; i < argc; i++)
	{
		string arg(argv[i]);
		if (arg == "-h" || arg == "--help")
		{
			print_usage();
			return 0;
		}
		if (arg == "--version")
		{
			cout << kVersion << endl;
			return 0;
		}
		if (arg == "-cp" || arg == "-classpath")
		{
			if (i + 1 >= argc)
			{
				cerr << "Error: missing class path after " << arg << endl;
				print_usage();
				return 2;
			}
			set_classpath(argv[++i]);
			classpath_set = true;
			continue;
		}
		if (arg.compare(0, 9, "-classpath") == 0 && arg.size() > 9)
		{
			set_classpath(arg.substr(9));
			classpath_set = true;
			continue;
		}
		if (arg == "-jar")
		{
			if (i + 1 >= argc)
			{
				cerr << "Error: missing jar file after -jar" << endl;
				print_usage();
				return 2;
			}
			string jar_path = argv[++i];
			ZipFile jar;
			if (!jar.Open(jar_path))
			{
				cerr << "Error: cannot open jar " << jar_path << ": " << jar.LastError() << endl;
				return 3;
			}
			string main_from_manifest = jar.GetManifestMainClass();
			if (main_from_manifest.empty())
			{
				cerr << "Error: no Main-Class in " << jar_path << endl;
				return 3;
			}
			Environment::AddClassPath(jar_path);
			jar_main_class = main_from_manifest;
			jar_mode = true;
			continue;
		}
		args.push_back(arg);
	}

	if (!classpath_set && !jar_mode)
	{
		const char *env_cp = getenv("DARKS_JVM_CLASSPATH");
		if (env_cp && *env_cp)
		{
			set_classpath(env_cp);
		}
		else
		{
			Environment::AddClassPath(".");
		}
	}

	if (jar_mode)
	{
		vector<char*> jar_argv;
		for (size_t i = 0; i < args.size(); i++)
		{
			jar_argv.push_back(const_cast<char*>(args[i].c_str()));
		}
		return run_class(jar_main_class, static_cast<int>(jar_argv.size()),
		                 jar_argv.empty() ? NULL : jar_argv.data());
	}

	if (args.empty())
	{
		print_usage();
		return 2;
	}

	if (args[0] == "--dump")
	{
		if (args.size() < 2)
		{
			cerr << "Error: --dump requires a class name" << endl;
			return 2;
		}
		return dump_class(args[1]);
	}

	string main_class = args[0];
	vector<string> main_args(args.begin() + 1, args.end());
	vector<char*> main_argv;
	for (size_t i = 0; i < main_args.size(); i++)
	{
		main_argv.push_back(const_cast<char*>(main_args[i].c_str()));
	}
	return run_class(main_class, static_cast<int>(main_argv.size()),
	                 main_argv.empty() ? NULL : main_argv.data());
}
