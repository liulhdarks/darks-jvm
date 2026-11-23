package javax.naming;

import java.util.Enumeration;
import java.util.Vector;

public class Reference implements Cloneable
{
    private final String className;
    private final Vector<RefAddr> addresses = new Vector<RefAddr>();
    private String classFactory;
    private String classFactoryLocation;

    public Reference(String className)
    {
        this.className = className;
    }

    public Reference(String className, String factory, String factoryLocation)
    {
        this.className = className;
        this.classFactory = factory;
        this.classFactoryLocation = factoryLocation;
    }

    public Reference(String className, RefAddr addr)
    {
        this.className = className;
        addresses.add(addr);
    }

    public String getClassName()
    {
        return className;
    }

    public String getFactoryClassName()
    {
        return classFactory;
    }

    public String getFactoryClassLocation()
    {
        return classFactoryLocation;
    }

    public RefAddr get(String addrType)
    {
        for (int i = 0; i < addresses.size(); i++)
        {
            if (addresses.get(i).getType().equals(addrType))
            {
                return addresses.get(i);
            }
        }
        return null;
    }

    public RefAddr get(int index)
    {
        return addresses.get(index);
    }

    public Enumeration<RefAddr> getAll()
    {
        return addresses.elements();
    }

    public int size()
    {
        return addresses.size();
    }

    public void add(RefAddr addr)
    {
        addresses.add(addr);
    }

    public void clear()
    {
        addresses.clear();
    }
}
