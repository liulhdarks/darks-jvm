import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class JdkWeakReferenceTest
{
    private static int checks;
    private static int failures;

    private static void check(String name, boolean condition)
    {
        checks++;
        if (!condition)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    private static WeakReference<Object> weak(ReferenceQueue<Object> queue)
    {
        Object value = new Object();
        return new WeakReference<Object>(value, queue);
    }

    private static SoftReference<Object> soft(ReferenceQueue<Object> queue)
    {
        Object value = new Object();
        return new SoftReference<Object>(value, queue);
    }

    private static PhantomReference<Object> phantom(ReferenceQueue<Object> queue)
    {
        Object value = new Object();
        return new PhantomReference<Object>(value, queue);
    }

    private static void fillWeakMap(WeakHashMap<Object, String> map, int count)
    {
        for (int i = 0; i < count; i++)
        {
            map.put(new Object(), "value" + i);
        }
    }

    private static void collect()
    {
        for (int i = 0; i < 4; i++)
        {
            byte[] pressure = new byte[64 * 1024];
            pressure[0] = 1;
            System.gc();
        }
    }

    public static void main(String[] args)
    {
        ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
        WeakReference<Object> weak = weak(queue);
        check("weak initially set", weak.get() != null);
        collect();
        check("weak cleared", weak.get() == null);
        check("weak enqueued", weak.isEnqueued());
        Reference<?> enqueued = queue.poll();
        check("weak queue poll", enqueued == weak);
        check("weak not enqueued after poll", !weak.isEnqueued());
        check("weak queue empty", queue.poll() == null);

        ReferenceQueue<Object> softQueue = new ReferenceQueue<Object>();
        SoftReference<Object> soft = soft(softQueue);
        check("soft initially set", soft.get() != null);
        collect();
        check("soft cleared under pressure", soft.get() == null);
        check("soft enqueued", softQueue.poll() == soft);

        ReferenceQueue<Object> phantomQueue = new ReferenceQueue<Object>();
        PhantomReference<Object> phantom = phantom(phantomQueue);
        check("phantom get null", phantom.get() == null);
        collect();
        check("phantom enqueued", phantomQueue.poll() == phantom);

        WeakHashMap<Object, String> map = new WeakHashMap<Object, String>();
        Object key = new Object();
        map.put(key, "kept");
        check("weak map get", "kept".equals(map.get(key)));
        check("weak map size", map.size() == 1);
        key = null;
        collect();
        check("weak map expired", map.isEmpty() && map.size() == 0);

        WeakHashMap<Object, String> many = new WeakHashMap<Object, String>();
        fillWeakMap(many, 64);
        check("weak map filled", many.size() == 64);
        collect();
        check("weak map cleaned", many.isEmpty());

        WeakHashMap<String, String> stringKeys = new WeakHashMap<String, String>();
        String alive = new String("alive");
        stringKeys.put(alive, "yes");
        collect();
        check("weak map live key", "yes".equals(stringKeys.get(alive)) && stringKeys.size() == 1);

        System.out.println("reference checks=" + checks + " failures=" + failures);
        if (failures == 0)
        {
            System.out.println("weak references: ok");
        }
    }
}
