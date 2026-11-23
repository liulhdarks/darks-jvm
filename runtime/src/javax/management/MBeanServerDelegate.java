package javax.management;

/**
 * Minimal notification source for MBean server lifecycle events.  Only the
 * well known delegate name is required by clients that listen for MBean
 * registrations.
 */
public class MBeanServerDelegate implements MBeanRegistration
{
    public static final ObjectName DELEGATE_NAME = delegateName();

    private static ObjectName delegateName()
    {
        try
        {
            return new ObjectName("JMImplementation:type=MBeanServerDelegate");
        }
        catch (MalformedObjectNameException e)
        {
            throw new ExceptionInInitializerError(e);
        }
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name)
        throws Exception
    {
        return name;
    }

    public void postRegister(Boolean registrationDone)
    {
    }

    public void preDeregister() throws Exception
    {
    }

    public void postDeregister()
    {
    }
}
