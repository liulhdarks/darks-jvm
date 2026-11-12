import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

// Compiled with `javac --release 11`; covers the java.util subset that the
// runtime provides together with java.util.function.
public class CollectionsDemo
{
    static int twice(int value)
    {
        return value * 2;
    }

    int scale(int value)
    {
        return value * 3;
    }

    public static void main(String[] args)
    {
        List<Integer> list = new ArrayList<Integer>();
        list.add(3);
        list.add(1);
        list.add(2);
        boolean hasTwo = list.contains(2);
        list.remove(Integer.valueOf(1));
        Collections.sort(list);
        Collections.reverse(list);
        int iterSum = 0;
        Iterator<Integer> iterator = list.iterator();
        while (iterator.hasNext())
        {
            iterSum += iterator.next();
        }

        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("b", 20);
        int mapSize = map.size();
        int b = map.get("b");
        map.remove("a");
        int afterRemove = map.size();

        Set<Integer> set = new HashSet<Integer>();
        set.add(3);
        set.add(1);
        set.add(3);
        set.add(2);
        int setSum = 0;
        for (int value : set)
        {
            setSum += value;
        }

        List<Integer> numbers = Arrays.asList(5, 3, 9, 1);
        Collections.sort(numbers);
        int max = Collections.max(numbers);

        Function<Integer, Integer> addOne = value -> value + 1;
        Function<Integer, Integer> staticRef = CollectionsDemo::twice;
        CollectionsDemo demo = new CollectionsDemo();
        Function<Integer, Integer> boundRef = demo::scale;
        Supplier<String> supplier = () -> "sup";
        Predicate<Integer> predicate = value -> value > 10;
        BiFunction<Integer, Integer, Integer> sum = (first, second) -> first + second;
        Supplier<ArrayList<String>> factory = ArrayList::new;
        Consumer<String> consumer = value -> { };
        consumer.accept("ignored");

        System.out.println("coll=" + list.size() + ":" + list.get(1) + ":" + hasTwo + ":"
            + iterSum + ":" + mapSize + ":" + b + ":" + afterRemove
            + ":" + set.size() + ":" + setSum + ":" + max
            + ":" + addOne.apply(4) + ":" + staticRef.apply(4) + ":" + boundRef.apply(4)
            + ":" + sum.apply(2, 5) + ":" + factory.get().size() + ":" + supplier.get().length()
            + ":" + (predicate.test(11) ? 1 : 0) + ":" + (predicate.test(2) ? 1 : 0));
        System.out.println("java11 collections: ok");
    }
}
