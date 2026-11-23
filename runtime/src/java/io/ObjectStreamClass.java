package java.io;

public class ObjectStreamClass implements Serializable
{
    public static ObjectStreamClass lookup(Class clazz)
    {
        return null;
    }

    public static ObjectStreamClass lookupAny(Class clazz)
    {
        return null;
    }

    public String getName()
    {
        return null;
    }

    public long getSerialVersionUID()
    {
        return 0L;
    }

    public Class forClass()
    {
        return null;
    }

    public ObjectStreamField[] getFields()
    {
        return new ObjectStreamField[0];
    }

    public ObjectStreamField getField(String name)
    {
        return null;
    }

    public String toString()
    {
        return "ObjectStreamClass";
    }
}
