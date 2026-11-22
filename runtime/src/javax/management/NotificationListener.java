package javax.management;

public interface NotificationListener extends java.util.EventListener
{
    void handleNotification(Notification notification, Object handback);
}
