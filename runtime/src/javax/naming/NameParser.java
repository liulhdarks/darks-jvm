package javax.naming;

public interface NameParser
{
    Name parse(String name) throws NamingException;
}
