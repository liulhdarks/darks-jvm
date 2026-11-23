package java.lang.reflect;

import java.lang.annotation.Annotation;

class AnnotatedTypeImpl implements AnnotatedType
{
    private final Type type;
    private final Annotation[] annotations;

    AnnotatedTypeImpl(Type type, Annotation[] annotations)
    {
        this.type = type;
        this.annotations = annotations == null ? new Annotation[0] : annotations;
    }

    static AnnotatedType of(Type type)
    {
        if (type instanceof ParameterizedType)
        {
            return new AnnotatedParameterizedTypeImpl(type);
        }
        if (type instanceof GenericArrayType)
        {
            return new AnnotatedArrayTypeImpl(type);
        }
        if (type instanceof Class && ((Class<?>)type).isArray())
        {
            return new AnnotatedArrayTypeImpl(type);
        }
        return new AnnotatedTypeImpl(type, null);
    }

    static AnnotatedType[] ofArray(Type[] types)
    {
        if (types == null)
        {
            return new AnnotatedType[0];
        }
        AnnotatedType[] result = new AnnotatedType[types.length];
        for (int i = 0; i < types.length; i++)
        {
            result[i] = of(types[i]);
        }
        return result;
    }

    public Type getType()
    {
        return type;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass)
    {
        return getAnnotation(annotationClass) != null;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
    {
        for (int i = 0; i < annotations.length; i++)
        {
            if (annotationClass.isInstance(annotations[i]))
            {
                return annotationClass.cast(annotations[i]);
            }
        }
        return null;
    }

    public Annotation[] getAnnotations()
    {
        Annotation[] copy = new Annotation[annotations.length];
        for (int i = 0; i < annotations.length; i++)
        {
            copy[i] = annotations[i];
        }
        return copy;
    }

    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass)
    {
        return getDeclaredAnnotationsByType(annotationClass);
    }

    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass)
    {
        return getAnnotation(annotationClass);
    }

    public Annotation[] getDeclaredAnnotations()
    {
        return getAnnotations();
    }

    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass)
    {
        int count = 0;
        for (int i = 0; i < annotations.length; i++)
        {
            if (annotationClass.isInstance(annotations[i]))
            {
                count++;
            }
        }
        T[] result = (T[])Array.newInstance(annotationClass, count);
        int pos = 0;
        for (int i = 0; i < annotations.length; i++)
        {
            if (annotationClass.isInstance(annotations[i]))
            {
                result[pos] = annotationClass.cast(annotations[i]);
                pos++;
            }
        }
        return result;
    }

    public String toString()
    {
        return String.valueOf(type);
    }
}
