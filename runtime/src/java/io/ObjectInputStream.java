package java.io;

public class ObjectInputStream extends InputStream implements ObjectInput
{
    public ObjectInputStream(InputStream in) throws IOException
    {
        if (in == null)
        {
            throw new NullPointerException();
        }
    }

    public final Object readObject() throws IOException, ClassNotFoundException
    {
        throw new IOException("object deserialization is not supported");
    }

    public void defaultReadObject() throws IOException, ClassNotFoundException
    {
        throw new IOException("object deserialization is not supported");
    }

    public int read() throws IOException
    {
        return -1;
    }

    public int read(byte[] buffer, int offset, int length) throws IOException
    {
        return -1;
    }

    public long skip(long count) throws IOException
    {
        return 0L;
    }

    public int available() throws IOException
    {
        return 0;
    }

    public void close() throws IOException
    {
    }

    public boolean readBoolean() throws IOException
    {
        throw new EOFException();
    }

    public byte readByte() throws IOException
    {
        throw new EOFException();
    }

    public char readChar() throws IOException
    {
        throw new EOFException();
    }

    public short readShort() throws IOException
    {
        throw new EOFException();
    }

    public int readInt() throws IOException
    {
        throw new EOFException();
    }

    public long readLong() throws IOException
    {
        throw new EOFException();
    }

    public float readFloat() throws IOException
    {
        throw new EOFException();
    }

    public double readDouble() throws IOException
    {
        throw new EOFException();
    }

    public String readUTF() throws IOException
    {
        throw new EOFException();
    }

    public void readFully(byte[] buffer) throws IOException
    {
        throw new EOFException();
    }

    public void readFully(byte[] buffer, int offset, int length) throws IOException
    {
        throw new EOFException();
    }
}
