package java.nio;

public class CharBuffer extends Buffer implements Comparable<CharBuffer>, CharSequence
{
    final char[] chars;
    final int offset;

    CharBuffer(int capacity, int offset, char[] chars)
    {
        super(capacity);
        this.offset = offset;
        this.chars = chars;
    }

    public static CharBuffer allocate(int capacity)
    {
        if (capacity < 0)
        {
            throw new IllegalArgumentException("capacity");
        }
        return new CharBuffer(capacity, 0, new char[capacity]);
    }

    public static CharBuffer wrap(char[] array)
    {
        return new CharBuffer(array.length, 0, array);
    }

    public static CharBuffer wrap(char[] array, int offset, int length)
    {
        if (offset < 0 || length < 0 || offset + length > array.length)
        {
            throw new IndexOutOfBoundsException();
        }
        CharBuffer buffer = new CharBuffer(length, offset, array);
        buffer.limit = length;
        return buffer;
    }

    public static CharBuffer wrap(String text)
    {
        return wrap(text.toCharArray());
    }

    public CharBuffer position(int newPosition)
    {
        super.position(newPosition);
        return this;
    }

    public CharBuffer limit(int newLimit)
    {
        super.limit(newLimit);
        return this;
    }

    public CharBuffer mark()
    {
        super.mark();
        return this;
    }

    public CharBuffer reset()
    {
        super.reset();
        return this;
    }

    public CharBuffer clear()
    {
        super.clear();
        return this;
    }

    public CharBuffer flip()
    {
        super.flip();
        return this;
    }

    public CharBuffer rewind()
    {
        super.rewind();
        return this;
    }

    public char get()
    {
        if (position >= limit)
        {
            throw new java.nio.BufferUnderflowException();
        }
        return chars[offset + position++];
    }

    public char get(int index)
    {
        if (index < 0 || index >= limit)
        {
            throw new IndexOutOfBoundsException();
        }
        return chars[offset + index];
    }

    public CharBuffer get(char[] target)
    {
        return get(target, 0, target.length);
    }

    public CharBuffer get(char[] target, int targetOffset, int length)
    {
        if (length > remaining())
        {
            throw new java.nio.BufferUnderflowException();
        }
        int i = 0;
        while (i < length)
        {
            target[targetOffset + i] = chars[offset + position + i];
            i = i + 1;
        }
        position = position + length;
        return this;
    }

    public CharBuffer put(char value)
    {
        if (!hasRemaining())
        {
            throw new java.nio.BufferOverflowException();
        }
        chars[offset + position++] = value;
        return this;
    }

    public CharBuffer put(int index, char value)
    {
        if (index < 0 || index >= limit)
        {
            throw new IndexOutOfBoundsException();
        }
        chars[offset + index] = value;
        return this;
    }

    public CharBuffer put(char[] source)
    {
        return put(source, 0, source.length);
    }

    public CharBuffer put(char[] source, int sourceOffset, int length)
    {
        if (length > remaining())
        {
            throw new java.nio.BufferOverflowException();
        }
        int i = 0;
        while (i < length)
        {
            chars[offset + position + i] = source[sourceOffset + i];
            i = i + 1;
        }
        position = position + length;
        return this;
    }

    public CharBuffer put(String text)
    {
        return put(text.toCharArray());
    }

    public CharBuffer put(String text, int start, int end)
    {
        if (start < 0 || end < start || end > text.length())
        {
            throw new IndexOutOfBoundsException("range");
        }
        put(text.substring(start, end).toCharArray());
        return this;
    }

    public CharBuffer put(CharBuffer source)
    {
        if (source == this)
        {
            throw new IllegalArgumentException();
        }
        int length = source.remaining();
        if (length > remaining())
        {
            throw new java.nio.BufferOverflowException();
        }
        int i = 0;
        while (i < length)
        {
            put(source.get());
            i = i + 1;
        }
        return this;
    }

    public boolean isReadOnly()
    {
        return false;
    }

    public boolean hasArray()
    {
        return true;
    }

    public char[] array()
    {
        return chars;
    }

    public int arrayOffset()
    {
        return offset;
    }

    public CharBuffer duplicate()
    {
        CharBuffer copy = new CharBuffer(capacity, offset, chars);
        copy.position = position;
        copy.limit = limit;
        return copy;
    }

    public CharBuffer slice()
    {
        CharBuffer copy = new CharBuffer(remaining(), offset + position, chars);
        return copy;
    }

    public int compareTo(CharBuffer other)
    {
        int left = remaining();
        int right = other.remaining();
        int n = left < right ? left : right;
        int i = 0;
        while (i < n)
        {
            char a = get(position + i);
            char b = other.get(other.position + i);
            if (a != b)
            {
                return a < b ? -1 : 1;
            }
            i = i + 1;
        }
        return left - right;
    }

    public int length()
    {
        return remaining();
    }

    public char charAt(int index)
    {
        if (index < 0 || index >= remaining())
        {
            throw new IndexOutOfBoundsException("index");
        }
        return get(position() + index);
    }

    public CharSequence subSequence(int start, int end)
    {
        if (start < 0 || end < start || end > remaining())
        {
            throw new IndexOutOfBoundsException("range");
        }
        CharBuffer slice = duplicate();
        slice.position(position() + start);
        slice.limit(position() + end);
        CharBuffer result = CharBuffer.allocate(end - start);
        result.put(slice);
        result.flip();
        return result;
    }

    public String toString()
    {
        return new String(chars, offset + position, remaining());
    }
}
