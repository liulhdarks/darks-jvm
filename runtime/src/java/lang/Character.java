package java.lang;

public final class Character implements Comparable<Character>, java.io.Serializable
{
    public static final Class TYPE = Class.getPrimitiveClass("char");

    public static final int MIN_RADIX = 2;
    public static final int MAX_RADIX = 36;

    public static final byte UNASSIGNED = 0;
    public static final byte UPPERCASE_LETTER = 1;
    public static final byte LOWERCASE_LETTER = 2;
    public static final byte TITLECASE_LETTER = 3;
    public static final byte MODIFIER_LETTER = 4;
    public static final byte OTHER_LETTER = 5;
    public static final byte NON_SPACING_MARK = 6;
    public static final byte ENCLOSING_MARK = 7;
    public static final byte COMBINING_SPACING_MARK = 8;
    public static final byte DECIMAL_DIGIT_NUMBER = 9;
    public static final byte LETTER_NUMBER = 10;
    public static final byte OTHER_NUMBER = 11;
    public static final byte SPACE_SEPARATOR = 12;
    public static final byte LINE_SEPARATOR = 13;
    public static final byte PARAGRAPH_SEPARATOR = 14;
    public static final byte CONTROL = 15;
    public static final byte FORMAT = 16;
    public static final byte PRIVATE_USE = 18;
    public static final byte SURROGATE = 19;
    public static final byte DASH_PUNCTUATION = 20;
    public static final byte START_PUNCTUATION = 21;
    public static final byte END_PUNCTUATION = 22;
    public static final byte CONNECTOR_PUNCTUATION = 23;
    public static final byte OTHER_PUNCTUATION = 24;
    public static final byte MATH_SYMBOL = 25;
    public static final byte CURRENCY_SYMBOL = 26;
    public static final byte MODIFIER_SYMBOL = 27;
    public static final byte OTHER_SYMBOL = 28;
    public static final byte INITIAL_QUOTE_PUNCTUATION = 29;
    public static final byte FINAL_QUOTE_PUNCTUATION = 30;

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
        return isJavaIdentifierStart(c) || isDigit(c) || isIdentifierIgnorable(c);
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

    public static int getType(char c)
    {
        return getType((int)c);
    }

    public static int getType(int codePoint)
    {
        if (isUpperCase(codePoint))
        {
            return UPPERCASE_LETTER;
        }
        if (isLowerCase(codePoint))
        {
            return LOWERCASE_LETTER;
        }
        if (isTitleCase(codePoint))
        {
            return TITLECASE_LETTER;
        }
        if (isDigit(codePoint))
        {
            return DECIMAL_DIGIT_NUMBER;
        }
        if (isLetter(codePoint))
        {
            return OTHER_LETTER;
        }
        if (isSpaceChar(codePoint))
        {
            return SPACE_SEPARATOR;
        }
        if (isISOControl(codePoint))
        {
            return CONTROL;
        }
        switch (codePoint)
        {
        case '_':
            return CONNECTOR_PUNCTUATION;
        case '$':
            return CURRENCY_SYMBOL;
        case '+':
        case '<':
        case '=':
        case '>':
        case '|':
        case '~':
            return MATH_SYMBOL;
        case '!':
        case '"':
        case '#':
        case '%':
        case '&':
        case '\'':
        case '(':
        case ')':
        case '*':
        case ',':
        case '-':
        case '.':
        case '/':
        case ':':
        case ';':
        case '?':
        case '@':
        case '[':
        case '\\':
        case ']':
        case '^':
        case '`':
        case '{':
        case '}':
            return OTHER_PUNCTUATION;
        default:
            return UNASSIGNED;
        }
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

    public static boolean isSupplementaryCodePoint(int codePoint)
    {
        return codePoint >= 0x10000 && codePoint <= 0x10FFFF;
    }

    public static boolean isBmpCodePoint(int codePoint)
    {
        return codePoint >= 0 && codePoint <= 0xFFFF;
    }

    public static boolean isValidCodePoint(int codePoint)
    {
        return codePoint >= 0 && codePoint <= 0x10FFFF;
    }

    public static int toCodePoint(char high, char low)
    {
        return ((high - 0xD800) << 10) + (low - 0xDC00) + 0x10000;
    }

    public static int codePointAt(CharSequence text, int index)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        char high = text.charAt(index);
        if (isHighSurrogate(high) && index + 1 < text.length())
        {
            char low = text.charAt(index + 1);
            if (isLowSurrogate(low))
            {
                return toCodePoint(high, low);
            }
        }
        return high;
    }

    public static int codePointAt(char[] chars, int index)
    {
        return codePointAt(chars, index, chars == null ? 0 : chars.length);
    }

    public static int codePointAt(char[] chars, int index, int limit)
    {
        if (chars == null)
        {
            throw new NullPointerException("chars");
        }
        char high = chars[index];
        if (isHighSurrogate(high) && index + 1 < limit)
        {
            char low = chars[index + 1];
            if (isLowSurrogate(low))
            {
                return toCodePoint(high, low);
            }
        }
        return high;
    }

    public static int codePointBefore(CharSequence text, int index)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        char low = text.charAt(index - 1);
        if (isLowSurrogate(low) && index - 2 >= 0)
        {
            char high = text.charAt(index - 2);
            if (isHighSurrogate(high))
            {
                return toCodePoint(high, low);
            }
        }
        return low;
    }

    public static int codePointBefore(char[] chars, int index)
    {
        return codePointBefore(chars, index, 0);
    }

    public static int codePointBefore(char[] chars, int index, int start)
    {
        if (chars == null)
        {
            throw new NullPointerException("chars");
        }
        char low = chars[index - 1];
        if (isLowSurrogate(low) && index - 2 >= start)
        {
            char high = chars[index - 2];
            if (isHighSurrogate(high))
            {
                return toCodePoint(high, low);
            }
        }
        return low;
    }

    public static int codePointCount(CharSequence text, int beginIndex, int endIndex)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        int count = 0;
        int index = beginIndex;
        while (index < endIndex)
        {
            char c = text.charAt(index);
            if (isHighSurrogate(c) && index + 1 < endIndex && isLowSurrogate(text.charAt(index + 1)))
            {
                index += 2;
            }
            else
            {
                index++;
            }
            count++;
        }
        return count;
    }

    public static int codePointCount(char[] chars, int offset, int count)
    {
        if (chars == null)
        {
            throw new NullPointerException("chars");
        }
        int end = offset + count;
        int result = 0;
        int index = offset;
        while (index < end)
        {
            char c = chars[index];
            if (isHighSurrogate(c) && index + 1 < end && isLowSurrogate(chars[index + 1]))
            {
                index += 2;
            }
            else
            {
                index++;
            }
            result++;
        }
        return result;
    }

    public static int offsetByCodePoints(CharSequence text, int index, int codePointOffset)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        int length = text.length();
        int position = index;
        int remaining = codePointOffset;
        while (remaining > 0)
        {
            char c = text.charAt(position);
            position += isHighSurrogate(c) && position + 1 < length && isLowSurrogate(text.charAt(position + 1)) ? 2 : 1;
            remaining--;
        }
        while (remaining < 0)
        {
            char c = text.charAt(position - 1);
            position -= isLowSurrogate(c) && position - 2 >= 0 && isHighSurrogate(text.charAt(position - 2)) ? 2 : 1;
            remaining++;
        }
        return position;
    }

    public static int offsetByCodePoints(char[] chars, int start, int count, int index, int codePointOffset)
    {
        if (chars == null)
        {
            throw new NullPointerException("chars");
        }
        int end = start + count;
        int position = index;
        int remaining = codePointOffset;
        while (remaining > 0)
        {
            char c = chars[position];
            position += isHighSurrogate(c) && position + 1 < end && isLowSurrogate(chars[position + 1]) ? 2 : 1;
            remaining--;
        }
        while (remaining < 0)
        {
            char c = chars[position - 1];
            position -= isLowSurrogate(c) && position - 2 >= start && isHighSurrogate(chars[position - 2]) ? 2 : 1;
            remaining++;
        }
        return position;
    }

    public static char[] toChars(int codePoint)
    {
        if (codePoint < 0 || codePoint > 0x10FFFF)
        {
            throw new IllegalArgumentException("invalid code point: " + codePoint);
        }
        if (codePoint < 0x10000)
        {
            char[] one = new char[1];
            one[0] = (char)codePoint;
            return one;
        }
        char[] two = new char[2];
        toChars(codePoint, two, 0);
        return two;
    }

    public static int toChars(int codePoint, char[] dest, int destIndex)
    {
        if (codePoint < 0 || codePoint > 0x10FFFF)
        {
            throw new IllegalArgumentException("invalid code point: " + codePoint);
        }
        if (codePoint < 0x10000)
        {
            dest[destIndex] = (char)codePoint;
            return 1;
        }
        int value = codePoint - 0x10000;
        dest[destIndex] = (char)(0xD800 + (value >> 10));
        dest[destIndex + 1] = (char)(0xDC00 + (value & 0x3FF));
        return 2;
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

    public static final char MIN_HIGH_SURROGATE = '\uD800';
    public static final char MAX_HIGH_SURROGATE = '\uDBFF';
    public static final char MIN_LOW_SURROGATE = '\uDC00';
    public static final char MAX_LOW_SURROGATE = '\uDFFF';
    public static final char MIN_SURROGATE = MIN_HIGH_SURROGATE;
    public static final char MAX_SURROGATE = MAX_LOW_SURROGATE;

    public static char highSurrogate(int codePoint)
    {
        return (char)((codePoint >>> 10)
            + (MIN_HIGH_SURROGATE - (MIN_SUPPLEMENTARY_CODE_POINT >>> 10)));
    }

    public static char lowSurrogate(int codePoint)
    {
        return (char)((codePoint & 0x3ff) + MIN_LOW_SURROGATE);
    }

    public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;
    public static final int MIN_CODE_POINT = 0x000000;
    public static final int MAX_CODE_POINT = 0x10FFFF;

    static final int ERROR = 0xFFFFFFFF;

    static int toUpperCaseEx(int codePoint)
    {
        return toUpperCase(codePoint);
    }

    static char[] toUpperCaseCharArray(int codePoint)
    {
        return null;
    }
}
