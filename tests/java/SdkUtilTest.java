import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SdkUtilTest
{
    static int checks = 0;
    static int failures = 0;

    static void check(String name, boolean ok)
    {
        checks = checks + 1;
        if (!ok)
        {
            failures = failures + 1;
            System.out.println("FAIL " + name);
        }
    }

    static void eq(String name, int got, int want)
    {
        check(name + " got=" + got + " want=" + want, got == want);
    }

    static void eq(String name, Object got, Object want)
    {
        check(name + " got=" + got + " want=" + want,
              want == null ? got == null : want.equals(got));
    }

    static void listEq(String name, List<?> got, String want)
    {
        check(name + " got=" + got + " want=" + want, want.equals(got.toString()));
    }

    static List<Integer> numbers()
    {
        List<Integer> values = new ArrayList<Integer>();
        values.add(Integer.valueOf(5));
        values.add(Integer.valueOf(3));
        values.add(Integer.valueOf(3));
        values.add(Integer.valueOf(9));
        values.add(Integer.valueOf(1));
        values.add(Integer.valueOf(7));
        return values;
    }

    static void optionalTests()
    {
        Optional<String> some = Optional.of("abc");
        check("optional-present", some.isPresent());
        check("optional-not-empty", !some.isEmpty());
        eq("optional-get", some.get(), "abc");
        eq("optional-map", Integer.valueOf(some.map(v -> Integer.valueOf(v.length())).get()), Integer.valueOf(3));
        eq("optional-flatmap", Integer.valueOf(some.flatMap(v -> Optional.of(Integer.valueOf(v.length()))).get()),
           Integer.valueOf(3));
        check("optional-filter-keep", some.filter(v -> v.startsWith("a")).isPresent());
        check("optional-filter-drop", !some.filter(v -> v.startsWith("z")).isPresent());
        eq("optional-orElse", some.orElse("fallback"), "abc");
        eq("optional-orElseGet", Integer.valueOf(some.map(v -> Integer.valueOf(3)).orElseGet(() -> Integer.valueOf(9))),
           Integer.valueOf(3));
        eq("optional-toString", some.toString(), "Optional[abc]");
        eq("optional-empty-string", Optional.empty().toString(), "Optional.empty");

        Optional<String> empty = Optional.empty();
        check("optional-empty", !empty.isPresent() && empty.isEmpty());
        check("optional-empty-eq", Optional.empty().equals(empty));
        eq("optional-empty-orElse", empty.orElse("fallback"), "fallback");
        eq("optional-empty-orElseGet", empty.orElseGet(() -> "supplied"), "supplied");

        boolean threw = false;
        try
        {
            empty.get();
        }
        catch (NoSuchElementException error)
        {
            threw = true;
        }
        check("optional-empty-get-throws", threw);

        threw = false;
        try
        {
            empty.orElseThrow(() -> new IllegalStateException("missing"));
        }
        catch (IllegalStateException error)
        {
            threw = "missing".equals(error.getMessage());
        }
        check("optional-orElseThrow", threw);

        threw = false;
        try
        {
            Optional.of(null);
        }
        catch (NullPointerException error)
        {
            threw = true;
        }
        check("optional-of-null", threw);

        check("optional-ofNullable-null", !Optional.ofNullable(null).isPresent());
        check("optional-ofNullable-value", Optional.ofNullable("x").get().equals("x"));
        eq("optional-equals", Optional.of("x"), Optional.of("x"));
        check("optional-hash", Optional.of("x").hashCode() == Optional.of("x").hashCode());

        final int[] seen = new int[1];
        some.ifPresent(v -> seen[0] = v.length());
        eq("optional-ifPresent", seen[0], 3);
        empty.ifPresent(v -> seen[0] = 99);
        eq("optional-ifPresent-empty", seen[0], 3);

        OptionalInt present = OptionalInt.of(41);
        check("optionalInt-present", present.isPresent());
        eq("optionalInt-get", present.getAsInt(), 41);
        eq("optionalInt-map", present.map(v -> v + 1).getAsInt(), 42);
        eq("optionalInt-orElse", OptionalInt.empty().orElse(7), 7);
        eq("optionalInt-orElseGet", OptionalInt.empty().orElseGet(() -> 8), 8);
        eq("optionalInt-empty-string", OptionalInt.empty().toString(), "OptionalInt.empty");
    }

    static void streamTests()
    {
        List<Integer> values = numbers();
        eq("stream-count", (int)values.stream().count(), 6);
        eq("stream-filter-count", (int)values.stream().filter(v -> v.intValue() > 2).count(), 5);
        check("stream-anyMatch", values.stream().anyMatch(v -> v.intValue() == 9));
        check("stream-anyMatch-false", !values.stream().anyMatch(v -> v.intValue() == 100));

        List<Integer> sorted = values.stream().sorted().collect(Collectors.toList());
        listEq("stream-sorted", sorted, "[1, 3, 3, 5, 7, 9]");
        List<Integer> reversed = values.stream()
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());
        listEq("stream-sorted-comparator", reversed, "[9, 7, 5, 3, 3, 1]");
        List<Integer> limited = values.stream().sorted().limit(3).collect(Collectors.toList());
        listEq("stream-limit", limited, "[1, 3, 3]");
        List<Integer> skipped = values.stream().sorted().skip(4).collect(Collectors.toList());
        listEq("stream-skip", skipped, "[7, 9]");
        eq("stream-distinct-count", (int)values.stream().distinct().count(), 5);

        List<String> mapped = values.stream()
            .map(v -> "n" + v)
            .collect(Collectors.toList());
        eq("stream-map-size", mapped.size(), 6);
        eq("stream-map-value", mapped.get(0), "n5");

        String joined = values.stream()
            .map(v -> Integer.toString(v.intValue()))
            .collect(Collectors.joining(","));
        eq("stream-joining", joined, "5,3,3,9,1,7");
        String decorated = values.stream()
            .limit(2)
            .map(v -> Integer.toString(v.intValue()))
            .collect(Collectors.joining("-", "[", "]"));
        eq("stream-joining-affixes", decorated, "[5-3]");

        Set<Integer> mods = values.stream()
            .map(v -> Integer.valueOf(v.intValue() % 3))
            .collect(Collectors.toSet());
        eq("stream-toSet-size", mods.size(), 3);
        check("stream-toSet-values", mods.contains(Integer.valueOf(0))
              && mods.contains(Integer.valueOf(1)) && mods.contains(Integer.valueOf(2)));

        Map<String, Integer> lengths = Stream.of("a", "bb", "ccc").collect(
            Collectors.toMap(v -> v, v -> Integer.valueOf(v.length())));
        eq("stream-toMap-size", lengths.size(), 3);
        eq("stream-toMap-value", lengths.get("bb"), Integer.valueOf(2));

        boolean duplicate = false;
        try
        {
            Stream.of("a", "a").collect(Collectors.toMap(v -> v, v -> Integer.valueOf(1)));
        }
        catch (IllegalStateException error)
        {
            duplicate = true;
        }
        check("stream-toMap-duplicate", duplicate);

        final int[] sum = new int[1];
        values.stream().forEach(v -> sum[0] += v.intValue());
        eq("stream-forEach", sum[0], 28);

        List<Integer> direct = values.stream().filter(v -> v.intValue() < 4).toList();
        listEq("stream-toList", direct, "[3, 3, 1]");
        eq("stream-empty", (int)Stream.empty().count(), 0);
        eq("stream-of", (int)Stream.of("x", "y", "z").count(), 3);
        eq("stream-toArray", values.stream().limit(2).toArray().length, 2);

        Collection<Integer> collection = new LinkedHashSet<Integer>();
        collection.add(Integer.valueOf(2));
        collection.add(Integer.valueOf(2));
        collection.add(Integer.valueOf(4));
        listEq("collection-stream", collection.stream().sorted().collect(Collectors.toList()), "[2, 4]");
    }

    static void mapTests()
    {
        LinkedHashMap<String, Integer> linked = new LinkedHashMap<String, Integer>();
        linked.put("a", Integer.valueOf(1));
        linked.put("b", Integer.valueOf(2));
        linked.put("c", Integer.valueOf(3));
        linked.remove("b");
        linked.put("b", Integer.valueOf(4));
        listEq("linked-order", new ArrayList<String>(linked.keySet()), "[a, c, b]");
        eq("linked-value", linked.get("b"), Integer.valueOf(4));
        eq("linked-size", linked.size(), 3);
        listEq("linked-values", new ArrayList<Integer>(linked.values()), "[1, 3, 4]");

        List<String> entryOrder = new ArrayList<String>();
        Iterator<Map.Entry<String, Integer>> entries = linked.entrySet().iterator();
        while (entries.hasNext())
        {
            entryOrder.add(entries.next().getKey());
        }
        listEq("linked-entry-set", entryOrder, "[a, c, b]");

        TreeMap<String, Integer> tree = new TreeMap<String, Integer>();
        tree.put("c", Integer.valueOf(3));
        tree.put("a", Integer.valueOf(1));
        tree.put("b", Integer.valueOf(2));
        listEq("tree-keys", new ArrayList<String>(tree.keySet()), "[a, b, c]");
        listEq("tree-values", new ArrayList<Integer>(tree.values()), "[1, 2, 3]");
        eq("tree-first", tree.firstKey(), "a");
        eq("tree-last", tree.lastKey(), "c");
        eq("tree-get", tree.get("b"), Integer.valueOf(2));
        eq("tree-remove", tree.remove("b"), Integer.valueOf(2));
        listEq("tree-after-remove", new ArrayList<String>(tree.keySet()), "[a, c]");
        eq("tree-size", tree.size(), 2);

        TreeMap<Integer, String> custom = new TreeMap<Integer, String>(Comparator.reverseOrder());
        custom.put(Integer.valueOf(1), "one");
        custom.put(Integer.valueOf(3), "three");
        custom.put(Integer.valueOf(2), "two");
        listEq("tree-comparator", new ArrayList<Integer>(custom.keySet()), "[3, 2, 1]");
    }

    static void dequeTests()
    {
        Deque<String> deque = new ArrayDeque<String>();
        deque.addLast("b");
        deque.addFirst("a");
        deque.addLast("c");
        deque.push("start");
        eq("deque-size", deque.size(), 4);
        eq("deque-first", deque.getFirst(), "start");
        eq("deque-last", deque.getLast(), "c");
        listEq("deque-order", new ArrayList<String>(deque), "[start, a, b, c]");
        eq("deque-pop", deque.pop(), "start");
        eq("deque-poll", deque.pollFirst(), "a");
        eq("deque-pollLast", deque.pollLast(), "c");
        eq("deque-remaining", deque.peek(), "b");
        check("deque-offer", deque.offer("d"));
        eq("deque-after-offer", deque.peekLast(), "d");
        check("deque-remove-middle", deque.remove("b"));
        eq("deque-after-remove", deque.size(), 1);
        eq("deque-empty-poll", new ArrayDeque<String>().poll(), null);

        boolean threw = false;
        try
        {
            deque.add(null);
        }
        catch (NullPointerException error)
        {
            threw = true;
        }
        check("deque-null", threw);
    }

    static void collectionTests()
    {
        List<Integer> fixed = Arrays.asList(Integer.valueOf(3), Integer.valueOf(1), Integer.valueOf(2));
        eq("asList-size", fixed.size(), 3);
        eq("asList-get", fixed.get(1), Integer.valueOf(1));
        eq("asList-set-old", fixed.set(1, Integer.valueOf(9)), Integer.valueOf(1));
        eq("asList-set-new", fixed.get(1), Integer.valueOf(9));
        Collections.sort(fixed);
        listEq("asList-sort", fixed, "[2, 3, 9]");

        boolean threw = false;
        try
        {
            fixed.add(Integer.valueOf(4));
        }
        catch (UnsupportedOperationException error)
        {
            threw = true;
        }
        check("asList-fixed", threw);

        List<Integer> mutable = new ArrayList<Integer>();
        mutable.add(Integer.valueOf(2));
        mutable.add(Integer.valueOf(1));
        Collections.sort(mutable);
        listEq("collections-sort", mutable, "[1, 2]");

        List<Integer> readOnly = Collections.unmodifiableList(mutable);
        eq("unmodifiable-get", readOnly.get(0), Integer.valueOf(1));
        eq("unmodifiable-size", readOnly.size(), 2);
        threw = false;
        try
        {
            readOnly.add(Integer.valueOf(3));
        }
        catch (UnsupportedOperationException error)
        {
            threw = true;
        }
        check("unmodifiable-add", threw);
        threw = false;
        try
        {
            readOnly.iterator().remove();
        }
        catch (UnsupportedOperationException error)
        {
            threw = true;
        }
        check("unmodifiable-iterator-remove", threw);

        List<String> words = new ArrayList<String>();
        words.add("bb");
        words.add("a");
        words.add("ccc");
        words.add("dd");

        Comparator<String> byLength = Comparator.comparing(
            new Function<String, Integer>()
            {
                public Integer apply(String value)
                {
                    return Integer.valueOf(value.length());
                }
            });
        Comparator<String> byLengthThenReverse = byLength.thenComparing(Comparator.<String>reverseOrder());
        Collections.sort(words, byLengthThenReverse);
        listEq("comparator-chain", words, "[a, dd, bb, ccc]");

        Collections.sort(words, byLength.reversed());
        listEq("comparator-reversed", words, "[ccc, dd, bb, a]");

        Comparator<String> byFirstChar = byLength.thenComparing(
            new Function<String, String>()
            {
                public String apply(String value)
                {
                    return value.substring(0, 1);
                }
            });
        Collections.sort(words, byFirstChar);
        listEq("comparator-key-chain", words, "[a, bb, dd, ccc]");

        Comparator<String> explicit = Comparator.comparing(
            new Function<String, Integer>()
            {
                public Integer apply(String value)
                {
                    return Integer.valueOf(value.length());
                }
            },
            Comparator.<Integer>reverseOrder());
        Collections.sort(words, explicit);
        listEq("comparator-key-comparator", words, "[ccc, bb, dd, a]");

        Collections.sort(words);
        listEq("comparator-natural", words, "[a, bb, ccc, dd]");
        Collections.sort(words, Comparator.<String>reverseOrder());
        listEq("comparator-reverse-order", words, "[dd, ccc, bb, a]");
    }

    static void loggingTests()
    {
        eq("level-parse", Level.parse("WARNING"), Level.WARNING);
        check("level-compare", Level.INFO.intValue() > Level.FINE.intValue());
        check("level-name", Level.SEVERE.getName().equals("SEVERE"));

        LogRecord record = new LogRecord(Level.INFO, "formatted {0}");
        record.setParameters(new Object[] { "value" });
        String text = new SimpleFormatter().format(record);
        check("formatter-format", text.equals("INFO: formatted value\n"));

        Handler quiet = new ConsoleHandler();
        quiet.setLevel(Level.SEVERE);
        check("handler-level-filter", !quiet.isLoggable(new LogRecord(Level.INFO, "x")));

        Logger log = Logger.getLogger("sdk.util.test");
        log.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        log.addHandler(handler);
        log.setLevel(Level.INFO);
        check("logger-handler-count", log.getHandlers().length == 1);
        check("logger-loggable", log.isLoggable(Level.INFO));
        check("logger-not-fine", !log.isLoggable(Level.FINE));

        log.setLevel(Level.ALL);
        log.info("log-info");
        log.warning("log-warning-42");
        log.log(Level.INFO, "log-param-{0}", "value");
        log.fine("log-fine");

        Logger child = Logger.getLogger("sdk.util.test.child");
        eq("logger-parent", child.getParent().getName(), "sdk.util.test");
        check("logger-anonymous", Logger.getAnonymousLogger().getName().length() == 0);
        check("logger-global", Logger.getGlobal() == Logger.global);
        log.logp(Level.INFO, "SdkUtilTest", "loggingTests", "logp-message");
        log.logp(Level.INFO, "SdkUtilTest", "loggingTests", "logp-param {0}", "x");
        log.logp(Level.INFO, "SdkUtilTest", "loggingTests", "logp-params {0}",
            new Object[] {"y"});
        log.logp(Level.FINE, "SdkUtilTest", "loggingTests", "logp-throwable",
            new IllegalStateException("expected"));
        check("logger-logp", log.isLoggable(Level.INFO));

        Logger.getLogger("sdk.util.root").info("log-root-info");
        log.removeHandler(handler);
        check("logger-remove-handler", log.getHandlers().length == 0);
    }

    static void eventTests()
    {
        EventListener listener = new EventListener() { };
        EventObject event = new EventObject(listener);
        check("event-source", event.getSource() == listener);
        check("event-to-string", event.toString().indexOf("EventObject") >= 0);
        boolean rejected = false;
        try
        {
            new EventObject(null);
        }
        catch (IllegalArgumentException expected)
        {
            rejected = true;
        }
        check("event-null-source", rejected);
    }

    public static void main(String[] args)
    {
        optionalTests();
        streamTests();
        mapTests();
        dequeTests();
        collectionTests();
        eventTests();
        loggingTests();

        System.out.println("util checks=" + checks + " failures=" + failures);
        System.out.println("sdk util: ok");
    }
}
