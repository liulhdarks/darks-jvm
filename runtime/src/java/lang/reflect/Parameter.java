package java.lang.reflect;

import java.lang.annotation.Annotation;

// Parameter metadata is derived from the descriptor; parameter names and
// parameter annotations are not available.
public final class Parameter implements AnnotatedElement
{
    private final Executable executable;
    private final int index;
    private final Class<?> type;

    Parameter(Executable executable, int index, Class<?> type)
    {
        this.executable = executable;
        this.index = index;
        this.type = type;
    }

    public int getIndex()
    {
        return index;
    }

    public String getName()
    {
        return "arg" + index;
    }

    public Class<?> getType()
    {
        return type;
    }

    public Type getParameterizedType()
    {
        return type;
    }

    public int getModifiers()
    {
        return 0;
    }

    public boolean isNamePresent()
    {
        return false;
    }

    public boolean isImplicit()
    {
        return false;
    }

    public boolean isSynthetic()
    {
        return false;
    }

    public Executable getDeclaringExecutable()
    {
        return executable;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass)
    {
        return false;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
    {
        return null;
    }

    public Annotation[] getAnnotations()
    {
        return new Annotation[0];
    }

    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass)
    {
        return (T[])AnnotationSupport.expand(new Annotation[0], annotationClass);
    }

    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass)
    {
        return null;
    }

    public Annotation[] getDeclaredAnnotations()
    {
        return new Annotation[0];
    }

    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass)
    {
        return (T[])AnnotationSupport.expand(new Annotation[0], annotationClass);
    }

    public String toString()
    {
        return type == null ? "null" : type.getName();
    }
}
