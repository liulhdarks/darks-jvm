package java.io;

public abstract class InputStream
{
    public abstract int read() throws IOException;

    public int read(byte[] buffer) throws IOException
    {
        return read(buffer, 0, buffer.length);
    }

    public int read(byte[] buffer, int offset, int length) throws IOException
    {
        if (buffer == null)
        {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || offset + length > buffer.length)
        {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0)
        {
            return 0;
        }
        int first = read();
        if (first < 0)
        {
            return -1;
        }
        buffer[offset] = (byte)first;
        int count = 1;
        while (count < length)
        {
            int value = read();
            if (value < 0)
            {
                break;
            }
            buffer[offset + count] = (byte)value;
            count = count + 1;
        }
        return count;
    }

    public long skip(long count) throws IOException
    {
        long skipped = 0;
        byte[] buffer = new byte[512];
        while (skipped < count)
        {
            int want = (int)Math.min(buffer.length, count - skipped);
            int read = read(buffer, 0, want);
            if (read < 0)
            {
                break;
            }
            skipped += read;
        }
        return skipped;
    }

    public int available() throws IOException
    {
        return 0;
    }

    public void mark(int limit)
    {
    }

    public void reset() throws IOException
    {
        throw new IOException("mark/reset not supported");
    }

    public boolean markSupported()
    {
        return false;
    }

    public void close() throws IOException
    {
    }
}
