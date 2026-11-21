package java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

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

    public synchronized void load(InputStream input) throws IOException
    {
        if (input == null)
        {
            throw new NullPointerException("input");
        }
        StringBuilder text = new StringBuilder();
        int b = input.read();
        while (b >= 0)
        {
            text.append((char)(b & 0xff));
            b = input.read();
        }
        loadText(text);
    }

    public synchronized void load(Reader reader) throws IOException
    {
        if (reader == null)
        {
            throw new NullPointerException("reader");
        }
        StringBuilder text = new StringBuilder();
        int c = reader.read();
        while (c >= 0)
        {
            text.append((char)c);
            c = reader.read();
        }
        loadText(text);
    }

    private void loadText(StringBuilder text)
    {
        int pos = 0;
        int length = text.length();
        while (pos < length)
        {
            StringBuilder logical = new StringBuilder();
            boolean more = true;
            while (more && pos < length)
            {
                int start = pos;
                while (pos < length && text.charAt(pos) != '\n' && text.charAt(pos) != '\r')
                {
                    pos++;
                }
                String physical = text.substring(start, pos);
                if (pos < length && text.charAt(pos) == '\r')
                {
                    pos++;
                    if (pos < length && text.charAt(pos) == '\n')
                    {
                        pos++;
                    }
                }
                else if (pos < length && text.charAt(pos) == '\n')
                {
                    pos++;
                }
                int backslashes = 0;
                int k = physical.length() - 1;
                while (k >= 0 && physical.charAt(k) == '\\')
                {
                    backslashes++;
                    k--;
                }
                if (backslashes % 2 == 1)
                {
                    logical.append(physical.substring(0, physical.length() - 1));
                    while (pos < length &&
                           (text.charAt(pos) == ' ' || text.charAt(pos) == '\t' ||
                            text.charAt(pos) == '\f'))
                    {
                        pos++;
                    }
                }
                else
                {
                    logical.append(physical);
                    more = false;
                }
            }
            parseLine(logical.toString());
        }
    }

    private void parseLine(String line)
    {
        int start = 0;
        int length = line.length();
        while (start < length &&
               (line.charAt(start) == ' ' || line.charAt(start) == '\t' ||
                line.charAt(start) == '\f'))
        {
            start++;
        }
        if (start >= length || line.charAt(start) == '#' || line.charAt(start) == '!')
        {
            return;
        }
        int keyEnd = start;
        boolean escaped = false;
        while (keyEnd < length)
        {
            char c = line.charAt(keyEnd);
            if (escaped)
            {
                escaped = false;
            }
            else if (c == '\\')
            {
                escaped = true;
            }
            else if (c == '=' || c == ':' || c == ' ' || c == '\t' || c == '\f')
            {
                break;
            }
            keyEnd++;
        }
        String key = unescape(line.substring(start, keyEnd));
        int valueStart = keyEnd;
        while (valueStart < length &&
               (line.charAt(valueStart) == ' ' || line.charAt(valueStart) == '\t' ||
                line.charAt(valueStart) == '\f'))
        {
            valueStart++;
        }
        if (valueStart < length &&
            (line.charAt(valueStart) == '=' || line.charAt(valueStart) == ':'))
        {
            valueStart++;
            while (valueStart < length &&
                   (line.charAt(valueStart) == ' ' || line.charAt(valueStart) == '\t' ||
                    line.charAt(valueStart) == '\f'))
            {
                valueStart++;
            }
        }
        String value = unescape(line.substring(valueStart));
        put(key, value);
    }

    private static String unescape(String text)
    {
        if (text.indexOf('\\') < 0)
        {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length())
        {
            char c = text.charAt(i++);
            if (c != '\\' || i >= text.length())
            {
                out.append(c);
                continue;
            }
            char next = text.charAt(i++);
            switch (next)
            {
            case 't':
                out.append('\t');
                break;
            case 'n':
                out.append('\n');
                break;
            case 'r':
                out.append('\r');
                break;
            case 'f':
                out.append('\f');
                break;
            case 'u':
                if (i + 4 <= text.length())
                {
                    try
                    {
                        out.append((char)Integer.parseInt(text.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    }
                    catch (NumberFormatException ignored)
                    {
                    }
                }
                out.append('u');
                break;
            default:
                out.append(next);
                break;
            }
        }
        return out.toString();
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
