package javax.management;

public class Notification
{
    private final String type;
    private final Object source;
    private final long sequenceNumber;
    private final long timeStamp;
    private String message;
    private Object userData;

    public Notification(String type, Object source, long sequenceNumber)
    {
        this(type, source, sequenceNumber, System.currentTimeMillis());
    }

    public Notification(String type, Object source, long sequenceNumber, long timeStamp)
    {
        this.type = type;
        this.source = source;
        this.sequenceNumber = sequenceNumber;
        this.timeStamp = timeStamp;
    }

    public Notification(String type, Object source, long sequenceNumber,
        String message)
    {
        this(type, source, sequenceNumber);
        this.message = message;
    }

    public Notification(String type, Object source, long sequenceNumber,
        long timeStamp, String message)
    {
        this(type, source, sequenceNumber, timeStamp);
        this.message = message;
    }

    public String getType()
    {
        return type;
    }

    public Object getSource()
    {
        return source;
    }

    public void setSource(Object source)
    {
        throw new UnsupportedOperationException("source is immutable here");
    }

    public long getSequenceNumber()
    {
        return sequenceNumber;
    }

    public long getTimeStamp()
    {
        return timeStamp;
    }

    public String getMessage()
    {
        return message;
    }

    public Object getUserData()
    {
        return userData;
    }

    public void setUserData(Object userData)
    {
        this.userData = userData;
    }

    public String toString()
    {
        return type + "[source=" + source + ", seq=" + sequenceNumber
            + ", time=" + timeStamp + ", msg=" + message + "]";
    }
}
