package java.util.concurrent;

import java.util.function.BiFunction;

public interface CompletionStage<T>
{
    <U> CompletionStage<U> handle(
        BiFunction<? super T, Throwable, ? extends U> fn);
}
