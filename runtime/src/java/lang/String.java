package java.lang;

public final class String implements Comparable<String>, CharSequence, java.io.Serializable
{
    private char[] value;

    public static final java.util.Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();

    private static final class CaseInsensitiveComparator implements java.util.Comparator<String>
    {
        public int compare(String first, String second)
        {
            if (first == null)
            {
                return second == null ? 0 : -1;
            }
            if (second == null)
            {
                return 1;
            }
            return first.compareToIgnoreCase(second);
        }
    }

    public String()
    {
        value = new char[0];
    }

    public String(char[] chars)
    {
        value = chars;
    }

    public String(char[] chars, int offset, int count)
    {
        if (offset < 0 || count < 0 || offset + count > chars.length)
        {
            throw new StringIndexOutOfBoundsException(offset);
        }
        value = new char[count];
        int i = 0;
        while (i < count)
        {
            value[i] = chars[offset + i];
            i = i + 1;
        }
    }

    public String(String other)
    {
        value = other.value;
    }

    public String(byte[] bytes)
    {
        value = decodeUtf8(bytes, 0, bytes.length);
    }

    public String(byte[] bytes, int offset, int length)
    {
        if (offset < 0 || length < 0 || offset + length > bytes.length)
        {
            throw new StringIndexOutOfBoundsException(offset);
        }
        value = decodeUtf8(bytes, offset, length);
    }

    public String(byte[] bytes, java.nio.charset.Charset charset)
    {
        if (charset == null)
        {
            throw new NullPointerException("charset");
        }
        value = charset.decode(bytes).toCharArray();
    }

    public String(byte[] bytes, int offset, int length,
                  java.nio.charset.Charset charset)
    {
        if (charset == null)
        {
            throw new NullPointerException("charset");
        }
        value = charset.decode(bytes, offset, length).toCharArray();
    }

    public String(byte[] bytes, String charsetName)
        throws java.io.UnsupportedEncodingException
    {
        value = charsetForName(charsetName).decode(bytes).toCharArray();
    }

    public String(byte[] bytes, int offset, int length, String charsetName)
        throws java.io.UnsupportedEncodingException
    {
        value = charsetForName(charsetName).decode(bytes, offset, length)
            .toCharArray();
    }

    // Deprecated JDK 1.1 byte-to-char constructors, needed by the ported
    // java.io.ByteArrayOutputStream.toString(int).  The JDK implementation
    // special-cases a zero high byte into a Latin-1 backing array; this
    // runtime always builds a char[], which is behaviourally equivalent.
    @Deprecated
    public String(byte[] ascii, int hibyte)
    {
        this(ascii, hibyte, 0, ascii.length);
    }

    @Deprecated
    public String(byte[] ascii, int hibyte, int offset, int count)
    {
        this(hibyteChars(ascii, hibyte, offset, count));
    }

    private static char[] hibyteChars(byte[] ascii, int hibyte, int offset,
                                      int count)
    {
        if (ascii == null)
        {
            throw new NullPointerException("ascii");
        }
        if (offset < 0 || count < 0 || offset > ascii.length - count)
        {
            throw new StringIndexOutOfBoundsException(offset);
        }
        char[] chars = new char[count];
        int high = (hibyte & 0xFF) << 8;
        int i = 0;
        while (i < count)
        {
            chars[i] = (char)(high | (ascii[offset + i] & 0xFF));
            i = i + 1;
        }
        return chars;
    }

    private static java.nio.charset.Charset charsetForName(String name)
        throws java.io.UnsupportedEncodingException
    {
        if (name == null)
        {
            throw new NullPointerException("charsetName");
        }
        try
        {
            return java.nio.charset.Charset.forName(name);
        }
        catch (java.nio.charset.UnsupportedCharsetException unsupported)
        {
            throw new java.io.UnsupportedEncodingException(name);
        }
    }

    public int length()
    {
        return value.length;
    }

    public boolean isEmpty()
    {
        return value.length == 0;
    }

    public char charAt(int index)
    {
        if (index < 0 || index >= value.length)
        {
            throw new StringIndexOutOfBoundsException(index);
        }
        return value[index];
    }

    public String toString()
    {
        return this;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof String))
        {
            return false;
        }
        String s = (String)other;
        if (s.value.length != value.length)
        {
            return false;
        }
        int i = 0;
        while (i < value.length)
        {
            if (value[i] != s.value[i])
            {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public int compareTo(String other)
    {
        int n = value.length < other.value.length ? value.length : other.value.length;
        int i = 0;
        while (i < n)
        {
            char a = value[i];
            char b = other.value[i];
            if (a != b)
            {
                return a - b;
            }
            i = i + 1;
        }
        return value.length - other.value.length;
    }

    public boolean equalsIgnoreCase(String other)
    {
        if (other == null || other.value.length != value.length)
        {
            return false;
        }
        int i = 0;
        while (i < value.length)
        {
            char a = value[i];
            char b = other.value[i];
            if (a != b)
            {
                if (upper(a) != upper(b))
                {
                    return false;
                }
            }
            i = i + 1;
        }
        return true;
    }

    private static char upper(char c)
    {
        if (c >= 'a' && c <= 'z')
        {
            return (char)(c - 32);
        }
        return c;
    }

    private static char lower(char c)
    {
        if (c >= 'A' && c <= 'Z')
        {
            return (char)(c + 32);
        }
        return c;
    }

    public int indexOf(int ch)
    {
        return indexOf(ch, 0);
    }

    public int indexOf(int ch, int from)
    {
        if (from < 0)
        {
            from = 0;
        }
        char c = (char)ch;
        int i = from;
        while (i < value.length)
        {
            if (value[i] == c)
            {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    public int indexOf(String s)
    {
        return indexOf(s, 0);
    }

    public int indexOf(String s, int from)
    {
        if (s == null)
        {
            return -1;
        }
        if (from < 0)
        {
            from = 0;
        }
        if (s.value.length == 0)
        {
            return from <= value.length ? from : value.length;
        }
        int i = from;
        while (i + s.value.length <= value.length)
        {
            int j = 0;
            boolean same = true;
            while (j < s.value.length)
            {
                if (value[i + j] != s.value[j])
                {
                    same = false;
                    break;
                }
                j = j + 1;
            }
            if (same)
            {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    public int lastIndexOf(int ch)
    {
        return lastIndexOf(ch, value.length - 1);
    }

    public int lastIndexOf(int ch, int from)
    {
        if (from >= value.length)
        {
            from = value.length - 1;
        }
        char c = (char)ch;
        int i = from;
        while (i >= 0)
        {
            if (value[i] == c)
            {
                return i;
            }
            i = i - 1;
        }
        return -1;
    }

    public int lastIndexOf(String s)
    {
        return lastIndexOf(s, value.length);
    }

    public int lastIndexOf(String s, int from)
    {
        if (s == null)
        {
            return -1;
        }
        if (s.value.length == 0)
        {
            return from < value.length ? from : value.length;
        }
        int i = from;
        if (i > value.length - s.value.length)
        {
            i = value.length - s.value.length;
        }
        while (i >= 0)
        {
            int j = 0;
            boolean same = true;
            while (j < s.value.length)
            {
                if (value[i + j] != s.value[j])
                {
                    same = false;
                    break;
                }
                j = j + 1;
            }
            if (same)
            {
                return i;
            }
            i = i - 1;
        }
        return -1;
    }

    public boolean contains(String s)
    {
        return indexOf(s) >= 0;
    }

    public boolean contains(CharSequence s)
    {
        if (s == null)
        {
            throw new NullPointerException("s");
        }
        return indexOf(s.toString()) >= 0;
    }

    public CharSequence subSequence(int begin, int end)
    {
        return substring(begin, end);
    }

    public String substring(int begin)
    {
        return substring(begin, value.length);
    }

    public String substring(int begin, int end)
    {
        if (begin < 0 || end > value.length || begin > end)
        {
            throw new StringIndexOutOfBoundsException(begin);
        }
        char[] result = new char[end - begin];
        int i = 0;
        while (i < result.length)
        {
            result[i] = value[begin + i];
            i = i + 1;
        }
        return new String(result);
    }

    public String toUpperCase()
    {
        char[] result = new char[value.length];
        int i = 0;
        while (i < value.length)
        {
            result[i] = upper(value[i]);
            i = i + 1;
        }
        return new String(result);
    }

    public int codePointAt(int index)
    {
        if (index < 0 || index >= value.length)
        {
            throw new StringIndexOutOfBoundsException(index);
        }
        char first = value[index];
        if (first >= 0xD800 && first <= 0xDBFF && index + 1 < value.length)
        {
            char second = value[index + 1];
            if (second >= 0xDC00 && second <= 0xDFFF)
            {
                return 0x10000 + ((first - 0xD800) << 10) + (second - 0xDC00);
            }
        }
        return first;
    }

    public java.util.stream.IntStream chars()
    {
        int[] result = new int[value.length];
        for (int i = 0; i < value.length; i++)
        {
            result[i] = value[i];
        }
        return java.util.stream.IntStream.of(result);
    }

    public java.util.stream.IntStream codePoints()
    {
        int[] result = new int[value.length];
        int count = 0;
        int i = 0;
        while (i < value.length)
        {
            int codePoint = codePointAt(i);
            result[count++] = codePoint;
            i += java.lang.Character.charCount(codePoint);
        }
        int[] trimmed = new int[count];
        for (int j = 0; j < count; j++)
        {
            trimmed[j] = result[j];
        }
        return java.util.stream.IntStream.of(trimmed);
    }

    public String toUpperCase(java.util.Locale locale)
    {
        return toUpperCase();
    }

    public String toLowerCase()
    {
        char[] result = new char[value.length];
        int i = 0;
        while (i < value.length)
        {
            result[i] = lower(value[i]);
            i = i + 1;
        }
        return new String(result);
    }

    public String toLowerCase(java.util.Locale locale)
    {
        return toLowerCase();
    }

    public String trim()
    {
        int begin = 0;
        int end = value.length;
        while (begin < end && value[begin] <= ' ')
        {
            begin = begin + 1;
        }
        while (end > begin && value[end - 1] <= ' ')
        {
            end = end - 1;
        }
        if (begin == 0 && end == value.length)
        {
            return this;
        }
        return substring(begin, end);
    }

    public String concat(String s)
    {
        char[] result = new char[value.length + s.value.length];
        int i = 0;
        while (i < value.length)
        {
            result[i] = value[i];
            i = i + 1;
        }
        int j = 0;
        while (j < s.value.length)
        {
            result[i + j] = s.value[j];
            j = j + 1;
        }
        return new String(result);
    }

    public boolean regionMatches(int toffset, String other, int ooffset, int length)
    {
        return regionMatches(false, toffset, other, ooffset, length);
    }

    public boolean regionMatches(boolean ignoreCase, int toffset, String other,
                                 int ooffset, int length)
    {
        if (other == null)
        {
            throw new NullPointerException("other");
        }
        if (toffset < 0 || ooffset < 0)
        {
            return false;
        }
        if (toffset + length > value.length || ooffset + length > other.value.length)
        {
            return false;
        }
        for (int i = 0; i < length; i++)
        {
            char a = value[toffset + i];
            char b = other.value[ooffset + i];
            if (a == b)
            {
                continue;
            }
            if (!ignoreCase)
            {
                return false;
            }
            char upperA = Character.toUpperCase(a);
            char upperB = Character.toUpperCase(b);
            if (upperA == upperB)
            {
                continue;
            }
            if (Character.toLowerCase(upperA) != Character.toLowerCase(upperB))
            {
                return false;
            }
        }
        return true;
    }

    public boolean startsWith(String s)
    {
        return startsWith(s, 0);
    }

    public boolean startsWith(String s, int offset)
    {
        if (s == null || offset < 0 || offset + s.value.length > value.length)
        {
            return false;
        }
        int i = 0;
        while (i < s.value.length)
        {
            if (value[offset + i] != s.value[i])
            {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public boolean endsWith(String s)
    {
        if (s == null || s.value.length > value.length)
        {
            return false;
        }
        return startsWith(s, value.length - s.value.length);
    }

    public String replace(char oldChar, char newChar)
    {
        char[] result = new char[value.length];
        int i = 0;
        while (i < value.length)
        {
            result[i] = value[i] == oldChar ? newChar : value[i];
            i = i + 1;
        }
        return new String(result);
    }

    public String replace(CharSequence target, CharSequence replacement)
    {
        if (target == null || replacement == null)
        {
            throw new NullPointerException();
        }
        String from = target.toString();
        String to = replacement.toString();
        if (from.length() == 0)
        {
            return this;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < value.length)
        {
            if (i + from.length() <= value.length && regionMatches(i, from))
            {
                out.append(to);
                i = i + from.length();
            }
            else
            {
                out.append(value[i]);
                i = i + 1;
            }
        }
        return out.toString();
    }

    private boolean regionMatches(int offset, String other)
    {
        int i = 0;
        while (i < other.value.length)
        {
            if (value[offset + i] != other.value[i])
            {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // A separator without regular-expression metacharacters keeps the simple
    // literal path; everything else goes through java.util.regex.
    public String[] split(String separator)
    {
        return split(separator, 0);
    }

    public String[] split(String separator, int limit)
    {
        if (separator == null)
        {
            throw new NullPointerException();
        }
        if (!isLiteralSeparator(separator))
        {
            return java.util.regex.Pattern.compile(separator).split(this, limit);
        }
        if (separator.length() == 0)
        {
            return splitEmpty(limit);
        }
        int parts = 1;
        int i = 0;
        while ((limit <= 0 || parts < limit) &&
               i + separator.length() <= value.length)
        {
            if (regionMatches(i, separator))
            {
                parts = parts + 1;
                i = i + separator.length();
            }
            else
            {
                i = i + 1;
            }
        }
        int[] starts = new int[parts];
        int[] ends = new int[parts];
        int count = 0;
        int start = 0;
        i = 0;
        while (count < parts - 1 && i + separator.length() <= value.length)
        {
            if (regionMatches(i, separator))
            {
                starts[count] = start;
                ends[count] = i;
                count = count + 1;
                i = i + separator.length();
                start = i;
            }
            else
            {
                i = i + 1;
            }
        }
        starts[count] = start;
        ends[count] = value.length;
        count = count + 1;
        if (limit == 0)
        {
            while (count > 1 && starts[count - 1] == ends[count - 1])
            {
                count = count - 1;
            }
        }
        String[] result = new String[count];
        i = 0;
        while (i < count)
        {
            result[i] = substring(starts[i], ends[i]);
            i = i + 1;
        }
        return result;
    }

    private String[] splitEmpty(int limit)
    {
        if (limit == 1)
        {
            String[] single = new String[1];
            single[0] = this;
            return single;
        }
        if (value.length == 0)
        {
            String[] empty = new String[1];
            empty[0] = "";
            return empty;
        }
        if (limit > 0)
        {
            int head = value.length < limit - 1 ? value.length : limit - 1;
            String[] result = new String[head + 1];
            int i = 0;
            while (i < head)
            {
                result[i] = substring(i, i + 1);
                i = i + 1;
            }
            result[head] = substring(head);
            return result;
        }
        String[] result = new String[value.length];
        int i = 0;
        while (i < value.length)
        {
            result[i] = substring(i, i + 1);
            i = i + 1;
        }
        return result;
    }

    private static boolean isLiteralSeparator(String separator)
    {
        int i = 0;
        while (i < separator.length())
        {
            char c = separator.charAt(i);
            if (c == '.' || c == '[' || c == ']' || c == '{' || c == '}' ||
                c == '(' || c == ')' || c == '*' || c == '+' || c == '-' ||
                c == '?' || c == '^' || c == '$' || c == '|' || c == '\\')
            {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin)
    {
        if (srcBegin < 0 || srcEnd > value.length || srcBegin > srcEnd)
        {
            throw new StringIndexOutOfBoundsException("range");
        }
        if (dstBegin < 0 || dstBegin + (srcEnd - srcBegin) > dst.length)
        {
            throw new ArrayIndexOutOfBoundsException("dstBegin");
        }
        int i = srcBegin;
        int j = dstBegin;
        while (i < srcEnd)
        {
            dst[j] = value[i];
            i = i + 1;
            j = j + 1;
        }
    }

    public char[] toCharArray()
    {
        char[] result = new char[value.length];
        int i = 0;
        while (i < value.length)
        {
            result[i] = value[i];
            i = i + 1;
        }
        return result;
    }

    public byte[] getBytes()
    {
        return encodeUtf8(value);
    }

    public byte[] getBytes(java.nio.charset.Charset charset)
    {
        if (charset == null)
        {
            throw new NullPointerException("charset");
        }
        return charset.encodeBytes(this);
    }

    public byte[] getBytes(String charsetName)
        throws java.io.UnsupportedEncodingException
    {
        return charsetForName(charsetName).encodeBytes(this);
    }

    private static byte[] encodeUtf8(char[] chars)
    {
        byte[] out = new byte[chars.length > 0 ? chars.length : 1];
        int count = 0;
        int i = 0;
        while (i < chars.length)
        {
            int cp = chars[i];
            i = i + 1;
            if (cp >= 0xD800 && cp <= 0xDBFF && i < chars.length &&
                chars[i] >= 0xDC00 && chars[i] <= 0xDFFF)
            {
                cp = 0x10000 + ((cp - 0xD800) << 10) + (chars[i] - 0xDC00);
                i = i + 1;
            }
            else if (cp >= 0xD800 && cp <= 0xDFFF)
            {
                cp = 0xFFFD;
            }
            if (count + 4 > out.length)
            {
                out = grow(out, count + 4);
            }
            if (cp < 0x80)
            {
                out[count++] = (byte)cp;
            }
            else if (cp < 0x800)
            {
                out[count++] = (byte)(0xC0 | (cp >> 6));
                out[count++] = (byte)(0x80 | (cp & 0x3F));
            }
            else if (cp < 0x10000)
            {
                out[count++] = (byte)(0xE0 | (cp >> 12));
                out[count++] = (byte)(0x80 | ((cp >> 6) & 0x3F));
                out[count++] = (byte)(0x80 | (cp & 0x3F));
            }
            else
            {
                out[count++] = (byte)(0xF0 | (cp >> 18));
                out[count++] = (byte)(0x80 | ((cp >> 12) & 0x3F));
                out[count++] = (byte)(0x80 | ((cp >> 6) & 0x3F));
                out[count++] = (byte)(0x80 | (cp & 0x3F));
            }
        }
        byte[] result = new byte[count];
        i = 0;
        while (i < count)
        {
            result[i] = out[i];
            i = i + 1;
        }
        return result;
    }

    private static byte[] grow(byte[] old, int minimum)
    {
        int size = old.length * 2;
        if (size < minimum)
        {
            size = minimum;
        }
        byte[] bigger = new byte[size];
        int i = 0;
        while (i < old.length)
        {
            bigger[i] = old[i];
            i = i + 1;
        }
        return bigger;
    }

    private static char[] decodeUtf8(byte[] bytes, int offset, int length)
    {
        char[] out = new char[length > 0 ? length : 1];
        int count = 0;
        int i = offset;
        int end = offset + length;
        while (i < end)
        {
            int b = bytes[i++] & 0xFF;
            int cp;
            if (b < 0x80)
            {
                cp = b;
            }
            else if ((b & 0xE0) == 0xC0)
            {
                cp = b & 0x1F;
                if (i < end)
                {
                    cp = (cp << 6) | (bytes[i++] & 0x3F);
                }
                else
                {
                    cp = 0xFFFD;
                }
            }
            else if ((b & 0xF0) == 0xE0)
            {
                cp = b & 0x0F;
                int k = 0;
                while (k < 2 && i < end)
                {
                    cp = (cp << 6) | (bytes[i++] & 0x3F);
                    k = k + 1;
                }
                if (k < 2)
                {
                    cp = 0xFFFD;
                }
            }
            else if ((b & 0xF8) == 0xF0)
            {
                cp = b & 0x07;
                int k = 0;
                while (k < 3 && i < end)
                {
                    cp = (cp << 6) | (bytes[i++] & 0x3F);
                    k = k + 1;
                }
                if (k < 3 || cp > 0x10FFFF)
                {
                    cp = 0xFFFD;
                }
            }
            else
            {
                cp = 0xFFFD;
            }
            if (count + 2 > out.length)
            {
                out = growChars(out, count + 2);
            }
            if (cp >= 0x10000)
            {
                cp = cp - 0x10000;
                out[count++] = (char)(0xD800 + (cp >> 10));
                out[count++] = (char)(0xDC00 + (cp & 0x3FF));
            }
            else
            {
                out[count++] = (char)cp;
            }
        }
        char[] result = new char[count];
        i = 0;
        while (i < count)
        {
            result[i] = out[i];
            i = i + 1;
        }
        return result;
    }

    private static char[] growChars(char[] old, int minimum)
    {
        int size = old.length * 2;
        if (size < minimum)
        {
            size = minimum;
        }
        char[] bigger = new char[size];
        int i = 0;
        while (i < old.length)
        {
            bigger[i] = old[i];
            i = i + 1;
        }
        return bigger;
    }

    public int hashCode()
    {
        int h = 0;
        int i = 0;
        while (i < value.length)
        {
            h = 31 * h + value[i];
            i = i + 1;
        }
        return h;
    }

    public String intern()
    {
        return this;
    }

    public static String valueOf(Object o)
    {
        if (o == null)
        {
            return "null";
        }
        return o.toString();
    }

    public static String valueOf(String s)
    {
        return s;
    }

    public static String valueOf(char c)
    {
        char[] chars = new char[1];
        chars[0] = c;
        return new String(chars);
    }

    public static String valueOf(boolean b)
    {
        return b ? "true" : "false";
    }

    public static String valueOf(int v)
    {
        return Integer.toString(v);
    }

    public static String valueOf(long v)
    {
        return Long.toString(v);
    }

    public static String valueOf(float v)
    {
        return Float.toString(v);
    }

    public static String valueOf(double v)
    {
        return Double.toString(v);
    }

    public static String valueOf(char[] chars)
    {
        return new String(chars);
    }

    public static String valueOf(char[] chars, int offset, int count)
    {
        return new String(chars, offset, count);
    }

    // The JDK formats according to the locale; this engine always uses '.'
    // as decimal separator and ',' as grouping separator (see below).
    public static String format(java.util.Locale locale, String format, Object... args)
    {
        return format(format, args);
    }

    // printf-compatible formatting.
    //
    // The JDK implements this in java.util.Formatter, which relies on the
    // locale-provider SPI and on jdk.internal.math's floating point helpers;
    // neither is available in this VM.  The engine below implements the
    // conversions used by the runtime and by the applications ported to it:
    // b/B, h/H, s/S, c/C, d, o, x/X, e/E, f, g/G, a/A, t/T, % and n, with
    // positional indexes ("%2$s"), the '<' previous-argument flag and the
    // '-', '+', ' ', '0', ',', '(' and '#' flags, width and precision.
    //
    // Simplifications, all deliberate:
    //   * Locale-sensitive output is fixed to '.' as decimal separator and
    //     ',' as grouping separator.
    //   * Floating point conversions format the shortest decimal
    //     representation returned by Double.toString(), not the exact binary
    //     expansion, so values whose shortest representation is not the
    //     correctly rounded one can round differently in the last digit.
    //   * %a ignores precision and always produces the minimal significand.
    //   * %s treats java.util.Formattable arguments as plain objects.
    //   * Date/time conversions use the default time zone and English
    //     month/day names.
    public static String format(String format, Object... args)
    {
        if (format == null)
        {
            throw new NullPointerException("format");
        }
        Object[] values = args == null ? new Object[0] : args;
        StringBuilder out = new StringBuilder(format.length() + 16);
        int next = 0;
        int last = -1;
        int i = 0;
        int length = format.length();
        while (i < length)
        {
            char ch = format.charAt(i++);
            if (ch != '%')
            {
                out.append(ch);
                continue;
            }
            int start = i - 1;
            int explicitIndex = -1;
            int digitsEnd = i;
            while (digitsEnd < length && format.charAt(digitsEnd) >= '0'
                   && format.charAt(digitsEnd) <= '9')
            {
                digitsEnd++;
            }
            if (digitsEnd > i && digitsEnd < length
                && format.charAt(digitsEnd) == '$')
            {
                explicitIndex = Integer.parseInt(format.substring(i, digitsEnd)) - 1;
                if (explicitIndex < 0)
                {
                    throw new java.util.MissingFormatArgumentException(
                        format.substring(start, digitsEnd + 1));
                }
                i = digitsEnd + 1;
            }
            boolean left = false;
            boolean plus = false;
            boolean space = false;
            boolean zero = false;
            boolean comma = false;
            boolean paren = false;
            boolean alternate = false;
            boolean previous = false;
            while (i < length)
            {
                char flag = format.charAt(i);
                if (flag == '-') { left = true; }
                else if (flag == '+') { plus = true; }
                else if (flag == ' ') { space = true; }
                else if (flag == '0') { zero = true; }
                else if (flag == ',') { comma = true; }
                else if (flag == '(') { paren = true; }
                else if (flag == '#') { alternate = true; }
                else if (flag == '<') { previous = true; }
                else { break; }
                i++;
            }
            int width = -1;
            int widthStart = i;
            while (i < length && format.charAt(i) >= '0' && format.charAt(i) <= '9')
            {
                i++;
            }
            if (i > widthStart)
            {
                width = Integer.parseInt(format.substring(widthStart, i));
            }
            int precision = -1;
            if (i < length && format.charAt(i) == '.')
            {
                i++;
                int precisionStart = i;
                while (i < length && format.charAt(i) >= '0' && format.charAt(i) <= '9')
                {
                    i++;
                }
                precision = i > precisionStart
                    ? Integer.parseInt(format.substring(precisionStart, i)) : 0;
            }
            if (i >= length)
            {
                throw new java.util.UnknownFormatConversionException("%");
            }
            char conversion = format.charAt(i++);
            if (conversion == '%')
            {
                out.append('%');
                continue;
            }
            if (conversion == 'n')
            {
                out.append(System.lineSeparator());
                continue;
            }
            char dateSuffix = 0;
            if (conversion == 't' || conversion == 'T')
            {
                if (i >= length)
                {
                    throw new java.util.UnknownFormatConversionException("t");
                }
                dateSuffix = format.charAt(i++);
            }
            String spec = format.substring(start, i);
            int argIndex;
            if (previous)
            {
                if (last < 0)
                {
                    throw new java.util.MissingFormatArgumentException(spec);
                }
                argIndex = last;
            }
            else if (explicitIndex >= 0)
            {
                argIndex = explicitIndex;
            }
            else
            {
                argIndex = next++;
            }
            last = argIndex;
            if (argIndex >= values.length)
            {
                throw new java.util.MissingFormatArgumentException(spec);
            }
            Object value = values[argIndex];
            if (dateSuffix != 0)
            {
                out.append(justify(formatDateTime(value, dateSuffix), width, left));
                continue;
            }
            if (conversion == 'd')
            {
                long number = asLong(value, 'd');
                boolean negative = number < 0;
                String digits = Long.toString(number);
                if (negative)
                {
                    digits = digits.substring(1);
                }
                if (comma)
                {
                    digits = group(digits);
                }
                String prefix = "";
                String suffix = "";
                if (negative)
                {
                    if (paren)
                    {
                        prefix = "(";
                        suffix = ")";
                    }
                    else
                    {
                        prefix = "-";
                    }
                }
                else if (plus)
                {
                    prefix = "+";
                }
                else if (space)
                {
                    prefix = " ";
                }
                out.append(justify(prefix, digits + suffix, width, left, zero && suffix.isEmpty()));
                continue;
            }
            if (conversion == 'o' || conversion == 'x' || conversion == 'X')
            {
                long number = asLong(value, conversion);
                String digits = conversion == 'o'
                    ? Long.toOctalString(number) : Long.toHexString(number);
                if (conversion == 'X')
                {
                    digits = digits.toUpperCase();
                }
                String prefix = "";
                if (alternate)
                {
                    prefix = conversion == 'o' ? "0" : (conversion == 'X' ? "0X" : "0x");
                }
                out.append(justify(prefix, digits, width, left, zero));
                continue;
            }
            if (conversion == 's' || conversion == 'S')
            {
                String text = String.valueOf(value);
                if (precision >= 0 && text.length() > precision)
                {
                    text = text.substring(0, precision);
                }
                if (conversion == 'S')
                {
                    text = text.toUpperCase();
                }
                out.append(justify(text, width, left));
                continue;
            }
            if (conversion == 'b' || conversion == 'B')
            {
                String text;
                if (value == null)
                {
                    text = "false";
                }
                else if (value instanceof Boolean)
                {
                    text = value.toString();
                }
                else
                {
                    text = "true";
                }
                if (conversion == 'B')
                {
                    text = text.toUpperCase();
                }
                out.append(justify(text, width, left));
                continue;
            }
            if (conversion == 'h' || conversion == 'H')
            {
                String text = value == null ? "null"
                    : Integer.toHexString(String.valueOf(value).hashCode());
                if (conversion == 'H')
                {
                    text = text.toUpperCase();
                }
                out.append(justify(text, width, left));
                continue;
            }
            if (conversion == 'c' || conversion == 'C')
            {
                char c;
                if (value instanceof Character)
                {
                    c = ((Character) value).charValue();
                }
                else
                {
                    int codePoint = (int) asLong(value, 'c');
                    if (codePoint < 0 || codePoint > 0x10ffff)
                    {
                        throw new java.util.IllegalFormatCodePointException(codePoint);
                    }
                    char[] chars = Character.toChars(codePoint);
                    if (chars.length == 1)
                    {
                        c = chars[0];
                    }
                    else
                    {
                        String text = new String(chars);
                        if (conversion == 'C')
                        {
                            text = text.toUpperCase();
                        }
                        out.append(justify(text, width, left));
                        continue;
                    }
                }
                String text = String.valueOf(c);
                if (conversion == 'C')
                {
                    text = text.toUpperCase();
                }
                out.append(justify(text, width, left));
                continue;
            }
            if (conversion == 'f' || conversion == 'e' || conversion == 'E'
                || conversion == 'g' || conversion == 'G' || conversion == 'a'
                || conversion == 'A')
            {
                double number = asDouble(value, conversion);
                if (Double.isNaN(number) || Double.isInfinite(number))
                {
                    out.append(justify(floatingSpecial(number), width, left));
                    continue;
                }
                boolean negative = number < 0 || (number == 0.0 && 1 / number < 0);
                double absolute = negative ? -number : number;
                String body;
                if (conversion == 'f')
                {
                    body = plainDecimalText(absolute, precision < 0 ? 6 : precision);
                    if (comma)
                    {
                        body = groupDecimal(body);
                    }
                }
                else if (conversion == 'e' || conversion == 'E')
                {
                    body = scientificText(absolute, precision < 0 ? 6 : precision,
                        conversion == 'E');
                }
                else if (conversion == 'g' || conversion == 'G')
                {
                    body = generalText(absolute, precision < 0 ? 6 : precision,
                        conversion == 'G');
                }
                else
                {
                    body = hexFloatText(absolute, conversion == 'A');
                }
                String prefix = "";
                if (negative)
                {
                    if (paren)
                    {
                        prefix = "(";
                        body = body + ")";
                    }
                    else
                    {
                        prefix = "-";
                    }
                }
                else if (plus)
                {
                    prefix = "+";
                }
                else if (space)
                {
                    prefix = " ";
                }
                out.append(justify(prefix, body, width, left, zero && !paren));
                continue;
            }
            throw new java.util.UnknownFormatConversionException(String.valueOf(conversion));
        }
        return out.toString();
    }

    private static long asLong(Object value, char conversion)
    {
        if (value instanceof Long) { return ((Long) value).longValue(); }
        if (value instanceof Integer) { return ((Integer) value).longValue(); }
        if (value instanceof Short) { return ((Short) value).longValue(); }
        if (value instanceof Byte) { return ((Byte) value).longValue(); }
        throw new java.util.IllegalFormatConversionException(conversion,
            value == null ? Object.class : value.getClass());
    }

    private static double asDouble(Object value, char conversion)
    {
        if (value instanceof Double) { return ((Double) value).doubleValue(); }
        if (value instanceof Float) { return ((Float) value).doubleValue(); }
        if (value instanceof Long) { return ((Long) value).doubleValue(); }
        if (value instanceof Integer) { return ((Integer) value).doubleValue(); }
        if (value instanceof Short) { return ((Short) value).doubleValue(); }
        if (value instanceof Byte) { return ((Byte) value).doubleValue(); }
        if (value instanceof Number) { return ((Number) value).doubleValue(); }
        throw new java.util.IllegalFormatConversionException(conversion,
            value == null ? Object.class : value.getClass());
    }

    private static String justify(String prefix, String body, int width,
                                  boolean left, boolean zero)
    {
        int total = prefix.length() + body.length();
        if (width <= total)
        {
            return prefix + body;
        }
        int pad = width - total;
        StringBuilder out = new StringBuilder(width + 8);
        if (left)
        {
            out.append(prefix).append(body);
            while (pad-- > 0)
            {
                out.append(' ');
            }
        }
        else if (zero)
        {
            out.append(prefix);
            while (pad-- > 0)
            {
                out.append('0');
            }
            out.append(body);
        }
        else
        {
            while (pad-- > 0)
            {
                out.append(' ');
            }
            out.append(prefix).append(body);
        }
        return out.toString();
    }

    private static String justify(String text, int width, boolean left)
    {
        return justify("", text, width, left, false);
    }

    private static String group(String digits)
    {
        if (digits.length() <= 3)
        {
            return digits;
        }
        StringBuilder out = new StringBuilder(digits.length() + digits.length() / 3);
        int head = digits.length() % 3;
        if (head == 0)
        {
            head = 3;
        }
        out.append(digits.substring(0, head));
        for (int p = head; p < digits.length(); p = p + 3)
        {
            out.append(',');
            out.append(digits.substring(p, p + 3));
        }
        return out.toString();
    }

    private static String groupDecimal(String text)
    {
        int dot = text.indexOf('.');
        if (dot < 0)
        {
            return group(text);
        }
        return group(text.substring(0, dot)) + text.substring(dot);
    }

    private static String floatingSpecial(double value)
    {
        if (Double.isNaN(value))
        {
            return "NaN";
        }
        return value < 0 ? "-Infinity" : "Infinity";
    }

    // Decimal digits of Double.toString(value) and the position of the decimal
    // point inside them: digits before point[0] are integral.
    private static String decimalDigits(double value, int[] point)
    {
        String text = Double.toString(value);
        int exponent = 0;
        int e = text.indexOf('E');
        if (e >= 0)
        {
            exponent = Integer.parseInt(text.substring(e + 1));
            text = text.substring(0, e);
        }
        int dot = text.indexOf('.');
        StringBuilder digits = new StringBuilder(text.length());
        if (dot < 0)
        {
            digits.append(text);
            dot = text.length();
        }
        else
        {
            digits.append(text.substring(0, dot));
            digits.append(text.substring(dot + 1));
        }
        point[0] = dot + exponent;
        return digits.toString();
    }

    private static String plainDecimalText(double value, int precision)
    {
        int[] point = new int[1];
        String digits = decimalDigits(value, point);
        int dot = point[0];
        StringBuilder integer = new StringBuilder();
        if (dot <= 0)
        {
            integer.append('0');
        }
        else
        {
            for (int k = 0; k < dot; k++)
            {
                integer.append(k < digits.length() ? digits.charAt(k) : '0');
            }
        }
        StringBuilder fraction = new StringBuilder();
        for (int k = 0; k < precision; k++)
        {
            int index = dot + k;
            fraction.append(index >= 0 && index < digits.length()
                ? digits.charAt(index) : '0');
        }
        int integerLength = integer.length();
        char[] buffer = (integer.toString() + fraction.toString()).toCharArray();
        int round = dot + precision;
        if (round >= 0 && round < digits.length() && digits.charAt(round) >= '5')
        {
            int k = buffer.length - 1;
            while (k >= 0)
            {
                if (buffer[k] == '9')
                {
                    buffer[k] = '0';
                    k--;
                }
                else
                {
                    buffer[k] = (char) (buffer[k] + 1);
                    break;
                }
            }
            if (k < 0)
            {
                char[] grown = new char[buffer.length + 1];
                grown[0] = '1';
                System.arraycopy(buffer, 0, grown, 1, buffer.length);
                buffer = grown;
                integerLength++;
            }
        }
        int first = 0;
        while (first < integerLength - 1 && buffer[first] == '0')
        {
            first++;
        }
        String integerText = new String(buffer, first, integerLength - first);
        if (precision <= 0)
        {
            return integerText;
        }
        return integerText + "." + new String(buffer, integerLength, precision);
    }

    private static String scientificText(double value, int precision, boolean upper)
    {
        int[] point = new int[1];
        String digits = decimalDigits(value, point);
        int first = 0;
        while (first < digits.length() && digits.charAt(first) == '0')
        {
            first++;
        }
        if (first == digits.length())
        {
            first = digits.length() - 1;
        }
        int exponent = point[0] - first - 1;
        StringBuilder significant = new StringBuilder(precision + 1);
        for (int k = 0; k <= precision; k++)
        {
            int index = first + k;
            significant.append(index < digits.length() ? digits.charAt(index) : '0');
        }
        int round = first + precision + 1;
        if (round < digits.length() && digits.charAt(round) >= '5')
        {
            int k = significant.length() - 1;
            while (k >= 0)
            {
                if (significant.charAt(k) == '9')
                {
                    significant.setCharAt(k, '0');
                    k--;
                }
                else
                {
                    significant.setCharAt(k, (char) (significant.charAt(k) + 1));
                    break;
                }
            }
            if (k < 0)
            {
                significant.insert(0, '1');
                significant.setLength(precision + 1);
                exponent++;
            }
        }
        StringBuilder out = new StringBuilder(precision + 8);
        out.append(significant.charAt(0));
        if (precision > 0)
        {
            out.append('.');
            out.append(significant.substring(1));
        }
        out.append(upper ? 'E' : 'e');
        out.append(exponent < 0 ? '-' : '+');
        int magnitude = exponent < 0 ? -exponent : exponent;
        if (magnitude < 10)
        {
            out.append('0');
        }
        out.append(magnitude);
        return out.toString();
    }

    private static String generalText(double value, int precision, boolean upper)
    {
        if (precision == 0)
        {
            precision = 1;
        }
        int[] point = new int[1];
        String digits = decimalDigits(value, point);
        int first = 0;
        while (first < digits.length() && digits.charAt(first) == '0')
        {
            first++;
        }
        int exponent = first == digits.length() ? 0 : point[0] - first - 1;
        String text;
        if (exponent < -4 || exponent >= precision)
        {
            text = scientificText(value, precision - 1, upper);
        }
        else
        {
            int fraction = precision - 1 - exponent;
            if (fraction < 0)
            {
                fraction = 0;
            }
            text = plainDecimalText(value, fraction);
        }
        int e = text.indexOf(upper ? 'E' : 'e');
        String mantissa = e < 0 ? text : text.substring(0, e);
        String tail = e < 0 ? "" : text.substring(e);
        if (mantissa.indexOf('.') >= 0)
        {
            int end = mantissa.length();
            while (end > 0 && mantissa.charAt(end - 1) == '0')
            {
                end--;
            }
            if (end > 0 && mantissa.charAt(end - 1) == '.')
            {
                end--;
            }
            mantissa = mantissa.substring(0, end);
        }
        return mantissa + tail;
    }

    private static String hexFloatText(double value, boolean upper)
    {
        if (value == 0.0)
        {
            return upper ? "0X0.0P0" : "0x0.0p0";
        }
        long bits = Double.doubleToLongBits(value);
        int rawExponent = (int) ((bits >> 52) & 0x7ff);
        String mantissa = Long.toHexString(bits & 0xfffffffffffffL);
        while (mantissa.length() < 13)
        {
            mantissa = "0" + mantissa;
        }
        int end = mantissa.length();
        while (end > 1 && mantissa.charAt(end - 1) == '0')
        {
            end--;
        }
        mantissa = mantissa.substring(0, end);
        if (upper)
        {
            mantissa = mantissa.toUpperCase();
        }
        int exponent = rawExponent == 0 ? -1022 : rawExponent - 1023;
        String prefix = rawExponent == 0 ? "0" : "1";
        String marker = upper ? "0X" : "0x";
        String exponentMarker = upper ? "P" : "p";
        return marker + prefix + "." + mantissa + exponentMarker + exponent;
    }

    private static String formatDateTime(Object value, char suffix)
    {
        java.util.Calendar calendar;
        if (value instanceof java.util.Calendar)
        {
            calendar = (java.util.Calendar) value;
        }
        else
        {
            long millis;
            if (value instanceof java.util.Date)
            {
                millis = ((java.util.Date) value).getTime();
            }
            else if (value instanceof Long)
            {
                millis = ((Long) value).longValue();
            }
            else if (value instanceof Integer)
            {
                millis = ((Integer) value).longValue();
            }
            else
            {
                throw new java.util.IllegalFormatConversionException('t',
                    value == null ? Object.class : value.getClass());
            }
            calendar = java.util.Calendar.getInstance();
            calendar.setTimeInMillis(millis);
        }
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH) + 1;
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = calendar.get(java.util.Calendar.MINUTE);
        int second = calendar.get(java.util.Calendar.SECOND);
        int millis = calendar.get(java.util.Calendar.MILLISECOND);
        String[] months = { "January", "February", "March", "April", "May",
            "June", "July", "August", "September", "October", "November",
            "December" };
        String[] days = { "Sunday", "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday" };
        switch (suffix)
        {
            case 'Y': return padNumber(year, 4);
            case 'y': return padNumber(year % 100, 2);
            case 'm': return padNumber(month, 2);
            case 'd': return padNumber(day, 2);
            case 'e': return Integer.toString(day);
            case 'H': return padNumber(hour, 2);
            case 'k': return Integer.toString(hour);
            case 'I': return padNumber(hour % 12 == 0 ? 12 : hour % 12, 2);
            case 'l': return Integer.toString(hour % 12 == 0 ? 12 : hour % 12);
            case 'M': return padNumber(minute, 2);
            case 'S': return padNumber(second, 2);
            case 'L': return padNumber(millis, 3);
            case 'N': return padNumber(millis * 1000000, 9);
            case 'p': return hour < 12 ? "am" : "pm";
            case 'B': return months[month - 1];
            case 'b': return months[month - 1].substring(0, 3);
            case 'A': return days[calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1];
            case 'a': return days[calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1].substring(0, 3);
            case 'j': return padNumber(calendar.get(java.util.Calendar.DAY_OF_YEAR), 3);
            case 's': return Long.toString(calendar.getTimeInMillis() / 1000L);
            case 'Q': return Long.toString(calendar.getTimeInMillis());
            case 'D': return padNumber(month, 2) + "/" + padNumber(day, 2) + "/"
                + padNumber(year % 100, 2);
            case 'F': return padNumber(year, 4) + "-" + padNumber(month, 2) + "-"
                + padNumber(day, 2);
            case 'T': return padNumber(hour, 2) + ":" + padNumber(minute, 2) + ":"
                + padNumber(second, 2);
            case 'R': return padNumber(hour, 2) + ":" + padNumber(minute, 2);
            case 'r': return padNumber(hour % 12 == 0 ? 12 : hour % 12, 2) + ":"
                + padNumber(minute, 2) + ":" + padNumber(second, 2) + " "
                + (hour < 12 ? "am" : "pm");
            default:
                throw new java.util.UnknownFormatConversionException("t" + suffix);
        }
    }

    private static String padNumber(long value, int width)
    {
        String text = Long.toString(value);
        while (text.length() < width)
        {
            text = "0" + text;
        }
        return text;
    }

    public boolean matches(String regex)
    {
        return java.util.regex.Pattern.matches(regex, this);
    }

    public String(StringBuilder builder)
    {
        if (builder == null)
        {
            throw new NullPointerException("builder");
        }
        this.value = builder.toString().toCharArray();
    }

    public String(StringBuffer buffer)
    {
        if (buffer == null)
        {
            throw new NullPointerException("buffer");
        }
        this.value = buffer.toString().toCharArray();
    }

    public String(int[] codePoints, int offset, int count)
    {
        if (offset < 0 || count < 0 || offset + count > codePoints.length)
        {
            throw new IndexOutOfBoundsException();
        }
        int length = 0;
        for (int i = 0; i < count; i++)
        {
            int cp = codePoints[offset + i];
            length += cp > 0xffff ? 2 : 1;
        }
        char[] chars = new char[length];
        int pos = 0;
        for (int i = 0; i < count; i++)
        {
            int cp = codePoints[offset + i];
            if (cp > 0xffff)
            {
                cp -= 0x10000;
                chars[pos++] = (char)(0xd800 + (cp >> 10));
                chars[pos++] = (char)(0xdc00 + (cp & 0x3ff));
            }
            else
            {
                chars[pos++] = (char)cp;
            }
        }
        this.value = chars;
    }

    public int codePointCount(int begin, int end)
    {
        if (begin < 0 || end > value.length || begin > end)
        {
            throw new IndexOutOfBoundsException();
        }
        int count = 0;
        for (int i = begin; i < end; i++)
        {
            char c = value[i];
            if (c >= 0xd800 && c <= 0xdbff && i + 1 < end)
            {
                char next = value[i + 1];
                if (next >= 0xdc00 && next <= 0xdfff)
                {
                    i++;
                }
            }
            count++;
        }
        return count;
    }

    public int compareToIgnoreCase(String other)
    {
        int length = this.value.length < other.value.length
            ? this.value.length : other.value.length;
        for (int i = 0; i < length; i++)
        {
            char a = Character.toLowerCase(this.value[i]);
            char b = Character.toLowerCase(other.value[i]);
            if (a != b)
            {
                return a - b;
            }
        }
        return this.value.length - other.value.length;
    }

    public boolean contentEquals(CharSequence other)
    {
        if (other == null)
        {
            return false;
        }
        if (other.length() != value.length)
        {
            return false;
        }
        for (int i = 0; i < value.length; i++)
        {
            if (value[i] != other.charAt(i))
            {
                return false;
            }
        }
        return true;
    }

    public String replaceAll(String regex, String replacement)
    {
        return java.util.regex.Pattern.compile(regex).matcher(this)
            .replaceAll(replacement);
    }

    public String replaceFirst(String regex, String replacement)
    {
        return java.util.regex.Pattern.compile(regex).matcher(this)
            .replaceFirst(replacement);
    }

    public static String join(CharSequence delimiter, CharSequence... elements)
    {
        if (delimiter == null || elements == null)
        {
            throw new NullPointerException();
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < elements.length; i++)
        {
            if (i > 0)
            {
                out.append(delimiter.toString());
            }
            out.append(elements[i] == null ? "null" : elements[i].toString());
        }
        return out.toString();
    }

    public static String join(CharSequence delimiter,
        Iterable<? extends CharSequence> elements)
    {
        if (delimiter == null || elements == null)
        {
            throw new NullPointerException();
        }
        StringBuilder out = new StringBuilder();
        java.util.Iterator<? extends CharSequence> it = elements.iterator();
        boolean first = true;
        while (it.hasNext())
        {
            if (!first)
            {
                out.append(delimiter.toString());
            }
            CharSequence item = it.next();
            out.append(item == null ? "null" : item.toString());
            first = false;
        }
        return out.toString();
    }
}
