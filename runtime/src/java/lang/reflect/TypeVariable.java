package java.lang.reflect;

public interface TypeVariable<D extends GenericDeclaration> extends Type
{
    String getName();

    Type[] getBounds();

    D getGenericDeclaration();
}
