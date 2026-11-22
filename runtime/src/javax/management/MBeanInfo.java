package javax.management;

public class MBeanInfo
{
    private final String className;
    private final String description;
    private final MBeanAttributeInfo[] attributes;
    private final MBeanConstructorInfo[] constructors;
    private final MBeanOperationInfo[] operations;
    private final MBeanNotificationInfo[] notifications;

    public MBeanInfo(String className, String description,
        MBeanAttributeInfo[] attributes, MBeanOperationInfo[] operations,
        MBeanNotificationInfo[] notifications)
    {
        this(className, description, attributes, null, operations, notifications);
    }

    public MBeanInfo(String className, String description,
        MBeanAttributeInfo[] attributes, MBeanConstructorInfo[] constructors,
        MBeanOperationInfo[] operations, MBeanNotificationInfo[] notifications)
    {
        this.className = className;
        this.description = description;
        this.attributes = attributes == null ? new MBeanAttributeInfo[0] : attributes;
        this.constructors = constructors == null ? new MBeanConstructorInfo[0] : constructors;
        this.operations = operations == null ? new MBeanOperationInfo[0] : operations;
        this.notifications = notifications == null
            ? new MBeanNotificationInfo[0] : notifications;
    }

    public String getClassName()
    {
        return className;
    }

    public String getDescription()
    {
        return description;
    }

    public MBeanAttributeInfo[] getAttributes()
    {
        return attributes;
    }

    public MBeanConstructorInfo[] getConstructors()
    {
        return constructors;
    }

    public MBeanOperationInfo[] getOperations()
    {
        return operations;
    }

    public MBeanNotificationInfo[] getNotifications()
    {
        return notifications;
    }
}
