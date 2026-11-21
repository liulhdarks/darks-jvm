package java.util.zip;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InflaterInputStream extends FilterInputStream
{
    protected Inflater inf;
    protected byte[] buf;
    protected int len;
    private boolean closed;

    public InflaterInputStream(InputStream input, Inflater inflater, int size)
    {
        super(input);
        if (input == null || inflater == null)
        {
            throw new NullPointerException();
        }
        if (size <= 0)
        {
            throw new IllegalArgumentException("buffer size <= 0");
        }
        this.inf = inflater;
        this.buf = new byte[size];
    }

    public InflaterInputStream(InputStream input, Inflater inflater)
    {
        this(input, inflater, 512);
    }

    public InflaterInputStream(InputStream input)
    {
        this(input, new Inflater());
    }

    public int read() throws IOException
    {
        byte[] one = new byte[1];
        int read = read(one, 0, 1);
        return read == -1 ? -1 : one[0] & 0xff;
    }

    public int read(byte[] target, int offset, int length) throws IOException
    {
        if (closed)
        {
            throw new IOException("stream closed");
        }
        if (target == null)
        {
            throw new NullPointerException("target");
        }
        if (offset < 0 || length < 0 || offset + length > target.length)
        {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0)
        {
            return 0;
        }
        try
        {
            while (true)
            {
                int produced = inf.inflate(target, offset, length);
                if (produced > 0)
                {
                    return produced;
                }
                if (inf.finished() || inf.needsDictionary())
                {
                    return -1;
                }
                if (inf.needsInput())
                {
                    fill();
                }
            }
        }
        catch (DataFormatException e)
        {
            throw new ZipException(e.getMessage());
        }
    }

    public int available() throws IOException
    {
        if (closed)
        {
            throw new IOException("stream closed");
        }
        return inf.finished() ? 0 : 1;
    }

    public void close() throws IOException
    {
        if (!closed)
        {
            closed = true;
            inf.end();
            super.close();
        }
    }

    protected void fill() throws IOException
    {
        int read = in.read(buf, 0, buf.length);
        if (read == -1)
        {
            throw new EOFException("Unexpected end of ZLIB input stream");
        }
        len = read;
        inf.setInput(buf, 0, read);
    }
}
