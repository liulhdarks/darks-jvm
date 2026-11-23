package java.lang.reflect;

class AnnotatedArrayTypeImpl extends AnnotatedTypeImpl
    implements AnnotatedArrayType
{
    AnnotatedArrayTypeImpl(Type type)
    {
        super(type, null);
    }

    public AnnotatedType getAnnotatedGenericComponentType()
    {
        Type declared = getType();
        if (declared instanceof GenericArrayType)
        {
            return AnnotatedTypeImpl.of(
                ((GenericArrayType)declared).getGenericComponentType());
        }
        if (declared instanceof Class && ((Class<?>)declared).isArray())
        {
            return AnnotatedTypeImpl.of(((Class<?>)declared).getComponentType());
        }
        return AnnotatedTypeImpl.of(Object.class);
    }
}
