// Object footprint probe.
//
// Allocates N instances of a three-int-field class and reports the process
// RSS delta read from /proc/self/status.  Run with DARKS_JVM_GC=0 so the
// measurement covers allocation only (no collector bookkeeping).

import java.io.FileInputStream;

public class PerfMemory {
    static class Triple {
        int a;
        int b;
        int c;
    }

    static long rssKb() {
        try {
            FileInputStream in = new FileInputStream("/proc/self/status");
            byte[] buf = new byte[8192];
            int n = in.read(buf, 0, buf.length);
            in.close();
            if (n <= 0) {
                return -1;
            }
            String s = new String(buf, 0, n);
            int idx = s.indexOf("VmRSS:");
            if (idx < 0) {
                return -1;
            }
            int end = s.indexOf('\n', idx);
            String line = s.substring(idx + 6, end).trim();
            int sp = line.indexOf(' ');
            if (sp > 0) {
                line = line.substring(0, sp);
            }
            return Long.parseLong(line);
        } catch (Throwable t) {
            return -1;
        }
    }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 300000;

        // Load the class and touch one instance before taking the baseline so
        // class metadata and the first allocation are not counted.
        Triple warm = new Triple();
        warm.a = 1;
        warm.b = 2;
        warm.c = 3;
        long rss0 = rssKb();

        Triple[] arr = new Triple[n];
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            Triple t = new Triple();
            t.a = i;
            t.b = i + 1;
            t.c = i + 2;
            arr[i] = t;
        }
        long t1 = System.nanoTime();
        long rss1 = rssKb();

        long sum = 0;
        for (int i = 0; i < n; i++) {
            sum += arr[i].a + arr[i].b + arr[i].c;
        }
        long rss2 = rssKb();

        System.out.println("objects=" + n);
        System.out.println("rss_before_kb=" + rss0);
        System.out.println("rss_after_kb=" + rss1);
        System.out.println("rss_touch_kb=" + rss2);
        System.out.println("rss_delta_kb=" + (rss1 - rss0));
        System.out.println("alloc_ns=" + (t1 - t0));
        System.out.println("checksum=" + sum);
    }
}
