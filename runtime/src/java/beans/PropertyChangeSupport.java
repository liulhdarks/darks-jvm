package java.beans;

import java.util.ArrayList;

public class PropertyChangeSupport
{
    private final Object source;
    private final ArrayList<PropertyChangeListener> listeners =
        new ArrayList<PropertyChangeListener>();
    private final java.util.HashMap<String, ArrayList<PropertyChangeListener>> named =
        new java.util.HashMap<String, ArrayList<PropertyChangeListener>>();

    public PropertyChangeSupport(Object sourceBean)
    {
        if (sourceBean == null)
            throw new IllegalArgumentException();
        this.source = sourceBean;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        if (listener == null)
            return;
        synchronized (listeners)
        {
            listeners.add(listener);
        }
    }

    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener)
    {
        if (listener == null)
            return;
        if (propertyName == null)
        {
            addPropertyChangeListener(listener);
            return;
        }
        synchronized (named)
        {
            ArrayList<PropertyChangeListener> list = named.get(propertyName);
            if (list == null)
            {
                list = new ArrayList<PropertyChangeListener>();
                named.put(propertyName, list);
            }
            list.add(listener);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        if (listener == null)
            return;
        synchronized (listeners)
        {
            listeners.remove(listener);
        }
    }

    public void removePropertyChangeListener(String propertyName,
                                             PropertyChangeListener listener)
    {
        if (listener == null)
            return;
        if (propertyName == null)
        {
            removePropertyChangeListener(listener);
            return;
        }
        synchronized (named)
        {
            ArrayList<PropertyChangeListener> list = named.get(propertyName);
            if (list != null)
            {
                list.remove(listener);
                if (list.isEmpty())
                    named.remove(propertyName);
            }
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners()
    {
        synchronized (listeners)
        {
            return listeners.toArray(new PropertyChangeListener[listeners.size()]);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName)
    {
        synchronized (named)
        {
            ArrayList<PropertyChangeListener> list = named.get(propertyName);
            if (list == null)
                return new PropertyChangeListener[0];
            return list.toArray(new PropertyChangeListener[list.size()]);
        }
    }

    public boolean hasListeners(String propertyName)
    {
        synchronized (listeners)
        {
            if (!listeners.isEmpty())
                return true;
        }
        synchronized (named)
        {
            ArrayList<PropertyChangeListener> list = named.get(propertyName);
            return list != null && !list.isEmpty();
        }
    }

    public void firePropertyChange(String propertyName, Object oldValue,
                                   Object newValue)
    {
        if (oldValue == null || newValue == null || !oldValue.equals(newValue))
            firePropertyChange(new PropertyChangeEvent(source, propertyName,
                                                       oldValue, newValue));
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue)
    {
        if (oldValue != newValue)
            firePropertyChange(propertyName, Integer.valueOf(oldValue),
                               Integer.valueOf(newValue));
    }

    public void firePropertyChange(String propertyName, boolean oldValue,
                                   boolean newValue)
    {
        if (oldValue != newValue)
            firePropertyChange(propertyName, Boolean.valueOf(oldValue),
                               Boolean.valueOf(newValue));
    }

    public void firePropertyChange(PropertyChangeEvent event)
    {
        PropertyChangeListener[] all = getPropertyChangeListeners();
        for (int i = 0; i < all.length; i++)
            all[i].propertyChange(event);
        if (event.getPropertyName() == null)
            return;
        PropertyChangeListener[] match =
            getPropertyChangeListeners(event.getPropertyName());
        for (int i = 0; i < match.length; i++)
            match[i].propertyChange(event);
    }

    public void fireIndexedPropertyChange(String propertyName, int index,
                                          Object oldValue, Object newValue)
    {
        firePropertyChange(new PropertyChangeEvent(source, propertyName,
                                                   oldValue, newValue));
    }

    public void fireIndexedPropertyChange(String propertyName, int index,
                                          int oldValue, int newValue)
    {
        if (oldValue != newValue)
            fireIndexedPropertyChange(propertyName, index,
                                      Integer.valueOf(oldValue),
                                      Integer.valueOf(newValue));
    }

    public void fireIndexedPropertyChange(String propertyName, int index,
                                          boolean oldValue, boolean newValue)
    {
        if (oldValue != newValue)
            fireIndexedPropertyChange(propertyName, index,
                                      Boolean.valueOf(oldValue),
                                      Boolean.valueOf(newValue));
    }
}
