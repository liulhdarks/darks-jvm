package java.util.jar;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Manifest
{
    private final Attributes mainAttributes = new Attributes();
    private final HashMap<String, Attributes> entries = new HashMap<String, Attributes>();

    public Manifest()
    {
    }

    public Manifest(InputStream input) throws IOException
    {
        if (input == null)
        {
            throw new NullPointerException("input");
        }
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        int value = input.read();
        while (value >= 0)
        {
            bytes.write(value);
            value = input.read();
        }
        parse(new String(bytes.toByteArray(), 0, bytes.size(), "UTF-8"));
    }

    private void parse(String text)
    {
        Attributes current = mainAttributes;
        String pendingKey = null;
        StringBuilder pendingValue = null;
        int start = 0;
        while (start <= text.length())
        {
            int end = text.indexOf('\n', start);
            if (end < 0)
            {
                end = text.length();
            }
            String line = text.substring(start, end);
            start = end + 1;
            if (line.length() > 0 && line.charAt(line.length() - 1) == '\r')
            {
                line = line.substring(0, line.length() - 1);
            }
            if (line.length() == 0)
            {
                flush(current, pendingKey, pendingValue);
                pendingKey = null;
                pendingValue = null;
                current = new Attributes();
                entries.put("", current);
                continue;
            }
            if (line.charAt(0) == ' ')
            {
                if (pendingValue != null)
                {
                    pendingValue.append(line.substring(1));
                }
                continue;
            }
            flush(current, pendingKey, pendingValue);
            int colon = line.indexOf(':');
            if (colon < 0)
            {
                pendingKey = null;
                pendingValue = null;
                continue;
            }
            pendingKey = line.substring(0, colon);
            String value = line.substring(colon + 1);
            int first = 0;
            while (first < value.length()
                && (value.charAt(first) == ' ' || value.charAt(first) == '\t'))
            {
                first++;
            }
            pendingValue = new StringBuilder(value.substring(first));
        }
        flush(current, pendingKey, pendingValue);
        entries.remove("");
    }

    private static void flush(Attributes target, String key, StringBuilder value)
    {
        if (key == null || key.length() == 0)
        {
            return;
        }
        target.putValue(key, value == null ? "" : value.toString());
    }

    public Attributes getMainAttributes()
    {
        return mainAttributes;
    }

    public Attributes getAttributes(String name)
    {
        return entries.get(name);
    }

    public Map<String, Attributes> getEntries()
    {
        return entries;
    }

    public void clear()
    {
        mainAttributes.clear();
        entries.clear();
    }
}
