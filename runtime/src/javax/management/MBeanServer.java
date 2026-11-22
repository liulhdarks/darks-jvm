package javax.management;

import java.util.Set;

public interface MBeanServer
{
    Object getAttribute(ObjectName name, String attribute)
        throws MBeanException, AttributeNotFoundException,
        InstanceNotFoundException, ReflectionException;

    void setAttribute(ObjectName name, Attribute attribute)
        throws InstanceNotFoundException, AttributeNotFoundException,
        InvalidAttributeValueException, MBeanException, ReflectionException;

    AttributeList getAttributes(ObjectName name, String[] attributes)
        throws InstanceNotFoundException, ReflectionException;

    AttributeList setAttributes(ObjectName name, AttributeList attributes)
        throws InstanceNotFoundException, ReflectionException;

    Object invoke(ObjectName name, String operationName, Object[] params,
        String[] signature)
        throws InstanceNotFoundException, MBeanException, ReflectionException;

    String getDefaultDomain();

    String[] getDomains();

    Integer getMBeanCount();

    MBeanInfo getMBeanInfo(ObjectName name)
        throws InstanceNotFoundException, IntrospectionException, ReflectionException;

    ObjectInstance registerMBean(Object object, ObjectName name)
        throws InstanceAlreadyExistsException, MBeanRegistrationException,
        NotCompliantMBeanException;

    void unregisterMBean(ObjectName name)
        throws InstanceNotFoundException, MBeanRegistrationException;

    boolean isRegistered(ObjectName name);

    ObjectInstance getObjectInstance(ObjectName name)
        throws InstanceNotFoundException;

    Set<ObjectName> queryNames(ObjectName name, QueryExp query);

    Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query);

    boolean isInstanceOf(ObjectName name, String className)
        throws InstanceNotFoundException;

    void addNotificationListener(ObjectName name, NotificationListener listener,
        NotificationFilter filter, Object handback)
        throws InstanceNotFoundException;

    void removeNotificationListener(ObjectName name, NotificationListener listener,
        NotificationFilter filter, Object handback)
        throws InstanceNotFoundException, ListenerNotFoundException;

    void removeNotificationListener(ObjectName name, NotificationListener listener)
        throws InstanceNotFoundException, ListenerNotFoundException;
}
