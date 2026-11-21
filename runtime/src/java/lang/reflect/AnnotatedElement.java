package java.lang.reflect;

import java.lang.annotation.Annotation;

public interface AnnotatedElement
{
    boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    Annotation[] getAnnotations();

    <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass);

    <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass);

    Annotation[] getDeclaredAnnotations();

    <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass);
}
