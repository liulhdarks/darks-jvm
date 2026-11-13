// Static field read/write throughput.  getstatic/putstatic resolve a
// FieldRefConstant to a class static slot and then load/store the value.
// The volatile long loops also exercise 64-bit non-tearing/ordering.

public class PerfStatic {
    static int a;
    static int b;
    static int c;
    static volatile long vsink;

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 2000000;
        long s = 0;
        for (int w = 0; w < 20000; w++) {
            a = w;
            b = w + 1;
            c = w + 2;
            s += a + b + c;
        }
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            s += a + b + c;
        }
        long t1 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            a = i;
            b = i + 1;
            c = i + 2;
        }
        long t2 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            s += vsink;
        }
        long t3 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            vsink = i;
        }
        long t4 = System.nanoTime();
        long ops = 3L * n;
        // Integer counters only; the harness derives throughput.
        System.out.println("read_ops=" + ops);
        System.out.println("read_ns=" + (t1 - t0));
        System.out.println("write_ops=" + ops);
        System.out.println("write_ns=" + (t2 - t1));
        System.out.println("vol_read_ops=" + n);
        System.out.println("vol_read_ns=" + (t3 - t2));
        System.out.println("vol_write_ops=" + n);
        System.out.println("vol_write_ns=" + (t4 - t3));
        System.out.println("checksum=" + (s + a + b + c + vsink));
    }
}
