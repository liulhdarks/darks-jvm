package java.util;

public class ServiceConfigurationError extends Error
{
    public ServiceConfigurationError(String message)
    {
        super(message);
    }

    public ServiceConfigurationError(String message, Throwable cause)
    {
        super(message);
        initCause(cause);
    }
}
