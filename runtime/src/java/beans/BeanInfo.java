package java.beans;

/**
 * Minimal bean metadata contract: enough for reflection based containers to
 * discover properties and public methods.
 */
public interface BeanInfo
{
    int ICON_COLOR_16x16 = 1;
    int ICON_COLOR_32x32 = 2;
    int ICON_MONO_16x16 = 3;
    int ICON_MONO_32x32 = 4;

    BeanDescriptor getBeanDescriptor();

    PropertyDescriptor[] getPropertyDescriptors();

    MethodDescriptor[] getMethodDescriptors();

    BeanInfo[] getAdditionalBeanInfo();

    int getDefaultPropertyIndex();

    int getDefaultEventIndex();
}
