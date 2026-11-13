package java.io;

// UTF-8 reader.  The runtime's strings are UTF-16; both directions live in
// java.lang.String so no native encoding tables are needed.
public class InputStreamReader extends Reader
{
    private InputStream in;

    public InputStreamReader(InputStream in)
    {
        this.in = in;
    }

    public int read() throws IOException
    {
        int first = in.read();
        if (first < 0)
        {
            return -1;
        }
        if (first < 0x80)
        {
            return first;
        }
        int extra;
        int value;
        if ((first & 0xE0) == 0xC0)
        {
            extra = 1;
            value = first & 0x1F;
        }
        else if ((first & 0xF0) == 0xE0)
        {
            extra = 2;
            value = first & 0x0F;
        }
        else if ((first & 0xF8) == 0xF0)
        {
            extra = 3;
            value = first & 0x07;
        }
        else
        {
            return 0xFFFD;
        }
        int i = 0;
        while (i < extra)
        {
            int next = in.read();
            if (next < 0)
            {
                return 0xFFFD;
            }
            value = (value << 6) | (next & 0x3F);
            i = i + 1;
        }
        return value;
    }

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
        in.close();
    }
}
