package java.lang.reflect;

import java.lang.annotation.Annotation;

public final class Constructor<T> extends Executable implements Member
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

    public boolean isVarArgs()
    {
        return (modifiers & 0x0080) != 0;
    }

    public int getParameterCount()
    {
        Class[] types = getParameterTypes();
        return types == null ? 0 : types.length;
    }

    public boolean isSynthetic()
    {
        return (modifiers & 0x1000) != 0;
    }

    public native Class[] getParameterTypes();

    public String getName()
    {
        return declaringClass == null ? null : declaringClass.getName();
    }

    void specificToGenericStringHeader(StringBuilder sb)
    {
    }

    void specificToStringHeader(StringBuilder sb)
    {
    }

    byte[] getAnnotationBytes()
    {
        return null;
    }

    boolean hasGenericInformation()
    {
        return false;
    }

    sun.reflect.generics.repository.ConstructorRepository getGenericInfo()
    {
        return null;
    }

    Class<?>[] getSharedParameterTypes()
    {
        return getParameterTypes();
    }

    Class<?>[] getSharedExceptionTypes()
    {
        return getExceptionTypes();
    }

    public String toGenericString()
    {
        return toString();
    }

    public java.lang.annotation.Annotation[][] getParameterAnnotations()
    {
        java.lang.annotation.Annotation[][] result = getParameterAnnotations0();
        if (result != null)
        {
            return result;
        }
        Class[] types = getParameterTypes();
        int count = types == null ? 0 : types.length;
        java.lang.annotation.Annotation[][] empty =
            new java.lang.annotation.Annotation[count][];
        for (int i = 0; i < count; i++)
        {
            empty[i] = new java.lang.annotation.Annotation[0];
        }
        return empty;
    }

    private native java.lang.annotation.Annotation[][] getParameterAnnotations0();

    boolean handleParameterNumberMismatch(int resultLength, Class<?>[] actual)
    {
        return false;
    }

    public java.lang.reflect.AnnotatedType getAnnotatedReturnType()
    {
        return java.lang.reflect.AnnotatedType.of(getDeclaringClass());
    }

    public java.lang.reflect.AnnotatedType[] getAnnotatedParameterTypes()
    {
        return java.lang.reflect.AnnotatedType.ofArray(getGenericParameterTypes());
    }

    public java.lang.reflect.AnnotatedType[] getAnnotatedExceptionTypes()
    {
        return java.lang.reflect.AnnotatedType.ofArray(getGenericExceptionTypes());
    }

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

    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof Constructor))
        {
            return false;
        }
        Constructor value = (Constructor)other;
        if (declaringClass != value.declaringClass)
        {
            return false;
        }
        Class[] mine = getParameterTypes();
        Class[] theirs = value.getParameterTypes();
        if (mine.length != theirs.length)
        {
            return false;
        }
        for (int i = 0; i < mine.length; i++)
        {
            if (mine[i] != theirs[i])
            {
                return false;
            }
        }
        return true;
    }

    public int hashCode()
    {
        return declaringClass.getName().hashCode();
    }
}
