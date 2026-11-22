package javax.management;

public class ObjectInstance
{
    private final ObjectName name;
    private final String className;

    public ObjectInstance(String objectName, String className)
        throws MalformedObjectNameException
    {
        this(new ObjectName(objectName), className);
    }

    public ObjectInstance(ObjectName objectName, String className)
    {
        this.name = objectName;
        this.className = className;
    }

    public ObjectName getObjectName()
    {
        return name;
    }

    public String getClassName()
    {
        return className;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ObjectInstance))
        {
            return false;
        }
        ObjectInstance instance = (ObjectInstance)other;
        return name.equals(instance.name)
            && (className == null ? instance.className == null
                : className.equals(instance.className));
    }

    public int hashCode()
    {
        return name.hashCode() ^ (className == null ? 0 : className.hashCode());
    }

    public String toString()
    {
        return name + "/" + className;
    }
}
