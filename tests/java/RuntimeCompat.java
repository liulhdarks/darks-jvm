import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;

public class RuntimeCompat
{
    static int checks = 0;
    static int failures = 0;

    static void check(String name, boolean ok)
    {
        checks++;
        if (!ok)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args)
    {
        // ---- String -----------------------------------------------------
        String padded = "  Hello, JVM World  ";
        check("trim", padded.trim().equals("Hello, JVM World"));
        check("substring", "abcdef".substring(2, 4).equals("cd"));
        check("substring-tail", "abcdef".substring(4).equals("ef"));
        check("indexOf-from", "abcabc".indexOf("bc", 2) == 4);
        check("indexOf-char", "abcabc".indexOf('c', 3) == 5);
        check("lastIndexOf", "abcabc".lastIndexOf("bc") == 4);
        check("lastIndexOf-char", "banana".lastIndexOf('a') == 5);
        check("equalsIgnoreCase", "HeLLo".equalsIgnoreCase("hello"));
        check("compareTo", "a".compareTo("b") < 0);
        check("startsWith", "foobar".startsWith("foo"));
        check("startsWith-offset", "foobar".startsWith("bar", 3));
        check("endsWith", "foobar".endsWith("bar"));
        check("contains", "foobar".contains("oba"));
        check("replace-char", "a-b-c".replace('-', '+').equals("a+b+c"));
        check("replace-seq", "a-b-c".replace("-", "::").equals("a::b::c"));
        String[] parts = "a,b,,c".split(",");
        check("split", parts.length == 4 && parts[0].equals("a") &&
                       parts[2].isEmpty() && parts[3].equals("c"));
        check("toCharArray", "abc".toCharArray().length == 3);
        byte[] utf8 = "h\u00e9llo".getBytes();
        check("getBytes-length", utf8.length == 6);
        check("new-String-bytes", new String(utf8).equals("h\u00e9llo"));
        check("valueOf", String.valueOf(42).equals("42") &&
                         String.valueOf(true).equals("true") &&
                         String.valueOf('x').equals("x"));
        check("valueOf-range", String.valueOf(new char[] {'a', 'b', 'c'}, 1, 2).equals("bc"));
        check("concat", "a".concat("b").equals("ab"));
        check("isEmpty", "".isEmpty() && !"x".isEmpty());
        check("format", String.format("%s-%02d-%.2f", "x", 7, 1.5).equals("x-07-1.50"));
        check("format-percent", String.format("%d%%", 50).equals("50%"));

        // ---- StringBuilder ----------------------------------------------
        StringBuilder sb = new StringBuilder();
        sb.append(1).append('c').append(2L).append(true).append(3.5).append("s");
        check("sb-append", sb.toString().equals("1c2true3.5s"));
        sb.setLength(2);
        check("sb-setLength", sb.toString().equals("1c"));
        sb.reverse();
        check("sb-reverse", sb.toString().equals("c1"));
        sb.deleteCharAt(0);
        check("sb-deleteCharAt", sb.toString().equals("1"));

        // ---- Number wrappers --------------------------------------------
        check("parseInt", Integer.parseInt("-123") == -123);
        check("parseLong", Long.parseLong("9223372036854775807") == Long.MAX_VALUE);
        check("parseDouble", Double.parseDouble("3.25") == 3.25);
        check("toHexString", Integer.toHexString(255).equals("ff") &&
                             Long.toHexString(16).equals("10"));
        check("compare", Integer.compare(1, 2) < 0 && Long.compare(2, 2) == 0 &&
                         Double.compare(2.0, 1.0) > 0);
        check("constants", Integer.MAX_VALUE == 2147483647 && Long.MIN_VALUE < 0);
        check("boolean", Boolean.valueOf("TRUE").booleanValue() &&
                         Boolean.compare(false, true) < 0);

        // ---- Collections -------------------------------------------------
        ArrayList<String> list = new ArrayList<String>();
        list.add("pear");
        list.add("apple");
        list.add("fig");
        check("list-size", list.size() == 3 && !list.isEmpty());
        check("list-contains", list.contains("apple") && list.indexOf("fig") == 2);
        list.remove(1);
        check("list-remove", list.size() == 2 && list.get(1).equals("fig"));
        Collections.sort(list);
        check("list-sort", list.get(0).equals("fig") && list.get(1).equals("pear"));
        Collections.reverse(list);
        check("list-reverse", list.get(0).equals("pear"));
        Object[] array = list.toArray();
        check("list-toArray", array.length == 2);

        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("a", Integer.valueOf(1));
        map.put("b", Integer.valueOf(2));
        map.put("a", Integer.valueOf(3));
        check("map-put", map.size() == 2 && map.get("a").intValue() == 3);
        check("map-contains", map.containsKey("b") && map.containsValue(Integer.valueOf(2)));
        check("map-default", map.getOrDefault("z", Integer.valueOf(9)).intValue() == 9);
        HashMap<String, Integer> extra = new HashMap<String, Integer>();
        extra.put("c", Integer.valueOf(4));
        map.putAll(extra);
        check("map-putAll", map.size() == 3);

        HashSet<String> set = new HashSet<String>();
        set.add("x");
        set.add("x");
        set.add("y");
        check("set", set.size() == 2 && set.contains("x") && set.remove("y"));

        int[] numbers = {3, 1, 2};
        Arrays.sort(numbers);
        check("arrays-sort", Arrays.equals(numbers, new int[] {1, 2, 3}));
        check("arrays-toString", Arrays.toString(numbers).equals("[1, 2, 3]"));
        int[] copied = Arrays.copyOf(numbers, 5);
        check("arrays-copyOf", copied.length == 5 && copied[4] == 0);
        java.util.List<Integer> asList = Arrays.asList(Integer.valueOf(2), Integer.valueOf(1));
        check("arrays-asList", asList.size() == 2);

        Random r1 = new Random(42);
        Random r2 = new Random(42);
        check("random-seed", r1.nextInt() == r2.nextInt() &&
                            r1.nextLong() == r2.nextLong() &&
                            r1.nextDouble() == r2.nextDouble());
        Random r3 = new Random(7);
        int bound = r3.nextInt(10);
        check("random-bound", bound >= 0 && bound < 10);

        check("objects", Objects.equals("a", "a") &&
                         Objects.hashCode(null) == 0 &&
                         Objects.toString(null, "d").equals("d"));
        boolean threw = false;
        try
        {
            Objects.requireNonNull(null, "must not be null");
        }
        catch (NullPointerException e)
        {
            threw = "must not be null".equals(e.getMessage());
        }
        check("objects-requireNonNull", threw);

        // ---- System ------------------------------------------------------
        check("property", "11.0.2".equals(System.getProperty("java.version")));
        check("property-default", "fallback".equals(System.getProperty("no.such.key", "fallback")));
        System.setProperty("compat.dynamic", "v");
        check("setProperty", "v".equals(System.getProperty("compat.dynamic")));
        check("clearProperty", "v".equals(System.clearProperty("compat.dynamic")) &&
                               System.getProperty("compat.dynamic") == null);
        check("getenv", System.getenv("PATH") != null);
        check("mapLibraryName", System.mapLibraryName("demo").startsWith("lib"));

        int[] overlap = {1, 2, 3, 4, 5};
        System.arraycopy(overlap, 0, overlap, 1, 4);
        check("arraycopy-overlap", Arrays.equals(overlap, new int[] {1, 1, 2, 3, 4}));
        boolean arraycopyThrew = false;
        try
        {
            System.arraycopy(overlap, 0, overlap, 4, 2);
        }
        catch (IndexOutOfBoundsException e)
        {
            arraycopyThrew = true;
        }
        check("arraycopy-bounds", arraycopyThrew);

        // ---- Math --------------------------------------------------------
        check("math", Math.abs(-4) == 4 && Math.max(2, 3) == 3 &&
                      Math.min(2, 3) == 2 && Math.pow(2.0, 10.0) == 1024.0 &&
                      Math.sqrt(81.0) == 9.0 && Math.floor(1.9) == 1.0 &&
                      Math.ceil(1.1) == 2.0 && Math.round(2.6) == 3L);

        Object identityObject = new Object();
        int identityHash = System.identityHashCode(identityObject);
        check("identityHashCode-stable", identityHash != 0 &&
              identityHash == System.identityHashCode(identityObject));
        long nanoStart = System.nanoTime();
        long nanoEnd = System.nanoTime();
        check("nanoTime-monotonic", nanoEnd >= nanoStart);

        System.out.println("runtime-compat checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
    }
}
