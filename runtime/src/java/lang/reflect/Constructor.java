package java.lang.reflect;

import java.lang.annotation.Annotation;

public final class Constructor<T> extends AccessibleObject
{
    private Class<T> declaringClass;
    private String descriptor;
    private int modifiers;

    Constructor()
    {
    }

    public Class<T> getDeclaringClass()
    {
        return declaringClass;
    }

    public int getModifiers()
    {
        return modifiers;
    }

    public native Class[] getParameterTypes();

    public native Class[] getExceptionTypes();

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

    public TypeVariable<Constructor<T>>[] getTypeParameters()
    {
        throw new UnsupportedOperationException(
            "generic signatures are not supported");
    }

    public native Object newInstance0(Object[] args);

    public T newInstance(Object... args) throws InstantiationException,
        IllegalAccessException, InvocationTargetException
    {
        return (T)newInstance0(args);
    }

    public native Annotation[] getDeclaredAnnotations0();

    public Annotation[] getDeclaredAnnotations()
    {
        return getDeclaredAnnotations0();
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationClass)
    {
        Annotation[] all = getDeclaredAnnotations0();
        for (int i = 0; i < all.length; i++)
        {
            if (annotationClass.isInstance(all[i]))
            {
                return (A)all[i];
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
        return declaringClass.getName();
    }
}
