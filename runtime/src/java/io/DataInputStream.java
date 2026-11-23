package java.io;

public class DataInputStream extends FilterInputStream implements DataInput
{
    public DataInputStream(InputStream in)
    {
        super(in);
    }

    public int read(byte[] buffer) throws IOException
    {
        return super.read(buffer);
    }

    public int read(byte[] buffer, int offset, int length) throws IOException
    {
        return super.read(buffer, offset, length);
    }

    public int readUnsignedByte() throws IOException
    {
        int value = read();
        if (value < 0)
        {
            throw new EOFException();
        }
        return value;
    }

    public byte readByte() throws IOException
    {
        return (byte)readUnsignedByte();
    }

    public int readUnsignedShort() throws IOException
    {
        int high = readUnsignedByte();
        int low = readUnsignedByte();
        return (high << 8) | low;
    }

    public short readShort() throws IOException
    {
        return (short)readUnsignedShort();
    }

    public int readInt() throws IOException
    {
        int b0 = readUnsignedByte();
        int b1 = readUnsignedByte();
        int b2 = readUnsignedByte();
        int b3 = readUnsignedByte();
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    public long readLong() throws IOException
    {
        long high = readInt() & 0xffffffffL;
        long low = readInt() & 0xffffffffL;
        return (high << 32) | low;
    }

    public boolean readBoolean() throws IOException
    {
        return readUnsignedByte() != 0;
    }

    public char readChar() throws IOException
    {
        return (char)readUnsignedShort();
    }

    public float readFloat() throws IOException
    {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() throws IOException
    {
        return Double.longBitsToDouble(readLong());
    }

    public void readFully(byte[] buffer) throws IOException
    {
        readFully(buffer, 0, buffer.length);
    }

    public void readFully(byte[] buffer, int offset, int length) throws IOException
    {
        int read = 0;
        while (read < length)
        {
            int count = in.read(buffer, offset + read, length - read);
            if (count < 0)
            {
                throw new EOFException();
            }
            read += count;
        }
    }

    public int skipBytes(int count) throws IOException
    {
        long skipped = in.skip(count);
        return (int)skipped;
    }

    public String readLine() throws IOException
    {
        StringBuilder result = new StringBuilder();
        int value = read();
        while (value >= 0 && value != '\n')
        {
            if (value == '\r')
            {
                int next = read();
                if (next >= 0 && next != '\n')
                {
                    // The caller cannot push back here; treat it as line end
                    // and lose the byte, matching the minimal runtime.
                }
                break;
            }
            result.append((char)value);
            value = read();
        }
        if (value < 0 && result.length() == 0)
        {
            return null;
        }
        return result.toString();
    }

    public String readUTF() throws IOException
    {
        int length = readUnsignedShort();
        byte[] bytes = new byte[length];
        readFully(bytes);
        StringBuilder result = new StringBuilder(length);
        int i = 0;
        while (i < length)
        {
            int b0 = bytes[i++] & 0xff;
            if ((b0 & 0x80) == 0)
            {
                result.append((char)b0);
            }
            else if ((b0 & 0xe0) == 0xc0)
            {
                if (i >= length)
                {
                    throw new IOException("malformed UTF-8 input");
                }
                int b1 = bytes[i++] & 0xff;
                result.append((char)(((b0 & 0x1f) << 6) | (b1 & 0x3f)));
            }
            else if ((b0 & 0xf0) == 0xe0)
            {
                if (i + 1 >= length)
                {
                    throw new IOException("malformed UTF-8 input");
                }
                int b1 = bytes[i++] & 0xff;
                int b2 = bytes[i++] & 0xff;
                result.append((char)(((b0 & 0x0f) << 12) | ((b1 & 0x3f) << 6)
                    | (b2 & 0x3f)));
            }
            else
            {
                throw new IOException("malformed UTF-8 input");
            }
        }
        return result.toString();
    }
}
