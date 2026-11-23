package java.io;

public class BufferedInputStream extends FilterInputStream
{
    private final byte[] buffer;
    private int position;
    private int limit;

    public BufferedInputStream(InputStream in)
    {
        this(in, 8192);
    }

    public BufferedInputStream(InputStream in, int size)
    {
        super(in);
        if (size <= 0)
        {
            throw new IllegalArgumentException("size");
        }
        this.buffer = new byte[size];
    }

    private int fill() throws IOException
    {
        limit = in.read(buffer, 0, buffer.length);
        position = 0;
        return limit;
    }

    public int read() throws IOException
    {
        if (position >= limit && fill() < 0)
        {
            return -1;
        }
        return buffer[position++] & 0xff;
    }

    public int read(byte[] target, int offset, int length) throws IOException
    {
        if (length == 0)
        {
            return 0;
        }
        int available = limit - position;
        if (available <= 0)
        {
            if (length >= buffer.length)
            {
                return in.read(target, offset, length);
            }
            if (fill() < 0)
            {
                return -1;
            }
            available = limit - position;
        }
        int count = available < length ? available : length;
        System.arraycopy(buffer, position, target, offset, count);
        position += count;
        return count;
    }

    public long skip(long count) throws IOException
    {
        long skipped = 0;
        while (skipped < count)
        {
            int available = limit - position;
            if (available <= 0)
            {
                if (fill() < 0)
                {
                    break;
                }
                available = limit - position;
            }
            long take = count - skipped;
            if (take > available)
            {
                take = available;
            }
            position += (int)take;
            skipped += take;
        }
        return skipped;
    }

    public int available() throws IOException
    {
        return (limit - position) + in.available();
    }

    public void close() throws IOException
    {
        in.close();
        position = 0;
        limit = 0;
    }
}
