package java.beans;

import java.util.HashMap;
import java.util.Map;

/**
 * Common metadata shared by the java.beans descriptor types.
 */
public class FeatureDescriptor
{
    private String name;
    private String displayName;
    private String shortDescription;
    private boolean expert;
    private boolean hidden;
    private boolean preferred;
    private Map<String, Object> values;

    public FeatureDescriptor()
    {
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDisplayName()
    {
        return displayName == null ? name : displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public String getShortDescription()
    {
        return shortDescription == null ? displayName : shortDescription;
    }

    public void setShortDescription(String shortDescription)
    {
        this.shortDescription = shortDescription;
    }

    public boolean isExpert()
    {
        return expert;
    }

    public void setExpert(boolean expert)
    {
        this.expert = expert;
    }

    public boolean isHidden()
    {
        return hidden;
    }

    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }

    public boolean isPreferred()
    {
        return preferred;
    }

    public void setPreferred(boolean preferred)
    {
        this.preferred = preferred;
    }

    public Object getValue(String attributeName)
    {
        return values == null ? null : values.get(attributeName);
    }

    public void setValue(String attributeName, Object value)
    {
        if (values == null)
        {
            values = new HashMap<String, Object>();
        }
        if (value == null)
        {
            values.remove(attributeName);
        }
        else
        {
            values.put(attributeName, value);
        }
    }

    public String toString()
    {
        return name == null ? getClass().getName() : name;
    }
}
