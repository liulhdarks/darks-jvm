package javax.naming;

public interface NamingEnumeration<T> extends java.util.Enumeration<T>
{
    T next() throws NamingException;

    boolean hasMore() throws NamingException;

    void close() throws NamingException;
}
