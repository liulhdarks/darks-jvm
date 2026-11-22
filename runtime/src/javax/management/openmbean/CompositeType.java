package javax.management.openmbean;

import java.util.Set;
import java.util.TreeSet;

public class CompositeType extends OpenType<CompositeData>
{
    private final Set<String> keys;

    public CompositeType(String typeName, String description,
        String[] itemNames, String[] itemDescriptions, OpenType<?>[] itemTypes)
        throws OpenDataException
    {
        super(checkName(typeName), description);
        if (itemNames == null)
        {
            throw new OpenDataException("null item names");
        }
        Set<String> set = new TreeSet<String>();
        for (int i = 0; i < itemNames.length; i++)
        {
            set.add(itemNames[i]);
        }
        this.keys = set;
    }

    static String checkName(String name) throws OpenDataException
    {
        if (name == null || name.length() == 0)
        {
            throw new OpenDataException("empty type name");
        }
        return name;
    }

    public Set<String> keySet()
    {
        return keys;
    }

    public boolean containsKey(String name)
    {
        return keys.contains(name);
    }
}
