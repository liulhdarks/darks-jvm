package javax.naming;

public class LinkRef extends Reference
{
    public LinkRef(Name name)
    {
        this(name.toString());
    }

    public LinkRef(String name)
    {
        super("LinkRef");
        add(new StringRefAddr("Link Address", name));
    }

    public String getLinkName()
    {
        RefAddr addr = get("Link Address");
        return addr == null ? null : (String)addr.getContent();
    }
}
