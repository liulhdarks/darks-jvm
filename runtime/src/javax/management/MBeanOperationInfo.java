package javax.management;

public class MBeanOperationInfo extends MBeanFeatureInfo
{
    public static final int INFO = 0;
    public static final int ACTION = 1;
    public static final int ACTION_INFO = 2;
    public static final int UNKNOWN = 3;

    private final String returnType;
    private final MBeanParameterInfo[] signature;
    private final int impact;

    public MBeanOperationInfo(String name, String description,
        MBeanParameterInfo[] signature, String returnType, int impact)
    {
        super(name, description);
        this.signature = signature == null ? new MBeanParameterInfo[0] : signature;
        this.returnType = returnType;
        this.impact = impact;
    }

    public String getReturnType()
    {
        return returnType;
    }

    public MBeanParameterInfo[] getSignature()
    {
        return signature;
    }

    public int getImpact()
    {
        return impact;
    }
}
