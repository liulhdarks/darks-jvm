import darks.concurrent.Channel;
import darks.concurrent.ClosedChannelException;

// Closing semantics: buffered values drain, subsequent receives return null,
// sends fail, and blocked senders/receivers are released.
public class ChannelClose
{
    public static void main(String[] args) throws Exception
    {
        Channel<Integer> channel = Channel.bounded(2);
        channel.send(1);
        channel.send(2);
        if (channel.trySend(3))
        {
            throw new RuntimeException("bounded channel accepted a third value");
        }
        channel.close();
        channel.close();
        if (!channel.isClosed())
        {
            throw new RuntimeException("isClosed() is false after close");
        }
        if (channel.receive() != 1 || channel.receive() != 2)
        {
            throw new RuntimeException("buffered values were not drained");
        }
        if (channel.receive() != null)
        {
            throw new RuntimeException("closed drained channel did not report end");
        }
        if (channel.tryReceive() != null || channel.trySend(9))
        {
            throw new RuntimeException("closed channel is still usable");
        }
        try
        {
            channel.send(9);
            throw new RuntimeException("send on closed channel did not fail");
        }
        catch (ClosedChannelException expected)
        {
        }

        final Channel<Integer> full = Channel.bounded(1);
        full.send(10);
        final boolean[] senderReleased = new boolean[1];
        Thread sender = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    full.send(11);
                }
                catch (ClosedChannelException expected)
                {
                    senderReleased[0] = true;
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException("blocked sender interrupted");
                }
            }
        });
        sender.start();
        Thread.sleep(20);
        full.close();
        sender.join(10000);
        if (sender.isAlive() || !senderReleased[0])
        {
            throw new RuntimeException("blocked sender was not released by close");
        }
        if (full.receive() != 10 || full.receive() != null)
        {
            throw new RuntimeException("closed buffer did not drain after sender wakeup");
        }

        final Channel<Integer> empty = Channel.bounded(1);
        final Integer[] received = new Integer[1];
        final boolean[] closedSeen = new boolean[1];
        Thread receiver = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    Integer v = empty.receive();
                    received[0] = v;
                    closedSeen[0] = (v == null);
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException("blocked receiver interrupted");
                }
            }
        });
        receiver.start();
        Thread.sleep(20);
        empty.close();
        receiver.join(10000);
        if (receiver.isAlive() || received[0] != null || !closedSeen[0])
        {
            throw new RuntimeException("blocked receiver was not released by close");
        }

        System.out.println("channel-close ok drain=true senders=true receivers=true");
    }
}
