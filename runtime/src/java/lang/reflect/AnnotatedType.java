package java.lang.reflect;

/**
 * A {@link Type} together with the type-use annotations declared on its use
 * site.  Only the shape of the type is exposed by this runtime; type-use
 * annotations are not retained in class files by the runtime's parser.
 */
public interface AnnotatedType extends AnnotatedElement
{
    Type getType();

    static AnnotatedType of(Type type)
    {
        return AnnotatedTypeImpl.of(type);
    }

    static AnnotatedType[] ofArray(Type[] types)
    {
        return AnnotatedTypeImpl.ofArray(types);
    }
}
