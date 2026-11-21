package java.lang.reflect;

import java.lang.annotation.Annotation;

public abstract class Executable extends AccessibleObject
    implements Member, GenericDeclaration
{
    public abstract Class<?> getDeclaringClass();

    public abstract String getName();

    public abstract int getModifiers();

    public abstract Class<?>[] getParameterTypes();

    public abstract int getParameterCount();

    public abstract TypeVariable<?>[] getTypeParameters();

    public abstract Type[] getGenericParameterTypes();

    public abstract Class<?>[] getExceptionTypes();

    public abstract Type[] getGenericExceptionTypes();

    public Parameter[] getParameters()
    {
        Class<?>[] types = getParameterTypes();
        Parameter[] parameters = new Parameter[types.length];
        for (int i = 0; i < types.length; i++)
        {
            parameters[i] = new Parameter(this, i, types[i]);
        }
        return parameters;
    }

    public boolean isVarArgs()
    {
        return (getModifiers() & 0x0080) != 0;
    }

    public boolean isSynthetic()
    {
        return (getModifiers() & 0x1000) != 0;
    }

    public Annotation[][] getParameterAnnotations()
    {
        Class<?>[] types = getParameterTypes();
        Annotation[][] result = new Annotation[types.length][];
        for (int i = 0; i < types.length; i++)
        {
            result[i] = new Annotation[0];
        }
        return result;
    }
}
