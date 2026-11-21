package java.util.stream;

import java.util.ArrayList;
import java.util.Spliterator;
import java.util.function.Consumer;

public final class StreamSupport
{
    private StreamSupport()
    {
    }

    public static <T> Stream<T> stream(Spliterator<T> spliterator,
                                       boolean parallel)
    {
        if (spliterator == null)
        {
            throw new NullPointerException("spliterator");
        }
        final ArrayList<T> list = new ArrayList<T>();
        spliterator.forEachRemaining(new Consumer<T>()
        {
            public void accept(T value)
            {
                list.add(value);
            }
        });
        return Stream.from(list);
    }
}
