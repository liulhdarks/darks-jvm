import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JdkIdentityHashMapTest
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

    public static void main(String[] args)
    {
        IdentityHashMap<Object, String> map = new IdentityHashMap<Object, String>();
        String first = new String("same");
        String second = new String("same");
        check("empty", map.isEmpty());
        check("put first", map.put(first, "first") == null);
        check("put second", map.put(second, "second") == null);
        check("identity size", map.size() == 2);
        check("identity get first", "first".equals(map.get(first)));
        check("identity get second", "second".equals(map.get(second)));
        check("identity not equals", map.get(new String("same")) == null);
        check("identity contains", map.containsKey(first) && map.containsKey(second));
        check("identity replace", "first".equals(map.put(first, "updated")));
        check("identity size stable", map.size() == 2);
        check("identity value", "updated".equals(map.get(first)));
        check("identity remove", "second".equals(map.remove(second)));
        check("identity size after remove", map.size() == 1);

        Integer[] keys = new Integer[500];
        for (int i = 0; i < 500; i++)
        {
            keys[i] = new Integer(i);
            map.put(keys[i], "v" + i);
        }
        check("identity grow", map.size() == 501);
        check("identity grown lookup", "v499".equals(map.get(keys[499])));
        map.clear();
        check("identity clear", map.isEmpty() && map.size() == 0);

        Set<String> fromMap = Collections.newSetFromMap(new IdentityHashMap<String, Boolean>());
        String a = new String("x");
        String b = new String("x");
        check("set add a", fromMap.add(a));
        check("set add b", fromMap.add(b));
        check("set identity size", fromMap.size() == 2);
        check("set contains a", fromMap.contains(a));
        check("set contains b", fromMap.contains(b));
        check("set not equals", !fromMap.contains(new String("x")));
        check("set remove", fromMap.remove(a));
        check("set size after remove", fromMap.size() == 1);

        List<String> emptyList = Collections.emptyList();
        Set<String> emptySet = Collections.emptySet();
        Map<String, String> emptyMap = Collections.emptyMap();
        check("empty list", emptyList.isEmpty() && emptyList.size() == 0);
        check("empty set", emptySet.isEmpty() && emptySet.size() == 0);
        check("empty map", emptyMap.isEmpty() && emptyMap.get("x") == null);
        check("empty iterator", !Collections.emptyIterator().hasNext());
        check("singleton list", Collections.singletonList("s").get(0).equals("s"));
        check("singleton", Collections.singleton("s").contains("s"));
        check("singleton map", "v".equals(Collections.singletonMap("k", "v").get("k")));

        Map<String, String> defaults = new HashMap<String, String>();
        check("putIfAbsent", defaults.putIfAbsent("k", "v") == null);
        check("putIfAbsent existing", "v".equals(defaults.putIfAbsent("k", "other")));
        check("computeIfAbsent", "computed".equals(defaults.computeIfAbsent("c", k -> "computed")));
        check("computeIfAbsent existing", "computed".equals(defaults.get("c")));
        check("replace", "v".equals(defaults.replace("k", "v2")));
        check("remove pair", defaults.remove("k", "v2"));
        check("compute", "3".equals(defaults.compute("n", (k, v) -> v == null ? "3" : v + "x")));
        check("merge", "3x".equals(defaults.merge("n", "x", (l, r) -> l + r)));

        Iterator<Map.Entry<Object, String>> entries = map.entrySet().iterator();
        check("entry iterator empty", !entries.hasNext());

        System.out.println("identity checks=" + checks + " failures=" + failures);
        if (failures == 0)
        {
            System.out.println("identity hashmap: ok");
        }
    }
}
