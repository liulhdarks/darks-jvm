package java.lang.reflect;

final class GenericArrayTypeImpl implements GenericArrayType, Type
{
    private final Type component;

    GenericArrayTypeImpl(Type component)
    {
        this.component = component;
    }

    public Type getGenericComponentType()
    {
        return component;
    }

    public String toString()
    {
        return String.valueOf(component) + "[]";
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof GenericArrayType))
        {
            return false;
        }
        Type theirs = ((GenericArrayType)other).getGenericComponentType();
        return component == null ? theirs == null : component.equals(theirs);
    }

    public int hashCode()
    {
        return component == null ? 0 : component.hashCode();
    }
}
