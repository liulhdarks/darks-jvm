package javax.naming;

public class BinaryRefAddr extends RefAddr
{
    private final byte[] content;

    public BinaryRefAddr(String addrType, byte[] src)
    {
        super(addrType);
        this.content = src;
    }

    public Object getContent()
    {
        return content;
    }
}
