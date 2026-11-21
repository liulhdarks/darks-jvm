package java.beans;

public class BeanDescriptor extends FeatureDescriptor
{
    private Class beanClass;
    private Class customizerClass;

    public BeanDescriptor(Class beanClass)
    {
        this(beanClass, null);
    }

    public BeanDescriptor(Class beanClass, Class customizerClass)
    {
        this.beanClass = beanClass;
        this.customizerClass = customizerClass;
        if (beanClass != null)
        {
            setName(beanClass.getName());
        }
    }

    public Class getBeanClass()
    {
        return beanClass;
    }

    public Class getCustomizerClass()
    {
        return customizerClass;
    }
}
