// java.util.regex natives.
//
// Patterns are compiled with std::wregex (ECMAScript grammar) over UTF-32
// code points so that match offsets have Java semantics.  The bridge keeps
// a UTF-16 <-> code-point index map for every input string.

#include "stdafx.h"
#include "jni_impl.h"
#include "platform/platform.h"

#include <regex>
#include <string>
#include <vector>

typedef wchar_t wide_char;

namespace {

enum
{
	FLAG_UNIX_LINES = 1,
	FLAG_CASE_INSENSITIVE = 2,
	FLAG_COMMENTS = 4,
	FLAG_MULTILINE = 8,
	FLAG_LITERAL = 16,
	FLAG_DOTALL = 32,
	FLAG_UNICODE_CASE = 64,
	FLAG_CANON_EQ = 128
};

struct WideInput
{
	std::wstring chars;
	// offsets[k] is the UTF-16 index of code point k; offsets[len] == length.
	std::vector<jsize> offsets;
	// utf16ToWide[i] is the code point index for UTF-16 index i.
	std::vector<jsize> utf16ToWide;
};

bool BuildWide(const jchar *units, jsize length, WideInput *out)
{
	if (!out || length < 0)
	{
		return false;
	}
	out->chars.clear();
	out->offsets.clear();
	jsize i = 0;
	while (i < length)
	{
		unsigned int cp = static_cast<unsigned int>(units[i]);
		out->offsets.push_back(i);
		if (cp >= 0xD800 && cp <= 0xDBFF && i + 1 < length &&
		    units[i + 1] >= 0xDC00 && units[i + 1] <= 0xDFFF)
		{
			cp = 0x10000u + ((cp - 0xD800u) << 10) +
			     (static_cast<unsigned int>(units[i + 1]) - 0xDC00u);
			i += 2;
		}
		else
		{
			if (cp >= 0xD800 && cp <= 0xDFFF)
			{
				cp = 0xFFFD;
			}
			i += 1;
		}
		out->chars.push_back(static_cast<wide_char>(cp));
	}
	out->offsets.push_back(length);
	out->utf16ToWide.assign(static_cast<size_t>(length) + 1, 0);
	for (size_t k = 0; k + 1 < out->offsets.size(); k++)
	{
		for (jsize u = out->offsets[k]; u < out->offsets[k + 1]; u++)
		{
			out->utf16ToWide[static_cast<size_t>(u)] = static_cast<jsize>(k);
		}
	}
	out->utf16ToWide[static_cast<size_t>(length)] =
		static_cast<jsize>(out->chars.size());
	return true;
}

std::wstring EscapeLiteral(const std::wstring &pattern)
{
	std::wstring out;
	for (size_t i = 0; i < pattern.size(); i++)
	{
		wide_char c = pattern[i];
		if (c == L'\\' || c == L'^' || c == L'$' || c == L'|' ||
		    c == L'(' || c == L')' || c == L'[' || c == L']' ||
		    c == L'{' || c == L'}' || c == L'.' || c == L'*' ||
		    c == L'+' || c == L'?' || c == L'-')
		{
			out.push_back(L'\\');
		}
		out.push_back(c);
	}
	return out;
}

// std::regex (C++11) has neither DOTALL nor MULTILINE.  Approximate both
// with constructs that the ECMAScript grammar does understand.
std::wstring AdjustPattern(const std::wstring &pattern, int flags)
{
	bool dotall = (flags & FLAG_DOTALL) != 0;
	bool multiline = (flags & FLAG_MULTILINE) != 0;
	bool comments = (flags & FLAG_COMMENTS) != 0;
	if (!dotall && !multiline && !comments)
	{
		return pattern;
	}
	std::wstring out;
	bool in_class = false;
	for (size_t i = 0; i < pattern.size(); i++)
	{
		wide_char c = pattern[i];
		if (c == L'\\' && i + 1 < pattern.size())
		{
			out.push_back(c);
			out.push_back(pattern[i + 1]);
			i++;
			continue;
		}
		if (comments && !in_class && (c == L' ' || c == L'\t' ||
		    c == L'\r' || c == L'\n' || c == L'\f'))
		{
			continue;
		}
		if (comments && !in_class && c == L'#')
		{
			while (i + 1 < pattern.size() && pattern[i + 1] != L'\n')
			{
				i++;
			}
			continue;
		}
		if (c == L'[')
		{
			in_class = true;
		}
		else if (c == L']')
		{
			in_class = false;
		}
		if (!in_class && dotall && c == L'.')
		{
			out.append(L"[\\s\\S]");
			continue;
		}
		if (!in_class && multiline && c == L'^')
		{
			out.append(L"(?:^|[\\n\\r])");
			continue;
		}
		if (!in_class && multiline && c == L'$')
		{
			out.append(L"(?=[\\n\\r]|$)");
			continue;
		}
		out.push_back(c);
	}
	return out;
}

std::wregex *WideRegexHandle(jlong handle)
{
	return reinterpret_cast<std::wregex *>(static_cast<intptr_t>(handle));
}

std::string Utf8Of(const std::wstring &text)
{
	std::string out;
	for (size_t i = 0; i < text.size(); i++)
	{
		unsigned int cp = static_cast<unsigned int>(text[i]);
		if (cp < 0x80)
		{
			out.push_back(static_cast<char>(cp));
		}
		else if (cp < 0x800)
		{
			out.push_back(static_cast<char>(0xC0 | (cp >> 6)));
			out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
		}
		else if (cp < 0x10000)
		{
			out.push_back(static_cast<char>(0xE0 | (cp >> 12)));
			out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
			out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
		}
		else
		{
			out.push_back(static_cast<char>(0xF0 | (cp >> 18)));
			out.push_back(static_cast<char>(0x80 | ((cp >> 12) & 0x3F)));
			out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
			out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
		}
	}
	return out;
}

} // namespace

extern "C" JNIEXPORT jlong JNICALL Java_java_util_regex_Pattern_nativeCompile
	(JNIEnv *env, jclass clazz, jstring pattern, jint flags)
{
	(void)clazz;
	jchar *units = NULL;
	jsize length = 0;
	if (!pattern || !JniStringChars(JniRef(pattern), &units, &length))
	{
		return 0;
	}
	std::wstring source(units, units + length);
	if ((flags & FLAG_LITERAL) != 0)
	{
		source = EscapeLiteral(source);
	}
	source = AdjustPattern(source, flags);

	std::regex_constants::syntax_option_type options =
		std::regex_constants::ECMAScript;
	if ((flags & FLAG_CASE_INSENSITIVE) != 0)
	{
		options |= std::regex_constants::icase;
	}
	try
	{
		std::wregex *compiled = new std::wregex(source, options);
		return static_cast<jlong>(reinterpret_cast<intptr_t>(compiled));
	}
	catch (const std::regex_error &error)
	{
		std::string message = Utf8Of(source);
		message += ": ";
		message += error.what();
		jclass error_class = reinterpret_cast<jclass>(
			JniFindClass(env, "java/util/regex/PatternSyntaxException"));
		if (error_class)
		{
			env->ThrowNew(error_class, message.c_str());
		}
		return 0;
	}
	catch (const std::bad_alloc &)
	{
		jclass error_class = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/OutOfMemoryError"));
		if (error_class)
		{
			env->ThrowNew(error_class, "regex compilation failed");
		}
		return 0;
	}
}

extern "C" JNIEXPORT void JNICALL Java_java_util_regex_Pattern_nativeFree
	(JNIEnv *env, jclass clazz, jlong handle)
{
	(void)env;
	(void)clazz;
	delete WideRegexHandle(handle);
}

extern "C" JNIEXPORT jint JNICALL Java_java_util_regex_Pattern_nativeGroupCount
	(JNIEnv *env, jclass clazz, jlong handle)
{
	(void)env;
	(void)clazz;
	std::wregex *regex = WideRegexHandle(handle);
	if (!regex)
	{
		return 0;
	}
	return static_cast<jint>(regex->mark_count());
}

// Returns {start0,end0,start1,end1,...} in UTF-16 units, or null when the
// pattern does not match.  Unmatched groups are reported as -1,-1.
extern "C" JNIEXPORT jlongArray JNICALL Java_java_util_regex_Pattern_nativeMatch
	(JNIEnv *env, jclass clazz, jlong handle, jstring input, jint start,
	 jboolean anchored)
{
	(void)clazz;
	std::wregex *regex = WideRegexHandle(handle);
	if (!regex || !input)
	{
		return NULL;
	}
	jchar *units = NULL;
	jsize length = 0;
	if (!JniStringChars(JniRef(input), &units, &length))
	{
		return NULL;
	}
	if (start < 0)
	{
		start = 0;
	}
	if (start > length)
	{
		return NULL;
	}
	WideInput text;
	if (!BuildWide(units, length, &text))
	{
		return NULL;
	}
	size_t wide_start = static_cast<size_t>(text.utf16ToWide[static_cast<size_t>(start)]);
	std::match_results<std::wstring::const_iterator> match;
	std::regex_constants::match_flag_type match_flags =
		std::regex_constants::match_default;
	if (anchored)
	{
		match_flags |= std::regex_constants::match_continuous;
	}
	bool found = false;
	try
	{
		found = std::regex_search(text.chars.cbegin() + wide_start,
			text.chars.cend(), match, *regex, match_flags);
	}
	catch (const std::regex_error &)
	{
		return NULL;
	}
	if (!found)
	{
		return NULL;
	}
	size_t groups = match.size();
	std::vector<jlong> bounds(groups * 2);
	for (size_t k = 0; k < groups; k++)
	{
		if (!match[k].matched)
		{
			bounds[k * 2] = -1;
			bounds[k * 2 + 1] = -1;
			continue;
		}
		size_t wide_begin = static_cast<size_t>(match.position(k)) + wide_start;
		size_t wide_end = wide_begin + static_cast<size_t>(match.length(k));
		if (wide_end > text.offsets.size() - 1)
		{
			wide_end = text.offsets.size() - 1;
		}
		bounds[k * 2] = static_cast<jlong>(text.offsets[wide_begin]);
		bounds[k * 2 + 1] = static_cast<jlong>(text.offsets[wide_end]);
	}
	jlongArray result = env->NewLongArray(static_cast<jsize>(bounds.size()));
	if (!result)
	{
		return NULL;
	}
	env->SetLongArrayRegion(result, 0, static_cast<jsize>(bounds.size()),
		&bounds[0]);
	return result;
}
