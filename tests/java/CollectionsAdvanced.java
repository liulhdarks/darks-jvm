import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CollectionsAdvanced
{
    private static int checks;
    private static int failures;

    private static void check(String name, boolean value)
    {
        checks++;
        if (!value)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    private static void eq(String name, int got, int want)
    {
        check(name + " got=" + got + " want=" + want, got == want);
    }

    private static void eq(String name, long got, long want)
    {
        check(name + " got=" + got + " want=" + want, got == want);
    }

    private static void eq(String name, String got, String want)
    {
        check(name + " got=" + got + " want=" + want,
            want == null ? got == null : want.equals(got));
    }

    private static void mapTests()
    {
        Map<String, Integer> map = new HashMap<String, Integer>();
        check("empty", map.isEmpty());
        check("put-new-returns-null", map.put("a", 1) == null);
        eq("put-old", map.put("a", 2).intValue(), 1);
        eq("get", map.get("a").intValue(), 2);
        check("contains-key", map.containsKey("a"));
        check("contains-missing", !map.containsKey("z"));
        check("contains-value", map.containsValue(2));
        check("contains-value-missing", !map.containsValue(99));

        check("null-key-put", map.put(null, 9) == null);
        eq("null-key-get", map.get(null).intValue(), 9);
        check("null-key-contains", map.containsKey(null));
        check("null-value-put", map.put("nil", null) == null);
        check("null-value-contains-key", map.containsKey("nil"));
        check("null-value-get", map.get("nil") == null);
        check("contains-null-value", map.containsValue(null));
        eq("size-three", map.size(), 3);

        Collection<Integer> values = map.values();
        eq("values-size", values.size(), 3);
        check("values-contains", values.contains(Integer.valueOf(9)));
        check("values-contains-null", values.contains(null));

        Set<String> keys = map.keySet();
        eq("keyset-size", keys.size(), 3);
        check("keyset-null", keys.contains(null));
        check("keyset-a", keys.contains("a"));

        Map<String, Integer> entries = new HashMap<String, Integer>();
        entries.put("x", 1);
        entries.put("y", 2);
        int entryCount = 0;
        int entrySum = 0;
        Iterator<Map.Entry<String, Integer>> it = entries.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, Integer> entry = it.next();
            entryCount++;
            entrySum += entry.getValue().intValue();
            if ("x".equals(entry.getKey()))
            {
                eq("entry-setValue-old", entry.setValue(10).intValue(), 1);
                eq("entry-setValue-new", entry.getValue().intValue(), 10);
            }
        }
        eq("entry-count", entryCount, 2);
        eq("entry-sum", entrySum, 3);
        eq("entry-setValue-visible", entries.get("x").intValue(), 10);

        Map<String, Integer> first = new HashMap<String, Integer>();
        first.put("a", 1);
        first.put("b", 2);
        Map<String, Integer> second = new HashMap<String, Integer>();
        second.put("a", 1);
        second.put("b", 2);
        HashSet<Map.Entry<String, Integer>> dedup = new HashSet<Map.Entry<String, Integer>>();
        Iterator<Map.Entry<String, Integer>> iter = first.entrySet().iterator();
        while (iter.hasNext())
        {
            dedup.add(iter.next());
        }
        iter = second.entrySet().iterator();
        while (iter.hasNext())
        {
            dedup.add(iter.next());
        }
        eq("entry-equals-hashcode", dedup.size(), 2);

        Map<String, Integer> resized = new HashMap<String, Integer>();
        int i = 0;
        while (i < 200)
        {
            resized.put("k" + i, i);
            i++;
        }
        eq("resize-size", resized.size(), 200);
        boolean preserved = true;
        i = 0;
        while (i < 200)
        {
            Integer value = resized.get("k" + i);
            if (value == null || value.intValue() != i)
            {
                preserved = false;
            }
            i++;
        }
        check("resize-preserves", preserved);

        String key1 = new String("shared");
        String key2 = new String("shared");
        Map<String, Integer> stringKeys = new HashMap<String, Integer>();
        stringKeys.put(key1, 7);
        eq("string-equals-get", stringKeys.get(key2).intValue(), 7);
        check("string-equals-hash", key1.hashCode() == key2.hashCode());

        eq("remove-null-key", map.remove(null).intValue(), 9);
        check("remove-null-gone", !map.containsKey(null));
        eq("remove-size", map.size(), 2);
        check("remove-value", map.remove("a") != null);
        check("remove-missing", map.remove("zzz") == null);
        eq("remove-final-size", map.size(), 1);
        map.clear();
        eq("clear-size", map.size(), 0);
        check("clear-empty", map.isEmpty());
    }

    private static void listTests()
    {
        List<Integer> list = new ArrayList<Integer>();
        list.add(1);
        list.add(3);
        list.add(5);
        List<Integer> extra = new ArrayList<Integer>();
        extra.add(2);
        extra.add(4);
        check("addAll-index", list.addAll(1, extra));
        eq("addAll-size", list.size(), 5);
        eq("addAll-order0", list.get(0).intValue(), 1);
        eq("addAll-order1", list.get(1).intValue(), 2);
        eq("addAll-order2", list.get(2).intValue(), 4);
        eq("addAll-order3", list.get(3).intValue(), 3);
        eq("addAll-order4", list.get(4).intValue(), 5);
        eq("lastIndexOf", list.lastIndexOf(Integer.valueOf(5)), 4);
        eq("lastIndexOf-missing", list.lastIndexOf(Integer.valueOf(9)), -1);

        Iterator<Integer> it = list.iterator();
        while (it.hasNext())
        {
            int value = it.next().intValue();
            if (value % 2 == 0)
            {
                it.remove();
            }
        }
        eq("iterator-remove-size", list.size(), 3);
        eq("iterator-remove-0", list.get(0).intValue(), 1);
        eq("iterator-remove-1", list.get(1).intValue(), 3);
        eq("iterator-remove-2", list.get(2).intValue(), 5);

        Iterable<Integer> iterable = list;
        int sum = 0;
        for (int value : iterable)
        {
            sum += value;
        }
        eq("iterable-sum", sum, 9);

        boolean caught = false;
        try
        {
            list.get(3);
        }
        catch (IndexOutOfBoundsException error)
        {
            caught = true;
        }
        check("index-bounds", caught);
    }

    private static void stringBuilderTests()
    {
        StringBuilder sb = new StringBuilder("abc");
        sb.append('d');
        sb.append(12);
        sb.append(3L);
        sb.append(1.5);
        sb.append(true);
        sb.append('x');
        sb.append(new char[] { 'y', 'z' }, 1, 1);
        eq("sb-append", sb.toString(), "abcd1231.5truexz");

        StringBuilder text = new StringBuilder("hello");
        text.replace(1, 3, "XY");
        eq("sb-replace", text.toString(), "hXYlo");
        eq("sb-indexOf", text.indexOf("XY"), 1);
        text.insert(0, 7);
        eq("sb-insert-int", text.toString(), "7hXYlo");
        text.deleteCharAt(1);
        eq("sb-deleteCharAt", text.toString(), "7XYlo");
        text.reverse();
        eq("sb-reverse", text.toString(), "olYX7");
        check("sb-charAt", text.charAt(3) == 'X');
        check("sb-capacity", text.length() <= text.capacity());
        text.ensureCapacity(100);
        check("sb-ensureCapacity", text.capacity() >= 100);
        text.setLength(2);
        eq("sb-setLength", text.toString(), "ol");
        eq("sb-substring", new StringBuilder("abcdef").substring(2, 4), "cd");
        eq("sb-insert-double", new StringBuilder("ab").insert(1, 2.5).toString(), "a2.5b");
    }

    private static void objectsMathTests()
    {
        check("objects-equals", Objects.equals("a", "a"));
        check("objects-equals-null", Objects.equals(null, null));
        check("objects-not-equals", !Objects.equals("a", "b"));
        eq("objects-hashCode-null", Objects.hashCode(null), 0);
        eq("objects-hash", Objects.hash("a", Integer.valueOf(1)), 3969);
        eq("objects-toString-default", Objects.toString(null, "d"), "d");
        eq("objects-toString", Objects.toString(Integer.valueOf(5), "d"), "5");

        boolean npe = false;
        try
        {
            Objects.requireNonNull(null, "msg");
        }
        catch (NullPointerException error)
        {
            npe = error.getMessage().equals("msg");
        }
        check("objects-requireNonNull", npe);

        eq("math-abs-int", Math.abs(-5), 5);
        eq("math-abs-long", Math.abs(-5L), 5L);
        check("math-abs-double", Math.abs(-2.5) == 2.5);
        eq("math-max", Math.max(3, 7), 7);
        eq("math-min-long", Math.min(9L, 4L), 4L);
        eq("math-sqrt", (long)Math.sqrt(81.0), 9L);
        eq("math-pow", (long)Math.pow(2.0, 10.0), 1024L);
        eq("math-floor", (long)Math.floor(2.9), 2L);
        eq("math-ceil", (long)Math.ceil(2.1), 3L);
        eq("math-round", Math.round(2.5), 3L);
        eq("math-round-negative", Math.round(-2.5), -3L);
        eq("math-signum", Math.signum(-3), -1);
        eq("math-toIntExact", Math.toIntExact(2147483647L), 2147483647);
        eq("math-floorDiv", Math.floorDiv(-7, 2), -4);
        eq("math-floorMod", Math.floorMod(-7, 2), 1);
        eq("math-hypot", (long)Math.hypot(3.0, 4.0), 5L);

        boolean overflow = false;
        try
        {
            Math.toIntExact(2147483648L);
        }
        catch (ArithmeticException error)
        {
            overflow = true;
        }
        check("math-toIntExact-overflow", overflow);
    }

    private static void boxTests()
    {
        Integer a = Integer.valueOf(127);
        Integer b = Integer.valueOf(127);
        check("integer-equals", a.equals(b));
        eq("integer-hashCode", a.hashCode(), 127);
        eq("integer-compareTo", a.compareTo(Integer.valueOf(200)), -1);
        eq("integer-parse", Integer.parseInt("-321"), -321);
        eq("integer-toString", Integer.toString(-45), "-45");

        Long big = Long.valueOf(5000000000L);
        eq("long-longValue", big.longValue(), 5000000000L);
        eq("long-intValue", big.intValue(), (int)5000000000L);
        check("long-equals", big.equals(Long.valueOf(5000000000L)));
        eq("long-hashCode", big.hashCode(), Long.hashCode(5000000000L));
        eq("long-byteValue", Long.valueOf(300L).byteValue(), (byte)300);
        eq("long-shortValue", Long.valueOf(70000L).shortValue(), (short)70000);
        eq("integer-byteValue", Integer.valueOf(300).byteValue(), (byte)300);
        eq("integer-shortValue", Integer.valueOf(70000).shortValue(), (short)70000);

        Map<Integer, String> boxed = new HashMap<Integer, String>();
        boxed.put(1, "one");
        boxed.put(2, "two");
        eq("boxed-get", boxed.get(Integer.valueOf(2)), "two");
        check("boxed-contains", boxed.containsKey(Integer.valueOf(1)));
        eq("boxed-size", boxed.size(), 2);
    }

    public static void main(String[] args)
    {
        mapTests();
        listTests();
        stringBuilderTests();
        objectsMathTests();
        boxTests();
        System.out.println("collections checks=" + checks + " failures=" + failures);
        if (failures == 0)
        {
            System.out.println("collections advanced: ok");
        }
    }
}
