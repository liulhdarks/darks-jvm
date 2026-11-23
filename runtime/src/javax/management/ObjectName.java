package javax.management;

import java.util.Hashtable;
import java.util.TreeMap;
import java.util.Map;
import java.util.Iterator;

/**
 * Minimal ObjectName: domain:key=value,key=value with quoting support.
 * Used by Tomcat for MBean names even when the MBean registry is disabled.
 */
public class ObjectName implements Comparable<ObjectName>
{
    private final String domain;
    private final TreeMap<String, String> properties;
    private final boolean propertyListPattern;

    public ObjectName(String name) throws MalformedObjectNameException
    {
        if (name == null)
        {
            throw new MalformedObjectNameException("null name");
        }
        int colon = name.indexOf(':');
        if (colon < 0)
        {
            throw new MalformedObjectNameException("missing domain separator: " + name);
        }
        this.domain = name.substring(0, colon).trim();
        if (this.domain.length() == 0)
        {
            throw new MalformedObjectNameException("empty domain: " + name);
        }
        this.properties = new TreeMap<String, String>();
        this.propertyListPattern = parseProperties(name.substring(colon + 1),
                                                    this.properties);
    }

    public ObjectName(String domain, Hashtable<String, String> table)
        throws MalformedObjectNameException
    {
        if (domain == null || domain.length() == 0)
        {
            throw new MalformedObjectNameException("empty domain");
        }
        this.domain = domain;
        this.properties = new TreeMap<String, String>();
        boolean pattern = false;
        if (table != null)
        {
            Iterator<String> keys = table.keySet().iterator();
            while (keys.hasNext())
            {
                String key = keys.next();
                if (key.equals("*"))
                {
                    pattern = true;
                    continue;
                }
                this.properties.put(key, table.get(key));
            }
        }
        this.propertyListPattern = pattern;
    }

    private static boolean parseProperties(String text, TreeMap<String, String> result)
        throws MalformedObjectNameException
    {
        boolean propertyListPattern = false;
        int index = 0;
        int length = text.length();
        while (index < length)
        {
            while (index < length && (text.charAt(index) == ' '
                || text.charAt(index) == '\t'))
            {
                index++;
            }
            if (index >= length)
            {
                break;
            }
            if (text.charAt(index) == '*')
            {
                index++;
                while (index < length && (text.charAt(index) == ' '
                    || text.charAt(index) == '\t'))
                {
                    index++;
                }
                if (index < length)
                {
                    throw new MalformedObjectNameException("malformed wildcard in: " + text);
                }
                propertyListPattern = true;
                break;
            }
            int equals = text.indexOf('=', index);
            if (equals < 0)
            {
                throw new MalformedObjectNameException("missing '=' in: " + text);
            }
            String key = text.substring(index, equals).trim();
            if (key.length() == 0)
            {
                throw new MalformedObjectNameException("empty key in: " + text);
            }
            index = equals + 1;
            while (index < length && (text.charAt(index) == ' '
                || text.charAt(index) == '\t'))
            {
                index++;
            }
            StringBuilder value = new StringBuilder();
            if (index < length && text.charAt(index) == '"')
            {
                index++;
                boolean closed = false;
                while (index < length)
                {
                    char c = text.charAt(index++);
                    if (c == '\\' && index < length)
                    {
                        value.append(text.charAt(index++));
                    }
                    else if (c == '"')
                    {
                        closed = true;
                        break;
                    }
                    else
                    {
                        value.append(c);
                    }
                }
                if (!closed)
                {
                    throw new MalformedObjectNameException("unterminated quote in: " + text);
                }
            }
            else
            {
                while (index < length && text.charAt(index) != ',')
                {
                    value.append(text.charAt(index++));
                }
            }
            if (result.containsKey(key))
            {
                throw new MalformedObjectNameException("duplicate key: " + key);
            }
            result.put(key, value.toString());
            if (index < length && text.charAt(index) == ',')
            {
                index++;
            }
            else
            {
                while (index < length && (text.charAt(index) == ' '
                    || text.charAt(index) == '\t'))
                {
                    index++;
                }
                if (index < length)
                {
                    throw new MalformedObjectNameException("trailing characters: " + text);
                }
            }
        }
        return propertyListPattern;
    }

    public String getDomain()
    {
        return domain;
    }

    public String getKeyProperty(String key)
    {
        return properties.get(key);
    }

    public Hashtable<String, String> getKeyPropertyList()
    {
        Hashtable<String, String> result = new Hashtable<String, String>();
        Iterator<Map.Entry<String, String>> entries = properties.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry<String, String> entry = entries.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public String getKeyPropertyListString()
    {
        return keyListString(null);
    }

    public String getKeyPropertyListString(String keyPropertyListOrder)
    {
        return keyListString(keyPropertyListOrder);
    }

    private String keyListString(String order)
    {
        StringBuilder builder = new StringBuilder();
        if (order != null && order.length() > 0)
        {
            String[] keys = order.split(",");
            for (int i = 0; i < keys.length; i++)
            {
                String key = keys[i].trim();
                if (!properties.containsKey(key))
                {
                    continue;
                }
                if (builder.length() > 0)
                {
                    builder.append(',');
                }
                builder.append(key).append('=').append(printValue(properties.get(key)));
            }
            return builder.toString();
        }
        Iterator<Map.Entry<String, String>> entries = properties.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry<String, String> entry = entries.next();
            if (builder.length() > 0)
            {
                builder.append(',');
            }
            builder.append(entry.getKey()).append('=').append(printValue(entry.getValue()));
        }
        return builder.toString();
    }

    private static String printValue(String value)
    {
        boolean plain = value.length() > 0;
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.'))
            {
                plain = false;
                break;
            }
        }
        return plain ? value : quote(value);
    }

    public String getCanonicalKeyPropertyListString()
    {
        return getKeyPropertyListString();
    }

    public String getCanonicalName()
    {
        String list = getCanonicalKeyPropertyListString();
        return list.length() == 0 ? domain + ':' : domain + ':' + list;
    }

    public boolean isPattern()
    {
        return isDomainPattern() || isPropertyPattern() || propertyListPattern;
    }

    public boolean isPropertyListPattern()
    {
        return propertyListPattern;
    }

    public boolean isDomainPattern()
    {
        return domain.indexOf('*') >= 0 || domain.indexOf('?') >= 0;
    }

    public boolean isPropertyPattern()
    {
        Iterator<String> values = properties.values().iterator();
        while (values.hasNext())
        {
            String value = values.next();
            if (value.indexOf('*') >= 0 || value.indexOf('?') >= 0)
            {
                return true;
            }
        }
        return false;
    }

    public boolean isPropertyValuePattern(String key)
    {
        String value = properties.get(key);
        return value != null && (value.indexOf('*') >= 0 || value.indexOf('?') >= 0);
    }

    public static String quote(String value)
    {
        StringBuilder builder = new StringBuilder(value.length() + 2);
        builder.append('"');
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);
            if (c == '"' || c == '\\' || c == '\n')
            {
                builder.append('\\');
            }
            builder.append(c);
        }
        builder.append('"');
        return builder.toString();
    }

    public static String unquote(String value)
    {
        if (value.length() < 2 || value.charAt(0) != '"'
            || value.charAt(value.length() - 1) != '"')
        {
            return value;
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 1; i < value.length() - 1; i++)
        {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length() - 1)
            {
                c = value.charAt(++i);
            }
            builder.append(c);
        }
        return builder.toString();
    }

    public String toString()
    {
        return getCanonicalName();
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ObjectName))
        {
            return false;
        }
        ObjectName name = (ObjectName)other;
        if (!domain.equals(name.domain) || properties.size() != name.properties.size())
        {
            return false;
        }
        Iterator<Map.Entry<String, String>> entries = properties.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry<String, String> entry = entries.next();
            String mine = entry.getValue();
            String theirs = name.properties.get(entry.getKey());
            if (mine == null ? theirs != null : !mine.equals(theirs))
            {
                return false;
            }
        }
        return true;
    }

    public int hashCode()
    {
        int hash = domain.hashCode();
        Iterator<Map.Entry<String, String>> entries = properties.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry<String, String> entry = entries.next();
            hash += entry.getKey().hashCode() * 31 + entry.getValue().hashCode();
        }
        return hash;
    }

    public int compareTo(ObjectName other)
    {
        int result = domain.compareTo(other.domain);
        if (result != 0)
        {
            return result;
        }
        return getCanonicalKeyPropertyListString().compareTo(
            other.getCanonicalKeyPropertyListString());
    }
}
