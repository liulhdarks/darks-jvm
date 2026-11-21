package java.beans;

/**
 * Convenience base class: subclasses override only the parts they provide.
 */
public class SimpleBeanInfo implements BeanInfo
{
    public BeanDescriptor getBeanDescriptor()
    {
        return null;
    }

    public PropertyDescriptor[] getPropertyDescriptors()
    {
        return new PropertyDescriptor[0];
    }

    public MethodDescriptor[] getMethodDescriptors()
    {
        return new MethodDescriptor[0];
    }

    public BeanInfo[] getAdditionalBeanInfo()
    {
        return null;
    }

    public int getDefaultPropertyIndex()
    {
        return -1;
    }

    public int getDefaultEventIndex()
    {
        return -1;
    }
}
