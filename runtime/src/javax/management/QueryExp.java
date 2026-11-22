package javax.management;

public interface QueryExp extends java.io.Serializable
{
    boolean apply(ObjectName name) throws BadStringOperationException,
        BadAttributeValueExpException, BadBinaryOpValueExpException,
        InvalidApplicationException;

    void setMBeanServer(MBeanServer server);
}
