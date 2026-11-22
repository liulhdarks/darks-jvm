package java.io;

public class BufferedReader extends Reader
{
    private Reader in;

    public BufferedReader(Reader in)
    {
        this.in = in;
    }

    public BufferedReader(Reader in, int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("size <= 0");
        }
        this.in = in;
    }

    public int read() throws IOException
    {
        return in.read();
    }

    public int read(char[] buffer, int offset, int length) throws IOException
    {
        return in.read(buffer, offset, length);
    }

    public String readLine() throws IOException
    {
        StringBuilder line = new StringBuilder();
        boolean any = false;
        while (true)
        {
            int value = in.read();
            if (value < 0)
            {
                return any ? line.toString() : null;
            }
            any = true;
            if (value == '\n')
            {
                return line.toString();
            }
            if (value == '\r')
            {
                continue;
            }
            line.append((char)value);
        }
    }

    public void close() throws IOException
    {
        in.close();
    }
}
