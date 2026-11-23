package java.text;

import java.util.Locale;

/**
 * Minimal MessageFormat: {n}, {n,number}, {n,number,pattern}, {n,date} and
 * {n,time}.  Single quotes quote literal text; '' produces one quote.
 */
public class MessageFormat
{
    private String pattern;
    private Locale locale = Locale.getDefault();

    public MessageFormat(String pattern)
    {
        applyPattern(pattern);
    }

    public MessageFormat(String pattern, Locale locale)
    {
        // The locale only affects parsing/formatting of arguments that need
        // locale-aware number or date formatting.
        this.locale = locale == null ? Locale.getDefault() : locale;
        applyPattern(pattern);
    }

    public void setLocale(Locale locale)
    {
        this.locale = locale;
    }

    public Locale getLocale()
    {
        return locale;
    }

    public void applyPattern(String pattern)
    {
        if (pattern == null)
        {
            throw new NullPointerException("pattern");
        }
        this.pattern = pattern;
    }

    public String toPattern()
    {
        return pattern;
    }

    public static String format(String pattern, Object... arguments)
    {
        return new MessageFormat(pattern).format(arguments);
    }

    public final String format(Object argument)
    {
        return format(new Object[] {argument});
    }

    public StringBuffer format(Object[] arguments, StringBuffer result,
        FieldPosition position)
    {
        result.append(format(arguments));
        return result;
    }

    public final String format(Object[] arguments)
    {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < pattern.length())
        {
            char c = pattern.charAt(i);
            if (c == '\'')
            {
                i = i + 1;
                if (i < pattern.length() && pattern.charAt(i) == '\'')
                {
                    out.append('\'');
                    i = i + 1;
                    continue;
                }
                while (i < pattern.length() && pattern.charAt(i) != '\'')
                {
                    out.append(pattern.charAt(i));
                    i = i + 1;
                }
                i = i + 1;
                continue;
            }
            if (c != '{')
            {
                out.append(c);
                i = i + 1;
                continue;
            }
            int close = pattern.indexOf('}', i + 1);
            if (close < 0)
            {
                throw new IllegalArgumentException("Unmatched '{' in pattern");
            }
            String body = pattern.substring(i + 1, close);
            int comma = body.indexOf(',');
            String indexText = comma < 0 ? body : body.substring(0, comma);
            String type = "";
            String style = "";
            if (comma >= 0)
            {
                String rest = body.substring(comma + 1);
                int comma2 = rest.indexOf(',');
                if (comma2 < 0)
                {
                    type = rest.trim();
                }
                else
                {
                    type = rest.substring(0, comma2).trim();
                    style = rest.substring(comma2 + 1).trim();
                }
            }
            int index = Integer.parseInt(indexText.trim());
            Object value = index >= 0 && index < arguments.length ? arguments[index] : null;
            out.append(formatArgument(value, type, style));
            i = close + 1;
        }
        return out.toString();
    }

    private static String formatArgument(Object value, String type, String style)
    {
        if (type.length() == 0)
        {
            return String.valueOf(value);
        }
        if (type.equals("number"))
        {
            NumberFormat format;
            if (style.length() > 0)
            {
                format = new DecimalFormat(style);
            }
            else
            {
                format = NumberFormat.getNumberInstance();
            }
            return format.format(value);
        }
        if (type.equals("date") || type.equals("time"))
        {
            SimpleDateFormat format;
            if (style.length() > 0 && !isStyleName(style))
            {
                format = new SimpleDateFormat(style);
            }
            else if (type.equals("date"))
            {
                format = new SimpleDateFormat("yyyy-MM-dd");
            }
            else
            {
                format = new SimpleDateFormat("HH:mm:ss");
            }
            return format.format((java.util.Date)value);
        }
        throw new IllegalArgumentException("Unsupported MessageFormat type: " + type);
    }

    private static boolean isStyleName(String style)
    {
        return style.equals("short") || style.equals("medium") ||
               style.equals("long") || style.equals("full");
    }
}
