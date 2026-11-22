package javax.management;

public class MBeanNotificationInfo extends MBeanFeatureInfo
{
    private final String[] types;

    public MBeanNotificationInfo(String[] notifTypes, String name, String description)
    {
        super(name, description);
        this.types = notifTypes == null ? new String[0] : notifTypes;
    }

    public String[] getNotifTypes()
    {
        return types;
    }
}
