package java.io;

public class StringWriter extends Writer
{
    private final StringBuilder buffer;

    public StringWriter()
    {
        buffer = new StringBuilder();
    }

    public StringWriter(int initialSize)
    {
        buffer = new StringBuilder(initialSize);
    }

    public void write(String text) throws IOException
    {
        buffer.append(text);
    }

    public void write(char[] chars, int offset, int length) throws IOException
    {
        buffer.append(new String(chars, offset, length));
    }

    public void write(int value) throws IOException
    {
        buffer.append((char)value);
    }

    public StringWriter append(CharSequence text)
    {
        buffer.append(text == null ? "null" : text.toString());
        return this;
    }

    public StringWriter append(CharSequence text, int start, int end)
    {
        String value = text == null ? "null" : text.toString();
        buffer.append(value.substring(start, end));
        return this;
    }

    public StringWriter append(char value)
    {
        buffer.append(value);
        return this;
    }

    public StringBuffer getBuffer()
    {
        return new StringBuffer(toString());
    }

    public String toString()
    {
        return buffer.toString();
    }

    public void flush()
    {
    }

    public void close()
    {
    }
}
