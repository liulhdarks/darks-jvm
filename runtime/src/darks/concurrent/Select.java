package darks.concurrent;

/**
 * Multiplexed receive/send with an optional default branch.
 *
 * Every operation is probed in declaration order while holding the channel
 * lock; if none can proceed, one waiter per operation is registered and the
 * selecting coroutine parks on a single shared {@link Parker}.  Waiters carry
 * a generation captured before the lock is released, and a completion
 * increments the generation and notifies only that parker, so a completion
 * that races with the park can never be lost and a wakeup never disturbs an
 * unrelated thread.  Once one operation completes, all other registrations
 * are cancelled before returning.
 *
 * The default branch is chosen only when no operation can proceed immediately;
 * it never causes the caller to block.
 */
public final class Select
{
    public static final class Result
    {
        public final int index;
        public final Object value;
        public final boolean send;
        public final boolean closed;

        Result(int index, Object value, boolean send, boolean closed)
        {
            this.index = index;
            this.value = value;
            this.send = send;
            this.closed = closed;
        }
    }

    private Channel[] channels = new Channel[4];
    private int[] kinds = new int[4];
    private Object[] values = new Object[4];
    private int count;
    private boolean hasDefault;
    // Set by Channel under Channel.LOCK when one operation completes; the
    // remaining registrations must not consume a second value.
    Waiter winner;

    public Select receive(Channel<?> channel)
    {
        add(channel, Channel.RECV, null);
        return this;
    }

    public Select send(Channel<?> channel, Object value)
    {
        if (value == null)
        {
            throw new NullPointerException("null channel value");
        }
        add(channel, Channel.SEND, value);
        return this;
    }

    public Select defaultBranch()
    {
        hasDefault = true;
        return this;
    }

    private void add(Channel<?> channel, int kind, Object value)
    {
        if (channel == null)
        {
            throw new NullPointerException("null channel");
        }
        if (count == channels.length)
        {
            int grown = channels.length * 2;
            Channel[] nc = new Channel[grown];
            int[] nk = new int[grown];
            Object[] nv = new Object[grown];
            for (int i = 0; i < count; i++)
            {
                nc[i] = channels[i];
                nk[i] = kinds[i];
                nv[i] = values[i];
            }
            channels = nc;
            kinds = nk;
            values = nv;
        }
        channels[count] = channel;
        kinds[count] = kind;
        values[count] = value;
        count++;
    }

    public Result select() throws InterruptedException
    {
        if (count == 0)
        {
            if (hasDefault)
            {
                return new Result(-1, null, false, false);
            }
            throw new IllegalStateException("select without operations");
        }

        Parker parker = new Parker();
        Waiter[] waiters = new Waiter[count];
        long generation;

        synchronized (Channel.LOCK)
        {
            Result early = null;
            for (int i = 0; i < count; i++)
            {
                Channel channel = channels[i];
                boolean isSend = kinds[i] == Channel.SEND;
                Waiter waiter = new Waiter(channel, kinds[i]);
                waiter.parker = parker;
                waiter.select = this;
                boolean ready = isSend
                    ? channel.probeSend(values[i], waiter)
                    : channel.probeReceive(waiter);
                if (ready)
                {
                    early = result(i, waiter, isSend);
                    break;
                }
                waiters[i] = waiter;
                channel.enqueueWaiter(waiter);
            }
            if (early != null)
            {
                cancelAll(waiters);
                return early;
            }
            if (hasDefault)
            {
                cancelAll(waiters);
                return new Result(-1, null, false, false);
            }
            generation = parker.generation();
        }

        try
        {
            parker.park(generation);
        }
        catch (InterruptedException e)
        {
            synchronized (Channel.LOCK)
            {
                cancelAll(waiters);
            }
            throw e;
        }

        synchronized (Channel.LOCK)
        {
            for (int i = 0; i < count; i++)
            {
                Waiter waiter = waiters[i];
                if (waiter != null && waiter.done)
                {
                    Result result = result(i, waiter, kinds[i] == Channel.SEND);
                    cancelAll(waiters);
                    return result;
                }
            }
            cancelAll(waiters);
        }
        // A wakeup without a completed operation is impossible, but retry
        // instead of silently dropping the select if it ever happens.
        return select();
    }

    private static Result result(int index, Waiter waiter, boolean send)
    {
        return new Result(index, waiter.value, send, waiter.closedSignal);
    }

    private static void cancelAll(Waiter[] waiters)
    {
        for (int i = 0; i < waiters.length; i++)
        {
            Waiter waiter = waiters[i];
            if (waiter != null && waiter.done)
            {
                continue;
            }
            if (waiter != null && !waiter.cancelled)
            {
                waiter.cancelled = true;
                waiter.channel.unlinkWaiter(waiter);
            }
        }
    }
}
