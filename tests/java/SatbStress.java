// Exercises the SATB/incremental-update write barrier from every mutator
// path: interpreted field/static/array stores, System.arraycopy of reference
// arrays, java.lang.reflect Field/Array writes and JNI SetObjectField /
// SetStaticObjectField / SetObjectArrayElement callbacks.
//
// Run with DARKS_GC_SATB_TEST=1 so the barrier is active for the whole
// process; the [satb] counters printed at exit prove each site was reached.
public class SatbStress {
    public static class Node {
        public Node next;
        public Object payload;
        public static Node shared;
    }

    static native void jniSetField(Node target, Node value);

    static native void jniSetStatic(Node value);

    static native void jniSetArray(Object[] array, int index, Object value);

    static int failures = 0;

    static void check(String name, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    static java.lang.reflect.Field nextField;
    static java.lang.reflect.Field sharedField;

    static void work(int id, int rounds) {
        Node head = new Node();
        head.payload = new byte[16];
        Object[] slot = new Object[4];
        for (int i = 0; i < rounds; i++) {
            Node n = new Node();
            n.payload = new byte[64];
            n.next = head;
            head = n;
            Node.shared = n;
            slot[i & 3] = n;
            slot[(i + 1) & 3] = head;
            Object[] source = new Object[] { n, head };
            System.arraycopy(source, 0, slot, 0, 2);
            if ((i & 15) == 0) {
                try {
                    nextField.set(head, n.next);
                    sharedField.set(null, n);
                    java.lang.reflect.Array.set(slot, 2, n);
                } catch (Throwable t) {
                    check("reflect id=" + id + " i=" + i, false);
                }
            }
            if ((i & 7) == 0) {
                jniSetField(head, n);
                jniSetStatic(n);
                jniSetArray(slot, 1, n);
            }
            if ((i & 15) == 0) {
                Node saved = head.next;
                head.next = null;
                head.next = saved;
            }
            if ((i & 31) == 0) {
                Thread.yield();
            }
        }
        check("chain id=" + id, head != null && head.next != null);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            System.load(args[0]);
        }
        nextField = Node.class.getDeclaredField("next");
        sharedField = Node.class.getDeclaredField("shared");

        final int threads = 4;
        final int rounds = 400;
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int id = i;
            ts[i] = new Thread(new Runnable() {
                public void run() {
                    work(id, rounds);
                }
            });
            ts[i].start();
        }
        // Keep allocating while the mutators write, so collections interleave
        // with barrier recording.
        for (int i = 0; i < 200; i++) {
            Object[] garbage = new Object[48];
            garbage[0] = new Node();
            if ((i & 7) == 0) {
                Thread.yield();
            }
        }
        for (int i = 0; i < threads; i++) {
            ts[i].join();
        }

        Node probe = new Node();
        Node.shared = probe;
        probe.next = probe;
        check("static visible", Node.shared == probe);
        check("field visible", probe.next == probe);
        Object[] probeArray = new Object[2];
        probeArray[1] = probe;
        check("array visible", probeArray[1] == probe);
        nextField.set(probe, probe);
        check("reflect field visible", nextField.get(probe) == probe);
        java.lang.reflect.Array.set(probeArray, 0, probe);
        check("reflect array visible", probeArray[0] == probe);

        if (failures != 0) {
            System.out.println("satb stress: failures=" + failures);
            System.exit(1);
        }
        System.out.println("satb stress: ok");
    }
}
