package java.io;

public class ByteArrayOutputStream extends OutputStream
{
    private byte[] data;
    private int count;

    public ByteArrayOutputStream()
    {
        data = new byte[32];
    }

    private void ensure(int needed)
    {
        if (needed <= data.length)
        {
            return;
        }
        int size = data.length * 2;
        if (size < needed)
        {
            size = needed;
        }
        byte[] bigger = new byte[size];
        int i = 0;
        while (i < count)
        {
            bigger[i] = data[i];
            i = i + 1;
        }
        data = bigger;
    }

    public void write(int value)
    {
        ensure(count + 1);
        data[count++] = (byte)value;
    }

    public void write(byte[] buffer, int offset, int length)
    {
        ensure(count + length);
        int i = 0;
        while (i < length)
        {
            data[count + i] = buffer[offset + i];
            i = i + 1;
        }
        count = count + length;
    }

    public byte[] toByteArray()
    {
        byte[] result = new byte[count];
        int i = 0;
        while (i < count)
        {
            result[i] = data[i];
            i = i + 1;
        }
        return result;
    }

    public int size()
    {
        return count;
    }

    public String toString()
    {
        return new String(toByteArray());
    }
}
