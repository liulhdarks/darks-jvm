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

    public static boolean isLetterOrDigit(char c)
    {
        return isLetter(c) || isDigit(c);
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

    public static boolean isDigit(int codePoint)
    {
        return codePoint >= '0' && codePoint <= '9';
    }

    public static boolean isLetter(int codePoint)
    {
        return (codePoint >= 'a' && codePoint <= 'z') || (codePoint >= 'A' && codePoint <= 'Z');
    }

    public static boolean isLetterOrDigit(int codePoint)
    {
        return isLetter(codePoint) || isDigit(codePoint);
    }

    public static boolean isAlphabetic(int codePoint)
    {
        return isLetter(codePoint);
    }

    public static int toUpperCase(int codePoint)
    {
        return toUpperCase((char)codePoint);
    }

    public static int toLowerCase(int codePoint)
    {
        return toLowerCase((char)codePoint);
    }

    public static boolean isWhitespace(int codePoint)
    {
        return isWhitespace((char)codePoint);
    }

    public static boolean isSpaceChar(char c)
    {
        return c == ' ' || c == '\u00A0' || c == '\u2007' || c == '\u202F';
    }

    public static boolean isSpaceChar(int codePoint)
    {
        return isSpaceChar((char)codePoint);
    }

    public static boolean isISOControl(char c)
    {
        return (c >= '\u0000' && c <= '\u001F') || (c >= '\u007F' && c <= '\u009F');
    }

    public static boolean isISOControl(int codePoint)
    {
        return isISOControl((char)codePoint);
    }

    public static boolean isJavaIdentifierStart(char c)
    {
        return isLetter(c) || c == '$' || c == '_' || c > 127;
    }

    public static boolean isJavaIdentifierStart(int codePoint)
    {
        return isJavaIdentifierStart((char)codePoint);
    }

    public static boolean isJavaIdentifierPart(char c)
    {
        return isJavaIdentifierStart(c) || isDigit(c) || isISOControl(c);
    }

    public static boolean isJavaIdentifierPart(int codePoint)
    {
        return isJavaIdentifierPart((char)codePoint);
    }

    public static int getNumericValue(char c)
    {
        return digit(c, 36);
    }

    public static int getNumericValue(int codePoint)
    {
        return digit((char)codePoint, 36);
    }

    public static boolean isUpperCase(char c)
    {
        return c >= 'A' && c <= 'Z';
    }

    public static boolean isUpperCase(int codePoint)
    {
        return isUpperCase((char)codePoint);
    }

    public static boolean isLowerCase(char c)
    {
        return c >= 'a' && c <= 'z';
    }

    public static boolean isLowerCase(int codePoint)
    {
        return isLowerCase((char)codePoint);
    }

    public static boolean isTitleCase(char c)
    {
        return false;
    }

    public static boolean isTitleCase(int codePoint)
    {
        return false;
    }

    public static boolean isDefined(char c)
    {
        return true;
    }

    public static boolean isDefined(int codePoint)
    {
        return true;
    }

    public static boolean isSurrogate(char c)
    {
        return c >= 0xD800 && c <= 0xDFFF;
    }

    public static boolean isSurrogatePair(char high, char low)
    {
        return isHighSurrogate(high) && isLowSurrogate(low);
    }

    public static boolean isIdentifierIgnorable(char c)
    {
        return (c >= '\u0000' && c <= '\u0008') || (c >= '\u000E' && c <= '\u001B')
            || (c >= '\u007F' && c <= '\u009F');
    }

    public static boolean isIdentifierIgnorable(int codePoint)
    {
        return isIdentifierIgnorable((char)codePoint);
    }

    public static boolean isUnicodeIdentifierStart(char c)
    {
        return isLetter(c) || c > 127;
    }

    public static boolean isUnicodeIdentifierStart(int codePoint)
    {
        return isUnicodeIdentifierStart((char)codePoint);
    }

    public static boolean isUnicodeIdentifierPart(char c)
    {
        return isUnicodeIdentifierStart(c) || isDigit(c) || c == '_'
            || isIdentifierIgnorable(c);
    }

    public static boolean isUnicodeIdentifierPart(int codePoint)
    {
        return isUnicodeIdentifierPart((char)codePoint);
    }

    public static char toTitleCase(char c)
    {
        return toUpperCase(c);
    }

    public static int toTitleCase(int codePoint)
    {
        return toUpperCase((char)codePoint);
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

    public static int charCount(int codePoint)
    {
        return codePoint >= 0x10000 ? 2 : 1;
    }

    public static boolean isHighSurrogate(char c)
    {
        return c >= 0xD800 && c <= 0xDBFF;
    }

    public static boolean isLowSurrogate(char c)
    {
        return c >= 0xDC00 && c <= 0xDFFF;
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

    public static int hashCode(char value)
    {
        return value;
    }

    public static int compare(char a, char b)
    {
        return a - b;
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
