package java.io;

public abstract class Reader
{
    public abstract int read() throws IOException;

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

    public void close() throws IOException
    {
    }
}
