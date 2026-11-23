package javax.naming.event;

import javax.naming.Binding;

public class NamingEvent extends java.util.EventObject
{
    public static final int OBJECT_ADDED = 0;
    public static final int OBJECT_REMOVED = 1;
    public static final int OBJECT_RENAMED = 2;
    public static final int OBJECT_CHANGED = 3;

    private static final long serialVersionUID = -273205960291179395L;

    private final int type;
    private final Binding newBinding;
    private final Binding oldBinding;
    private final Object changeInfo;

    public NamingEvent(Object source, int type, Binding newBinding,
                       Binding oldBinding, Object changeInfo)
    {
        super(source);
        this.type = type;
        this.newBinding = newBinding;
        this.oldBinding = oldBinding;
        this.changeInfo = changeInfo;
    }

    public int getType()
    {
        return type;
    }

    public Binding getNewBinding()
    {
        return newBinding;
    }

    public Binding getOldBinding()
    {
        return oldBinding;
    }

    public Object getChangeInfo()
    {
        return changeInfo;
    }

    public Object getEventContext()
    {
        return getSource();
    }
}
