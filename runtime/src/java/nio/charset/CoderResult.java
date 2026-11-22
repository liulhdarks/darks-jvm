package java.nio.charset;

public class CoderResult
{
    public static final CoderResult UNDERFLOW = new CoderResult(0, true);
    public static final CoderResult OVERFLOW = new CoderResult(1, true);

    private final int kind;
    private final int length;

    private CoderResult(int kind, boolean ignored)
    {
        this.kind = kind;
        this.length = 0;
    }

    private CoderResult(int kind, int length)
    {
        this.kind = kind;
        this.length = length;
    }

    public static CoderResult malformedForLength(int length)
    {
        return new CoderResult(2, length);
    }

    public static CoderResult unmappableForLength(int length)
    {
        return new CoderResult(3, length);
    }

    public boolean isUnderflow()
    {
        return kind == 0;
    }

    public boolean isOverflow()
    {
        return kind == 1;
    }

    public boolean isError()
    {
        return kind >= 2;
    }

    public boolean isMalformed()
    {
        return kind == 2;
    }

    public boolean isUnmappable()
    {
        return kind == 3;
    }

    public int length()
    {
        return length;
    }

    public void throwException() throws CharacterCodingException
    {
        if (kind == 2)
        {
            throw new MalformedInputException(length);
        }
        if (kind == 3)
        {
            throw new UnmappableCharacterException(length);
        }
        throw new CharacterCodingException();
    }

    public String toString()
    {
        if (kind == 0)
        {
            return "UNDERFLOW";
        }
        if (kind == 1)
        {
            return "OVERFLOW";
        }
        return (kind == 2 ? "Malformed input" : "Unmappable character") +
            " of length " + length;
    }
}
