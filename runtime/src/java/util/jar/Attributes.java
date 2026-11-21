package java.util.jar;

import java.util.HashMap;
import java.util.Map;

public class Attributes extends HashMap<Object, Object>
{
    private static final long serialVersionUID = 1L;

    public Attributes()
    {
    }

    public Attributes(int size)
    {
        super(size);
    }

    public Attributes(Attributes source)
    {
        putAll(source);
    }

    public String getValue(String name)
    {
        if (name == null)
        {
            return null;
        }
        return getValue(new Name(name));
    }

    public String getValue(Name name)
    {
        Object value = get(name);
        return value == null ? null : value.toString();
    }

    public Object put(Object key, Object value)
    {
        if (key == null)
        {
            throw new NullPointerException("key");
        }
        if (value != null && !(value instanceof String))
        {
            throw new ClassCastException("value must be a String");
        }
        return super.put(key, value);
    }

    public String putValue(String name, String value)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        Object previous = put(new Name(name), value);
        return previous == null ? null : previous.toString();
    }

    public static class Name
    {
        private final String name;

        public Name(String name)
        {
            if (name == null)
            {
                throw new NullPointerException("name");
            }
            this.name = name;
        }

        public String toString()
        {
            return name;
        }

        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }
            if (!(other instanceof Name))
            {
                return false;
            }
            return name.equalsIgnoreCase(((Name)other).name);
        }

        public int hashCode()
        {
            return name.toLowerCase().hashCode();
        }
    }
}
