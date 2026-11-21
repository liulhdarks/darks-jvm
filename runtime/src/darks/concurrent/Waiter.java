package darks.concurrent;

/**
 * One blocked send or receive.  Waiters are ordinary heap objects: a channel
 * owns the live ones through its doubly linked queues, and the coroutine that
 * is parked keeps its own waiter in a local variable, so the mark-sweep
 * collector sees the whole graph.  The parker itself is additionally the
 * coroutine's block object while it waits and therefore a GC root.
 */
final class Waiter
{
    final Channel channel;
    final int kind;
    // Owning select when this waiter is one of several multiplexed operations;
    // a normal send/receive leaves it null.
    Select select;
    Object value;
    boolean done;
    boolean cancelled;
    boolean closedSignal;
    RuntimeException error;
    Parker parker;
    long generation;
    Waiter prev;
    Waiter next;

    Waiter(Channel channel, int kind)
    {
        this.channel = channel;
        this.kind = kind;
        this.parker = new Parker();
    }

    void unlink()
    {
        channel.unlinkWaiter(this);
    }

    void await() throws InterruptedException    {
        try
        {
            parker.park(generation);
        }
        catch (InterruptedException e)
        {
            synchronized (Channel.LOCK)
            {
                if (!done)
                {
                    cancelled = true;
                    channel.unlinkWaiter(this);
                }
            }
            if (!done)
            {
                throw e;
            }
        }
    }
}
