package java.lang.reflect;

import java.lang.annotation.Annotation;

public final class Method extends Executable
    implements AnnotatedElement
{
    private Class declaringClass;
    private String name;
    private String descriptor;
    private int modifiers;
    private TypeVariable<Method>[] typeParametersCache;

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

    public boolean isBridge()
    {
        return (modifiers & 0x0040) != 0;
    }

    public boolean isSynthetic()
    {
        return (modifiers & 0x1000) != 0;
    }

    public boolean isVarArgs()
    {
        return (modifiers & 0x0080) != 0;
    }

    public boolean isDefault()
    {
        return declaringClass != null && declaringClass.isInterface() &&
            (modifiers & (0x0400 | 0x0008 | 0x0002)) == 0;
    }

    public native Class getReturnType();

    public native Class[] getParameterTypes();

    public int getParameterCount()
    {
        return getParameterTypes().length;
    }

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

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof Method))
        {
            return false;
        }
        Method other = (Method)obj;
        return declaringClass == other.declaringClass &&
            (name == null ? other.name == null : name.equals(other.name)) &&
            (descriptor == null ? other.descriptor == null
                : descriptor.equals(other.descriptor));
    }

    public int hashCode()
    {
        int h = declaringClass == null ? 0 : declaringClass.getName().hashCode();
        return h ^ (name == null ? 0 : name.hashCode());
    }

    public String toString()
    {
        return declaringClass.getName() + "." + name;
    }
}
