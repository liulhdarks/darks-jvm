package javax.naming;

public class InitialContext
{
    private final java.util.Hashtable environment;

    public InitialContext()
    {
        this.environment = new java.util.Hashtable();
    }

    public InitialContext(java.util.Hashtable environment)
    {
        this.environment = environment == null ? new java.util.Hashtable() : environment;
    }

    public Object lookup(String name) throws NamingException
    {
        throw new NamingException("JNDI lookups are not supported: " + name);
    }

    public Object lookup(Name name) throws NamingException
    {
        return lookup(name.toString());
    }

    public void close() throws NamingException
    {
    }

    public java.util.Hashtable getEnvironment() throws NamingException
    {
        return environment;
    }

    public Object addToEnvironment(String name, Object value) throws NamingException
    {
        return environment.put(name, value);
    }

    public Object removeFromEnvironment(String name) throws NamingException
    {
        return environment.remove(name);
    }
}
