package java.beans;

public class PropertyEditorSupport implements PropertyEditor
{
    private Object value;
    private Object source;
    private java.util.List<PropertyChangeListener> listeners;

    public PropertyEditorSupport()
    {
    }

    public PropertyEditorSupport(Object source)
    {
        this.source = source;
    }

    public Object getSource()
    {
        return source;
    }

    public void setSource(Object source)
    {
        this.source = source;
    }

    public void setValue(Object value)
    {
        Object old = this.value;
        this.value = value;
        firePropertyChange(old, value);
    }

    public Object getValue()
    {
        return value;
    }

    public boolean isPaintable()
    {
        return false;
    }

    public String getJavaInitializationString()
    {
        return "???";
    }

    public String getAsText()
    {
        return value == null ? null : value.toString();
    }

    public void setAsText(String text) throws IllegalArgumentException
    {
        if (value instanceof String)
        {
            setValue(text);
            return;
        }
        throw new IllegalArgumentException("No setAsText support for "
            + (value == null ? "null" : value.getClass().getName()));
    }

    public String[] getTags()
    {
        return null;
    }

    public boolean supportsCustomEditor()
    {
        return false;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        if (listener == null)
        {
            return;
        }
        if (listeners == null)
        {
            listeners = new java.util.ArrayList<PropertyChangeListener>();
        }
        listeners.add(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        if (listeners != null)
        {
            listeners.remove(listener);
        }
    }

    public void firePropertyChange(Object oldValue, Object newValue)
    {
        if (listeners == null || oldValue == newValue ||
            (oldValue != null && oldValue.equals(newValue)))
        {
            return;
        }
        PropertyChangeEvent event = new PropertyChangeEvent(source, null,
            oldValue, newValue);
        PropertyChangeListener[] copy = new PropertyChangeListener[listeners.size()];
        copy = listeners.toArray(copy);
        for (int i = 0; i < copy.length; i++)
        {
            copy[i].propertyChange(event);
        }
    }
}
