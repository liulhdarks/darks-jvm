package javax.management;

public class MBeanAttributeInfo extends MBeanFeatureInfo
{
    private final String type;
    private final boolean readable;
    private final boolean writable;
    private final boolean isIs;

    public MBeanAttributeInfo(String name, String type, String description,
        boolean isReadable, boolean isWritable, boolean isIs)
    {
        super(name, description);
        this.type = type;
        this.readable = isReadable;
        this.writable = isWritable;
        this.isIs = isIs;
    }

    public String getType()
    {
        return type;
    }

    public boolean isReadable()
    {
        return readable;
    }

    public boolean isWritable()
    {
        return writable;
    }

    public boolean isIs()
    {
        return isIs;
    }

    public String toString()
    {
        return getClass().getName() + "[name=" + getName() + ", type=" + type
            + ", readable=" + readable + ", writable=" + writable + "]";
    }
}
