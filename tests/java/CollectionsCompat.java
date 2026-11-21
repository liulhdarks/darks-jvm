import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class CollectionsCompat
{
    private static int failures;

    private static void check(String name, boolean value)
    {
        if (!value)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args)
    {
        List<Integer> values = new ArrayList<Integer>();
        values.add(2);
        values.add(1);
        values.add(3);
        Collections.sort(values, Collections.<Integer>reverseOrder());
        check("reverse-order-first", values.get(0) == 3);
        check("reverse-order-last", values.get(2) == 1);

        Comparator<Integer> base = new Comparator<Integer>()
        {
            public int compare(Integer a, Integer b)
            {
                return a.intValue() - b.intValue();
            }
        };
        Collections.sort(values, Collections.reverseOrder(base));
        check("reverse-cmp-first", values.get(0) == 3);

        Set<String> set = new HashSet<String>();
        set.add("a");
        Set<String> syncSet = Collections.synchronizedSet(set);
        check("sync-set-add", syncSet.add("b"));
        check("sync-set-size", syncSet.size() == 2);
        check("sync-set-contains", syncSet.contains("a"));

        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("one", 1);
        Map<String, Integer> syncMap = Collections.synchronizedMap(map);
        syncMap.put("two", 2);
        check("sync-map-size", syncMap.size() == 2);
        check("sync-map-get", syncMap.get("two") == 2);
        check("sync-map-keyset", syncMap.keySet().contains("one"));

        List<String> single = Collections.singletonList("x");
        check("singleton-list", single.size() == 1 && single.get(0).equals("x"));
        check("singleton-set", Collections.singleton("y").contains("y"));
        check("singleton-map", Collections.singletonMap("k", "v").get("k").equals("v"));

        if (failures == 0)
        {
            System.out.println("collections-compat: ok");
        }
        else
        {
            System.out.println("collections-compat failures=" + failures);
            System.exit(1);
        }
    }
}
