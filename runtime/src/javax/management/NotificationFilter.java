package javax.management;

public interface NotificationFilter extends java.io.Serializable
{
    boolean isNotificationEnabled(Notification notification);
}
