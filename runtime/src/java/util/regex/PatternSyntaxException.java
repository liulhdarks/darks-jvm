package java.util.regex;

public class PatternSyntaxException extends IllegalArgumentException
{
    private final String description;
    private final String pattern;
    private final int index;

    public PatternSyntaxException(String description, String pattern, int index)
    {
        super(buildMessage(description, pattern, index));
        this.description = description;
        this.pattern = pattern;
        this.index = index;
    }

    private static String buildMessage(String description, String pattern,
                                       int index)
    {
        StringBuilder message = new StringBuilder();
        message.append(description == null ? "invalid pattern" : description);
        if (pattern != null)
        {
            message.append(" near index ");
            message.append(index);
            message.append('\n');
            message.append(pattern);
        }
        return message.toString();
    }

    public String getDescription()
    {
        return description;
    }

    public String getPattern()
    {
        return pattern;
    }

    public int getIndex()
    {
        return index;
    }
}
