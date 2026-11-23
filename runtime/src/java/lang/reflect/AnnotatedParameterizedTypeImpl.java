package java.lang.reflect;

class AnnotatedParameterizedTypeImpl extends AnnotatedTypeImpl
    implements AnnotatedParameterizedType
{
    AnnotatedParameterizedTypeImpl(Type type)
    {
        super(type, null);
    }

    public AnnotatedType[] getAnnotatedActualTypeArguments()
    {
        Type declared = getType();
        if (!(declared instanceof ParameterizedType))
        {
            return new AnnotatedType[0];
        }
        return AnnotatedTypeImpl.ofArray(
            ((ParameterizedType)declared).getActualTypeArguments());
    }
}
