package java.util.stream;

import java.util.ArrayList;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class StreamSupport
{
    private StreamSupport()
    {
    }

    public static <T> Stream<T> stream(Spliterator<T> spliterator, boolean parallel)
    {
        return drain(spliterator);
    }

    public static <T> Stream<T> stream(Supplier<? extends Spliterator<T>> supplier,
        int characteristics, boolean parallel)
    {
        return drain(supplier.get());
    }

    private static <T> Stream<T> drain(Spliterator<T> spliterator)
    {
        ArrayList<T> list = new ArrayList<T>();
        spliterator.forEachRemaining(new ListConsumer<T>(list));
        return Stream.from(list);
    }

    private static final class ListConsumer<T> implements Consumer<T>
    {
        private final ArrayList<T> values;

        ListConsumer(ArrayList<T> values)
        {
            this.values = values;
        }

        public void accept(T value)
        {
            values.add(value);
        }
    }
}
