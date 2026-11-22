package java.nio.channels;

import java.io.IOException;
import java.net.SocketOps;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

class SelectorImpl extends Selector
{
    private final ArrayList<SelectionKeyImpl> registered = new ArrayList<SelectionKeyImpl>();
    private final HashSet<SelectionKey> selected = new HashSet<SelectionKey>();
    private final int[] wakePipe = SocketOps.pipe0();
    private boolean wakeupPending;
    private boolean open = true;
    private boolean inSelect;

    SelectionKey register(SelectableChannel channel, int ops, Object attachment)
    {
        SelectionKey existing = channel.keys.get(this);
        if (existing != null)
        {
            existing.interestOps(ops);
            existing.attach(attachment);
            return existing;
        }
        SelectionKeyImpl key = new SelectionKeyImpl(channel, this, ops, attachment);
        registered.add(key);
        channel.keys.put(this, key);
        return key;
    }

    void cancel(SelectionKeyImpl key)
    {
        key.invalidate();
        registered.remove(key);
        selected.remove(key);
        if (key.channel() != null)
        {
            key.channel().keys.remove(this);
        }
        wakeup();
    }

    public boolean isOpen()
    {
        return open;
    }

    public void close() throws IOException
    {
        if (!open)
        {
            return;
        }
        open = false;
        Object[] all = registered.toArray();
        for (int i = 0; i < all.length; i++)
        {
            ((SelectionKeyImpl)all[i]).invalidate();
        }
        registered.clear();
        selected.clear();
        if (wakePipe != null)
        {
            SocketOps.close0(wakePipe[0]);
            SocketOps.close0(wakePipe[1]);
        }
    }

    public Set<SelectionKey> keys()
    {
        if (!open)
        {
            throw new ClosedSelectorException();
        }
        HashSet<SelectionKey> copy = new HashSet<SelectionKey>();
        for (int i = 0; i < registered.size(); i++)
        {
            copy.add(registered.get(i));
        }
        return copy;
    }

    public Set<SelectionKey> selectedKeys()
    {
        if (!open)
        {
            throw new ClosedSelectorException();
        }
        return selected;
    }

    public int select() throws IOException
    {
        return select(0L);
    }

    public int select(long timeout) throws IOException
    {
        if (!open)
        {
            throw new ClosedSelectorException();
        }
        if (inSelect)
        {
            throw new IllegalStateException("select in progress");
        }
        inSelect = true;
        try
        {
            long deadline = timeout > 0 ? System.currentTimeMillis() + timeout : 0;
            while (true)
            {
                if (wakeupPending)
                {
                    wakeupPending = false;
                    if (wakePipe != null)
                    {
                        SocketOps.drain0(wakePipe[0]);
                    }
                    return 0;
                }
                int ready = pollNow();
                if (ready > 0)
                {
                    return ready;
                }
                if (timeout == 0)
                {
                    return 0;
                }
                if (timeout > 0 && System.currentTimeMillis() >= deadline)
                {
                    return 0;
                }
                try
                {
                    Thread.sleep(1);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    return 0;
                }
            }
        }
        finally
        {
            inSelect = false;
        }
    }

    public int selectNow() throws IOException
    {
        if (!open)
        {
            throw new ClosedSelectorException();
        }
        if (wakeupPending)
        {
            wakeupPending = false;
            if (wakePipe != null)
            {
                SocketOps.drain0(wakePipe[0]);
            }
            return 0;
        }
        return pollNow();
    }

    private int pollNow()
    {
        ArrayList<SelectionKeyImpl> pending = new ArrayList<SelectionKeyImpl>();
        for (int i = registered.size() - 1; i >= 0; i--)
        {
            SelectionKeyImpl key = registered.get(i);
            if (!key.isValid())
            {
                registered.remove(i);
                selected.remove(key);
                continue;
            }
            key.clearReadyOps();
            pending.add(key);
        }
        int count = pending.size();
        int total = count + (wakePipe != null ? 1 : 0);
        int[] fds = new int[total];
        int[] ops = new int[total];
        int[] out = new int[total];
        for (int i = 0; i < count; i++)
        {
            fds[i] = pending.get(i).nativeFd();
            ops[i] = pending.get(i).interestOps();
        }
        if (wakePipe != null)
        {
            fds[count] = wakePipe[0];
            ops[count] = SelectionKey.OP_READ;
        }
        SocketOps.poll0(fds, ops, out);
        int ready = 0;
        for (int i = 0; i < count; i++)
        {
            int interest = pending.get(i).interestOps();
            int raw = out[i];
            int masked = 0;
            if ((raw & SelectionKey.OP_READ) != 0)
            {
                masked |= interest & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT);
            }
            if ((raw & SelectionKey.OP_WRITE) != 0)
            {
                masked |= interest & (SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
            }
            if (masked != 0)
            {
                pending.get(i).setReadyOps(masked);
                selected.add(pending.get(i));
                ready++;
            }
        }
        if (wakePipe != null && out[count] != 0)
        {
            SocketOps.drain0(wakePipe[0]);
            wakeupPending = false;
        }
        return ready;
    }

    public Selector wakeup()
    {
        wakeupPending = true;
        if (wakePipe != null)
        {
            byte[] byteValue = new byte[] { 1 };
            SocketOps.write0(wakePipe[1], byteValue, 0, 1);
        }
        return this;
    }

    public SelectorProvider provider()
    {
        return SelectorProvider.provider();
    }
}
