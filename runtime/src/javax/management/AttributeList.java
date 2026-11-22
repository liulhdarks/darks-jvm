package javax.management;

import java.util.ArrayList;

public class AttributeList extends ArrayList<Object>
{
    public AttributeList()
    {
        super();
    }

    public AttributeList(int capacity)
    {
        super(capacity);
    }

    public void add(Attribute object)
    {
        super.add(object);
    }

    public void add(int index, Attribute object)
    {
        super.add(index, object);
    }

    public boolean addAll(AttributeList list)
    {
        return super.addAll(list);
    }

    public Attribute set(int index, Attribute object)
    {
        return (Attribute)super.set(index, object);
    }

}
