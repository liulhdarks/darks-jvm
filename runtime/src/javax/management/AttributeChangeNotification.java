package javax.management;

public class AttributeChangeNotification extends Notification
{
    public static final String ATTRIBUTE_CHANGE = "jmx.attribute.change";

    private final String attributeName;
    private final String attributeType;
    private final Object oldValue;
    private final Object newValue;

    public AttributeChangeNotification(Object source, long sequenceNumber,
        long timeStamp, String message, String attributeName,
        String attributeType, Object oldValue, Object newValue)
    {
        super(ATTRIBUTE_CHANGE, source, sequenceNumber, timeStamp, message);
        this.attributeName = attributeName;
        this.attributeType = attributeType;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getAttributeName()
    {
        return attributeName;
    }

    public String getAttributeType()
    {
        return attributeType;
    }

    public Object getOldValue()
    {
        return oldValue;
    }

    public Object getNewValue()
    {
        return newValue;
    }
}
