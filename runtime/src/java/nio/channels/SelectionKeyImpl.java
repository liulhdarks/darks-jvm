package java.nio.channels;

class SelectionKeyImpl extends SelectionKey
{
    private final SelectableChannel channel;
    private final SelectorImpl selector;
    private int interestOps;
    private int readyOps;
    private Object attachment;
    private boolean valid = true;

    SelectionKeyImpl(SelectableChannel channel, SelectorImpl selector, int ops,
        Object attachment)
    {
        this.channel = channel;
        this.selector = selector;
        this.interestOps = ops;
        this.attachment = attachment;
    }

    public SelectableChannel channel()
    {
        return channel;
    }

    public Selector selector()
    {
        return selector;
    }

    public boolean isValid()
    {
        return valid && channel.isOpen();
    }

    public void cancel()
    {
        selector.cancel(this);
    }

    public int interestOps()
    {
        return interestOps;
    }

    public SelectionKey interestOps(int ops)
    {
        this.interestOps = ops;
        selector.wakeup();
        return this;
    }

    public int readyOps()
    {
        // Readiness is fixed by the last select() call.  Masking it with the
        // current interest set would lose the event as soon as the consumer
        // drops the corresponding interest op (Tomcat clears OP_READ before
        // dispatching the readable socket to a worker).
        return readyOps;
    }

    public Object attachment()
    {
        return attachment;
    }

    public SelectionKey attach(Object attachment)
    {
        this.attachment = attachment;
        return this;
    }

    int rawReadyOps()
    {
        return readyOps;
    }

    void setReadyOps(int ops)
    {
        this.readyOps = ops;
    }

    void clearReadyOps()
    {
        this.readyOps = 0;
    }

    void invalidate()
    {
        valid = false;
        readyOps = 0;
    }

    int nativeFd()
    {
        return channel.getNativeFd();
    }
}
