// Self-contained unit checks for the cross-platform port.
//
// Usage: unit_tests <classes-dir>
// The classes directory must contain Hello.class, Clock.class and the minimal
// java/lang and java/io runtime classes produced by javac.

#include "stdafx.h"
#include "platform/platform.h"
#include "vm/classfiles/parseclass.h"
#include "vm/classfiles/constant.h"
#include "vm/executor/jclass.h"
#include "vm/executor/jmethod.h"
#include "vm/executor/attributes.h"
#include "env/env.h"

#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

static int g_passed = 0;
static int g_failed = 0;

#define CHECK(cond) \
	do { \
		if (cond) { ++g_passed; } \
		else { ++g_failed; std::fprintf(stderr, "FAIL %s:%d: %s\n", __FILE__, __LINE__, #cond); } \
	} while (0)

static lpClass load_class_file(const std::string &path)
{
	std::vector<uint8_t> data;
	if (!dk::ReadFileBytes(path, data) || data.empty())
	{
		return NULL;
	}
	uint8_t *buffer = new uint8_t[data.size()];
	memcpy(buffer, data.data(), data.size());
	lpClass clazz = NULL;
	if (!parse_class_bytes(buffer, static_cast<int>(data.size()), clazz))
	{
		return NULL;
	}
	return clazz;
}

static lpMethod find_method(lpClass clazz, const char *name)
{
	if (!clazz || !clazz->methods)
	{
		return NULL;
	}
	for (U2 i = 0; i < clazz->methodCount; i++)
	{
		lpMethod method = clazz->methods[i];
		if (method && method->methodName && method->methodName->GetName() &&
		    strcmp(method->methodName->GetName(), name) == 0)
		{
			return method;
		}
	}
	return NULL;
}

static bool pool_has_utf8(lpClass clazz, const char *text)
{
	if (!clazz || !clazz->const_pool)
	{
		return false;
	}
	for (int i = 1; i < clazz->const_pool->size(); i++)
	{
		lpConstant constant = clazz->const_pool->GetConstant(i);
		if (constant && constant->tag == UTF8_CONSTANT)
		{
			Utf8Constant *utf8 = static_cast<Utf8Constant*>(constant);
			if (utf8->GetName() && strcmp(utf8->GetName(), text) == 0)
			{
				return true;
			}
		}
	}
	return false;
}

int main(int argc, char **argv)
{
	std::string classes_dir = ".";
	if (argc > 1)
	{
		classes_dir = argv[1];
	}

	// ---------------------------------------------------------------- UTF-8
	const char ascii[] = "abc";
	std::vector<uint16_t> ascii_units = dk::Utf8ToUtf16(ascii, 3);
	CHECK(ascii_units.size() == 3);
	CHECK(!ascii_units.empty() && ascii_units[0] == 'a');
	CHECK(dk::Utf16ToUtf8(ascii_units) == "abc");

	const char two_byte[] = "\xC3\xA9"; // U+00E9
	std::vector<uint16_t> two_units = dk::Utf8ToUtf16(two_byte, 2);
	CHECK(two_units.size() == 1);
	CHECK(!two_units.empty() && two_units[0] == 0x00E9);
	CHECK(dk::Utf16ToUtf8(two_units) == std::string(two_byte, 2));

	const char three_byte[] = "\xE4\xB8\xAD"; // U+4E2D
	std::vector<uint16_t> three_units = dk::Utf8ToUtf16(three_byte, 3);
	CHECK(three_units.size() == 1);
	CHECK(!three_units.empty() && three_units[0] == 0x4E2D);
	CHECK(dk::Utf16ToUtf8(three_units) == std::string(three_byte, 3));

	const char four_byte[] = "\xF0\x9F\x98\x80"; // U+1F600
	std::vector<uint16_t> four_units = dk::Utf8ToUtf16(four_byte, 4);
	CHECK(four_units.size() == 2);
	CHECK(four_units.size() == 2 && four_units[0] == 0xD83D);
	CHECK(dk::Utf16ToUtf8(four_units) == std::string(four_byte, 4));

	// ---------------------------------------------------------------- paths
	const std::string hello_path = dk::JoinPath(classes_dir, "Hello.class");
	CHECK(dk::PathListSeparator() == ':');
	CHECK(dk::JoinPath("a", "b") == "a/b");
	CHECK(dk::JoinPath("a/", "b") == "a/b");
	CHECK(dk::DirectoryName("/x/y") == "/x");
	CHECK(dk::DirectoryName("y") == "");
	CHECK(dk::FileExists(hello_path));
	CHECK(!dk::FileExists(dk::JoinPath(classes_dir, "NoSuchFile.class")));
	std::vector<uint8_t> hello_bytes;
	CHECK(dk::ReadFileBytes(hello_path, hello_bytes) && !hello_bytes.empty());

	// ------------------------------------------------------- Hello.class
	lpClass hello = load_class_file(hello_path);
	CHECK(hello != NULL);
	CHECK(hello && hello->const_pool != NULL);
	CHECK(hello && hello->const_pool->size() > 1);
	CHECK(hello && hello->const_pool->GetConstant(0) == NULL);
	CHECK(hello && hello->const_pool->GetConstant(1) != NULL);
	CHECK(hello && hello->const_pool->GetConstant(65535) == NULL);
	CHECK(hello && hello->thisClass && hello->thisClass->GetName() &&
	      strcmp(hello->thisClass->GetName(), "Hello") == 0);
	CHECK(hello && hello->superClass && hello->superClass->GetName() &&
	      strcmp(hello->superClass->GetName(), "java/lang/Object") == 0);
	CHECK(hello && hello->fieldCount == 0);
	CHECK(hello && hello->methodCount == 2);
	lpMethod init_method = find_method(hello, "<init>");
	lpMethod main_method = find_method(hello, "main");
	CHECK(init_method != NULL);
	CHECK(main_method != NULL);
	CHECK(main_method && main_method->methodDesc && main_method->methodDesc->GetName() &&
	      strcmp(main_method->methodDesc->GetName(), "([Ljava/lang/String;)V") == 0);
	CHECK(main_method && (main_method->accFlag & ACM_STATIC) != 0);
	CHECK(main_method && main_method->code_attr != NULL);
	CHECK(main_method && main_method->code_attr && main_method->code_attr->code_length > 0);
	CHECK(main_method && main_method->code_attr && main_method->code_attr->max_stack >= 1);
	CHECK(main_method && main_method->code_attr && main_method->code_attr->max_local >= 1);
	CHECK(hello && hello->source_file != NULL);
	CHECK(hello && hello->source_file && hello->source_file->source_file &&
	      hello->source_file->source_file->GetName() &&
	      strcmp(hello->source_file->source_file->GetName(), "Hello.java") == 0);
	CHECK(main_method && main_method->GetJniName() &&
	      strcmp(main_method->GetJniName(), "Java_Hello_main") == 0);
	CHECK(pool_has_utf8(hello, "Hello, darks-jvm!"));
	if (hello)
	{
		delete hello;
	}

	// ------------------------------------------------------ System.class
	lpClass system = load_class_file(dk::JoinPath(classes_dir, "java/lang/System.class"));
	CHECK(system != NULL);
	lpMethod millis = find_method(system, "currentTimeMillis");
	CHECK(millis != NULL);
	CHECK(millis && millis->methodDesc && millis->methodDesc->GetName() &&
	      strcmp(millis->methodDesc->GetName(), "()J") == 0);
	CHECK(millis && (millis->accFlag & ACM_NATIVE) != 0);
	lpMethod nanos = find_method(system, "nanoTime");
	CHECK(nanos != NULL);
	CHECK(nanos && nanos->methodDesc && nanos->methodDesc->GetName() &&
	      strcmp(nanos->methodDesc->GetName(), "()J") == 0);
	if (system)
	{
		delete system;
	}

	// --------------------------------------------------- PrintStream.class
	lpClass print_stream = load_class_file(dk::JoinPath(classes_dir, "java/io/PrintStream.class"));
	CHECK(print_stream != NULL);
	lpMethod write0 = find_method(print_stream, "write0");
	CHECK(write0 != NULL);
	CHECK(write0 && (write0->accFlag & ACM_NATIVE) != 0);
	if (print_stream)
	{
		delete print_stream;
	}

	// ----------------------------------------------------------- classpath
	Environment::ClearClassPath();
	std::vector<std::string> classpath;
	Environment::GetClassPath(&classpath);
	CHECK(classpath.empty());
	Environment::AddClassPath("a");
	Environment::GetClassPath(&classpath);
	CHECK(classpath.size() == 1);
	CHECK(classpath.size() == 1 && classpath[0] == "a");
	Environment::ClearClassPath();

	std::printf("unit checks: %d/54 passed\n", g_passed);
	if (g_failed != 0)
	{
		std::printf("unit checks: %d failed\n", g_failed);
		return 1;
	}
	return 0;
}
