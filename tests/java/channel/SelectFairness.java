import darks.concurrent.Channel;
import darks.concurrent.Select;

// Select fairness: per-channel FIFO order, exactly one waiter woken per value,
// and the default branch only fires when nothing is ready.
public class SelectFairness
{
    public static void main(String[] args) throws Exception
    {
        final int perChannel = 2000;
        final Channel<Integer> a = Channel.bounded(4);
        final Channel<Integer> b = Channel.unbounded();

        Thread producerA = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    for (int i = 0; i < perChannel; i++)
                    {
                        a.send(i);
                    }
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException("producer a interrupted");
                }
            }
        });
        Thread producerB = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    for (int i = 0; i < perChannel; i++)
                    {
                        b.send(1000000 + i);
                    }
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException("producer b interrupted");
                }
            }
        });
        producerA.start();
        producerB.start();

        int nextA = 0;
        int nextB = 0;
        while (nextA < perChannel || nextB < perChannel)
        {
            Select select = new Select();
            int opA = -1;
            int opB = -1;
            if (nextA < perChannel)
            {
                opA = 0;
                select.receive(a);
            }
            if (nextB < perChannel)
            {
                opB = (opA < 0) ? 0 : 1;
                select.receive(b);
            }
            Select.Result result = select.select();
            if (result.closed)
            {
                throw new RuntimeException("unexpected close during select");
            }
            if (result.index == opA)
            {
                if (result.value == null || ((Integer) result.value).intValue() != nextA)
                {
                    throw new RuntimeException("channel a FIFO mismatch at " + nextA);
                }
                nextA++;
            }
            else if (result.index == opB)
            {
                if (result.value == null || ((Integer) result.value).intValue() != 1000000 + nextB)
                {
                    throw new RuntimeException("channel b FIFO mismatch at " + nextB);
                }
                nextB++;
            }
            else
            {
                throw new RuntimeException("select returned unknown index " + result.index);
            }
        }
        producerA.join(10000);
        producerB.join(10000);
        if (producerA.isAlive() || producerB.isAlive())
        {
            throw new RuntimeException("producers did not finish");
        }

        // Default branch fires immediately on empty channels.
        Channel<Integer> empty = Channel.bounded(1);
        Select.Result dflt = new Select().receive(empty).defaultBranch().select();
        if (dflt.index != -1 || dflt.value != null)
        {
            throw new RuntimeException("default branch did not fire");
        }

        // Three blocked selectors, three values: each waiter receives exactly
        // one value, so the values are all distinct.
        final Channel<Integer> shared = Channel.bounded(0);
        final int[] got = new int[3];
        Thread[] readers = new Thread[3];
        for (int i = 0; i < 3; i++)
        {
            final int slot = i;
            readers[i] = new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        Select.Result r = new Select().receive(shared).select();
                        got[slot] = ((Integer) r.value).intValue();
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException("select waiter interrupted");
                    }
                }
            });
            readers[i].start();
        }
        Thread.sleep(50);
        if (!shared.trySend(7))
        {
            // A blocked receiver exists, so a rendezvous must succeed.
            shared.send(7);
        }
        shared.send(8);
        shared.send(9);
        for (int i = 0; i < 3; i++)
        {
            readers[i].join(10000);
            if (readers[i].isAlive())
            {
                throw new RuntimeException("select waiter " + i + " did not finish");
            }
        }
        boolean seen7 = false;
        boolean seen8 = false;
        boolean seen9 = false;
        for (int i = 0; i < 3; i++)
        {
            if (got[i] == 7)
            {
                seen7 = true;
            }
            else if (got[i] == 8)
            {
                seen8 = true;
            }
            else if (got[i] == 9)
            {
                seen9 = true;
            }
            else
            {
                throw new RuntimeException("waiter received unexpected value " + got[i]);
            }
        }
        if (!seen7 || !seen8 || !seen9)
        {
            throw new RuntimeException("a select waiter was woken twice or starved");
        }

        System.out.println("select-fairness ok fifo=true default=true oneWake=true");
    }
}
