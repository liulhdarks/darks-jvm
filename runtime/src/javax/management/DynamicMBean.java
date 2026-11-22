package javax.management;

public interface DynamicMBean
{
    Object getAttribute(String attribute)
        throws AttributeNotFoundException, MBeanException, ReflectionException;

    void setAttribute(Attribute attribute)
        throws AttributeNotFoundException, InvalidAttributeValueException,
        MBeanException, ReflectionException;

    AttributeList getAttributes(String[] attributes);

    AttributeList setAttributes(AttributeList attributes);

    Object invoke(String actionName, Object[] params, String[] signature)
        throws MBeanException, ReflectionException;

    MBeanInfo getMBeanInfo();
}
