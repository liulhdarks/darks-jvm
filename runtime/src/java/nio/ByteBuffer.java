package java.nio;

public class ByteBuffer extends Buffer
{
    private final byte[] data;
    private final int offset;

    private ByteBuffer(byte[] data, int offset, int length)
    {
        super(length);
        this.data = data;
        this.offset = offset;
    }

    public static ByteBuffer allocate(int capacity)
    {
        if (capacity < 0)
        {
            throw new IllegalArgumentException("capacity " + capacity);
        }
        return new ByteBuffer(new byte[capacity], 0, capacity);
    }

    public static ByteBuffer wrap(byte[] array)
    {
        if (array == null)
        {
            throw new NullPointerException("array");
        }
        return new ByteBuffer(array, 0, array.length);
    }

    public static ByteBuffer wrap(byte[] array, int offset, int length)
    {
        if (array == null)
        {
            throw new NullPointerException("array");
        }
        if (offset < 0 || length < 0 || offset + length > array.length)
        {
            throw new IndexOutOfBoundsException();
        }
        return new ByteBuffer(array, offset, length);
    }

    public ByteBuffer duplicate()
    {
        ByteBuffer copy = new ByteBuffer(data, offset, capacity);
        copy.position = position;
        copy.limit = limit;
        return copy;
    }

    public ByteBuffer slice()
    {
        ByteBuffer copy = new ByteBuffer(data, offset + position, remaining());
        return copy;
    }

    public byte[] array()
    {
        return data;
    }

    public int arrayOffset()
    {
        return offset;
    }

    private int checkedIndex()
    {
        if (position >= limit)
        {
            throw new IndexOutOfBoundsException("position " + position);
        }
        return offset + position;
    }

    public byte get()
    {
        int index = checkedIndex();
        position++;
        return data[index];
    }

    public ByteBuffer get(byte[] dst)
    {
        if (dst.length > remaining())
        {
            throw new IndexOutOfBoundsException();
        }
        int index = offset + position;
        for (int i = 0; i < dst.length; i++)
        {
            dst[i] = data[index + i];
        }
        position += dst.length;
        return this;
    }

    public byte get(int index)
    {
        if (index < 0 || index >= limit)
        {
            throw new IndexOutOfBoundsException("index " + index);
        }
        return data[offset + index];
    }

    public ByteBuffer put(byte value)
    {
        int index = offset + position;
        if (position >= limit)
        {
            throw new IndexOutOfBoundsException("position " + position);
        }
        data[index] = value;
        position++;
        return this;
    }

    public ByteBuffer put(byte[] src)
    {
        if (src.length > remaining())
        {
            throw new IndexOutOfBoundsException();
        }
        int index = offset + position;
        for (int i = 0; i < src.length; i++)
        {
            data[index + i] = src[i];
        }
        position += src.length;
        return this;
    }

    public ByteBuffer put(int index, byte value)
    {
        if (index < 0 || index >= limit)
        {
            throw new IndexOutOfBoundsException("index " + index);
        }
        data[offset + index] = value;
        return this;
    }

    public String toString()
    {
        return "java.nio.HeapByteBuffer[pos=" + position + " lim=" + limit
            + " cap=" + capacity + "]";
    }
}
