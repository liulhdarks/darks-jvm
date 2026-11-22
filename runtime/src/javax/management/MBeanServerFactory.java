package javax.management;

import java.util.ArrayList;

public class MBeanServerFactory
{
    private static final ArrayList<MBeanServer> SERVERS = new ArrayList<MBeanServer>();

    private MBeanServerFactory()
    {
    }

    public static ArrayList<MBeanServer> findMBeanServer(String agentId)
    {
        ArrayList<MBeanServer> result = new ArrayList<MBeanServer>();
        synchronized (SERVERS)
        {
            for (int i = 0; i < SERVERS.size(); i++)
            {
                result.add(SERVERS.get(i));
            }
        }
        return result;
    }

    public static MBeanServer createMBeanServer()
    {
        return createMBeanServer(null);
    }

    public static MBeanServer createMBeanServer(String domain)
    {
        MBeanServer server = new BaseMBeanServer(domain);
        synchronized (SERVERS)
        {
            SERVERS.add(server);
        }
        return server;
    }

    public static synchronized void releaseMBeanServer(MBeanServer mbeanServer)
    {
        SERVERS.remove(mbeanServer);
    }
}
