package java.lang;

public final class String implements Comparable<String>, CharSequence
{
    private char[] value;

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

    // The separator is matched literally (not as a regular expression); this
    // is the documented boundary of the small runtime.  A trailing empty
    // field is dropped, matching String.split's common case.
    public String[] split(String separator)
    {
        if (separator == null)
        {
            throw new NullPointerException();
        }
        if (separator.length() == 0)
        {
            throw new IllegalArgumentException("empty separator");
        }
        int parts = 1;
        int i = 0;
        while (i + separator.length() <= value.length)
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
        while (i + separator.length() <= value.length)
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
        if (count > 1 && starts[count - 1] == ends[count - 1])
        {
            count = count - 1;
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

    // A deliberately small printf: %s %d %f %x %X %c %b %%, optional '-'/'0'
    // flags and a width/precision for %s and numeric conversions.
    public static String format(String format, Object... args)
    {
        if (format == null)
        {
            throw new NullPointerException("format");
        }
        StringBuilder out = new StringBuilder();
        int arg = 0;
        int i = 0;
        while (i < format.length())
        {
            char c = format.charAt(i++);
            if (c != '%')
            {
                out.append(c);
                continue;
            }
            if (i < format.length() && format.charAt(i) == '%')
            {
                out.append('%');
                i = i + 1;
                continue;
            }
            boolean left = false;
            boolean zero = false;
            while (i < format.length() &&
                   (format.charAt(i) == '-' || format.charAt(i) == '0'))
            {
                if (format.charAt(i) == '-')
                {
                    left = true;
                }
                else
                {
                    zero = true;
                }
                i = i + 1;
            }
            int width = 0;
            while (i < format.length() && format.charAt(i) >= '0' && format.charAt(i) <= '9')
            {
                width = width * 10 + (format.charAt(i) - '0');
                i = i + 1;
            }
            int precision = -1;
            if (i < format.length() && format.charAt(i) == '.')
            {
                i = i + 1;
                precision = 0;
                while (i < format.length() && format.charAt(i) >= '0' && format.charAt(i) <= '9')
                {
                    precision = precision * 10 + (format.charAt(i) - '0');
                    i = i + 1;
                }
            }
            if (i >= format.length())
            {
                out.append('%');
                break;
            }
            char conv = format.charAt(i++);
            Object value = arg < args.length ? args[arg] : null;
            arg = arg + 1;
            String text;
            if (conv == 's')
            {
                text = String.valueOf(value);
                if (precision >= 0 && text.length() > precision)
                {
                    text = text.substring(0, precision);
                }
            }
            else if (conv == 'd')
            {
                long n = value instanceof Long ? ((Long)value).longValue()
                    : (long)((Integer)value).intValue();
                text = Long.toString(n);
            }
            else if (conv == 'f')
            {
                double d = value instanceof Double ? ((Double)value).doubleValue()
                    : (double)((Integer)value).intValue();
                text = Double.toString(d);
                if (precision >= 0)
                {
                    int dot = text.indexOf('.');
                    if (dot < 0)
                    {
                        text = text + ".";
                        dot = text.length() - 1;
                    }
                    while (text.length() - dot - 1 < precision)
                    {
                        text = text + "0";
                    }
                    if (text.length() - dot - 1 > precision)
                    {
                        text = text.substring(0, dot + 1 + precision);
                    }
                }
            }
            else if (conv == 'x')
            {
                text = Integer.toHexString(value instanceof Integer
                    ? ((Integer)value).intValue() : ((Long)value).intValue());
            }
            else if (conv == 'X')
            {
                text = Integer.toHexString(value instanceof Integer
                    ? ((Integer)value).intValue() : ((Long)value).intValue()).toUpperCase();
            }
            else if (conv == 'c')
            {
                text = String.valueOf((char)(value instanceof Character
                    ? ((Character)value).charValue() : 0));
            }
            else if (conv == 'b')
            {
                text = value instanceof Boolean ? ((Boolean)value).toString()
                    : String.valueOf(value != null);
            }
            else
            {
                text = String.valueOf(value);
            }
            if (width > text.length())
            {
                StringBuilder padded = new StringBuilder();
                int pad = width - text.length();
                int k = 0;
                while (k < pad)
                {
                    padded.append(zero && !left ? '0' : ' ');
                    k = k + 1;
                }
                text = left ? text + padded.toString() : padded.toString() + text;
            }
            out.append(text);
        }
        return out.toString();
    }

    // Full regular expressions are outside this runtime; refusing explicitly
    // is better than silently mis-matching.
    public boolean matches(String regex)
    {
        throw new UnsupportedOperationException(
            "String.matches: regular expressions are not supported");
    }
}
