package darks.concurrent;

/**
 * A bounded or unbounded FIFO channel.
 *
 * All channel state is guarded by one package-wide monitor.  A coarse lock
 * keeps registration, matching and wakeups atomic with respect to each other,
 * which is what makes select registration race free; the payload work outside
 * the lock is only the park/unpark handshake.  Senders and receivers are kept
 * in doubly linked FIFO queues, so matching is fair (oldest waiter first) and
 * a completed waiter is unlinked in O(1).
 *
 * Blocking uses Object.wait/notify on a per-waiter {@link Parker}; the
 * scheduler already turns that into a coroutine suspension (WaitCurrent) and
 * tracks the parker as the coroutine's GC root (block_obj).
 *
 * Never park while holding the channel lock: every blocking path releases it
 * before waiting.
 */
public final class Channel<T>
{
    static final Object LOCK = new Object();
    static final int SEND = 0;
    static final int RECV = 1;

    private static long nextId = 1;

    private final long id;
    private final int capacity;
    private Object[] buffer;
    private int head;
    private int count;
    private boolean closed;

    private Waiter sendHead;
    private Waiter sendTail;
    private Waiter recvHead;
    private Waiter recvTail;

    private Channel(int capacity)
    {
        this.id = nextId++;
        this.capacity = capacity;
        this.buffer = new Object[capacity > 0 ? capacity : 8];
    }

    public static <T> Channel<T> bounded(int capacity)
    {
        if (capacity < 0)
        {
            throw new IllegalArgumentException("capacity < 0");
        }
        return new Channel<T>(capacity);
    }

    public static <T> Channel<T> unbounded()
    {
        return new Channel<T>(-1);
    }

    public long id()
    {
        return id;
    }

    public int capacity()
    {
        return capacity;
    }

    public boolean isClosed()
    {
        synchronized (LOCK)
        {
            return closed;
        }
    }

    public void send(T value) throws InterruptedException
    {
        if (value == null)
        {
            throw new NullPointerException("null channel value");
        }
        while (true)
        {
            Waiter self;
            synchronized (LOCK)
            {
                if (closed)
                {
                    throw closedError();
                }
                Waiter receiver = claimReceiver();
                if (receiver != null)
                {
                    receiver.unlink();
                    receiver.done = true;
                    receiver.value = value;
                    receiver.parker.unpark();
                    return;
                }
                if (capacity < 0 || count < capacity)
                {
                    put(value);
                    return;
                }
                self = new Waiter(this, SEND);
                self.value = value;
                enqueueSend(self);
                self.generation = self.parker.generation();
            }
            self.await();
            synchronized (LOCK)
            {
                if (self.done)
                {
                    if (self.error != null)
                    {
                        throw self.error;
                    }
                    return;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public T receive() throws InterruptedException
    {
        while (true)
        {
            Waiter self;
            synchronized (LOCK)
            {
                if (count > 0)
                {
                    Object value = take();
                    signalOneSender();
                    return (T) value;
                }
                Waiter sender = claimSender();
                if (sender != null)
                {
                    sender.unlink();
                    sender.done = true;
                    sender.parker.unpark();
                    return (T) sender.value;
                }
                if (closed)
                {
                    return null;
                }
                self = new Waiter(this, RECV);
                enqueueRecv(self);
                self.generation = self.parker.generation();
            }
            self.await();
            synchronized (LOCK)
            {
                if (self.done)
                {
                    if (self.error != null)
                    {
                        throw self.error;
                    }
                    if (self.closedSignal)
                    {
                        return null;
                    }
                    return (T) self.value;
                }
            }
        }
    }

    public boolean trySend(T value)
    {
        if (value == null)
        {
            throw new NullPointerException("null channel value");
        }
        synchronized (LOCK)
        {
            if (closed)
            {
                return false;
            }
            Waiter receiver = claimReceiver();
            if (receiver != null)
            {
                receiver.unlink();
                receiver.done = true;
                receiver.value = value;
                receiver.parker.unpark();
                return true;
            }
            if (capacity < 0 || count < capacity)
            {
                put(value);
                return true;
            }
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public T tryReceive()
    {
        synchronized (LOCK)
        {
            if (count > 0)
            {
                Object value = take();
                signalOneSender();
                return (T) value;
            }
            Waiter sender = claimSender();
            if (sender != null)
            {
                sender.unlink();
                sender.done = true;
                sender.parker.unpark();
                return (T) sender.value;
            }
            return null;
        }
    }

    /**
     * Closes the channel.  The buffer is still drained by receivers; blocked
     * senders fail with {@link ClosedChannelException} and blocked receivers
     * observe end of stream.  Closing an already closed channel is a no-op.
     */
    public void close()
    {
        synchronized (LOCK)
        {
            if (closed)
            {
                return;
            }
            closed = true;
            Waiter sender = sendHead;
            while (sender != null)
            {
                Waiter next = sender.next;
                if (claimWaiter(sender))
                {
                    sender.unlink();
                    sender.done = true;
                    sender.error = closedError();
                    sender.parker.unpark();
                }
                sender = next;
            }
            Waiter receiver = recvHead;
            while (receiver != null)
            {
                Waiter next = receiver.next;
                if (claimWaiter(receiver))
                {
                    receiver.unlink();
                    receiver.done = true;
                    receiver.closedSignal = true;
                    receiver.parker.unpark();
                }
                receiver = next;
            }
        }
    }

    // ---- internals, all called with LOCK held ---------------------------

    static ClosedChannelException closedError()
    {
        return new ClosedChannelException();
    }

    void enqueueWaiter(Waiter waiter)
    {
        if (waiter.kind == SEND)
        {
            enqueueSend(waiter);
        }
        else
        {
            enqueueRecv(waiter);
        }
    }

    private void enqueueSend(Waiter waiter)
    {
        waiter.prev = sendTail;
        waiter.next = null;
        if (sendTail != null)
        {
            sendTail.next = waiter;
        }
        else
        {
            sendHead = waiter;
        }
        sendTail = waiter;
    }

    private void enqueueRecv(Waiter waiter)
    {
        waiter.prev = recvTail;
        waiter.next = null;
        if (recvTail != null)
        {
            recvTail.next = waiter;
        }
        else
        {
            recvHead = waiter;
        }
        recvTail = waiter;
    }

    void unlinkWaiter(Waiter waiter)
    {
        if (waiter.kind == SEND)
        {
            if (waiter.prev != null)
            {
                waiter.prev.next = waiter.next;
            }
            else if (sendHead == waiter)
            {
                sendHead = waiter.next;
            }
            if (waiter.next != null)
            {
                waiter.next.prev = waiter.prev;
            }
            else if (sendTail == waiter)
            {
                sendTail = waiter.prev;
            }
        }
        else
        {
            if (waiter.prev != null)
            {
                waiter.prev.next = waiter.next;
            }
            else if (recvHead == waiter)
            {
                recvHead = waiter.next;
            }
            if (waiter.next != null)
            {
                waiter.next.prev = waiter.prev;
            }
            else if (recvTail == waiter)
            {
                recvTail = waiter.prev;
            }
        }
        waiter.prev = null;
        waiter.next = null;
    }


    // Called with LOCK held.  A Select may complete only one of its
    // operations: the first delivery claims the select, and later deliveries
    // to its other registered waiters are skipped so that no value is taken
    // out of a channel just to be discarded by the losing branch.
    static boolean claimWaiter(Waiter waiter)
    {
        Select select = waiter.select;
        if (select == null)
        {
            return true;
        }
        if (select.winner != null)
        {
            return false;
        }
        select.winner = waiter;
        return true;
    }

    private Waiter claimable(Waiter waiter)
    {
        while (waiter != null && !claimWaiter(waiter))
        {
            waiter = waiter.next;
        }
        return waiter;
    }

    private Waiter claimReceiver()
    {
        return claimable(recvHead);
    }

    private Waiter claimSender()
    {
        return claimable(sendHead);
    }

    // Select helpers.  They either complete the probe waiter, report that the
    // operation would block, or leave the caller to register the waiter.

    boolean probeReceive(Waiter probe)
    {
        if (count > 0)
        {
            probe.done = true;
            probe.value = take();
            signalOneSender();
            return true;
        }
        Waiter sender = claimSender();
        if (sender != null)
        {
            sender.unlink();
            sender.done = true;
            probe.done = true;
            probe.value = sender.value;
            sender.parker.unpark();
            return true;
        }
        if (closed)
        {
            probe.done = true;
            probe.closedSignal = true;
            return true;
        }
        return false;
    }

    boolean probeSend(Object value, Waiter probe)
    {
        if (closed)
        {
            probe.done = true;
            probe.closedSignal = true;
            return true;
        }
        Waiter receiver = claimReceiver();
        if (receiver != null)
        {
            receiver.unlink();
            receiver.done = true;
            receiver.value = value;
            receiver.parker.unpark();
            probe.done = true;
            probe.value = value;
            return true;
        }
        if (capacity < 0 || count < capacity)
        {
            put(value);
            probe.done = true;
            probe.value = value;
            return true;
        }
        return false;
    }

    private void signalOneSender()
    {
        if (sendHead == null)
        {
            return;
        }
        if (capacity == 0)
        {
            return;
        }
        Waiter sender = claimSender();
        if (sender == null)
        {
            return;
        }
        sender.unlink();
        put(sender.value);
        sender.value = null;
        sender.done = true;
        sender.parker.unpark();
    }

    private void put(Object value)
    {
        if (count == buffer.length)
        {
            Object[] grown = new Object[buffer.length * 2];
            for (int i = 0; i < count; i++)
            {
                grown[i] = buffer[(head + i) % buffer.length];
            }
            buffer = grown;
            head = 0;
        }
        buffer[(head + count) % buffer.length] = value;
        count++;
    }

    private Object take()
    {
        Object value = buffer[head];
        buffer[head] = null;
        head = (head + 1) % buffer.length;
        count--;
        return value;
    }
}
