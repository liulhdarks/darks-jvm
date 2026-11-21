package java.beans;

import java.util.ArrayList;

public class PropertyEditorSupport implements PropertyEditor
{
    private Object value;
    private Object source;
    private ArrayList<PropertyChangeListener> listeners;

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
        this.value = value;
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
        return "null";
    }

    public String getAsText()
    {
        return value == null ? null : value.toString();
    }

    public void setAsText(String text) throws IllegalArgumentException
    {
        if (text == null)
        {
            throw new IllegalArgumentException();
        }
        setValue(text);
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
        if (listeners == null)
        {
            listeners = new ArrayList<PropertyChangeListener>();
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

    protected void firePropertyChange(String propertyName,
                                      Object oldValue, Object newValue)
    {
        if (listeners == null)
        {
            return;
        }
        PropertyChangeEvent event = new PropertyChangeEvent(source,
            propertyName, oldValue, newValue);
        for (int i = 0; i < listeners.size(); i++)
        {
            listeners.get(i).propertyChange(event);
        }
    }
}
