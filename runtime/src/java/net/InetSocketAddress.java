package java.net;

public class InetSocketAddress extends SocketAddress
{
    private final InetAddress address;
    private final int port;
    private final String hostName;

    public InetSocketAddress(int port)
    {
        this.port = port;
        this.hostName = null;
        this.address = wildcard();
    }

    private static InetAddress wildcard()
    {
        try
        {
            return InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 });
        }
        catch (UnknownHostException e)
        {
            return InetAddress.getLoopbackAddress();
        }
    }

    public InetSocketAddress(InetAddress address, int port)
    {
        this.address = address == null ? wildcard() : address;
        this.port = port;
        this.hostName = this.address.getHostName();
    }

    public InetSocketAddress(String hostName, int port)
    {
        this.port = port;
        this.hostName = hostName;
        InetAddress resolved = null;
        try
        {
            resolved = InetAddress.getByName(hostName);
        }
        catch (UnknownHostException ignored)
        {
        }
        this.address = resolved;
    }

    public static InetSocketAddress createUnresolved(String host, int port)
    {
        return new InetSocketAddress(host, port, true);
    }

    private InetSocketAddress(String hostName, int port, boolean unresolved)
    {
        this.port = port;
        this.hostName = hostName;
        this.address = null;
    }

    public final int getPort()
    {
        return port;
    }

    public final InetAddress getAddress()
    {
        return address;
    }

    public final String getHostName()
    {
        return hostName != null ? hostName
            : (address == null ? "0.0.0.0" : address.getHostName());
    }

    public final String getHostString()
    {
        return hostName != null ? hostName
            : (address == null ? "0.0.0.0" : address.getHostAddress());
    }

    public final boolean isUnresolved()
    {
        return address == null;
    }

    public String toString()
    {
        if (isUnresolved())
        {
            return hostName + ":" + port;
        }
        return address.getHostName() + "/" + address.getHostAddress() + ":" + port;
    }

    public final boolean equals(Object other)
    {
        if (!(other instanceof InetSocketAddress))
        {
            return false;
        }
        InetSocketAddress address = (InetSocketAddress)other;
        if (port != address.port)
        {
            return false;
        }
        if (this.address == null)
        {
            return address.address == null
                && (hostName == null ? address.hostName == null
                    : hostName.equals(address.hostName));
        }
        return this.address.equals(address.address);
    }

    public final int hashCode()
    {
        int hash = 31 * port + (address == null ? 0 : address.hashCode());
        return hash;
    }
}
