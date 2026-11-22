package java.net;

import java.io.IOException;

public class SocketException extends IOException
{
    public SocketException()
    {
    }

    public SocketException(String message)
    {
        super(message);
    }
}
