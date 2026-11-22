package javax.management;

public class JMException extends Exception
{
    private static final long serialVersionUID = 1L;

    public JMException() {}
    public JMException(String message) { super(message); }
}
