package java.net;

public class URISyntaxException extends Exception
{
    private final String input;
    private final String reason;

    public URISyntaxException(String input, String reason)
    {
        super(reason);
        this.input = input;
        this.reason = reason;
    }

    public URISyntaxException(String input, String reason, int index)
    {
        this(input, reason);
    }

    public String getInput()
    {
        return input;
    }

    public String getReason()
    {
        return reason;
    }

    public int getIndex()
    {
        return -1;
    }
}
