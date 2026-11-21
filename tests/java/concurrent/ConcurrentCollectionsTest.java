import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

// ConcurrentHashMap, ConcurrentLinkedQueue and CopyOnWriteArrayList under
// concurrent mutation.
public class ConcurrentCollectionsTest {
    static int checks;
    static final AtomicInteger readerErrors = new AtomicInteger();

    static void check(boolean condition, String what) {
        checks++;
        if (!condition) {
            throw new RuntimeException("concurrent-collections failed: " + what);
        }
    }

    public static void main(String[] args) throws Exception {
        ConcurrentMap<String, Integer> concurrentMap =
            new ConcurrentHashMap<String, Integer>();
        check(concurrentMap.putIfAbsent("cm", Integer.valueOf(1)) == null,
              "ConcurrentMap putIfAbsent");
        check(concurrentMap.putIfAbsent("cm", Integer.valueOf(2)) != null,
              "ConcurrentMap putIfAbsent existing");
        check(concurrentMap.replace("cm", Integer.valueOf(1),
                                    Integer.valueOf(3)),
              "ConcurrentMap replace");
        check(concurrentMap.get("cm").intValue() == 3, "ConcurrentMap get");

        final ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<String, Integer>();
        Thread[] putters = new Thread[8];
        for (int t = 0; t < putters.length; t++) {
            final int id = t;
            putters[t] = new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; i < 500; i++) {
                        map.put("t" + id + "-" + i, Integer.valueOf(i));
                    }
                }
            });
            putters[t].start();
        }
        for (int t = 0; t < putters.length; t++) {
            putters[t].join();
        }
        check(map.size() == 4000, "map size " + map.size());
        check(map.get("t3-250").intValue() == 250, "map get");
        check(map.containsKey("t7-499"), "map containsKey");
        check(map.containsValue(Integer.valueOf(123)), "map containsValue");

        final AtomicInteger winners = new AtomicInteger();
        Thread[] racers = new Thread[8];
        for (int t = 0; t < racers.length; t++) {
            racers[t] = new Thread(new Runnable() {
                public void run() {
                    if (map.putIfAbsent("shared", Integer.valueOf(1)) == null) {
                        winners.incrementAndGet();
                    }
                }
            });
            racers[t].start();
        }
        for (int t = 0; t < racers.length; t++) {
            racers[t].join();
        }
        check(winners.get() == 1, "one putIfAbsent winner, got " + winners.get());
        check(map.get("shared").intValue() == 1, "putIfAbsent value");
        check(map.replace("shared", Integer.valueOf(1), Integer.valueOf(2)), "replace 3-arg");
        check(!map.replace("shared", Integer.valueOf(9), Integer.valueOf(3)), "replace mismatch");
        check(!map.remove("shared", Integer.valueOf(9)), "remove mismatch");
        check(map.remove("shared", Integer.valueOf(2)), "remove match");
        check(map.get("shared") == null, "removed shared");

        map.putIfAbsent("count", Integer.valueOf(0));
        map.merge("count", Integer.valueOf(2), new BiFunction<Integer, Integer, Integer>() {
            public Integer apply(Integer a, Integer b) {
                return Integer.valueOf(a.intValue() + b.intValue());
            }
        });
        check(map.get("count").intValue() == 2, "merge");
        Integer computed = map.computeIfAbsent("computed", new Function<String, Integer>() {
            public Integer apply(String key) {
                return Integer.valueOf(7);
            }
        });
        check(computed.intValue() == 7, "computeIfAbsent");
        check(map.keySet().size() == 4002, "keySet snapshot " + map.keySet().size());
        check(map.values().size() == 4002, "values snapshot");
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        check(entries.size() == 4002, "entrySet snapshot");
        Iterator<Map.Entry<String, Integer>> entryIt = entries.iterator();
        int entrySum = 0;
        while (entryIt.hasNext()) {
            entrySum += entryIt.next().getValue().intValue();
        }
        check(entrySum > 0, "entry values readable");

        final ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<Integer>();
        Thread[] producers = new Thread[4];
        for (int t = 0; t < producers.length; t++) {
            final int id = t;
            producers[t] = new Thread(new Runnable() {
                public void run() {
                    for (int i = 0; i < 500; i++) {
                        queue.offer(Integer.valueOf(id * 500 + i));
                    }
                }
            });
            producers[t].start();
        }
        for (int t = 0; t < producers.length; t++) {
            producers[t].join();
        }
        check(queue.size() == 2000, "queue size " + queue.size());
        check(queue.peek() != null, "peek");
        HashSet<Integer> seen = new HashSet<Integer>();
        Integer value;
        int polled = 0;
        while ((value = queue.poll()) != null) {
            seen.add(value);
            polled++;
        }
        check(polled == 2000, "polled " + polled);
        check(seen.size() == 2000, "unique values");
        check(queue.isEmpty(), "queue empty after drain");
        check(queue.poll() == null, "poll empty");
        queue.offer(Integer.valueOf(9));
        check(queue.remove(Integer.valueOf(9)), "queue remove(Object)");
        check(queue.isEmpty(), "queue empty again");

        final CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<Integer>();
        Thread writer = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 200; i++) {
                    list.add(Integer.valueOf(i));
                }
            }
        });
        Thread[] readers = new Thread[2];
        for (int r = 0; r < readers.length; r++) {
            readers[r] = new Thread(new Runnable() {
                public void run() {
                    for (int pass = 0; pass < 200; pass++) {
                        int count = 0;
                        for (Integer item : list) {
                            if (item == null) {
                                readerErrors.incrementAndGet();
                            }
                            count++;
                        }
                        if (count > 200) {
                            readerErrors.incrementAndGet();
                        }
                    }
                }
            });
        }
        writer.start();
        for (int r = 0; r < readers.length; r++) {
            readers[r].start();
        }
        writer.join();
        for (int r = 0; r < readers.length; r++) {
            readers[r].join();
        }
        check(list.size() == 200, "cow size " + list.size());
        check(readerErrors.get() == 0, "reader errors");
        check(list.get(0).intValue() == 0 && list.get(199).intValue() == 199, "cow get");
        check(!list.addIfAbsent(Integer.valueOf(0)), "addIfAbsent duplicate");
        check(list.addIfAbsent(Integer.valueOf(1000)), "addIfAbsent new");
        check(list.remove(Integer.valueOf(1000)), "cow remove(Object)");
        check(list.size() == 200, "cow size restored");

        System.out.println("concurrent-collections ok checks=" + checks
                + " map=4000 queue=2000 cow=200");
    }
}
