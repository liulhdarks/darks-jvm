package javax.naming;

public class StringRefAddr extends RefAddr
{
    private final String content;

    public StringRefAddr(String addrType, String addr)
    {
        super(addrType);
        this.content = addr;
    }

    public Object getContent()
    {
        return content;
    }
}
