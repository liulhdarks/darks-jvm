package java.lang.reflect;

import java.lang.annotation.Annotation;

public final class Constructor<T> extends AccessibleObject
    implements GenericDeclaration, AnnotatedElement
{
    private Class<T> declaringClass;
    private String descriptor;
    private int modifiers;
    private TypeVariable<Constructor<T>>[] typeParametersCache;

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

    native String getSignature0();

    public Type[] getGenericParameterTypes()
    {
        return GenericSignatureParser.getGenericConstructorParameterTypes(this);
    }

    public Type[] getGenericExceptionTypes()
    {
        return GenericSignatureParser.getGenericConstructorExceptionTypes(this);
    }

    public TypeVariable<Constructor<T>>[] getTypeParameters()
    {
        if (typeParametersCache == null)
        {
            Type[] parsed = GenericSignatureParser.getConstructorTypeParameters(this);
            TypeVariable<Constructor<T>>[] typed =
                (TypeVariable<Constructor<T>>[])new TypeVariable[parsed.length];
            for (int i = 0; i < parsed.length; i++)
            {
                typed[i] = (TypeVariable<Constructor<T>>)parsed[i];
            }
            typeParametersCache = typed;
        }
        return typeParametersCache;
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

    public Annotation[] getAnnotations()
    {
        return getDeclaredAnnotations0();
    }

    public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass)
    {
        if (annotationClass == null)
        {
            throw new NullPointerException("annotationClass");
        }
        Annotation[] all = getDeclaredAnnotations0();
        for (int i = 0; i < all.length; i++)
        {
            if (all[i].annotationType() == annotationClass)
            {
                return (A)all[i];
            }
        }
        return null;
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationClass)
    {
        return getDeclaredAnnotation(annotationClass);
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass)
    {
        return getAnnotation(annotationClass) != null;
    }

    public <T extends Annotation> T[] getDeclaredAnnotationsByType(
        Class<T> annotationClass)
    {
        if (annotationClass == null)
        {
            throw new NullPointerException("annotationClass");
        }
        return (T[])AnnotationSupport.expand(getDeclaredAnnotations0(),
            annotationClass);
    }

    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass)
    {
        return getDeclaredAnnotationsByType(annotationClass);
    }

    public String toString()
    {
        return declaringClass.getName();
    }
}
