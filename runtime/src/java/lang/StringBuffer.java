package java.lang;

public final class StringBuffer extends StringBuilder
{
    public StringBuffer()
    {
        super();
    }

    public StringBuffer(int capacity)
    {
        super(capacity);
    }

    public StringBuffer(String initial)
    {
        super(initial);
    }

    public StringBuffer append(String s)
    {
        super.append(s);
        return this;
    }

    public StringBuffer append(char c)
    {
        super.append(c);
        return this;
    }

    public StringBuffer append(CharSequence sequence)
    {
        super.append(sequence);
        return this;
    }

    public StringBuffer append(CharSequence sequence, int start, int end)
    {
        super.append(sequence, start, end);
        return this;
    }

    public StringBuffer append(char[] chars)
    {
        super.append(chars);
        return this;
    }

    public StringBuffer append(char[] chars, int offset, int length)
    {
        super.append(chars, offset, length);
        return this;
    }

    public StringBuffer append(boolean b)
    {
        super.append(b);
        return this;
    }

    public StringBuffer append(int v)
    {
        super.append(v);
        return this;
    }

    public StringBuffer append(long v)
    {
        super.append(v);
        return this;
    }

    public StringBuffer append(float v)
    {
        super.append(v);
        return this;
    }

    public StringBuffer append(double v)
    {
        super.append(v);
        return this;
    }

    public StringBuffer append(Object o)
    {
        super.append(o);
        return this;
    }
}
