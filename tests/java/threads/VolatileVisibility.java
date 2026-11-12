// volatile write -> volatile read visibility and ordering.
public class VolatileVisibility {
    static volatile boolean flag;
    static volatile int data;

    public static void main(String[] a) throws Exception {
        Thread writer = new Thread(new Runnable() {
            public void run() {
                data = 42;
                flag = true;
            }
        });
        writer.start();
        int seen = -1;
        while (!flag) {
            Thread.yield();
        }
        seen = data;
        writer.join();
        if (seen != 42) {
            throw new RuntimeException("volatile visibility failed, data=" + seen);
        }
        System.out.println("volatile ok flag=" + flag + " data=" + seen);
    }
}
