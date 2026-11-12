package java.lang;

public final class String
{
    private char[] value;

    public String(char[] value)
    {
        this.value = value;
    }

    public int length()
    {
        return value.length;
    }
}
