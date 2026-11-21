package java.net;

import java.io.IOException;

public class ProtocolException extends IOException
{
    public ProtocolException()
    {
        super();
    }

    public ProtocolException(String message)
    {
        super(message);
    }
}
