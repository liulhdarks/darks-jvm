package javax.management.openmbean;

public abstract class OpenType<T> implements java.io.Serializable
{
    private final String typeName;
    private final String description;

    protected OpenType(String typeName, String description)
    {
        this.typeName = typeName;
        this.description = description;
    }

    public String getTypeName()
    {
        return typeName;
    }

    public String getDescription()
    {
        return description;
    }

    public String getClassName()
    {
        return Object.class.getName();
    }

    public boolean isArray()
    {
        return false;
    }
}
