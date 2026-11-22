package java.util;

import java.io.InputStream;
import java.io.IOException;

public class PropertyResourceBundle extends ResourceBundle
{
    private final HashMap<String, Object> values = new HashMap<String, Object>();

    public PropertyResourceBundle(InputStream stream) throws IOException
    {
        byte[] data = new byte[8192];
        StringBuilder text = new StringBuilder();
        int read;
        while ((read = stream.read(data)) > 0)
        {
            text.append(new String(data, 0, read, "ISO-8859-1"));
        }
        String[] lines = text.toString().split("\n");
        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i].trim();
            if (line.length() == 0 || line.startsWith("#") || line.startsWith("!"))
            {
                continue;
            }
            int equals = line.indexOf('=');
            if (equals < 0)
            {
                equals = line.indexOf(':');
            }
            if (equals <= 0)
            {
                continue;
            }
            values.put(line.substring(0, equals).trim(),
                line.substring(equals + 1).trim());
        }
    }

    public Object getObject(String key)
    {
        Object value = values.get(key);
        if (value == null && getParent() != null)
        {
            return getParent().getObject(key);
        }
        if (value == null)
        {
            throw new MissingResourceException("key not found: " + key,
                getClass().getName(), key);
        }
        return value;
    }

    public boolean handleKey(String key)
    {
        return values.containsKey(key);
    }
}
