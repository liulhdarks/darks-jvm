package java.util;

public class Properties extends HashMap<Object, Object>
{
    public Properties()
    {
        super();
    }

    public Properties(Properties defaults)
    {
        super();
        putAll(defaults);
    }

    public Object setProperty(String key, String value)
    {
        return put(key, value);
    }

    public String getProperty(String key)
    {
        Object value = get(key);
        return value instanceof String ? (String)value : null;
    }

    public String getProperty(String key, String defaultValue)
    {
        String value = getProperty(key);
        return value == null ? defaultValue : value;
    }

    public synchronized void load(java.io.InputStream input) throws java.io.IOException
    {
        if (input == null)
        {
            throw new NullPointerException();
        }
        StringBuilder text = new StringBuilder();
        int ch = input.read();
        while (ch >= 0)
        {
            text.append((char)(ch & 0xff));
            ch = input.read();
        }
        parse(text.toString());
    }

    public synchronized void load(java.io.Reader reader) throws java.io.IOException
    {
        if (reader == null)
        {
            throw new NullPointerException();
        }
        StringBuilder text = new StringBuilder();
        int ch = reader.read();
        while (ch >= 0)
        {
            text.append((char)ch);
            ch = reader.read();
        }
        parse(text.toString());
    }

    private void parse(String text)
    {
        int position = 0;
        int length = text.length();
        while (position < length)
        {
            int lineStart = position;
            int lineEnd = text.indexOf('\n', position);
            if (lineEnd < 0)
            {
                lineEnd = length;
            }
            position = lineEnd + 1;
            String line = text.substring(lineStart, lineEnd);
            if (line.endsWith("\r"))
            {
                line = line.substring(0, line.length() - 1);
            }
            line = stripLeading(line);
            if (line.length() == 0 || line.charAt(0) == '#' || line.charAt(0) == '!')
            {
                continue;
            }
            while (endsWithContinuation(line) && position < length)
            {
                int nextEnd = text.indexOf('\n', position);
                if (nextEnd < 0)
                {
                    nextEnd = length;
                }
                String next = text.substring(position, nextEnd);
                position = nextEnd + 1;
                if (next.endsWith("\r"))
                {
                    next = next.substring(0, next.length() - 1);
                }
                line = line.substring(0, line.length() - 1) + stripLeading(next);
            }
            int separator = findSeparator(line);
            String key;
            String value;
            if (separator < 0)
            {
                key = line;
                value = "";
            }
            else
            {
                key = line.substring(0, separator);
                int valueStart = separator;
                if (line.charAt(valueStart) == '=' || line.charAt(valueStart) == ':')
                {
                    valueStart = valueStart + 1;
                }
                value = stripLeading(line.substring(valueStart));
            }
            put(unescape(key), unescape(value));
        }
    }

    private static String stripLeading(String text)
    {
        int index = 0;
        while (index < text.length())
        {
            char ch = text.charAt(index);
            if (ch != ' ' && ch != '\t' && ch != '\f')
            {
                break;
            }
            index = index + 1;
        }
        return text.substring(index);
    }

    private static boolean endsWithContinuation(String line)
    {
        int slashes = 0;
        int index = line.length() - 1;
        while (index >= 0 && line.charAt(index) == '\\')
        {
            slashes = slashes + 1;
            index = index - 1;
        }
        return (slashes % 2) == 1;
    }

    private static int findSeparator(String line)
    {
        int index = 0;
        while (index < line.length())
        {
            char ch = line.charAt(index);
            if (ch == '\\')
            {
                index = index + 2;
                continue;
            }
            if (ch == '=' || ch == ':' || ch == ' ' || ch == '\t' || ch == '\f')
            {
                return index;
            }
            index = index + 1;
        }
        return -1;
    }

    private static String unescape(String text)
    {
        StringBuilder result = new StringBuilder();
        int index = 0;
        while (index < text.length())
        {
            char ch = text.charAt(index);
            if (ch != '\\' || index + 1 >= text.length())
            {
                result.append(ch);
                index = index + 1;
                continue;
            }
            char next = text.charAt(index + 1);
            index = index + 2;
            if (next == 't')
            {
                result.append('\t');
            }
            else if (next == 'n')
            {
                result.append('\n');
            }
            else if (next == 'r')
            {
                result.append('\r');
            }
            else if (next == 'f')
            {
                result.append('\f');
            }
            else if (next == 'u')
            {
                if (index + 4 <= text.length())
                {
                    try
                    {
                        result.append((char)Integer.parseInt(
                            text.substring(index, index + 4), 16));
                        index = index + 4;
                    }
                    catch (RuntimeException ignored)
                    {
                        result.append('u');
                    }
                }
                else
                {
                    result.append('u');
                }
            }
            else
            {
                result.append(next);
            }
        }
        return result.toString();
    }

    // Lexicographically sorted key set, matching java.util.Properties.
    public Set<String> stringPropertyNames()
    {
        HashSet<String> result = new HashSet<String>();
        Iterator<?> it = keySet().iterator();
        while (it.hasNext())
        {
            Object key = it.next();
            if (key instanceof String)
            {
                result.add((String)key);
            }
        }
        return result;
    }
}
