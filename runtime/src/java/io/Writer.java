package java.io;

public abstract class Writer
{
    public abstract void write(String text) throws IOException;

    public void write(char[] chars, int offset, int length) throws IOException
    {
        write(new String(chars, offset, length));
    }

    public void write(char value) throws IOException
    {
        char[] one = new char[1];
        one[0] = value;
        write(new String(one));
    }

    public void flush() throws IOException
    {
    }

    public void close() throws IOException
    {
    }
}
