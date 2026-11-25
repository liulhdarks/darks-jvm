package java.lang;

public class NumberFormatException extends IllegalArgumentException
{
    public NumberFormatException()
    {
        super();
    }

    public NumberFormatException(String message)
    {
        super(message);
    }

    static NumberFormatException forInputString(String s)
    {
        return new NumberFormatException("For input string: \"" + s + "\"");
    }

    static NumberFormatException forInputString(String s, int radix)
    {
        return new NumberFormatException("For input string: \"" + s + "\"" +
                (radix == 10 ? "" : " under radix " + radix));
    }

    static NumberFormatException forCharSequence(CharSequence s, int beginIndex, int endIndex, int radix)
    {
        return new NumberFormatException("Error at index " +
                (endIndex - beginIndex) + " in: \"" + s.toString() + "\"");
    }
}
