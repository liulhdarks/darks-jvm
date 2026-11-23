package java.lang.reflect;

import java.lang.annotation.Annotation;

public abstract class Executable extends AccessibleObject
    implements Member, GenericDeclaration, AnnotatedElement
{
    Executable()
    {
    }

    public abstract Class getDeclaringClass();

    public abstract int getModifiers();

    public abstract Class[] getParameterTypes();

    public abstract Class[] getExceptionTypes();

    public boolean isVarArgs()
    {
        return (getModifiers() & 0x0080) != 0;
    }

    public boolean isSynthetic()
    {
        return (getModifiers() & 0x1000) != 0;
    }

    public int getParameterCount()
    {
        Class[] types = getParameterTypes();
        return types == null ? 0 : types.length;
    }

    public Parameter[] getParameters()
    {
        Class[] types = getParameterTypes();
        int count = types == null ? 0 : types.length;
        Parameter[] parameters = new Parameter[count];
        for (int i = 0; i < count; i++)
        {
            parameters[i] = new Parameter(this, types[i], i);
        }
        return parameters;
    }

    public Annotation[][] getParameterAnnotations()
    {
        Class[] types = getParameterTypes();
        int count = types == null ? 0 : types.length;
        Annotation[][] result = new Annotation[count][];
        for (int i = 0; i < count; i++)
        {
            result[i] = new Annotation[0];
        }
        return result;
    }

    public abstract Type[] getGenericParameterTypes();

    public abstract Type[] getGenericExceptionTypes();

    public AnnotatedType getAnnotatedReturnType()
    {
        return null;
    }

    public AnnotatedType[] getAnnotatedParameterTypes()
    {
        return AnnotatedTypeImpl.ofArray(getGenericParameterTypes());
    }

    public AnnotatedType[] getAnnotatedExceptionTypes()
    {
        return AnnotatedTypeImpl.ofArray(getGenericExceptionTypes());
    }
}
