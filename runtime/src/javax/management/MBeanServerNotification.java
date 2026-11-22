package javax.management;

public class MBeanServerNotification extends Notification
{
    public static final String REGISTRATION_NOTIFICATION =
        "JMX.mbean.registered";
    public static final String UNREGISTRATION_NOTIFICATION =
        "JMX.mbean.unregistered";

    private final ObjectName mbeanName;

    public MBeanServerNotification(String type, Object source,
        long sequenceNumber, ObjectName objectName)
    {
        super(type, source, sequenceNumber);
        this.mbeanName = objectName;
    }

    public ObjectName getMBeanName()
    {
        return mbeanName;
    }
}
