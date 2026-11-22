package javax.management;

public class Attribute
{
    private final String name;
    private final Object value;

    public Attribute(String name, Object value)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("null name");
        }
        this.name = name;
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public Object getValue()
    {
        return value;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Attribute))
        {
            return false;
        }
        Attribute attribute = (Attribute)other;
        return name.equals(attribute.name)
            && (value == null ? attribute.value == null : value.equals(attribute.value));
    }

    public int hashCode()
    {
        return name.hashCode() ^ (value == null ? 0 : value.hashCode());
    }

    public String toString()
    {
        return name + " = " + value;
    }
}
