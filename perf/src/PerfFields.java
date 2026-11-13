// Field read/write throughput.  The getfield/putfield opcodes are executed by
// the interpreter and by the threaded-code JIT fast path; both go through
// StackFrame::GetField/PutField and JObject slot storage.

public class PerfFields {
    static class Triple {
        int a;
        int b;
        int c;
    }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 2000000;
        Triple t = new Triple();
        long s = 0;
        for (int w = 0; w < 20000; w++) {
            t.a = w;
            t.b = w + 1;
            t.c = w + 2;
            s += t.a + t.b + t.c;
        }
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            s += t.a + t.b + t.c;
        }
        long t1 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            t.a = i;
            t.b = i + 1;
            t.c = i + 2;
        }
        long t2 = System.nanoTime();
        long ops = 3L * n;
        // Only integer counters are printed: throughput is derived by the
        // harness, which keeps the measurement independent of the VM's
        // floating-point formatting.
        System.out.println("read_ops=" + ops);
        System.out.println("read_ns=" + (t1 - t0));
        System.out.println("write_ops=" + ops);
        System.out.println("write_ns=" + (t2 - t1));
        System.out.println("checksum=" + (s + t.a + t.b + t.c));
    }
}
