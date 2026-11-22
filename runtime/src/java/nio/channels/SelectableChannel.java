package java.nio.channels;

import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;

public abstract class SelectableChannel implements Channel
{
    int fd = -1;
    boolean blocking = true;
    boolean open = true;
    final HashMap<Selector, SelectionKey> keys = new HashMap<Selector, SelectionKey>();

    protected SelectableChannel()
    {
    }

    public abstract int validOps();

    public final boolean isBlocking()
    {
        return blocking;
    }

    public final Object blockingLock()
    {
        return this;
    }

    public final SelectableChannel configureBlocking(boolean block)
    {
        this.blocking = block;
        return this;
    }

    public final boolean isRegistered()
    {
        return !keys.isEmpty();
    }

    public final SelectionKey keyFor(Selector selector)
    {
        return keys.get(selector);
    }

    public final SelectionKey register(Selector selector, int operations)
        throws ClosedChannelException
    {
        return register(selector, operations, null);
    }

    public final SelectionKey register(Selector selector, int operations,
        Object attachment) throws ClosedChannelException
    {
        if (!isOpen())
        {
            throw new ClosedChannelException();
        }
        return ((SelectorImpl)selector).register(this, operations, attachment);
    }

    public SelectorProvider provider()
    {
        return SelectorProvider.provider();
    }

    public int getNativeFd()
    {
        return fd;
    }

    public boolean isOpen()
    {
        return open;
    }

    void cancelAllKeys()
    {
        Object[] all = keys.values().toArray();
        for (int i = 0; i < all.length; i++)
        {
            ((SelectionKeyImpl)all[i]).invalidate();
        }
        keys.clear();
    }
}
