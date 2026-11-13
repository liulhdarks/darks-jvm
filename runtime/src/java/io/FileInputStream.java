package java.io;

public class FileInputStream extends InputStream
{
    private long handle;

    public FileInputStream(String name) throws FileNotFoundException
    {
        handle = open0(name);
        if (handle == 0)
        {
            throw new FileNotFoundException(name);
        }
    }

    public FileInputStream(File file) throws FileNotFoundException
    {
        this(file.getPath());
    }

    public int read() throws IOException
    {
        if (handle == 0)
        {
            throw new IOException("stream closed");
        }
        return read0(handle);
    }

    public int read(byte[] buffer, int offset, int length) throws IOException
    {
        if (handle == 0)
        {
            throw new IOException("stream closed");
        }
        if (buffer == null)
        {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || offset + length > buffer.length)
        {
            throw new IndexOutOfBoundsException();
        }
        return readBytes0(handle, buffer, offset, length);
    }

    public void close() throws IOException
    {
        if (handle != 0)
        {
            close0(handle);
            handle = 0;
        }
    }

    private static native long open0(String name);

    private static native int read0(long handle);

    private static native int readBytes0(long handle, byte[] buffer, int offset, int length);

    private static native void close0(long handle);
}
