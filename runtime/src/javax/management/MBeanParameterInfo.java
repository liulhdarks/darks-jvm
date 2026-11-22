package javax.management;

public class MBeanParameterInfo extends MBeanFeatureInfo
{
    private final String type;

    public MBeanParameterInfo(String name, String type, String description)
    {
        super(name, description);
        this.type = type;
    }

    public String getType()
    {
        return type;
    }
}
