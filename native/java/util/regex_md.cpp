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
#include <algorithm>
#include <map>
#include <mutex>

#ifdef DARKS_HAVE_PCRE2
#define PCRE2_CODE_UNIT_WIDTH 32
#include <pcre2.h>
#endif

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

bool IsAsciiLetter(wide_char c)
{
	return (c >= L'a' && c <= L'z') || (c >= L'A' && c <= L'Z');
}

void CopyEscape(const std::wstring &pattern, size_t &i, std::wstring &out)
{
	out.push_back(pattern[i]);
	if (i + 1 >= pattern.size())
	{
		return;
	}
	wide_char e = pattern[i + 1];
	out.push_back(e);
	i++;
	if (e == L'x')
	{
		for (int k = 0; k < 2 && i + 1 < pattern.size(); k++)
		{
			out.push_back(pattern[++i]);
		}
	}
	else if (e == L'u')
	{
		for (int k = 0; k < 4 && i + 1 < pattern.size(); k++)
		{
			out.push_back(pattern[++i]);
		}
	}
	else if ((e == L'p' || e == L'P') && i + 1 < pattern.size() &&
	         pattern[i + 1] == L'{')
	{
		out.push_back(pattern[++i]);
		while (i + 1 < pattern.size())
		{
			out.push_back(pattern[++i]);
			if (pattern[i] == L'}')
			{
				break;
			}
		}
	}
}

wide_char ToggleAsciiCase(wide_char c)
{
	if (c >= L'a' && c <= L'z')
	{
		return static_cast<wide_char>(c - L'a' + L'A');
	}
	if (c >= L'A' && c <= L'Z')
	{
		return static_cast<wide_char>(c - L'A' + L'a');
	}
	return c;
}

// std::regex (C++11) has neither scoped inline flags nor (?i).  Java
// libraries rely on both, so fold the affected literals into explicit
// character classes and turn (?i:...) into a plain non-capturing group.
std::wstring TranslateInlineFlags(const std::wstring &pattern, bool icase)
{
	if (pattern.find(L"(?") == std::wstring::npos)
	{
		return pattern;
	}
	std::wstring out;
	std::vector<bool> saved_icase;
	bool in_class = false;
	for (size_t i = 0; i < pattern.size(); i++)
	{
		wide_char c = pattern[i];
		if (c == L'\\')
		{
			CopyEscape(pattern, i, out);
			continue;
		}
		if (in_class)
		{
			if (c == L']')
			{
				in_class = false;
				out.push_back(c);
				continue;
			}
			if (icase && IsAsciiLetter(c))
			{
				if (i + 2 < pattern.size() && pattern[i + 1] == L'-' &&
				    IsAsciiLetter(pattern[i + 2]) &&
				    ((c >= L'a' && c <= L'z' && pattern[i + 2] >= L'a' &&
				      pattern[i + 2] <= L'z') ||
				     (c >= L'A' && c <= L'Z' && pattern[i + 2] >= L'A' &&
				      pattern[i + 2] <= L'Z')))
				{
					out.push_back(c);
					out.push_back(L'-');
					out.push_back(pattern[i + 2]);
					out.push_back(ToggleAsciiCase(c));
					out.push_back(L'-');
					out.push_back(ToggleAsciiCase(pattern[i + 2]));
					i += 2;
					continue;
				}
				out.push_back(c);
				out.push_back(ToggleAsciiCase(c));
				continue;
			}
			out.push_back(c);
			continue;
		}
		if (c == L'[')
		{
			in_class = true;
			out.push_back(c);
			continue;
		}
		if (c == L'(' && i + 1 < pattern.size() && pattern[i + 1] == L'?')
		{
			size_t j = i + 2;
			bool negate = false;
			bool seen_flag = false;
			bool enable = false;
			bool disable = false;
			while (j < pattern.size())
			{
				wide_char f = pattern[j];
				if (f == L'-')
				{
					negate = true;
					j++;
					continue;
				}
				if (f == L'i')
				{
					if (negate)
					{
						disable = true;
					}
					else
					{
						enable = true;
					}
					seen_flag = true;
					j++;
					continue;
				}
				if (f == L'd' || f == L'm' || f == L's' || f == L'u' ||
				    f == L'x' || f == L'U')
				{
					seen_flag = true;
					j++;
					continue;
				}
				break;
			}
			if (seen_flag && j < pattern.size() && pattern[j] == L':')
			{
				out.append(L"(?:");
				saved_icase.push_back(icase);
				if (disable)
				{
					icase = false;
				}
				if (enable)
				{
					icase = true;
				}
				i = j;
				continue;
			}
			if (seen_flag && j < pattern.size() && pattern[j] == L')')
			{
				if (disable)
				{
					icase = false;
				}
				if (enable)
				{
					icase = true;
				}
				i = j;
				continue;
			}
		}
		if (c == L'(')
		{
			saved_icase.push_back(icase);
			out.push_back(c);
			continue;
		}
		if (c == L')')
		{
			if (!saved_icase.empty())
			{
				icase = saved_icase.back();
				saved_icase.pop_back();
			}
			out.push_back(c);
			continue;
		}
		if (icase && IsAsciiLetter(c))
		{
			out.push_back(L'[');
			out.push_back(c);
			out.push_back(ToggleAsciiCase(c));
			out.push_back(L']');
			continue;
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

struct CodePointRange
{
	unsigned int lo;
	unsigned int hi;
};

// std::regex knows neither \p{...} nor Unicode categories.  Translate the
// properties that show up in practice into explicit character ranges.  The
// ranges are written as literal wchar values, which the ECMAScript grammar
// accepts inside a character class.
bool PropertyRanges(const std::wstring &raw, std::vector<CodePointRange> &out)
{
	std::wstring name = raw;
	if (name.size() > 2 && (name[0] == L'I' || name[0] == L'i') &&
	    name[1] == L's')
	{
		name = name.substr(2);
	}
	else if (name.size() > 2 && (name[0] == L'I' || name[0] == L'i') &&
	         name[1] == L'n')
	{
		name = name.substr(2);
	}
	if (name == L"Cc" || name == L"Cntrl")
	{
		out.push_back(CodePointRange{0x00, 0x1F});
		out.push_back(CodePointRange{0x7F, 0x9F});
		return true;
	}
	if (name == L"Cf")
	{
		out.push_back(CodePointRange{0x00AD, 0x00AD});
		out.push_back(CodePointRange{0x0600, 0x0605});
		out.push_back(CodePointRange{0x200B, 0x200F});
		out.push_back(CodePointRange{0x202A, 0x202E});
		out.push_back(CodePointRange{0x2060, 0x2064});
		out.push_back(CodePointRange{0xFEFF, 0xFEFF});
		return true;
	}
	if (name == L"Lu" || name == L"Upper" || name == L"javaUpperCase")
	{
		out.push_back(CodePointRange{0x0041, 0x005A});
		out.push_back(CodePointRange{0x00C0, 0x00D6});
		out.push_back(CodePointRange{0x00D8, 0x00DE});
		return true;
	}
	if (name == L"Ll" || name == L"Lower" || name == L"javaLowerCase")
	{
		out.push_back(CodePointRange{0x0061, 0x007A});
		out.push_back(CodePointRange{0x00DF, 0x00F6});
		out.push_back(CodePointRange{0x00F8, 0x00FF});
		return true;
	}
	if (name == L"L" || name == L"Lt" || name == L"Lm" || name == L"Lo" ||
	    name == L"Alpha" || name == L"IsAlphabetic")
	{
		out.push_back(CodePointRange{0x0041, 0x005A});
		out.push_back(CodePointRange{0x0061, 0x007A});
		out.push_back(CodePointRange{0x00C0, 0x00D6});
		out.push_back(CodePointRange{0x00D8, 0x00F6});
		out.push_back(CodePointRange{0x00F8, 0x00FF});
		return true;
	}
	if (name == L"Nd" || name == L"Digit" || name == L"IsDigit")
	{
		out.push_back(CodePointRange{0x0030, 0x0039});
		return true;
	}
	if (name == L"N" || name == L"Nl" || name == L"No")
	{
		out.push_back(CodePointRange{0x0030, 0x0039});
		out.push_back(CodePointRange{0x00B2, 0x00B3});
		out.push_back(CodePointRange{0x00B9, 0x00B9});
		out.push_back(CodePointRange{0x00BC, 0x00BE});
		return true;
	}
	if (name == L"Z" || name == L"Space" || name == L"Blank" ||
	    name == L"javaWhitespace")
	{
		out.push_back(CodePointRange{0x0009, 0x000D});
		out.push_back(CodePointRange{0x0020, 0x0020});
		out.push_back(CodePointRange{0x00A0, 0x00A0});
		out.push_back(CodePointRange{0x2000, 0x200A});
		out.push_back(CodePointRange{0x2028, 0x2029});
		out.push_back(CodePointRange{0x202F, 0x202F});
		out.push_back(CodePointRange{0x205F, 0x205F});
		out.push_back(CodePointRange{0x3000, 0x3000});
		return true;
	}
	if (name == L"P" || name == L"Punct")
	{
		out.push_back(CodePointRange{0x0021, 0x0023});
		out.push_back(CodePointRange{0x0025, 0x002A});
		out.push_back(CodePointRange{0x002C, 0x002F});
		out.push_back(CodePointRange{0x003A, 0x003B});
		out.push_back(CodePointRange{0x003F, 0x0040});
		out.push_back(CodePointRange{0x005B, 0x005D});
		out.push_back(CodePointRange{0x005F, 0x005F});
		out.push_back(CodePointRange{0x007B, 0x007B});
		out.push_back(CodePointRange{0x007D, 0x007D});
		return true;
	}
	if (name == L"S")
	{
		out.push_back(CodePointRange{0x0024, 0x0024});
		out.push_back(CodePointRange{0x002B, 0x002B});
		out.push_back(CodePointRange{0x003C, 0x003E});
		out.push_back(CodePointRange{0x005E, 0x005E});
		out.push_back(CodePointRange{0x0060, 0x0060});
		out.push_back(CodePointRange{0x007C, 0x007C});
		out.push_back(CodePointRange{0x007E, 0x007E});
		return true;
	}
	if (name == L"M")
	{
		out.push_back(CodePointRange{0x0300, 0x036F});
		out.push_back(CodePointRange{0x20D0, 0x20FF});
		return true;
	}
	if (name == L"Graph")
	{
		out.push_back(CodePointRange{0x0021, 0x007E});
		out.push_back(CodePointRange{0x00A1, 0x00FF});
		return true;
	}
	if (name == L"Print")
	{
		out.push_back(CodePointRange{0x0020, 0x007E});
		out.push_back(CodePointRange{0x00A0, 0x00FF});
		return true;
	}
	if (name == L"XDigit")
	{
		out.push_back(CodePointRange{0x0030, 0x0039});
		out.push_back(CodePointRange{0x0041, 0x0046});
		out.push_back(CodePointRange{0x0061, 0x0066});
		return true;
	}
	return false;
}

struct RangeLess
{
	bool operator()(const CodePointRange &a, const CodePointRange &b) const
	{
		return a.lo < b.lo || (a.lo == b.lo && a.hi < b.hi);
	}
};

void NormalizeRanges(std::vector<CodePointRange> &ranges)
{
	if (ranges.empty())
	{
		return;
	}
	std::sort(ranges.begin(), ranges.end(), RangeLess());
	std::vector<CodePointRange> merged;
	merged.push_back(ranges[0]);
	for (size_t i = 1; i < ranges.size(); i++)
	{
		CodePointRange &last = merged.back();
		if (ranges[i].lo <= last.hi + 1)
		{
			if (ranges[i].hi > last.hi)
			{
				last.hi = ranges[i].hi;
			}
		}
		else
		{
			merged.push_back(ranges[i]);
		}
	}
	ranges.swap(merged);
}

std::vector<CodePointRange> IntersectRanges(const std::vector<CodePointRange> &a,
                                            const std::vector<CodePointRange> &b)
{
	std::vector<CodePointRange> out;
	size_t i = 0;
	size_t j = 0;
	while (i < a.size() && j < b.size())
	{
		unsigned int lo = a[i].lo > b[j].lo ? a[i].lo : b[j].lo;
		unsigned int hi = a[i].hi < b[j].hi ? a[i].hi : b[j].hi;
		if (lo <= hi)
		{
			out.push_back(CodePointRange{lo, hi});
		}
		if (a[i].hi < b[j].hi)
		{
			i++;
		}
		else
		{
			j++;
		}
	}
	return out;
}

std::vector<CodePointRange> ComplementRanges(const std::vector<CodePointRange> &ranges)
{
	std::vector<CodePointRange> out;
	unsigned int next = 0;
	for (size_t i = 0; i < ranges.size(); i++)
	{
		if (ranges[i].lo > next)
		{
			out.push_back(CodePointRange{next, ranges[i].lo - 1});
		}
		if (ranges[i].hi + 1 > next)
		{
			next = ranges[i].hi + 1;
		}
	}
	if (next <= 0x10FFFFu)
	{
		out.push_back(CodePointRange{next, 0x10FFFFu});
	}
	return out;
}

void AppendClassChar(std::wstring &out, unsigned int cp)
{
	if (cp < 0x20 || (cp >= 0x7F && cp <= 0x9F))
	{
		static const char *hex = "0123456789abcdef";
		out.push_back(L'\\');
		out.push_back(L'x');
		out.push_back(hex[(cp >> 4) & 0xF]);
		out.push_back(hex[cp & 0xF]);
		return;
	}
	if (cp == L'\\' || cp == L']' || cp == L'^' || cp == L'[' || cp == L'-')
	{
		out.push_back(L'\\');
	}
	out.push_back(static_cast<wide_char>(cp));
}

void AppendClass(std::wstring &out, const std::vector<CodePointRange> &ranges)
{
	out.push_back(L'[');
	if (ranges.empty())
	{
		out.append(L"^\\s\\S");
		out.push_back(L']');
		return;
	}
	for (size_t i = 0; i < ranges.size(); i++)
	{
		AppendClassChar(out, ranges[i].lo);
		if (ranges[i].hi != ranges[i].lo)
		{
			out.push_back(L'-');
			AppendClassChar(out, ranges[i].hi);
		}
	}
	out.push_back(L']');
}

// Java allows nested character classes and '&&' intersection inside them.
// std::regex knows neither, so a class using those features is evaluated here
// into an explicit set of code point ranges.
class ClassParser
{
public:
	ClassParser(const std::wstring &pattern, size_t &pos)
		: p(pattern), i(pos), complex(false)
	{
	}

	bool Parse(std::vector<CodePointRange> &out);

	bool complex;

private:
	const std::wstring &p;
	size_t &i;

	bool ParseUnion(std::vector<CodePointRange> &out);
	bool ParseItem(std::vector<CodePointRange> &out);
	bool ParseAtom(unsigned int &cp, bool &single);
	bool ParseRange(unsigned int lo, std::vector<CodePointRange> &out);
};

bool ClassParser::Parse(std::vector<CodePointRange> &out)
{
	if (i >= p.size() || p[i] != L'[')
	{
		return false;
	}
	i++;
	bool negate = false;
	if (i < p.size() && p[i] == L'^')
	{
		negate = true;
		i++;
	}
	if (!ParseUnion(out))
	{
		return false;
	}
	while (i + 1 < p.size() && p[i] == L'&' && p[i + 1] == L'&')
	{
		complex = true;
		i += 2;
		std::vector<CodePointRange> rhs;
		if (!ParseUnion(rhs))
		{
			return false;
		}
		out = IntersectRanges(out, rhs);
	}
	if (i >= p.size() || p[i] != L']')
	{
		return false;
	}
	i++;
	NormalizeRanges(out);
	if (negate)
	{
		out = ComplementRanges(out);
	}
	return true;
}

bool ClassParser::ParseUnion(std::vector<CodePointRange> &out)
{
	out.clear();
	while (i < p.size())
	{
		if (p[i] == L']')
		{
			break;
		}
		if (p[i] == L'&' && i + 1 < p.size() && p[i + 1] == L'&')
		{
			break;
		}
		std::vector<CodePointRange> item;
		if (!ParseItem(item))
		{
			return false;
		}
		out.insert(out.end(), item.begin(), item.end());
	}
	NormalizeRanges(out);
	return true;
}

bool ClassParser::ParseItem(std::vector<CodePointRange> &out)
{
	out.clear();
	if (i >= p.size())
	{
		return false;
	}
	if (p[i] == L'[')
	{
		complex = true;
		return Parse(out);
	}
	if (p[i] == L'\\' && i + 1 < p.size())
	{
		wide_char e = p[i + 1];
		if ((e == L'p' || e == L'P') && i + 2 < p.size() && p[i + 2] == L'{')
		{
			complex = true;
			size_t close = p.find(L'}', i + 3);
			if (close == std::wstring::npos)
			{
				return false;
			}
			std::vector<CodePointRange> ranges;
			if (!PropertyRanges(p.substr(i + 3, close - i - 3), ranges))
			{
				return false;
			}
			if (e == L'P')
			{
				ranges = ComplementRanges(ranges);
			}
			out = ranges;
			i = close + 1;
			return true;
		}
		if (e == L'd' || e == L'D' || e == L'w' || e == L'W' ||
		    e == L's' || e == L'S')
		{
			std::vector<CodePointRange> ranges;
			if (e == L'd' || e == L'D')
			{
				ranges.push_back(CodePointRange{0x30, 0x39});
			}
			else if (e == L'w' || e == L'W')
			{
				ranges.push_back(CodePointRange{0x30, 0x39});
				ranges.push_back(CodePointRange{0x41, 0x5A});
				ranges.push_back(CodePointRange{0x5F, 0x5F});
				ranges.push_back(CodePointRange{0x61, 0x7A});
			}
			else
			{
				ranges.push_back(CodePointRange{0x09, 0x0D});
				ranges.push_back(CodePointRange{0x20, 0x20});
			}
			if (e == L'D' || e == L'W' || e == L'S')
			{
				ranges = ComplementRanges(ranges);
			}
			out = ranges;
			i += 2;
			return true;
		}
	}
	unsigned int lo = 0;
	bool single = false;
	if (!ParseAtom(lo, single) || !single)
	{
		return false;
	}
	return ParseRange(lo, out);
}

bool ClassParser::ParseAtom(unsigned int &cp, bool &single)
{
	single = true;
	if (i >= p.size())
	{
		return false;
	}
	if (p[i] != L'\\')
	{
		cp = static_cast<unsigned int>(p[i]);
		i++;
		return true;
	}
	if (i + 1 >= p.size())
	{
		return false;
	}
	wide_char e = p[i + 1];
	i += 2;
	switch (e)
	{
	case L't': cp = 0x09; return true;
	case L'n': cp = 0x0A; return true;
	case L'r': cp = 0x0D; return true;
	case L'f': cp = 0x0C; return true;
	case L'e': cp = 0x1B; return true;
	case L'a': cp = 0x07; return true;
	case L'b': cp = 0x08; return true;
	case L'0': cp = 0x00; return true;
	case L'x':
	case L'u':
	{
		int limit = (e == L'x') ? 2 : 4;
		unsigned int value = 0;
		int digits = 0;
		while (digits < limit && i < p.size())
		{
			wide_char h = p[i];
			unsigned int d;
			if (h >= L'0' && h <= L'9') { d = static_cast<unsigned int>(h - L'0'); }
			else if (h >= L'a' && h <= L'f') { d = static_cast<unsigned int>(h - L'a' + 10); }
			else if (h >= L'A' && h <= L'F') { d = static_cast<unsigned int>(h - L'A' + 10); }
			else { break; }
			value = value * 16 + d;
			i++;
			digits++;
		}
		if (digits == 0)
		{
			return false;
		}
		cp = value;
		return true;
	}
	default:
		cp = static_cast<unsigned int>(e);
		return true;
	}
}

bool ClassParser::ParseRange(unsigned int lo, std::vector<CodePointRange> &out)
{
	out.clear();
	if (i + 1 < p.size() && p[i] == L'-' && p[i + 1] != L']')
	{
		size_t save = i;
		i++;
		unsigned int hi = 0;
		bool single = false;
		if (ParseAtom(hi, single) && single)
		{
			if (hi < lo)
			{
				unsigned int tmp = lo;
				lo = hi;
				hi = tmp;
			}
			out.push_back(CodePointRange{lo, hi});
			return true;
		}
		i = save;
	}
	out.push_back(CodePointRange{lo, lo});
	return true;
}

size_t FindClassEnd(const std::wstring &pattern, size_t start)
{
	int depth = 0;
	for (size_t i = start; i < pattern.size(); i++)
	{
		wide_char c = pattern[i];
		if (c == L'\\')
		{
			i++;
			continue;
		}
		if (c == L'[')
		{
			depth++;
		}
		else if (c == L']')
		{
			depth--;
			if (depth == 0)
			{
				return i + 1;
			}
		}
	}
	return std::wstring::npos;
}

bool ClassBodyComplex(const std::wstring &body)
{
	for (size_t i = 0; i < body.size(); i++)
	{
		if (body[i] == L'\\')
		{
			if (i + 2 < body.size() && (body[i + 1] == L'p' || body[i + 1] == L'P') &&
			    body[i + 2] == L'{')
			{
				return true;
			}
			i++;
			continue;
		}
		if (body[i] == L'[')
		{
			return true;
		}
		if (body[i] == L'&' && i + 1 < body.size() && body[i + 1] == L'&')
		{
			return true;
		}
	}
	return false;
}

std::wstring TranslateUnicodeProperties(const std::wstring &pattern)
{
	std::wstring out;
	size_t i = 0;
	while (i < pattern.size())
	{
		wide_char c = pattern[i];
		if (c == L'\\' && i + 1 < pattern.size())
		{
			wide_char next = pattern[i + 1];
			if ((next == L'p' || next == L'P') && i + 2 < pattern.size() &&
			    pattern[i + 2] == L'{')
			{
				size_t close = pattern.find(L'}', i + 3);
				if (close != std::wstring::npos)
				{
					std::vector<CodePointRange> ranges;
					if (PropertyRanges(pattern.substr(i + 3, close - i - 3), ranges))
					{
						if (next == L'P')
						{
							ranges = ComplementRanges(ranges);
						}
						AppendClass(out, ranges);
						i = close + 1;
						continue;
					}
				}
			}
			out.push_back(c);
			out.push_back(next);
			i += 2;
			continue;
		}
		if (c == L'[')
		{
			size_t end = FindClassEnd(pattern, i);
			if (end == std::wstring::npos)
			{
				out.push_back(c);
				i++;
				continue;
			}
			std::wstring body = pattern.substr(i + 1, end - i - 2);
			if (ClassBodyComplex(body))
			{
				std::wstring text = pattern.substr(i, end - i);
				size_t pos = 0;
				ClassParser parser(text, pos);
				std::vector<CodePointRange> ranges;
				if (parser.Parse(ranges) && pos == text.size())
				{
					AppendClass(out, ranges);
					i = end;
					continue;
				}
			}
			out.append(pattern, i, end - i);
			i = end;
			continue;
		}
		out.push_back(c);
		i++;
	}
	return out;
}

struct CompiledRegex
{
	CompiledRegex()
		: std_regex(NULL), group_count(0)
#ifdef DARKS_HAVE_PCRE2
		, pc_code(NULL)
#endif
	{
	}

	~CompiledRegex()
	{
		if (std_regex)
		{
			delete std_regex;
		}
#ifdef DARKS_HAVE_PCRE2
		if (pc_code)
		{
			pcre2_code_free(pc_code);
		}
#endif
	}

	std::wregex *std_regex;
	int group_count;
#ifdef DARKS_HAVE_PCRE2
	pcre2_code *pc_code;
#endif
};

CompiledRegex *WideRegexHandle(jlong handle)
{
	return reinterpret_cast<CompiledRegex *>(static_cast<intptr_t>(handle));
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

// Process-lifetime store of compiled patterns.  Every handle returned to
// java.util.regex.Pattern lives here, so the allocations stay reachable and
// are shared by all Pattern objects with the same expression and flags.
std::mutex g_regex_cache_mutex;
std::map<std::string, CompiledRegex *> g_regex_cache;

struct RegexCacheCleanup
{
	~RegexCacheCleanup()
	{
		for (std::map<std::string, CompiledRegex *>::iterator it =
			     g_regex_cache.begin();
		     it != g_regex_cache.end(); ++it)
		{
			delete it->second;
		}
		g_regex_cache.clear();
	}
};

RegexCacheCleanup g_regex_cache_cleanup;

std::string RegexCacheKey(const std::wstring &source, int flags)
{
	std::string key = Utf8Of(source);
	key.push_back('\0');
	for (int i = 0; i < 4; i++)
	{
		key.push_back(static_cast<char>((flags >> (i * 8)) & 0xFF));
	}
	return key;
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
	else
	{
		source = TranslateInlineFlags(source,
			(flags & FLAG_CASE_INSENSITIVE) != 0);
	}
	source = AdjustPattern(source, flags);
	source = TranslateUnicodeProperties(source);

	// Pattern stores only the integer handle and the runtime has no
	// finalization, so a compiled handle could never be freed once the Java
	// object became garbage.  Intern by (translated expression, flags)
	// instead: identical patterns share one handle, which also avoids
	// recompiling patterns that String.split and matcher helpers rebuild.
	std::string cache_key = RegexCacheKey(source, flags);
	{
		std::lock_guard<std::mutex> lk(g_regex_cache_mutex);
		std::map<std::string, CompiledRegex *>::iterator it =
			g_regex_cache.find(cache_key);
		if (it != g_regex_cache.end())
		{
			return static_cast<jlong>(reinterpret_cast<intptr_t>(it->second));
		}
	}

	std::regex_constants::syntax_option_type options =
		std::regex_constants::ECMAScript;
	if ((flags & FLAG_CASE_INSENSITIVE) != 0)
	{
		options |= std::regex_constants::icase;
	}
	CompiledRegex *compiled = new CompiledRegex();
	try
	{
		compiled->std_regex = new std::wregex(source, options);
		compiled->group_count = static_cast<int>(compiled->std_regex->mark_count());
	}
	catch (const std::regex_error &error)
	{
#ifdef DARKS_HAVE_PCRE2
		// std::regex covers the common ECMAScript subset only.  Java libraries
		// also use lookbehind and other PCRE features; fall back to PCRE2 so
		// those patterns still compile.
		uint32_t pc_options = PCRE2_UTF;
		if ((flags & FLAG_CASE_INSENSITIVE) != 0)
		{
			pc_options |= PCRE2_CASELESS;
		}
		if ((flags & FLAG_DOTALL) != 0)
		{
			pc_options |= PCRE2_DOTALL;
		}
		if ((flags & FLAG_MULTILINE) != 0)
		{
			pc_options |= PCRE2_MULTILINE;
		}
		if ((flags & FLAG_COMMENTS) != 0)
		{
			pc_options |= PCRE2_EXTENDED;
		}
		if ((flags & FLAG_LITERAL) != 0)
		{
			pc_options |= PCRE2_LITERAL;
		}
		if ((flags & FLAG_UNIX_LINES) != 0)
		{
			pc_options |= PCRE2_NEWLINE_LF;
		}
		int error_code = 0;
		PCRE2_SIZE error_offset = 0;
		pcre2_code *code = pcre2_compile(
			reinterpret_cast<PCRE2_SPTR>(source.data()), source.size(),
			pc_options, &error_code, &error_offset, NULL);
		if (code)
		{
			delete compiled->std_regex;
			compiled->std_regex = NULL;
			compiled->pc_code = code;
			uint32_t capture_count = 0;
			pcre2_pattern_info(code, PCRE2_INFO_CAPTURECOUNT, &capture_count);
			compiled->group_count = static_cast<int>(capture_count);
		}
		else
		{
			PCRE2_UCHAR buffer[256];
			pcre2_get_error_message(error_code, buffer, sizeof(buffer));
			std::string message = Utf8Of(source);
			message += ": ";
			message += Utf8Of(std::wstring(reinterpret_cast<wchar_t *>(buffer)));
			jclass error_class = reinterpret_cast<jclass>(
				JniFindClass(env, "java/util/regex/PatternSyntaxException"));
			if (error_class)
			{
				env->ThrowNew(error_class, message.c_str());
			}
			delete compiled;
			return 0;
		}
#else
		std::string message = Utf8Of(source);
		message += ": ";
		message += error.what();
		jclass error_class = reinterpret_cast<jclass>(
			JniFindClass(env, "java/util/regex/PatternSyntaxException"));
		if (error_class)
		{
			env->ThrowNew(error_class, message.c_str());
		}
		delete compiled;
		return 0;
#endif
	}
	catch (const std::bad_alloc &)
	{
		delete compiled;
		jclass error_class = reinterpret_cast<jclass>(
			JniFindClass(env, "java/lang/OutOfMemoryError"));
		if (error_class)
		{
			env->ThrowNew(error_class, "regex compilation failed");
		}
		return 0;
	}

	{
		std::lock_guard<std::mutex> lk(g_regex_cache_mutex);
		std::pair<std::map<std::string, CompiledRegex *>::iterator, bool> inserted =
			g_regex_cache.insert(std::make_pair(cache_key, compiled));
		if (!inserted.second)
		{
			// Another thread compiled the same expression first; keep the
			// canonical handle so the cache is the only owner.
			delete compiled;
			compiled = inserted.first->second;
		}
	}
	return static_cast<jlong>(reinterpret_cast<intptr_t>(compiled));
}

extern "C" JNIEXPORT void JNICALL Java_java_util_regex_Pattern_nativeFree
	(JNIEnv *env, jclass clazz, jlong handle)
{
	(void)env;
	(void)clazz;
	CompiledRegex *regex = WideRegexHandle(handle);
	if (!regex)
	{
		return;
	}
	// Handles are interned and may be shared with other Pattern objects, so
	// only a handle that never entered the cache can be destroyed here.
	std::lock_guard<std::mutex> lk(g_regex_cache_mutex);
	for (std::map<std::string, CompiledRegex *>::iterator it = g_regex_cache.begin();
	     it != g_regex_cache.end(); ++it)
	{
		if (it->second == regex)
		{
			return;
		}
	}
	delete regex;
}

extern "C" JNIEXPORT jint JNICALL Java_java_util_regex_Pattern_nativeGroupCount
	(JNIEnv *env, jclass clazz, jlong handle)
{
	(void)env;
	(void)clazz;
	CompiledRegex *regex = WideRegexHandle(handle);
	if (!regex)
	{
		return 0;
	}
	return static_cast<jint>(regex->group_count);
}

// Returns {start0,end0,start1,end1,...} in UTF-16 units, or null when the
// pattern does not match.  Unmatched groups are reported as -1,-1.
extern "C" JNIEXPORT jlongArray JNICALL Java_java_util_regex_Pattern_nativeMatch
	(JNIEnv *env, jclass clazz, jlong handle, jstring input, jint start,
	 jboolean anchored)
{
	(void)clazz;
	CompiledRegex *regex = WideRegexHandle(handle);
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
	std::vector<jlong> bounds;
	size_t groups = 0;

	if (regex->std_regex)
	{
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
				text.chars.cend(), match, *regex->std_regex, match_flags);
		}
		catch (const std::regex_error &)
		{
			return NULL;
		}
		if (!found)
		{
			return NULL;
		}
		groups = match.size();
		bounds.assign(groups * 2, -1);
		for (size_t k = 0; k < groups; k++)
		{
			if (!match[k].matched)
			{
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
	}
#ifdef DARKS_HAVE_PCRE2
	else if (regex->pc_code)
	{
		pcre2_match_data *match_data =
			pcre2_match_data_create_from_pattern(regex->pc_code, NULL);
		if (!match_data)
		{
			return NULL;
		}
		uint32_t match_options = anchored ? PCRE2_ANCHORED : 0;
		int rc = pcre2_match(regex->pc_code,
			reinterpret_cast<PCRE2_SPTR>(text.chars.c_str()), text.chars.size(),
			wide_start, match_options, match_data, NULL);
		if (rc < 0)
		{
			pcre2_match_data_free(match_data);
			return NULL;
		}
		PCRE2_SIZE *ovector = pcre2_get_ovector_pointer(match_data);
		groups = static_cast<size_t>(regex->group_count) + 1;
		bounds.assign(groups * 2, -1);
		for (size_t k = 0; k < groups; k++)
		{
			if (static_cast<int>(k) >= rc || ovector[k * 2] == PCRE2_UNSET ||
			    ovector[k * 2 + 1] == PCRE2_UNSET)
			{
				continue;
			}
			size_t wide_begin = static_cast<size_t>(ovector[k * 2]);
			size_t wide_end = static_cast<size_t>(ovector[k * 2 + 1]);
			if (wide_end > text.offsets.size() - 1)
			{
				wide_end = text.offsets.size() - 1;
			}
			bounds[k * 2] = static_cast<jlong>(text.offsets[wide_begin]);
			bounds[k * 2 + 1] = static_cast<jlong>(text.offsets[wide_end]);
		}
		pcre2_match_data_free(match_data);
	}
#endif
	else
	{
		return NULL;
	}
	if (bounds.empty())
	{
		return NULL;
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
