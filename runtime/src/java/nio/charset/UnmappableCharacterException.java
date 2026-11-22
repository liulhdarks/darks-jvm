package java.nio.charset;

public class UnmappableCharacterException extends CharacterCodingException
{
    private final int inputLength;

    public UnmappableCharacterException(int inputLength)
    {
        super();
        this.inputLength = inputLength;
    }

    public int getInputLength()
    {
        return inputLength;
    }

    public String getMessage()
    {
        return "Input length = " + inputLength;
    }
}
