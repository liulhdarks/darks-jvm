package java.util.concurrent;

import java.util.function.BiFunction;

public interface CompletionStage<T>
{
    <U> CompletionStage<U> thenCompose(
        java.util.function.Function<? super T, ? extends CompletionStage<U>> fn);

    <U> CompletionStage<U> handle(
        BiFunction<? super T, Throwable, ? extends U> fn);

    CompletionStage<T> whenComplete(
        java.util.function.BiConsumer<? super T, ? super Throwable> action);

    CompletableFuture<T> toCompletableFuture();
}
