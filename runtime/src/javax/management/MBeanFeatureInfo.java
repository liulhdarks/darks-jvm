package javax.management;

public class MBeanFeatureInfo
{
    private final String name;
    private final String description;

    public MBeanFeatureInfo(String name, String description)
    {
        this.name = name;
        this.description = description;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof MBeanFeatureInfo))
        {
            return false;
        }
        MBeanFeatureInfo info = (MBeanFeatureInfo)other;
        return equal(name, info.name) && equal(description, info.description);
    }

    static boolean equal(Object left, Object right)
    {
        return left == null ? right == null : left.equals(right);
    }

    public int hashCode()
    {
        return (name == null ? 0 : name.hashCode())
            ^ (description == null ? 0 : description.hashCode());
    }
}
