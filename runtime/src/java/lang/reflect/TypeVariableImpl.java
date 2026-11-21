package java.lang.reflect;

final class TypeVariableImpl<D extends GenericDeclaration> implements TypeVariable<D>, Type
{
    private final String name;
    private final D declaration;
    private Type[] bounds;

    TypeVariableImpl(String name, D declaration)
    {
        this.name = name;
        this.declaration = declaration;
        this.bounds = new Type[] { Object.class };
    }

    void setBounds(Type[] bounds)
    {
        this.bounds = bounds == null || bounds.length == 0
            ? new Type[] { Object.class } : bounds;
    }

    public String getName()
    {
        return name;
    }

    public Type[] getBounds()
    {
        return bounds;
    }

    public D getGenericDeclaration()
    {
        return declaration;
    }

    public String toString()
    {
        return name;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof TypeVariable))
        {
            return false;
        }
        TypeVariable<?> variable = (TypeVariable<?>)other;
        return name.equals(variable.getName()) &&
            declaration.equals(variable.getGenericDeclaration());
    }

    public int hashCode()
    {
        return name.hashCode() ^ declaration.hashCode();
    }
}
