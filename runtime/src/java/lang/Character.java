package java.lang;

public final class Character implements Comparable<Character>
{
    public static final Class TYPE = Class.getPrimitiveClass("char");

    private final char value;

    public Character(char value)
    {
        this.value = value;
    }

    public static Character valueOf(char value)
    {
        return new Character(value);
    }

    public char charValue()
    {
        return value;
    }

    public static boolean isDigit(char c)
    {
        return c >= '0' && c <= '9';
    }

    public static boolean isLetter(char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public static char toUpperCase(char c)
    {
        if (c >= 'a' && c <= 'z')
        {
            return (char)(c - 32);
        }
        return c;
    }

    public static char toLowerCase(char c)
    {
        if (c >= 'A' && c <= 'Z')
        {
            return (char)(c + 32);
        }
        return c;
    }

    public static String toString(char c)
    {
        char[] chars = new char[1];
        chars[0] = c;
        return new String(chars);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Character))
        {
            return false;
        }
        return ((Character)other).value == value;
    }

    public int hashCode()
    {
        return value;
    }

    public int compareTo(Character other)
    {
        return value - other.value;
    }

    public String toString()
    {
        return toString(value);
    }
}
