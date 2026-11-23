package javax.naming;

public abstract class RefAddr
{
    protected String addrType;

    protected RefAddr(String addrType)
    {
        this.addrType = addrType;
    }

    public String getType()
    {
        return addrType;
    }

    public abstract Object getContent();

    public boolean equals(Object other)
    {
        if (!(other instanceof RefAddr))
        {
            return false;
        }
        RefAddr o = (RefAddr)other;
        Object a = getContent();
        Object b = o.getContent();
        return addrType.equals(o.addrType) && (a == null ? b == null : a.equals(b));
    }

    public int hashCode()
    {
        Object value = getContent();
        return addrType.hashCode() + (value == null ? 0 : value.hashCode());
    }

    public String toString()
    {
        return getType() + ": " + getContent();
    }
}
