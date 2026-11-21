package java.util;

public abstract class ResourceBundle
{
    private ResourceBundle parent;

    public final String getString(String key)
    {
        return (String)getObject(key);
    }

    public final String[] getStringArray(String key)
    {
        return (String[])getObject(key);
    }

    public abstract Object getObject(String key);

    public ResourceBundle getParent()
    {
        return parent;
    }

    public void setParent(ResourceBundle parent)
    {
        this.parent = parent;
    }
}
