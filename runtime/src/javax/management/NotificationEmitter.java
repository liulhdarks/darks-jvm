package javax.management;

public interface NotificationEmitter extends NotificationBroadcaster
{
    void removeNotificationListener(NotificationListener listener,
        NotificationFilter filter, Object handback)
        throws ListenerNotFoundException;
}
