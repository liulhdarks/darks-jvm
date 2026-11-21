package java.util;

/**
 * Splits a string into tokens separated by a set of delimiter characters.
 * Implemented without relying on regular expressions.
 */
public class StringTokenizer implements Enumeration<Object>
{
    private String str;
    private String delimiters;
    private boolean returnDelimiters;
    private int position;

    public StringTokenizer(String str)
    {
        this(str, " \t\n\r\f", false);
    }

    public StringTokenizer(String str, String delimiters)
    {
        this(str, delimiters, false);
    }

    public StringTokenizer(String str, String delimiters,
        boolean returnDelimiters)
    {
        if (str == null)
        {
            throw new NullPointerException();
        }
        this.str = str;
        this.delimiters = delimiters == null ? " \t\n\r\f" : delimiters;
        this.returnDelimiters = returnDelimiters;
        this.position = 0;
    }

    public boolean hasMoreTokens()
    {
        if (returnDelimiters)
        {
            return position < str.length();
        }
        int length = str.length();
        int i = position;
        while (i < length && delimiters.indexOf(str.charAt(i)) >= 0)
        {
            i++;
        }
        position = i;
        return i < length;
    }

    public String nextToken()
    {
        int length = str.length();
        int i = position;
        if (returnDelimiters)
        {
            if (i >= length)
            {
                throw new NoSuchElementException();
            }
            char c = str.charAt(i);
            position = i + 1;
            if (delimiters.indexOf(c) >= 0)
            {
                return String.valueOf(c);
            }
            int start = i;
            while (position < length && delimiters.indexOf(str.charAt(position)) < 0)
            {
                position++;
            }
            return str.substring(start, position);
        }
        while (i < length && delimiters.indexOf(str.charAt(i)) >= 0)
        {
            i++;
        }
        if (i >= length)
        {
            position = i;
            throw new NoSuchElementException();
        }
        int start = i;
        while (i < length && delimiters.indexOf(str.charAt(i)) < 0)
        {
            i++;
        }
        position = i;
        return str.substring(start, i);
    }

    public String nextToken(String delimiters)
    {
        this.delimiters = delimiters == null ? " \t\n\r\f" : delimiters;
        return nextToken();
    }

    public int countTokens()
    {
        int count = 0;
        int i = position;
        int length = str.length();
        if (returnDelimiters)
        {
            return length - position;
        }
        while (i < length)
        {
            while (i < length && delimiters.indexOf(str.charAt(i)) >= 0)
            {
                i++;
            }
            if (i < length)
            {
                count++;
                while (i < length && delimiters.indexOf(str.charAt(i)) < 0)
                {
                    i++;
                }
            }
        }
        return count;
    }

    public boolean hasMoreElements()
    {
        return hasMoreTokens();
    }

    public Object nextElement()
    {
        return nextToken();
    }
}
