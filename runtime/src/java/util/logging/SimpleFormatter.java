package java.util.logging;

public class SimpleFormatter extends Formatter
{
    public String format(LogRecord record)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(record.getLevel().getName()).append(": ").append(formatMessage(record)).append("\n");
        if (record.getThrown() != null)
        {
            sb.append(record.getThrown().toString()).append("\n");
        }
        return sb.toString();
    }

    protected String formatMessage(LogRecord record)
    {
        String message = record.getMessage() == null ? "" : record.getMessage();
        Object[] parameters = record.getParameters();
        if (parameters != null)
        {
            for (int i = 0; i < parameters.length; i++)
            {
                message = message.replace("{" + i + "}", String.valueOf(parameters[i]));
            }
        }
        return message;
    }
}
