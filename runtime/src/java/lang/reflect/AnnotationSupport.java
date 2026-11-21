package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;

/**
 * Shared helpers for the reflective annotation surface.  The native side
 * materialises runtime-visible annotations as proxies and knows how to
 * unwrap repeatable containers, so the Java layer stays free of bytecode
 * parsing.
 */
public final class AnnotationSupport
{
    private AnnotationSupport()
    {
    }

    public static Annotation[] expand(Annotation[] annotations,
        Class<? extends Annotation> type)
    {
        if (type == null)
        {
            throw new NullPointerException("annotationClass");
        }
        return expand0(annotations == null ? new Annotation[0] : annotations, type);
    }

    public static boolean isInherited(Class<?> annotationClass)
    {
        return annotationClass != null &&
            annotationClass.getAnnotation(Inherited.class) != null;
    }

    private static native Annotation[] expand0(Annotation[] annotations,
        Class annotationClass);
}
