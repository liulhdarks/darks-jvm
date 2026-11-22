package javax.management;

public interface NotificationBroadcaster
{
    void addNotificationListener(NotificationListener listener,
        NotificationFilter filter, Object handback) throws IllegalArgumentException;

    void removeNotificationListener(NotificationListener listener)
        throws ListenerNotFoundException;

    MBeanNotificationInfo[] getNotificationInfo();
}
