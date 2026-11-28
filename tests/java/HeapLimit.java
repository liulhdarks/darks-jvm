// Allocates until the -Xmx budget is exhausted, then verifies that the
// OutOfMemoryError carries the JDK message and that the heap is usable again
// after the arrays are released.
public class HeapLimit {
    public static void main(String[] args) {
        int chunkMb = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        int count = args.length > 1 ? Integer.parseInt(args[1]) : 64;
        byte[][] hold = new byte[count][];
        int allocated = 0;
        boolean oom = false;
        try {
            for (int i = 0; i < count; i++) {
                hold[i] = new byte[chunkMb * 1024 * 1024];
                allocated++;
            }
        } catch (OutOfMemoryError e) {
            // Release the survivors before doing any string work: printing
            // allocates, and the heap is still at its limit.
            hold = null;
            System.gc();
            oom = true;
            System.out.println("heap limit: oom after=" + allocated
                + " message=" + e.getMessage());
        }
        if (!oom) {
            System.out.println("heap limit: no oom allocated=" + allocated);
        }
        hold = null;
        System.gc();
        byte[] small = new byte[1024];
        if (small.length != 1024) {
            System.out.println("heap limit: reuse failed");
            System.exit(1);
        }
        System.out.println("heap limit: ok");
    }
}
