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
