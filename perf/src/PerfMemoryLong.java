// Same footprint probe as PerfMemory, but with field names long enough that
// the baseline name-keyed map has to heap-allocate its keys.  This reproduces
// the historical ~650 B/object measurement (193 MB RSS for 300k objects).

import java.io.FileInputStream;

public class PerfMemoryLong {
    static class Triple {
        int firstComponentValue;
        int secondComponentValue;
        int thirdComponentValue;
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
        Triple warm = new Triple();
        warm.firstComponentValue = 1;
        long rss0 = rssKb();

        Triple[] arr = new Triple[n];
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) {
            Triple t = new Triple();
            t.firstComponentValue = i;
            t.secondComponentValue = i + 1;
            t.thirdComponentValue = i + 2;
            arr[i] = t;
        }
        long t1 = System.nanoTime();
        long rss1 = rssKb();

        long sum = 0;
        for (int i = 0; i < n; i++) {
            sum += arr[i].firstComponentValue + arr[i].secondComponentValue +
                   arr[i].thirdComponentValue;
        }

        System.out.println("objects=" + n);
        System.out.println("rss_before_kb=" + rss0);
        System.out.println("rss_after_kb=" + rss1);
        System.out.println("rss_delta_kb=" + (rss1 - rss0));
        System.out.println("alloc_ns=" + (t1 - t0));
        System.out.println("checksum=" + sum);
    }
}
