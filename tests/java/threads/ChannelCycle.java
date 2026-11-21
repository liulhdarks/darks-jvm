import darks.concurrent.Channel;

/**
 * Channel deadlock: two unbuffered channels and two threads, each receiving
 * from one channel before sending on the other.  Both end up parked as
 * receivers with nobody able to send, so the whole process is quiescent.
 *
 * -Ddarks.diag.deadlock=1 must report the deadlock and exit with code 5.
 */
public final class ChannelCycle
{
    static final class Receiver extends Thread
    {
        private final Channel<Integer> in;
        private final Channel<Integer> out;

        Receiver(String name, Channel<Integer> in, Channel<Integer> out)
        {
            super(name);
            this.in = in;
            this.out = out;
        }

        public void run()
        {
            try
            {
                Integer value = in.receive();
                out.send(value == null ? Integer.valueOf(0) : value);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            System.out.println(getName() + ": no deadlock detected");
        }
    }

    public static void main(String[] args) throws Exception
    {
        Channel<Integer> left = Channel.bounded(0);
        Channel<Integer> right = Channel.bounded(0);
        Thread a = new Receiver("channel-a", left, right);
        Thread b = new Receiver("channel-b", right, left);
        a.start();
        b.start();
        a.join();
        System.out.println("channel-cycle: no deadlock detected");
    }
}
