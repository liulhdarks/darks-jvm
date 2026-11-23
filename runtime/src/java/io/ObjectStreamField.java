package java.io;

public class ObjectStreamField implements Comparable<ObjectStreamField>
{
    private final String name;
    private final Class type;

    public ObjectStreamField(String name, Class type)
    {
        this.name = name;
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public Class getType()
    {
        return type;
    }

    public char getTypeCode()
    {
        return 'L';
    }

    public String getTypeString()
    {
        return type == null ? null : type.getName();
    }

    public boolean isPrimitive()
    {
        return type != null && type.isPrimitive();
    }

    public int compareTo(ObjectStreamField other)
    {
        if (other == null)
        {
            return 1;
        }
        return name.compareTo(other.name);
    }

    public String toString()
    {
        return (type == null ? "?" : type.getName()) + " " + name;
    }
}
