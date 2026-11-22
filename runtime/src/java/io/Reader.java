package java.io;

public abstract class Reader
{
    public abstract int read() throws IOException;

    public int read(char[] buffer) throws IOException
    {
        return read(buffer, 0, buffer.length);
    }

    public int read(char[] buffer, int offset, int length) throws IOException
    {
        int count = 0;
        while (count < length)
        {
            int value = read();
            if (value < 0)
            {
                break;
            }
            buffer[offset + count] = (char)value;
            count = count + 1;
        }
        return count == 0 ? -1 : count;
    }

    public boolean markSupported()
    {
        return false;
    }

    public void mark(int readAheadLimit) throws IOException
    {
        throw new IOException("mark not supported");
    }

    public void reset() throws IOException
    {
        throw new IOException("reset not supported");
    }

    public long skip(long count) throws IOException
    {
        long skipped = 0;
        while (skipped < count)
        {
            if (read() < 0)
            {
                break;
            }
            skipped = skipped + 1;
        }
        return skipped;
    }

    public boolean ready() throws IOException
    {
        return false;
    }

    public void close() throws IOException
    {
    }
}
