package java.beans;

import java.lang.reflect.Method;

public class MethodDescriptor extends FeatureDescriptor
{
    private Method method;

    public MethodDescriptor(Method method)
    {
        this.method = method;
        if (method != null)
        {
            setName(method.getName());
        }
    }

    public Method getMethod()
    {
        return method;
    }

    public void setMethod(Method method)
    {
        this.method = method;
        if (method != null)
        {
            setName(method.getName());
        }
    }
}
