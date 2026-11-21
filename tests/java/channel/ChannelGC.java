import darks.concurrent.Channel;
import darks.concurrent.Select;

// High-frequency channel churn followed by forced collections.  Live channels,
// buffered payloads and parked waiters (which are registered in channel queues
// and rooted through the coroutine's block object) must survive the GC.
public class ChannelGC
{
    public static void main(String[] args) throws Exception
    {
        Runtime runtime = Runtime.getRuntime();

        for (int round = 0; round < 200; round++)
        {
            Channel<Integer> temp = Channel.bounded(4);
            temp.send(round);
            if (temp.receive() != round)
            {
                throw new RuntimeException("churn mismatch at round " + round);
            }
            temp.close();
            if (round % 20 == 0)
            {
                runtime.gc();
            }
        }

        // A live channel keeps its buffered payloads across collections.
        Channel<Integer> live = Channel.unbounded();
        for (int i = 0; i < 64; i++)
        {
            live.send(i);
        }
        for (int i = 0; i < 4; i++)
        {
            runtime.gc();
        }
        for (int i = 0; i < 64; i++)
        {
            Integer v = live.receive();
            if (v == null || v != i)
            {
                throw new RuntimeException("live channel lost payload at " + i);
            }
        }

        // A receiver parked in Channel.receive survives repeated collections.
        final Channel<Integer> parkedChannel = Channel.bounded(1);
        final int[] parkedResult = new int[1];
        Thread receiver = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    Integer v = parkedChannel.receive();
                    parkedResult[0] = (v == null) ? -1 : v.intValue();
                }
                catch (InterruptedException e)
                {
                    parkedResult[0] = -2;
                }
            }
        });
        receiver.start();
        Thread.sleep(20);
        for (int i = 0; i < 8; i++)
        {
            runtime.gc();
        }
        parkedChannel.send(1234);
        receiver.join(10000);
        if (receiver.isAlive() || parkedResult[0] != 1234)
        {
            throw new RuntimeException("parked receiver did not survive GC: "
                + parkedResult[0]);
        }

        // A select parked on two channels also survives, and its registrations
        // are collected with the select.
        final Channel<Integer> first = Channel.bounded(1);
        final Channel<Integer> second = Channel.bounded(1);
        final int[] selected = new int[1];
        Thread selector = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    Select.Result r = new Select().receive(first).receive(second).select();
                    selected[0] = (r.value == null) ? -1 : ((Integer) r.value).intValue();
                }
                catch (InterruptedException e)
                {
                    selected[0] = -2;
                }
            }
        });
        selector.start();
        Thread.sleep(20);
        for (int i = 0; i < 8; i++)
        {
            runtime.gc();
        }
        second.send(4321);
        selector.join(10000);
        if (selector.isAlive() || selected[0] != 4321)
        {
            throw new RuntimeException("parked select did not survive GC: " + selected[0]);
        }
        if (first.tryReceive() != null)
        {
            throw new RuntimeException("cancelled select registration consumed a value");
        }

        System.out.println("channel-gc ok live=true parked=true select=true");
    }
}
