package java.io;

public class FilterOutputStream extends OutputStream
{
    protected OutputStream out;

    protected FilterOutputStream(OutputStream out)
    {
        this.out = out;
    }

    public void write(int value) throws IOException
    {
        out.write(value);
    }

    public void write(byte[] buffer) throws IOException
    {
        out.write(buffer);
    }

    public void write(byte[] buffer, int offset, int length) throws IOException
    {
        out.write(buffer, offset, length);
    }

    public void flush() throws IOException
    {
        out.flush();
    }

    public void close() throws IOException
    {
        out.close();
    }
}
