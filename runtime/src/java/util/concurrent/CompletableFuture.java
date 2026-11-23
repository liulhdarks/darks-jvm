package java.util.concurrent;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CompletableFuture<T> implements Future<T>, CompletionStage<T>
{
    private final Object lock = new Object();
    private final ArrayList<Runnable> dependents = new ArrayList<Runnable>();
    private T value;
    private Throwable failure;
    private boolean completed;
    private boolean cancelled;

    public CompletableFuture()
    {
    }

    public static <U> CompletableFuture<U> completedFuture(U value)
    {
        CompletableFuture<U> future = new CompletableFuture<U>();
        future.complete(value);
        return future;
    }

    public static <U> CompletableFuture<U> failedFuture(Throwable ex)
    {
        if (ex == null)
        {
            throw new NullPointerException();
        }
        CompletableFuture<U> future = new CompletableFuture<U>();
        future.completeExceptionally(ex);
        return future;
    }

    public boolean isDone()
    {
        synchronized (lock)
        {
            return completed;
        }
    }

    public boolean isCancelled()
    {
        synchronized (lock)
        {
            return cancelled;
        }
    }

    public boolean complete(T v)
    {
        ArrayList<Runnable> run;
        synchronized (lock)
        {
            if (completed)
            {
                return false;
            }
            value = v;
            failure = null;
            completed = true;
            run = new ArrayList<Runnable>(dependents);
            dependents.clear();
            lock.notifyAll();
        }
        runAll(run);
        return true;
    }

    public boolean completeExceptionally(Throwable ex)
    {
        if (ex == null)
        {
            throw new NullPointerException();
        }
        ArrayList<Runnable> run;
        synchronized (lock)
        {
            if (completed)
            {
                return false;
            }
            value = null;
            failure = ex;
            completed = true;
            run = new ArrayList<Runnable>(dependents);
            dependents.clear();
            lock.notifyAll();
        }
        runAll(run);
        return true;
    }

    public boolean cancel(boolean mayInterruptIfRunning)
    {
        ArrayList<Runnable> run;
        synchronized (lock)
        {
            if (completed)
            {
                return false;
            }
            cancelled = true;
            completed = true;
            run = new ArrayList<Runnable>(dependents);
            dependents.clear();
            lock.notifyAll();
        }
        runAll(run);
        return true;
    }

    public T get() throws InterruptedException, ExecutionException
    {
        synchronized (lock)
        {
            while (!completed)
            {
                lock.wait();
            }
            if (cancelled)
            {
                throw new CancellationException();
            }
            if (failure != null)
            {
                throw new ExecutionException(failure);
            }
            return value;
        }
    }

    public T get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException
    {
        long millis = unit.toMillis(timeout);
        if (timeout > 0 && millis <= 0)
        {
            millis = 1;
        }
        long deadline = System.currentTimeMillis() + millis;
        synchronized (lock)
        {
            while (!completed)
            {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    throw new TimeoutException("Timeout after " + millis + " milliseconds");
                }
                lock.wait(remaining);
            }
            if (cancelled)
            {
                throw new CancellationException();
            }
            if (failure != null)
            {
                throw new ExecutionException(failure);
            }
            return value;
        }
    }

    public T join()
    {
        try
        {
            return get();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        }
        catch (ExecutionException e)
        {
            throw new CompletionException(e.getCause());
        }
    }

    public T getNow(T valueIfAbsent)
    {
        Throwable cause;
        boolean wasCancelled;
        T result;
        synchronized (lock)
        {
            if (!completed)
            {
                return valueIfAbsent;
            }
            cause = failure;
            wasCancelled = cancelled;
            result = value;
        }
        if (wasCancelled)
        {
            throw new CancellationException();
        }
        if (cause != null)
        {
            throw new CompletionException(cause);
        }
        return result;
    }

    public CompletableFuture<T> toCompletableFuture()
    {
        return this;
    }

    public <U> CompletableFuture<U> thenApply(final Function<? super T, ? extends U> fn)
    {
        final CompletableFuture<U> next = new CompletableFuture<U>();
        onCompletion(new Runnable()
        {
            public void run()
            {
                if (isCancelledInternal())
                {
                    next.cancel(false);
                    return;
                }
                Throwable cause = failureInternal();
                if (cause != null)
                {
                    next.completeExceptionally(cause);
                    return;
                }
                try
                {
                    next.complete(fn.apply(resultInternal()));
                }
                catch (Throwable t)
                {
                    next.completeExceptionally(t);
                }
            }
        });
        return next;
    }

    public CompletableFuture<Void> thenAccept(final Consumer<? super T> action)
    {
        final CompletableFuture<Void> next = new CompletableFuture<Void>();
        onCompletion(new Runnable()
        {
            public void run()
            {
                if (isCancelledInternal())
                {
                    next.cancel(false);
                    return;
                }
                Throwable cause = failureInternal();
                if (cause != null)
                {
                    next.completeExceptionally(cause);
                    return;
                }
                try
                {
                    action.accept(resultInternal());
                    next.complete(null);
                }
                catch (Throwable t)
                {
                    next.completeExceptionally(t);
                }
            }
        });
        return next;
    }

    public CompletableFuture<Void> thenRun(final Runnable action)
    {
        final CompletableFuture<Void> next = new CompletableFuture<Void>();
        onCompletion(new Runnable()
        {
            public void run()
            {
                if (isCancelledInternal())
                {
                    next.cancel(false);
                    return;
                }
                Throwable cause = failureInternal();
                if (cause != null)
                {
                    next.completeExceptionally(cause);
                    return;
                }
                try
                {
                    action.run();
                    next.complete(null);
                }
                catch (Throwable t)
                {
                    next.completeExceptionally(t);
                }
            }
        });
        return next;
    }

    public <U> CompletableFuture<U> thenCompose(
        final Function<? super T, ? extends CompletionStage<U>> fn)
    {
        final CompletableFuture<U> next = new CompletableFuture<U>();
        onCompletion(new Runnable()
        {
            public void run()
            {
                if (isCancelledInternal())
                {
                    next.cancel(false);
                    return;
                }
                Throwable cause = failureInternal();
                if (cause != null)
                {
                    next.completeExceptionally(cause);
                    return;
                }
                try
                {
                    CompletionStage<U> stage = fn.apply(resultInternal());
                    if (stage == null)
                    {
                        next.completeExceptionally(new NullPointerException());
                        return;
                    }
                    next.complete(stage.toCompletableFuture().join());
                }
                catch (Throwable t)
                {
                    next.completeExceptionally(t);
                }
            }
        });
        return next;
    }

    public <U> CompletableFuture<U> handle(
        final BiFunction<? super T, Throwable, ? extends U> fn)
    {
        final CompletableFuture<U> next = new CompletableFuture<U>();
        onCompletion(new Runnable()
        {
            public void run()
            {
                if (isCancelledInternal())
                {
                    next.cancel(false);
                    return;
                }
                try
                {
                    next.complete(fn.apply(resultInternal(), failureInternal()));
                }
                catch (Throwable t)
                {
                    next.completeExceptionally(t);
                }
            }
        });
        return next;
    }

    public CompletableFuture<T> whenComplete(
        final BiConsumer<? super T, ? super Throwable> action)
    {
        final CompletableFuture<T> next = new CompletableFuture<T>();
        onCompletion(new Runnable()
        {
            public void run()
            {
                if (isCancelledInternal())
                {
                    next.cancel(false);
                    return;
                }
                Throwable cause = failureInternal();
                try
                {
                    action.accept(resultInternal(), cause);
                }
                catch (Throwable t)
                {
                    next.completeExceptionally(t);
                    return;
                }
                if (cause != null)
                {
                    next.completeExceptionally(cause);
                }
                else
                {
                    next.complete(resultInternal());
                }
            }
        });
        return next;
    }

    public CompletableFuture<T> exceptionally(final Function<Throwable, ? extends T> fn)
    {
        final CompletableFuture<T> next = new CompletableFuture<T>();
        onCompletion(new Runnable()
        {
            public void run()
            {
                if (isCancelledInternal())
                {
                    next.cancel(false);
                    return;
                }
                Throwable cause = failureInternal();
                if (cause == null)
                {
                    next.complete(resultInternal());
                    return;
                }
                try
                {
                    next.complete(fn.apply(cause));
                }
                catch (Throwable t)
                {
                    next.completeExceptionally(t);
                }
            }
        });
        return next;
    }

    public static <U> CompletableFuture<U> supplyAsync(final Supplier<U> supplier)
    {
        CompletableFuture<U> future = new CompletableFuture<U>();
        try
        {
            future.complete(supplier.get());
        }
        catch (Throwable t)
        {
            future.completeExceptionally(t);
        }
        return future;
    }

    public static <U> CompletableFuture<U> supplyAsync(final Supplier<U> supplier,
                                                       Executor executor)
    {
        final CompletableFuture<U> future = new CompletableFuture<U>();
        executor.execute(new Runnable()
        {
            public void run()
            {
                try
                {
                    future.complete(supplier.get());
                }
                catch (Throwable t)
                {
                    future.completeExceptionally(t);
                }
            }
        });
        return future;
    }

    public static CompletableFuture<Void> runAsync(final Runnable runnable)
    {
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        try
        {
            runnable.run();
            future.complete(null);
        }
        catch (Throwable t)
        {
            future.completeExceptionally(t);
        }
        return future;
    }

    public static CompletableFuture<Void> runAsync(final Runnable runnable, Executor executor)
    {
        final CompletableFuture<Void> future = new CompletableFuture<Void>();
        executor.execute(new Runnable()
        {
            public void run()
            {
                try
                {
                    runnable.run();
                    future.complete(null);
                }
                catch (Throwable t)
                {
                    future.completeExceptionally(t);
                }
            }
        });
        return future;
    }

    public static CompletableFuture<Void> allOf(CompletableFuture<?>... futures)
    {
        CompletableFuture<Void> result = new CompletableFuture<Void>();
        try
        {
            for (int i = 0; i < futures.length; i++)
            {
                futures[i].join();
            }
            result.complete(null);
        }
        catch (Throwable t)
        {
            result.completeExceptionally(t);
        }
        return result;
    }

    public static CompletableFuture<Object> anyOf(CompletableFuture<?>... futures)
    {
        final CompletableFuture<Object> result = new CompletableFuture<Object>();
        for (int i = 0; i < futures.length; i++)
        {
            futures[i].whenComplete(new BiConsumer<Object, Throwable>()
            {
                public void accept(Object v, Throwable t)
                {
                    if (t != null)
                    {
                        result.completeExceptionally(t);
                    }
                    else
                    {
                        result.complete(v);
                    }
                }
            });
        }
        return result;
    }

    private void onCompletion(Runnable action)
    {
        boolean runNow;
        synchronized (lock)
        {
            runNow = completed;
            if (!runNow)
            {
                dependents.add(action);
            }
        }
        if (runNow)
        {
            action.run();
        }
    }

    private static void runAll(ArrayList<Runnable> actions)
    {
        for (int i = 0; i < actions.size(); i++)
        {
            actions.get(i).run();
        }
    }

    private boolean isCancelledInternal()
    {
        synchronized (lock)
        {
            return cancelled;
        }
    }

    private T resultInternal()
    {
        synchronized (lock)
        {
            return value;
        }
    }

    private Throwable failureInternal()
    {
        synchronized (lock)
        {
            return failure;
        }
    }
}
