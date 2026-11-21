package java.io;

public class RandomAccessFile
{
    private long handle;
    private final String path;
    private final boolean writeable;

    public RandomAccessFile(File file, String mode) throws FileNotFoundException
    {
        this(file == null ? null : file.getPath(), mode);
    }

    public RandomAccessFile(String name, String mode) throws FileNotFoundException
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        writeable = "rw".equals(mode) || "rws".equals(mode) || "rwd".equals(mode);
        handle = open0(name, writeable);
        if (handle == 0)
        {
            throw new FileNotFoundException(name);
        }
        path = name;
    }

    public int read() throws IOException
    {
        ensureOpen();
        return read0(handle);
    }

    public int read(byte[] buffer, int offset, int length) throws IOException
    {
        ensureOpen();
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

    public int read(byte[] buffer) throws IOException
    {
        return read(buffer, 0, buffer.length);
    }

    public void write(int value) throws IOException
    {
        ensureOpen();
        write0(handle, value);
    }

    public void write(byte[] buffer, int offset, int length) throws IOException
    {
        ensureOpen();
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

    public void write(byte[] buffer) throws IOException
    {
        write(buffer, 0, buffer.length);
    }

    public void seek(long position) throws IOException
    {
        ensureOpen();
        seek0(handle, position);
    }

    public long getFilePointer() throws IOException
    {
        ensureOpen();
        return position0(handle);
    }

    public long length() throws IOException
    {
        ensureOpen();
        return length0(handle);
    }

    public void setLength(long length) throws IOException
    {
        ensureOpen();
        setLength0(handle, length);
    }

    public void close() throws IOException
    {
        if (handle != 0)
        {
            close0(handle);
            handle = 0;
        }
    }

    public String toString()
    {
        return path;
    }

    private void ensureOpen() throws IOException
    {
        if (handle == 0)
        {
            throw new IOException("stream closed");
        }
    }

    private static native long open0(String name, boolean writeable);

    private static native int read0(long handle);

    private static native int readBytes0(long handle, byte[] buffer, int offset, int length);

    private static native void write0(long handle, int value);

    private static native void writeBytes0(long handle, byte[] buffer, int offset, int length);

    private static native void seek0(long handle, long position);

    private static native long position0(long handle);

    private static native long length0(long handle);

    private static native void setLength0(long handle, long length);

    private static native void close0(long handle);
}
