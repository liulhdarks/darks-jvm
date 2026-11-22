package javax.naming;

public class NameClassPair implements java.io.Serializable
{
    private static final long serialVersionUID = 5620776610161004159L;

    private String name;
    private String className;
    private boolean isRelative;

    public NameClassPair(String name, String className)
    {
        this(name, className, true);
    }

    public NameClassPair(String name, String className, boolean isRelative)
    {
        this.name = name;
        this.className = className;
        this.isRelative = isRelative;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String name)
    {
        className = name;
    }

    public boolean isRelative()
    {
        return isRelative;
    }

    public void setRelative(boolean relative)
    {
        isRelative = relative;
    }

    public String toString()
    {
        return "(" + name + "," + className + ")";
    }
}
