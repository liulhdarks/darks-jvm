package java.io;

public class ObjectOutputStream extends OutputStream implements ObjectOutput
{
    private final OutputStream out;

    public ObjectOutputStream(OutputStream out) throws IOException
    {
        if (out == null)
        {
            throw new NullPointerException();
        }
        this.out = out;
    }

    public final void writeObject(Object value) throws IOException
    {
        throw new IOException("object serialization is not supported");
    }

    public void defaultWriteObject() throws IOException
    {
    }

    public void write(int value) throws IOException
    {
        out.write(value);
    }

    public void write(byte[] buffer) throws IOException
    {
        out.write(buffer);
    }

    public void write(byte[] buffer, int offset, int length) throws IOException
    {
        out.write(buffer, offset, length);
    }

    public void flush() throws IOException
    {
        out.flush();
    }

    public void close() throws IOException
    {
        out.close();
    }

    public void writeBoolean(boolean value) throws IOException
    {
        write(value ? 1 : 0);
    }

    public void writeByte(int value) throws IOException
    {
        write(value);
    }

    public void writeShort(int value) throws IOException
    {
        write((value >>> 8) & 0xff);
        write(value & 0xff);
    }

    public void writeChar(int value) throws IOException
    {
        writeShort(value);
    }

    public void writeInt(int value) throws IOException
    {
        write((value >>> 24) & 0xff);
        write((value >>> 16) & 0xff);
        write((value >>> 8) & 0xff);
        write(value & 0xff);
    }

    public void writeLong(long value) throws IOException
    {
        writeInt((int)(value >>> 32));
        writeInt((int)value);
    }

    public void writeFloat(float value) throws IOException
    {
        writeInt(Float.floatToIntBits(value));
    }

    public void writeDouble(double value) throws IOException
    {
        writeLong(Double.doubleToLongBits(value));
    }

    public void writeUTF(String value) throws IOException
    {
        byte[] bytes = value.getBytes("UTF-8");
        writeShort(bytes.length);
        write(bytes);
    }

    public void writeBytes(String value) throws IOException
    {
        for (int i = 0; i < value.length(); i++)
        {
            write(value.charAt(i) & 0xff);
        }
    }

    public void writeChars(String value) throws IOException
    {
        for (int i = 0; i < value.length(); i++)
        {
            writeChar(value.charAt(i));
        }
    }
}
