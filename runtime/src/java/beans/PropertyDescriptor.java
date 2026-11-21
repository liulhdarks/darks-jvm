package java.beans;

import java.lang.reflect.Method;

public class PropertyDescriptor extends FeatureDescriptor
{
    private Class propertyType;
    private Method readMethod;
    private Method writeMethod;
    private Class propertyEditorClass;
    private boolean bound;
    private boolean constrained;

    public PropertyDescriptor(String propertyName, Class beanClass)
        throws IntrospectionException
    {
        setName(propertyName);
        if (beanClass == null)
        {
            throw new IntrospectionException("null bean class");
        }
        String suffix = capitalize(propertyName);
        Method[] methods = beanClass.getMethods();
        for (int i = 0; i < methods.length; i++)
        {
            Method method = methods[i];
            Class[] parameters = method.getParameterTypes();
            if (parameters.length == 0 &&
                (method.getName().equals("get" + suffix) ||
                 method.getName().equals("is" + suffix)) &&
                method.getReturnType() != void.class)
            {
                readMethod = method;
            }
            else if (parameters.length == 1 &&
                method.getName().equals("set" + suffix))
            {
                writeMethod = method;
            }
        }
        if (readMethod == null && writeMethod == null)
        {
            throw new IntrospectionException(
                "no getter or setter for " + propertyName);
        }
        if (readMethod != null)
        {
            propertyType = readMethod.getReturnType();
        }
        else
        {
            propertyType = writeMethod.getParameterTypes()[0];
        }
    }

    public PropertyDescriptor(String propertyName, Class beanClass,
        Method readMethod, Method writeMethod) throws IntrospectionException
    {
        setName(propertyName);
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        if (readMethod != null)
        {
            propertyType = readMethod.getReturnType();
        }
        else if (writeMethod != null && writeMethod.getParameterTypes().length == 1)
        {
            propertyType = writeMethod.getParameterTypes()[0];
        }
    }

    public PropertyDescriptor(String propertyName, Method readMethod,
        Method writeMethod) throws IntrospectionException
    {
        this(propertyName, null, readMethod, writeMethod);
    }

    public Class getPropertyType()
    {
        return propertyType;
    }

    public Class getPropertyEditorClass()
    {
        return propertyEditorClass;
    }

    public void setPropertyEditorClass(Class propertyEditorClass)
    {
        this.propertyEditorClass = propertyEditorClass;
    }

    public boolean isBound()
    {
        return bound;
    }

    public void setBound(boolean bound)
    {
        this.bound = bound;
    }

    public boolean isConstrained()
    {
        return constrained;
    }

    public void setConstrained(boolean constrained)
    {
        this.constrained = constrained;
    }

    public Method getReadMethod()
    {
        return readMethod;
    }

    public void setReadMethod(Method readMethod)
    {
        this.readMethod = readMethod;
        if (propertyType == null && readMethod != null)
        {
            propertyType = readMethod.getReturnType();
        }
    }

    public Method getWriteMethod()
    {
        return writeMethod;
    }

    public void setWriteMethod(Method writeMethod)
    {
        this.writeMethod = writeMethod;
        if (propertyType == null && writeMethod != null &&
            writeMethod.getParameterTypes().length == 1)
        {
            propertyType = writeMethod.getParameterTypes()[0];
        }
    }

    private static String capitalize(String text)
    {
        if (text == null || text.length() == 0)
        {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
