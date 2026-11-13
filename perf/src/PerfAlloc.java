// Allocation and collection throughput.  The first block measures allocation
// of live objects, the second measures a full mark-sweep with the objects
// reachable, and the third measures a collection after they become garbage.

public class PerfAlloc {
    static class Triple {
        int a;
        int b;
        int c;
    }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 300000;
        Triple warm = new Triple();
        warm.a = 1;

        long t0 = System.nanoTime();
        Triple[] arr = new Triple[n];
        for (int i = 0; i < n; i++) {
            Triple t = new Triple();
            t.a = i;
            t.b = i + 1;
            t.c = i + 2;
            arr[i] = t;
        }
        long t1 = System.nanoTime();

        long sum = 0;
        for (int i = 0; i < n; i++) {
            sum += arr[i].a + arr[i].b + arr[i].c;
        }

        Runtime rt = Runtime.getRuntime();
        long g0 = System.nanoTime();
        rt.gc();
        long g1 = System.nanoTime();

        arr = null;
        long g2 = System.nanoTime();
        rt.gc();
        long g3 = System.nanoTime();

        System.out.println("objects=" + n);
        System.out.println("alloc_ns=" + (t1 - t0));
        System.out.println("gc_live_ns=" + (g1 - g0));
        System.out.println("gc_dead_ns=" + (g3 - g2));
        System.out.println("checksum=" + sum);
    }
}
