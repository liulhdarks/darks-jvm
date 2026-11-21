package java.util.stream;

import java.util.Iterator;
import java.util.Spliterator;

/**
 * Common contract of the stream types.  The eager in-memory streams in this
 * runtime never run in parallel; the parallel-related operations are no-ops
 * that keep the stream sequential.
 */
public interface BaseStream<T, S extends BaseStream<T, S>> extends AutoCloseable
{
    Iterator<T> iterator();

    Spliterator<T> spliterator();

    boolean isParallel();

    S sequential();

    S parallel();

    S unordered();

    S onClose(Runnable closeHandler);

    void close();
}
