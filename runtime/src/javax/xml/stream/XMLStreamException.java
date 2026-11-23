package javax.xml.stream;

public class XMLStreamException extends Exception
{
    private Throwable nested;
    private Location location;

    public XMLStreamException()
    {
        super();
    }

    public XMLStreamException(String msg)
    {
        super(msg);
    }

    public XMLStreamException(Throwable th)
    {
        super(th);
        nested = th;
    }

    public XMLStreamException(String msg, Throwable th)
    {
        super(msg);
        nested = th;
    }

    public XMLStreamException(String msg, Location location)
    {
        super(msg);
        this.location = location;
    }

    public XMLStreamException(String msg, Location location, Throwable th)
    {
        super(msg);
        this.location = location;
        nested = th;
    }

    public Location getLocation()
    {
        return location;
    }

    public Throwable getNestedException()
    {
        return nested;
    }
}
