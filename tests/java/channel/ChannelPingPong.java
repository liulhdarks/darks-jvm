import darks.concurrent.Channel;

// 100k request/response round trips through two bounded channels, plus FIFO
// checks on a buffered channel.  Exercises blocking send/receive, direct
// handoff and the bounded fast paths.
public class ChannelPingPong
{
    static final class Msg
    {
        final int value;

        Msg(int value)
        {
            this.value = value;
        }
    }

    static final int ROUNDS = 100000;

    public static void main(String[] args) throws Exception
    {
        final Channel<Msg> request = Channel.bounded(1);
        final Channel<Msg> response = Channel.bounded(1);

        Thread pong = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    for (int i = 0; i < ROUNDS; i++)
                    {
                        Msg m = request.receive();
                        if (m == null || m.value != i)
                        {
                            throw new RuntimeException("pong expected " + i
                                + " got " + (m == null ? "closed" : String.valueOf(m.value)));
                        }
                        response.send(new Msg(i));
                    }
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException("pong interrupted");
                }
            }
        });
        pong.start();

        long start = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++)
        {
            request.send(new Msg(i));
            Msg m = response.receive();
            if (m == null || m.value != i)
            {
                throw new RuntimeException("pinger expected " + i
                    + " got " + (m == null ? "closed" : String.valueOf(m.value)));
            }
        }
        long elapsed = (System.nanoTime() - start) / 1000000L;
        pong.join(60000);
        if (pong.isAlive())
        {
            throw new RuntimeException("pong did not finish");
        }

        // Bounded channel: trySend fails while full, then FIFO order holds.
        Channel<Integer> bounded = Channel.bounded(2);
        if (!bounded.trySend(1) || !bounded.trySend(2) || bounded.trySend(3))
        {
            throw new RuntimeException("bounded trySend capacity mismatch");
        }
        if (bounded.tryReceive() != 1 || bounded.tryReceive() != 2)
        {
            throw new RuntimeException("bounded FIFO mismatch");
        }
        if (bounded.tryReceive() != null)
        {
            throw new RuntimeException("bounded receive from empty channel");
        }

        // Unbounded channel buffers without blocking and keeps FIFO order.
        Channel<Integer> unbounded = Channel.unbounded();
        for (int i = 0; i < 10000; i++)
        {
            unbounded.send(i);
        }
        for (int i = 0; i < 10000; i++)
        {
            Integer v = unbounded.receive();
            if (v == null || v != i)
            {
                throw new RuntimeException("unbounded FIFO mismatch at " + i);
            }
        }

        System.out.println("channel-pingpong ok rounds=" + ROUNDS
            + " elapsed=" + elapsed + "ms fifo=true");
    }
}
