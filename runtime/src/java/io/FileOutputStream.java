package java.io;

public class FileOutputStream extends OutputStream
{
    private long handle;

    public FileOutputStream(String name) throws FileNotFoundException
    {
        this(name, false);
    }

    public FileOutputStream(String name, boolean append) throws FileNotFoundException
    {
        handle = open0(name, append);
        if (handle == 0)
        {
            throw new FileNotFoundException(name);
        }
    }

    public FileOutputStream(File file) throws FileNotFoundException
    {
        this(file.getPath(), false);
    }

    public FileOutputStream(File file, boolean append) throws FileNotFoundException
    {
        this(file.getPath(), append);
    }

    public void write(int value) throws IOException
    {
        if (handle == 0)
        {
            throw new IOException("stream closed");
        }
        write0(handle, value & 0xFF);
    }

    public void write(byte[] buffer, int offset, int length) throws IOException
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
        writeBytes0(handle, buffer, offset, length);
    }

    public void flush() throws IOException
    {
        if (handle != 0)
        {
            flush0(handle);
        }
    }

    public void close() throws IOException
    {
        if (handle != 0)
        {
            close0(handle);
            handle = 0;
        }
    }

    private static native long open0(String name, boolean append);

    private static native void write0(long handle, int value);

    private static native void writeBytes0(long handle, byte[] buffer, int offset, int length);

    private static native void flush0(long handle);

    private static native void close0(long handle);
}
