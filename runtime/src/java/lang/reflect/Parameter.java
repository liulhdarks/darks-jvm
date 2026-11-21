package java.lang.reflect;

import java.lang.annotation.Annotation;

public final class Parameter implements AnnotatedElement
{
    private final Executable executable;
    private final Class type;
    private final int index;

    Parameter(Executable executable, Class type, int index)
    {
        this.executable = executable;
        this.type = type;
        this.index = index;
    }

    public String getName()
    {
        return "arg" + index;
    }

    public int getModifiers()
    {
        return 0;
    }

    public boolean isNamePresent()
    {
        return false;
    }

    public Executable getDeclaringExecutable()
    {
        return executable;
    }

    public int getIndex()
    {
        return index;
    }

    public Class getType()
    {
        return type;
    }

    public Type getParameterizedType()
    {
        return type;
    }

    public boolean isImplicit()
    {
        return false;
    }

    public boolean isSynthetic()
    {
        return false;
    }

    public boolean isVarArgs()
    {
        return executable != null && executable.isVarArgs()
            && index == executable.getParameterCount() - 1;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
    {
        return null;
    }

    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass)
    {
        return (T[])java.lang.reflect.Array.newInstance(annotationClass, 0);
    }

    public Annotation[] getAnnotations()
    {
        return new Annotation[0];
    }

    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass)
    {
        return null;
    }

    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass)
    {
        return (T[])java.lang.reflect.Array.newInstance(annotationClass, 0);
    }

    public Annotation[] getDeclaredAnnotations()
    {
        return new Annotation[0];
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass)
    {
        return false;
    }

    public String toString()
    {
        return type == null ? "null" : type.getName();
    }
}
