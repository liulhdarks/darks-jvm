package java.lang;

public final class String implements Comparable<String>
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

    public boolean contains(String s)
    {
        return indexOf(s) >= 0;
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
        if (s.value.length > value.length)
        {
            return false;
        }
        int i = 0;
        while (i < s.value.length)
        {
            if (value[i] != s.value[i])
            {
                return false;
            }
            i = i + 1;
        }
        return true;
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

    public static String valueOf(double v)
    {
        return Double.toString(v);
    }

    public static String valueOf(char[] chars)
    {
        return new String(chars);
    }
}
