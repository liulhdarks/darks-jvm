// Interface dispatch throughput.  This phase deliberately does not touch the
// method-dispatch implementation; the number is a regression guard and a
// baseline for the later vtable work.

public class PerfVirtual {
    interface Op {
        int apply(int x);
    }

    static class Inc implements Op {
        public int apply(int x) { return x + 1; }
    }

    static class Dec implements Op {
        public int apply(int x) { return x - 1; }
    }

    static class Xor implements Op {
        public int apply(int x) { return x ^ 0x5a5a; }
    }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 500000;
        Op[] ops = new Op[3];
        ops[0] = new Inc();
        ops[1] = new Dec();
        ops[2] = new Xor();
        int x = 0;
        for (int w = 0; w < 20000; w++) {
            x = ops[w % 3].apply(x);
        }
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            x = ops[i % 3].apply(x);
        }
        long t1 = System.nanoTime();
        System.out.println("calls=" + n);
        System.out.println("call_ns=" + (t1 - t0));
        System.out.println("checksum=" + x);
    }
}
