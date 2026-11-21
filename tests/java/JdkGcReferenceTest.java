import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

// Repeated collection rounds with references: every round must clear the
// weakly held object and enqueue exactly one reference.  This doubles as a
// stress test for the weak-reference handling in the mark-sweep collector.
public class JdkGcReferenceTest
{
    private static int failures;

    private static WeakReference<Object> create(ReferenceQueue<Object> queue)
    {
        Object value = new Object();
        return new WeakReference<Object>(value, queue);
    }

    public static void main(String[] args)
    {
        int rounds = 100;
        for (int round = 0; round < rounds; round++)
        {
            ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
            WeakReference<Object> reference = create(queue);
            if (reference.get() == null)
            {
                failures++;
                System.out.println("FAIL round " + round + ": cleared too early");
                continue;
            }
            byte[] junk = new byte[32 * 1024];
            junk[0] = 1;
            System.gc();
            if (reference.get() != null)
            {
                failures++;
                System.out.println("FAIL round " + round + ": referent survived");
            }
            Reference<?> polled = queue.poll();
            if (polled != reference)
            {
                failures++;
                System.out.println("FAIL round " + round + ": not enqueued");
            }
        }
        System.out.println("gc-reference rounds=" + rounds + " failures=" + failures);
        if (failures == 0)
        {
            System.out.println("gc reference stress: ok");
        }
    }
}
