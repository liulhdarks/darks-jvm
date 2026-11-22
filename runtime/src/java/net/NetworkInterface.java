package java.net;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Single loopback interface: enough for Tomcat's unlockAccept() address search.
 */
public final class NetworkInterface
{
    private final String name;
    private final ArrayList<InetAddress> addresses = new ArrayList<InetAddress>();

    private NetworkInterface(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public String getDisplayName()
    {
        return name;
    }

    public Enumeration<InetAddress> getInetAddresses()
    {
        Vector<InetAddress> copy = new Vector<InetAddress>();
        for (int i = 0; i < addresses.size(); i++)
        {
            copy.add(addresses.get(i));
        }
        return copy.elements();
    }

    public static Enumeration<NetworkInterface> getNetworkInterfaces()
    {
        Vector<NetworkInterface> interfaces = new Vector<NetworkInterface>();
        NetworkInterface loopback = new NetworkInterface("lo");
        loopback.addresses.add(InetAddress.getLoopbackAddress());
        loopback.addresses.add(new InetAddress("any", "0.0.0.0"));
        interfaces.add(loopback);
        return interfaces.elements();
    }

    public static NetworkInterface getByInetAddress(InetAddress address)
    {
        Enumeration<NetworkInterface> interfaces = getNetworkInterfaces();
        while (interfaces.hasMoreElements())
        {
            NetworkInterface candidate = interfaces.nextElement();
            for (int i = 0; i < candidate.addresses.size(); i++)
            {
                if (candidate.addresses.get(i).equals(address))
                {
                    return candidate;
                }
            }
        }
        return null;
    }

    public static NetworkInterface getByName(String name)
    {
        return "lo".equals(name) ? getNetworkInterfaces().nextElement() : null;
    }

    public String toString()
    {
        return name;
    }
}
