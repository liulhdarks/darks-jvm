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

    public static boolean isDigit(int c)
    {
        return c >= '0' && c <= '9';
    }

    public static boolean isLetter(char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public static boolean isLetter(int c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public static boolean isLetterOrDigit(char c)
    {
        return isLetter(c) || isDigit(c);
    }

    public static boolean isLetterOrDigit(int c)
    {
        return isLetter(c) || isDigit(c);
    }

    public static boolean isAlphabetic(int c)
    {
        return isLetter(c);
    }

    public static boolean isUpperCase(char c)
    {
        return c >= 'A' && c <= 'Z';
    }

    public static boolean isUpperCase(int c)
    {
        return c >= 'A' && c <= 'Z';
    }

    public static boolean isLowerCase(char c)
    {
        return c >= 'a' && c <= 'z';
    }

    public static boolean isLowerCase(int c)
    {
        return c >= 'a' && c <= 'z';
    }

    public static int toUpperCase(int c)
    {
        if (c >= 'a' && c <= 'z')
        {
            return c - 32;
        }
        return c;
    }

    public static int toLowerCase(int c)
    {
        if (c >= 'A' && c <= 'Z')
        {
            return c + 32;
        }
        return c;
    }

    public static int digit(int c, int radix)
    {
        return digit((char)c, radix);
    }

    public static int getNumericValue(char c)
    {
        if (c >= '0' && c <= '9')
        {
            return c - '0';
        }
        if (c >= 'a' && c <= 'z')
        {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'Z')
        {
            return c - 'A' + 10;
        }
        return -1;
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


    public static int digit(char c, int radix)
    {
        if (radix < 2 || radix > 36)
        {
            return -1;
        }
        int value;
        if (c >= '0' && c <= '9')
        {
            value = c - '0';
        }
        else if (c >= 'a' && c <= 'z')
        {
            value = c - 'a' + 10;
        }
        else if (c >= 'A' && c <= 'Z')
        {
            value = c - 'A' + 10;
        }
        else
        {
            return -1;
        }
        return value < radix ? value : -1;
    }

    public static char forDigit(int digit, int radix)
    {
        if (radix < 2 || radix > 36 || digit < 0 || digit >= radix)
        {
            return '\0';
        }
        if (digit < 10)
        {
            return (char)('0' + digit);
        }
        return (char)('a' + (digit - 10));
    }

    public static boolean isWhitespace(char c)
    {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\u000B';
    }

    public static boolean isWhitespace(int c)
    {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\u000B';
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
