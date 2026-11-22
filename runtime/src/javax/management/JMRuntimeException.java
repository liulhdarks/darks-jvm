package javax.management;

public class JMRuntimeException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public JMRuntimeException() {}
    public JMRuntimeException(String message) { super(message); }
}
