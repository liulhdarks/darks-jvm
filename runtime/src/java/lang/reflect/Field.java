package java.lang.reflect;

import java.lang.annotation.Annotation;

public final class Field extends AccessibleObject
{
    private Class declaringClass;
    private String name;
    private String descriptor;
    private int modifiers;

    Field()
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

    public native Class getType();

    public Type getGenericType()
    {
        throw new UnsupportedOperationException(
            "generic signatures are not supported");
    }

    public native Object get0(Object obj);

    public Object get(Object obj) throws IllegalAccessException
    {
        return get0(obj);
    }

    public native void set0(Object obj, Object value);

    public void set(Object obj, Object value) throws IllegalAccessException
    {
        set0(obj, value);
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
