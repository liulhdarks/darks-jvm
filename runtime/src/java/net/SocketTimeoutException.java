package java.net;

import java.io.InterruptedIOException;

public class SocketTimeoutException extends InterruptedIOException
{
    public SocketTimeoutException()
    {
    }

    public SocketTimeoutException(String message)
    {
        super(message);
    }
}
