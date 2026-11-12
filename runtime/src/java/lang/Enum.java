package java.lang;

/**
 * Minimal java.lang.Enum runtime support for darks-jvm.
 *
 * javac compiles a Java enum into a final class extending this type; the
 * generated class owns the constants and the static values()/valueOf(String)
 * helpers.  This class only provides the common state (name/ordinal) and the
 * reflective valueOf(Class, String) hook used by the generated code.
 */
public abstract class Enum<E extends Enum<E>>
{
    private final String name;
    private final int ordinal;

    protected Enum(String name, int ordinal)
    {
        this.name = name;
        this.ordinal = ordinal;
    }

    public final String name()
    {
        return name;
    }

    public final int ordinal()
    {
        return ordinal;
    }

    public String toString()
    {
        return name;
    }

    public final boolean equals(Object other)
    {
        return this == other;
    }

    public final int hashCode()
    {
        return super.hashCode();
    }

    public final int compareTo(E other)
    {
        return ordinal - other.ordinal();
    }

    public native static <T extends Enum<T>> T valueOf(Class enumType, String name);
}
