package java.io;

public class StringReader extends Reader
{
    private final String text;
    private int position;

    public StringReader(String text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        this.text = text;
    }

    public int read() throws IOException
    {
        if (position >= text.length())
        {
            return -1;
        }
        return text.charAt(position++);
    }

    public int read(char[] buffer, int offset, int length) throws IOException
    {
        if (offset < 0 || length < 0 || offset + length > buffer.length)
        {
            throw new IndexOutOfBoundsException("range");
        }
        if (position >= text.length())
        {
            return -1;
        }
        int count = text.length() - position;
        if (count > length)
        {
            count = length;
        }
        int i = 0;
        while (i < count)
        {
            buffer[offset + i] = text.charAt(position + i);
            i = i + 1;
        }
        position = position + count;
        return count;
    }

    public long skip(long count) throws IOException
    {
        long available = text.length() - position;
        if (count > available)
        {
            count = available;
        }
        if (count < 0)
        {
            count = 0;
        }
        position = position + (int)count;
        return count;
    }

    public boolean ready() throws IOException
    {
        return true;
    }

    public boolean markSupported()
    {
        return true;
    }

    public void mark(int readAheadLimit) throws IOException
    {
    }

    public void reset() throws IOException
    {
        position = 0;
    }

    public void close()
    {
        position = text.length();
    }
}
