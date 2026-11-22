package javax.naming;

public class Binding extends NameClassPair
{
    private static final long serialVersionUID = 8839217842691845890L;

    private Object boundObject;

    public Binding(String name, Object obj)
    {
        super(name, obj == null ? null : obj.getClass().getName());
        boundObject = obj;
    }

    public Binding(String name, Object obj, boolean isRelative)
    {
        super(name, obj == null ? null : obj.getClass().getName(), isRelative);
        boundObject = obj;
    }

    public Binding(String name, String className, Object obj)
    {
        super(name, className);
        boundObject = obj;
    }

    public Binding(String name, String className, Object obj, boolean isRelative)
    {
        super(name, className, isRelative);
        boundObject = obj;
    }

    public Object getObject()
    {
        return boundObject;
    }

    public void setObject(Object obj)
    {
        boundObject = obj;
    }

    public String getClassName()
    {
        return super.getClassName();
    }

    public void setClassName(String name)
    {
        super.setClassName(name);
    }

    public String toString()
    {
        return super.toString() + ":" + boundObject;
    }
}
