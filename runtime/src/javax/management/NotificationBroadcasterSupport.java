package javax.management;

import java.util.ArrayList;
import java.util.Iterator;

public class NotificationBroadcasterSupport implements NotificationEmitter
{
    private static class Entry
    {
        NotificationListener listener;
        NotificationFilter filter;
        Object handback;
    }

    private final ArrayList<Entry> listeners = new ArrayList<Entry>();

    public NotificationBroadcasterSupport()
    {
    }

    public void addNotificationListener(NotificationListener listener,
        NotificationFilter filter, Object handback)
    {
        if (listener == null)
        {
            throw new IllegalArgumentException("null listener");
        }
        Entry entry = new Entry();
        entry.listener = listener;
        entry.filter = filter;
        entry.handback = handback;
        synchronized (listeners)
        {
            listeners.add(entry);
        }
    }

    public void removeNotificationListener(NotificationListener listener)
        throws ListenerNotFoundException
    {
        boolean removed = false;
        synchronized (listeners)
        {
            for (int i = listeners.size() - 1; i >= 0; i--)
            {
                if (listeners.get(i).listener == listener)
                {
                    listeners.remove(i);
                    removed = true;
                }
            }
        }
        if (!removed)
        {
            throw new ListenerNotFoundException("listener not registered");
        }
    }

    public void removeNotificationListener(NotificationListener listener,
        NotificationFilter filter, Object handback)
        throws ListenerNotFoundException
    {
        boolean removed = false;
        synchronized (listeners)
        {
            for (int i = listeners.size() - 1; i >= 0; i--)
            {
                Entry entry = listeners.get(i);
                if (entry.listener == listener && entry.filter == filter
                    && entry.handback == handback)
                {
                    listeners.remove(i);
                    removed = true;
                }
            }
        }
        if (!removed)
        {
            throw new ListenerNotFoundException("listener not registered");
        }
    }

    public MBeanNotificationInfo[] getNotificationInfo()
    {
        return new MBeanNotificationInfo[0];
    }

    public void sendNotification(Notification notification)
    {
        Entry[] snapshot;
        synchronized (listeners)
        {
            snapshot = new Entry[listeners.size()];
            listeners.toArray(snapshot);
        }
        for (int i = 0; i < snapshot.length; i++)
        {
            Entry entry = snapshot[i];
            if (entry.filter == null || entry.filter.isNotificationEnabled(notification))
            {
                entry.listener.handleNotification(notification, entry.handback);
            }
        }
    }
}
