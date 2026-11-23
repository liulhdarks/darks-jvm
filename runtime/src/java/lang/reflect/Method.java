package java.lang.reflect;

import java.lang.annotation.Annotation;

public final class Method extends Executable implements Member
{
    private Class declaringClass;
    private String name;
    private String descriptor;
    private int modifiers;
    private TypeVariable<Method>[] typeParametersCache;
    private boolean accessible;

    Method()
    {
    }

    public boolean isAccessible()
    {
        return accessible;
    }

    public void setAccessible(boolean flag)
    {
        accessible = flag;
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

    public boolean isBridge()
    {
        return (modifiers & 0x0040) != 0;
    }

    public int getParameterCount()
    {
        Class[] types = getParameterTypes();
        return types == null ? 0 : types.length;
    }

    public boolean isVarArgs()
    {
        return (modifiers & 0x0080) != 0;
    }

    public boolean isSynthetic()
    {
        return (modifiers & 0x1000) != 0;
    }

    public boolean isDefault()
    {
        return declaringClass != null && declaringClass.isInterface()
            && (modifiers & 0x0001) != 0 && (modifiers & 0x0400) == 0
            && (modifiers & 0x0008) == 0;
    }

    public native Class getReturnType();

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

    sun.reflect.generics.repository.MethodRepository getGenericInfo()
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
        return java.lang.reflect.AnnotatedType.of(getReturnType());
    }

    public java.lang.reflect.AnnotatedType[] getAnnotatedParameterTypes()
    {
        return java.lang.reflect.AnnotatedType.ofArray(getGenericParameterTypes());
    }

    public java.lang.reflect.AnnotatedType[] getAnnotatedExceptionTypes()
    {
        return java.lang.reflect.AnnotatedType.ofArray(getGenericExceptionTypes());
    }

    public native Class[] getParameterTypes();

    public native Class[] getExceptionTypes();

    native String getSignature0();

    public Type getGenericReturnType()
    {
        return GenericSignatureParser.getGenericReturnType(this);
    }

    public Type[] getGenericParameterTypes()
    {
        return GenericSignatureParser.getGenericParameterTypes(this);
    }

    public Type[] getGenericExceptionTypes()
    {
        return GenericSignatureParser.getGenericExceptionTypes(this);
    }

    public TypeVariable<Method>[] getTypeParameters()
    {
        if (typeParametersCache == null)
        {
            Type[] parsed = GenericSignatureParser.getMethodTypeParameters(this);
            TypeVariable<Method>[] typed =
                (TypeVariable<Method>[])new TypeVariable[parsed.length];
            for (int i = 0; i < parsed.length; i++)
            {
                typed[i] = (TypeVariable<Method>)parsed[i];
            }
            typeParametersCache = typed;
        }
        return typeParametersCache;
    }

    public native Object invoke0(Object obj, Object[] args);

    public Object invoke(Object obj, Object... args)
        throws IllegalAccessException, InvocationTargetException
    {
        return invoke0(obj, args);
    }

    public native Annotation[] getDeclaredAnnotations0();

    public native Object getDefaultValue();

    public Annotation[] getDeclaredAnnotations()
    {
        return getDeclaredAnnotations0();
    }

    public Annotation[] getAnnotations()
    {
        return getDeclaredAnnotations0();
    }

    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass)
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
                return (T)all[i];
            }
        }
        return null;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
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
        return declaringClass.getName() + "." + name;
    }

    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof Method))
        {
            return false;
        }
        Method value = (Method)other;
        if (declaringClass != value.declaringClass || !name.equals(value.name))
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
        return getReturnType() == value.getReturnType();
    }

    public int hashCode()
    {
        return declaringClass.getName().hashCode() ^ name.hashCode();
    }
}
