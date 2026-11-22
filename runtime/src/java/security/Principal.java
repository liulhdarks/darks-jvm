package java.security;

/** Minimal representation of an identity, as used by Tomcat principals. */
public interface Principal
{
    String getName();

    boolean equals(Object other);

    int hashCode();

    String toString();
}
