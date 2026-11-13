package java.lang;

public class StringBuilder implements CharSequence
{
    private char[] value;
    private int count;

    public StringBuilder()
    {
        value = new char[16];
        count = 0;
    }

    public StringBuilder(int capacity)
    {
        if (capacity < 0)
        {
            capacity = 0;
        }
        value = new char[capacity];
        count = 0;
    }

    public StringBuilder(String initial)
    {
        value = new char[16];
        count = 0;
        append(initial);
    }

    private void ensure(int minimum)
    {
        if (minimum <= value.length)
        {
            return;
        }
        int next = value.length * 2;
        if (next < minimum)
        {
            next = minimum;
        }
        char[] bigger = new char[next];
        int i = 0;
        while (i < count)
        {
            bigger[i] = value[i];
            i = i + 1;
        }
        value = bigger;
    }

    public int length()
    {
        return count;
    }

    public StringBuilder append(String s)
    {
        if (s == null)
        {
            s = "null";
        }
        int n = s.length();
        ensure(count + n);
        int i = 0;
        while (i < n)
        {
            value[count + i] = s.charAt(i);
            i = i + 1;
        }
        count = count + n;
        return this;
    }

    public StringBuilder append(char c)
    {
        ensure(count + 1);
        value[count] = c;
        count = count + 1;
        return this;
    }

    public StringBuilder append(char[] chars)
    {
        if (chars == null)
        {
            return append("null");
        }
        int n = chars.length;
        ensure(count + n);
        int i = 0;
        while (i < n)
        {
            value[count + i] = chars[i];
            i = i + 1;
        }
        count = count + n;
        return this;
    }

    public StringBuilder append(boolean b)
    {
        return append(String.valueOf(b));
    }

    public StringBuilder append(int v)
    {
        return append(Integer.toString(v));
    }

    public StringBuilder append(long v)
    {
        return append(Long.toString(v));
    }

    public StringBuilder append(double v)
    {
        return append(Double.toString(v));
    }

    public StringBuilder append(Object o)
    {
        return append(String.valueOf(o));
    }

    public StringBuilder append(float v)
    {
        return append(new Float(v).toString());
    }

    public StringBuilder append(char[] chars, int offset, int length)
    {
        if (chars == null)
        {
            return append("null");
        }
        if (offset < 0 || length < 0 || offset + length > chars.length)
        {
            throw new StringIndexOutOfBoundsException(offset);
        }
        int i = 0;
        while (i < length)
        {
            append(chars[offset + i]);
            i = i + 1;
        }
        return this;
    }

    public int capacity()
    {
        return value.length;
    }

    public void ensureCapacity(int minimum)
    {
        ensure(minimum);
    }

    public char charAt(int index)
    {
        if (index < 0 || index >= count)
        {
            throw new StringIndexOutOfBoundsException(index);
        }
        return value[index];
    }

    public void setCharAt(int index, char c)
    {
        if (index < 0 || index >= count)
        {
            throw new StringIndexOutOfBoundsException(index);
        }
        value[index] = c;
    }

    public void setLength(int newLength)
    {
        if (newLength < 0)
        {
            throw new StringIndexOutOfBoundsException(newLength);
        }
        ensure(newLength);
        if (newLength > count)
        {
            int i = count;
            while (i < newLength)
            {
                value[i] = 0;
                i = i + 1;
            }
        }
        count = newLength;
    }

    public StringBuilder insert(int offset, String s)
    {
        if (offset < 0 || offset > count)
        {
            throw new StringIndexOutOfBoundsException(offset);
        }
        if (s == null)
        {
            s = "null";
        }
        int n = s.length();
        ensure(count + n);
        int i = count - 1;
        while (i >= offset)
        {
            value[i + n] = value[i];
            i = i - 1;
        }
        i = 0;
        while (i < n)
        {
            value[offset + i] = s.charAt(i);
            i = i + 1;
        }
        count = count + n;
        return this;
    }

    public StringBuilder insert(int offset, char c)
    {
        return insert(offset, String.valueOf(c));
    }

    public StringBuilder insert(int offset, int v)
    {
        return insert(offset, Integer.toString(v));
    }

    public StringBuilder insert(int offset, long v)
    {
        return insert(offset, Long.toString(v));
    }

    public StringBuilder insert(int offset, double v)
    {
        return insert(offset, Double.toString(v));
    }

    public StringBuilder insert(int offset, boolean v)
    {
        return insert(offset, String.valueOf(v));
    }

    public StringBuilder deleteCharAt(int index)
    {
        if (index < 0 || index >= count)
        {
            throw new StringIndexOutOfBoundsException(index);
        }
        int i = index;
        while (i < count - 1)
        {
            value[i] = value[i + 1];
            i = i + 1;
        }
        count = count - 1;
        return this;
    }

    public StringBuilder delete(int start, int end)
    {
        if (start < 0 || start > count || start > end)
        {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (end > count)
        {
            end = count;
        }
        int remove = end - start;
        int i = start;
        while (i + remove < count)
        {
            value[i] = value[i + remove];
            i = i + 1;
        }
        count = count - remove;
        return this;
    }

    public StringBuilder replace(int start, int end, String replacement)
    {
        if (start < 0 || start > count || start > end)
        {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (end > count)
        {
            end = count;
        }
        delete(start, end);
        insert(start, replacement == null ? "null" : replacement);
        return this;
    }

    public int indexOf(String text)
    {
        if (text == null)
        {
            return -1;
        }
        int n = text.length();
        if (n == 0)
        {
            return 0;
        }
        int i = 0;
        while (i + n <= count)
        {
            int j = 0;
            while (j < n && value[i + j] == text.charAt(j))
            {
                j = j + 1;
            }
            if (j == n)
            {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    public StringBuilder reverse()
    {
        int i = 0;
        int j = count - 1;
        while (i < j)
        {
            char tmp = value[i];
            value[i] = value[j];
            value[j] = tmp;
            i = i + 1;
            j = j - 1;
        }
        return this;
    }

    public CharSequence subSequence(int start, int end)
    {
        return substring(start, end);
    }

    public String substring(int start)
    {
        return substring(start, count);
    }

    public String substring(int start, int end)
    {
        if (start < 0 || end > count || start > end)
        {
            throw new StringIndexOutOfBoundsException(start);
        }
        char[] result = new char[end - start];
        int i = 0;
        while (i < result.length)
        {
            result[i] = value[start + i];
            i = i + 1;
        }
        return new String(result);
    }

    public String toString()
    {
        char[] result = new char[count];
        int i = 0;
        while (i < count)
        {
            result[i] = value[i];
            i = i + 1;
        }
        return new String(result);
    }
}
