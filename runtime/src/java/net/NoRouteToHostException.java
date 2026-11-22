package java.net;

public class NoRouteToHostException extends SocketException
{
    public NoRouteToHostException(String message)
    {
        super(message);
    }
}
