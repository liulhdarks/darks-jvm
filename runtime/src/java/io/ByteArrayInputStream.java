package java.io;

public class ByteArrayInputStream extends InputStream
{
    private byte[] data;
    private int position;
    private int limit;

    public ByteArrayInputStream(byte[] data)
    {
        this(data, 0, data.length);
    }

    public ByteArrayInputStream(byte[] data, int offset, int length)
    {
        this.data = data;
        this.position = offset;
        this.limit = offset + length;
    }

    public int read()
    {
        if (position >= limit)
        {
            return -1;
        }
        return data[position++] & 0xFF;
    }

    public int read(byte[] buffer, int offset, int length)
    {
        if (position >= limit)
        {
            return -1;
        }
        int count = limit - position;
        if (count > length)
        {
            count = length;
        }
        int i = 0;
        while (i < count)
        {
            buffer[offset + i] = data[position + i];
            i = i + 1;
        }
        position = position + count;
        return count;
    }

    public int available()
    {
        return limit - position;
    }
}
