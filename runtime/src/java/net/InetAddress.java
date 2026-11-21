package java.net;

public class InetAddress implements java.io.Serializable
{
    private final String hostName;
    private final String hostAddress;

    InetAddress(String hostName, String hostAddress)
    {
        this.hostName = hostName;
        this.hostAddress = hostAddress;
    }

    public static InetAddress getLocalHost() throws UnknownHostException
    {
        String name = null;
        try
        {
            name = java.lang.System.getenv("HOSTNAME");
        }
        catch (Throwable ignored)
        {
        }
        if (name == null || name.length() == 0)
        {
            name = "localhost";
        }
        return new InetAddress(name, "127.0.0.1");
    }

    public static InetAddress getLoopbackAddress()
    {
        return new InetAddress("localhost", "127.0.0.1");
    }

    public static InetAddress getByName(String host) throws UnknownHostException
    {
        if (host == null)
        {
            throw new UnknownHostException("null host");
        }
        if (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1"))
        {
            return new InetAddress(host, host);
        }
        return new InetAddress(host, host);
    }

    public static InetAddress[] getAllByName(String host) throws UnknownHostException
    {
        return new InetAddress[] { getByName(host) };
    }

    public static InetAddress getByAddress(byte[] address) throws UnknownHostException
    {
        if (address == null || (address.length != 4 && address.length != 16))
        {
            throw new UnknownHostException("invalid address length");
        }
        String text = addressToText(address);
        return new InetAddress(text, text);
    }

    public static InetAddress getByAddress(String host, byte[] address)
        throws UnknownHostException
    {
        InetAddress resolved = getByAddress(address);
        return new InetAddress(host == null ? resolved.hostName : host, resolved.hostAddress);
    }

    private static String addressToText(byte[] address)
    {
        if (address.length == 4)
        {
            return (address[0] & 0xFF) + "." + (address[1] & 0xFF) + "."
                + (address[2] & 0xFF) + "." + (address[3] & 0xFF);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < address.length; i += 2)
        {
            if (i > 0)
            {
                builder.append(':');
            }
            int value = ((address[i] & 0xFF) << 8) | (address[i + 1] & 0xFF);
            builder.append(Integer.toHexString(value));
        }
        return builder.toString();
    }

    public String getHostName()
    {
        return hostName;
    }

    public String getCanonicalHostName()
    {
        return hostName;
    }

    public String getHostAddress()
    {
        return hostAddress;
    }

    public byte[] getAddress()
    {
        String[] parts = hostAddress.split("\\.");
        if (parts.length == 4)
        {
            byte[] result = new byte[4];
            for (int i = 0; i < 4; i++)
            {
                result[i] = (byte)Integer.parseInt(parts[i]);
            }
            return result;
        }
        return new byte[] { 127, 0, 0, 1 };
    }

    public boolean isAnyLocalAddress()
    {
        return "0.0.0.0".equals(hostAddress);
    }

    public boolean isLoopbackAddress()
    {
        return "127.0.0.1".equals(hostAddress) || "::1".equals(hostAddress);
    }

    public boolean isMulticastAddress()
    {
        return false;
    }

    public boolean isReachable(int timeout)
    {
        return isLoopbackAddress();
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof InetAddress))
        {
            return false;
        }
        return hostAddress.equals(((InetAddress)other).hostAddress);
    }

    public int hashCode()
    {
        return hostAddress.hashCode();
    }

    public String toString()
    {
        return hostName + "/" + hostAddress;
    }
}
