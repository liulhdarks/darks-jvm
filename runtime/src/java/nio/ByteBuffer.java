package java.nio;

/**
 * Minimal heap byte buffer.  Only the byte[] backed operations used by the
 * runtime and Spring's conversion service are provided.
 */
public class ByteBuffer extends Buffer
{
    private final byte[] data;
    private final int offset;

    private ByteBuffer(byte[] data, int offset, int capacity)
    {
        super(capacity);
        this.data = data;
        this.offset = offset;
    }

    public ByteBuffer position(int newPosition)
    {
        super.position(newPosition);
        return this;
    }

    public ByteBuffer limit(int newLimit)
    {
        super.limit(newLimit);
        return this;
    }

    public ByteBuffer mark()
    {
        super.mark();
        return this;
    }

    public ByteBuffer reset()
    {
        super.reset();
        return this;
    }

    public ByteBuffer clear()
    {
        super.clear();
        return this;
    }

    public ByteBuffer flip()
    {
        super.flip();
        return this;
    }

    public ByteBuffer rewind()
    {
        super.rewind();
        return this;
    }

    public static ByteBuffer allocate(int capacity)
    {
        if (capacity < 0)
        {
            throw new IllegalArgumentException("negative capacity");
        }
        return new ByteBuffer(new byte[capacity], 0, capacity);
    }

    public static ByteBuffer allocateDirect(int capacity)
    {
        return allocate(capacity);
    }

    public static ByteBuffer wrap(byte[] array)
    {
        return wrap(array, 0, array.length);
    }

    public static ByteBuffer wrap(byte[] array, int offset, int length)
    {
        if (offset < 0 || length < 0 || offset + length > array.length)
        {
            throw new IndexOutOfBoundsException();
        }
        ByteBuffer buffer = new ByteBuffer(array, 0, array.length);
        buffer.position = offset;
        buffer.limit = offset + length;
        buffer.mark = -1;
        return buffer;
    }

    public ByteBuffer duplicate()
    {
        ByteBuffer copy = new ByteBuffer(data, offset, capacity);
        copy.position = position;
        copy.limit = limit;
        copy.mark = mark;
        return copy;
    }

    public ByteBuffer slice()
    {
        int size = remaining();
        ByteBuffer copy = new ByteBuffer(data, offset + position, size);
        copy.position = 0;
        copy.limit = size;
        return copy;
    }

    public ByteBuffer asReadOnlyBuffer()
    {
        return duplicate();
    }

    public boolean isReadOnly()
    {
        return false;
    }

    public boolean hasArray()
    {
        return true;
    }

    public byte[] array()
    {
        return data;
    }

    public int arrayOffset()
    {
        return offset;
    }

    public byte get()
    {
        checkBounds(position, 1);
        return data[offset + position++];
    }

    public ByteBuffer get(byte[] target)
    {
        return get(target, 0, target.length);
    }

    public ByteBuffer get(byte[] target, int targetOffset, int length)
    {
        checkBounds(position, length);
        if (targetOffset < 0 || length > target.length - targetOffset)
        {
            throw new IndexOutOfBoundsException();
        }
        System.arraycopy(data, offset + position, target, targetOffset, length);
        position += length;
        return this;
    }

    public byte get(int index)
    {
        checkBounds(index, 1);
        return data[offset + index];
    }

    public ByteBuffer put(byte value)
    {
        checkBounds(position, 1);
        data[offset + position++] = value;
        return this;
    }

    public ByteBuffer put(byte[] source)
    {
        return put(source, 0, source.length);
    }

    public ByteBuffer put(byte[] source, int sourceOffset, int length)
    {
        checkBounds(position, length);
        if (sourceOffset < 0 || length > source.length - sourceOffset)
        {
            throw new IndexOutOfBoundsException();
        }
        System.arraycopy(source, sourceOffset, data, offset + position, length);
        position += length;
        return this;
    }

    public ByteBuffer put(int index, byte value)
    {
        checkBounds(index, 1);
        data[offset + index] = value;
        return this;
    }

    public ByteBuffer put(ByteBuffer source)
    {
        int length = source.remaining();
        checkBounds(position, length);
        source.get(data, offset + position, length);
        position += length;
        return this;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ByteBuffer))
        {
            return false;
        }
        ByteBuffer o = (ByteBuffer)other;
        if (remaining() != o.remaining())
        {
            return false;
        }
        for (int i = 0; i < remaining(); i++)
        {
            if (get(position + i) != o.get(o.position + i))
            {
                return false;
            }
        }
        return true;
    }

    public int hashCode()
    {
        int hash = 1;
        for (int i = position; i < limit; i++)
        {
            hash = 31 * hash + get(i);
        }
        return hash;
    }

    public String toString()
    {
        return "java.nio.HeapByteBuffer[pos=" + position + " lim=" + limit +
            " cap=" + capacity + "]";
    }
}
