package javax.management.modelmbean;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationListener;
import javax.management.RuntimeOperationsException;

public interface ModelMBeanNotificationBroadcaster extends NotificationBroadcaster
{
    void addAttributeChangeNotificationListener(NotificationListener listener,
        String attributeName, Object handback) throws IllegalArgumentException;

    void removeAttributeChangeNotificationListener(NotificationListener listener,
        String attributeName) throws ListenerNotFoundException;

    void sendAttributeChangeNotification(AttributeChangeNotification notification)
        throws MBeanException, RuntimeOperationsException;

    void sendAttributeChangeNotification(Attribute oldValue, Attribute newValue)
        throws MBeanException, RuntimeOperationsException;

    void sendNotification(Notification notification)
        throws MBeanException, RuntimeOperationsException;

    void sendNotification(String message)
        throws MBeanException, RuntimeOperationsException;
}
