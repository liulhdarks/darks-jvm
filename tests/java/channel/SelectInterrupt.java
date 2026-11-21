import darks.concurrent.Channel;
import darks.concurrent.Select;

// Interrupting a blocked select and a blocked send must raise
// InterruptedException without leaving stale registrations behind.
public class SelectInterrupt
{
    public static void main(String[] args) throws Exception
    {
        final Channel<Integer> channel = Channel.bounded(1);
        final boolean[] selectInterrupted = new boolean[1];
        Thread selector = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    new Select().receive(channel).select();
                }
                catch (InterruptedException expected)
                {
                    selectInterrupted[0] = true;
                }
            }
        });
        selector.start();
        Thread.sleep(50);
        selector.interrupt();
        selector.join(10000);
        if (selector.isAlive() || !selectInterrupted[0])
        {
            throw new RuntimeException("blocked select was not interrupted");
        }

        // The cancelled registration must not swallow a later value.
        if (channel.tryReceive() != null)
        {
            throw new RuntimeException("interrupted select left a phantom value");
        }
        channel.send(7);
        if (channel.receive() != 7)
        {
            throw new RuntimeException("channel unusable after select interrupt");
        }

        // A blocked send is interruptible as well.
        final Channel<Integer> full = Channel.bounded(1);
        full.send(1);
        final boolean[] sendInterrupted = new boolean[1];
        Thread sender = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    full.send(2);
                }
                catch (InterruptedException expected)
                {
                    sendInterrupted[0] = true;
                }
            }
        });
        sender.start();
        Thread.sleep(50);
        sender.interrupt();
        sender.join(10000);
        if (sender.isAlive() || !sendInterrupted[0])
        {
            throw new RuntimeException("blocked send was not interrupted");
        }
        if (full.receive() != 1 || full.tryReceive() != null)
        {
            throw new RuntimeException("interrupted send left stale data");
        }
        full.send(3);
        if (full.receive() != 3)
        {
            throw new RuntimeException("channel unusable after send interrupt");
        }

        // Select send branch on a full channel is interruptible too.
        final Channel<Integer> full2 = Channel.bounded(0);
        final boolean[] selectSendInterrupted = new boolean[1];
        Thread selectSender = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    new Select().send(full2, Integer.valueOf(5)).select();
                }
                catch (InterruptedException expected)
                {
                    selectSendInterrupted[0] = true;
                }
            }
        });
        selectSender.start();
        Thread.sleep(50);
        selectSender.interrupt();
        selectSender.join(10000);
        if (selectSender.isAlive() || !selectSendInterrupted[0])
        {
            throw new RuntimeException("blocked select-send was not interrupted");
        }
        if (full2.tryReceive() != null)
        {
            throw new RuntimeException("interrupted select-send left stale data");
        }

        System.out.println("select-interrupt ok select=true send=true cancelled=true");
    }
}
