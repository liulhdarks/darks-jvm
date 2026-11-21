package java.lang;

// StringBuffer is the synchronized sibling of StringBuilder in the JDK.  This
// runtime provides the same storage and relies on the VM's monitor support
// only where callers synchronize explicitly; the return types are widened to
// StringBuffer so chained calls match JDK bytecode.
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

    public StringBuffer append(String text)
    {
        super.append(text);
        return this;
    }

    public StringBuffer append(char value)
    {
        super.append(value);
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

    public StringBuffer append(boolean value)
    {
        super.append(value);
        return this;
    }

    public StringBuffer append(int value)
    {
        super.append(value);
        return this;
    }

    public StringBuffer append(long value)
    {
        super.append(value);
        return this;
    }

    public StringBuffer append(float value)
    {
        super.append(value);
        return this;
    }

    public StringBuffer append(double value)
    {
        super.append(value);
        return this;
    }

    public StringBuffer append(Object value)
    {
        super.append(value);
        return this;
    }

    public StringBuffer append(CharSequence value)
    {
        super.append(value);
        return this;
    }

    public StringBuffer append(CharSequence value, int start, int end)
    {
        super.append(value, start, end);
        return this;
    }

    public StringBuffer insert(int offset, String text)
    {
        super.insert(offset, text);
        return this;
    }

    public StringBuffer insert(int offset, char value)
    {
        super.insert(offset, value);
        return this;
    }

    public StringBuffer insert(int offset, int value)
    {
        super.insert(offset, value);
        return this;
    }

    public StringBuffer insert(int offset, long value)
    {
        super.insert(offset, value);
        return this;
    }

    public StringBuffer insert(int offset, double value)
    {
        super.insert(offset, value);
        return this;
    }

    public StringBuffer insert(int offset, boolean value)
    {
        super.insert(offset, value);
        return this;
    }

    public StringBuffer deleteCharAt(int index)
    {
        super.deleteCharAt(index);
        return this;
    }

    public StringBuffer delete(int start, int end)
    {
        super.delete(start, end);
        return this;
    }

    public StringBuffer replace(int start, int end, String replacement)
    {
        super.replace(start, end, replacement);
        return this;
    }

    public StringBuffer reverse()
    {
        super.reverse();
        return this;
    }
}
