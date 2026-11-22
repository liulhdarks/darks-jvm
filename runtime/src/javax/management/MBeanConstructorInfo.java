package javax.management;

public class MBeanConstructorInfo extends MBeanFeatureInfo
{
    private final MBeanParameterInfo[] signature;

    public MBeanConstructorInfo(String name, String description,
        MBeanParameterInfo[] signature)
    {
        super(name, description);
        this.signature = signature == null ? new MBeanParameterInfo[0] : signature;
    }

    public MBeanParameterInfo[] getSignature()
    {
        return signature;
    }
}
