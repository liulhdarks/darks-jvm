package java.lang.reflect;

import java.lang.annotation.Annotation;

public final class Method extends AccessibleObject
{
    private Class declaringClass;
    private String name;
    private String descriptor;
    private int modifiers;

    Method()
    {
    }

    public String getName()
    {
        return name;
    }

    public Class getDeclaringClass()
    {
        return declaringClass;
    }

    public int getModifiers()
    {
        return modifiers;
    }

    public native Class getReturnType();

    public native Class[] getParameterTypes();

    public native Class[] getExceptionTypes();

    public Type getGenericReturnType()
    {
        throw new UnsupportedOperationException(
            "generic signatures are not supported");
    }

    public Type[] getGenericParameterTypes()
    {
        throw new UnsupportedOperationException(
            "generic signatures are not supported");
    }

    public Type[] getGenericExceptionTypes()
    {
        throw new UnsupportedOperationException(
            "generic signatures are not supported");
    }

    public TypeVariable<Method>[] getTypeParameters()
    {
        throw new UnsupportedOperationException(
            "generic signatures are not supported");
    }

    public native Object invoke0(Object obj, Object[] args);

    public Object invoke(Object obj, Object... args)
        throws IllegalAccessException, InvocationTargetException
    {
        return invoke0(obj, args);
    }

    public native Annotation[] getDeclaredAnnotations0();

    public Annotation[] getDeclaredAnnotations()
    {
        return getDeclaredAnnotations0();
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
    {
        Annotation[] all = getDeclaredAnnotations0();
        for (int i = 0; i < all.length; i++)
        {
            if (annotationClass.isInstance(all[i]))
            {
                return (T)all[i];
            }
        }
        return null;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass)
    {
        return getAnnotation(annotationClass) != null;
    }

    public String toString()
    {
        return declaringClass.getName() + "." + name;
    }
}
