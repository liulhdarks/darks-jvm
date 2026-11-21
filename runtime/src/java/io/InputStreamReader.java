package java.io;

// UTF-8 reader.  The runtime's strings are UTF-16; both directions live in
// java.lang.String so no native encoding tables are needed.
public class InputStreamReader extends Reader
{
    private InputStream in;
    private java.nio.charset.Charset charset;

    public InputStreamReader(InputStream in)
    {
        this.in = in;
    }

    public InputStreamReader(InputStream in, java.nio.charset.Charset charset)
    {
        this.in = in;
        this.charset = charset;
    }

    public InputStreamReader(InputStream in, String charsetName)
        throws UnsupportedEncodingException
    {
        this.in = in;
        this.charset = java.nio.charset.Charset.forName(charsetName);
    }

    public String getEncoding()
    {
        return charset == null ? "UTF-8" : charset.name();
    }

    public int read() throws IOException
    {
        if (charset != null && charset != java.nio.charset.StandardCharsets.UTF_8)
        {
            return readEncoded();
        }
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

    private int readEncoded() throws IOException
    {
        String name = charset.name();
        if (name == "ISO-8859-1")
        {
            int value = in.read();
            return value < 0 ? -1 : value;
        }
        if (name == "US-ASCII")
        {
            int value = in.read();
            return value < 0 ? -1 : (value & 0x7F);
        }
        boolean bigEndian = name != "UTF-16LE";
        if (name == "UTF-16")
        {
            // The first code unit decides the byte order; there is no build
            // time platform default to consult, so big endian is assumed.
            bigEndian = true;
        }
        int first = in.read();
        if (first < 0)
        {
            return -1;
        }
        int second = in.read();
        if (second < 0)
        {
            return 0xFFFD;
        }
        return bigEndian ? ((first << 8) | second) : ((second << 8) | first);
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
