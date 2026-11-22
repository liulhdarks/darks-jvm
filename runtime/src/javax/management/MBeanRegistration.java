package javax.management;

public interface MBeanRegistration
{
    ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception;

    void postRegister(Boolean registrationDone);

    void preDeregister() throws Exception;

    void postDeregister();
}
