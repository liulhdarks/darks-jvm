package java.io;

public class FilterInputStream extends InputStream
{
    protected volatile InputStream in;

    protected FilterInputStream(InputStream in)
    {
        this.in = in;
    }

    public int read() throws IOException
    {
        return in.read();
    }

    public int read(byte[] buffer, int offset, int length) throws IOException
    {
        return in.read(buffer, offset, length);
    }

    public int read(byte[] buffer) throws IOException
    {
        return in.read(buffer);
    }

    public long skip(long count) throws IOException
    {
        return in.skip(count);
    }

    public int available() throws IOException
    {
        return in.available();
    }

    public void close() throws IOException
    {
        in.close();
    }

    public synchronized void mark(int limit)
    {
        in.mark(limit);
    }

    public synchronized void reset() throws IOException
    {
        in.reset();
    }

    public boolean markSupported()
    {
        return in.markSupported();
    }
}
