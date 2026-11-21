import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeoutException;

// StructuredTaskScope smoke test: shutdown policies, deadline, cancellation,
// leak detection and nested scopes on the coroutine scheduler.
public class StructuredScopeTest {
    static volatile boolean ignoreInterrupts;

    public static void main(String[] args) throws Exception {
        if (StructuredTaskScope.currentScope() != null) {
            throw new RuntimeException("main thread must not belong to a scope");
        }
        failureCancelsSiblings();
        successFirstWins();
        deadlineTimesOut();
        leakAndCloseRejectFork();
        leakDetectionThrows();
        nestedScopes();
        ownerConfinement();
        System.out.println("structured-scope ok cancel=true first=true timedout=true"
                + " leak=true nested=true owner=true suppressed=true");
    }

    private static int suppressedCount(ExecutionException failure) {
        return failure.getSuppressed().length;
    }

    private static void failureCancelsSiblings() throws Exception {
        final StructuredTaskScope.ShutdownOnFailure scope =
                new StructuredTaskScope.ShutdownOnFailure();
        StructuredTaskScope.Subtask first = scope.fork(new Callable() {
            public Object call() throws Exception {
                Thread.sleep(10000);
                return "first";
            }
        });
        StructuredTaskScope.Subtask second = scope.fork(new Callable() {
            public Object call() throws Exception {
                Thread.sleep(10000);
                return "second";
            }
        });
        scope.fork(new Callable() {
            public Object call() {
                throw new RuntimeException("boom");
            }
        });

        long started = System.currentTimeMillis();
        scope.join();
        long elapsed = System.currentTimeMillis() - started;
        if (elapsed > 3000) {
            throw new RuntimeException("failure did not cancel siblings, elapsed=" + elapsed);
        }
        if (first.state() != StructuredTaskScope.Subtask.State.FAILED
                || second.state() != StructuredTaskScope.Subtask.State.FAILED) {
            throw new RuntimeException("cancelled siblings not marked failed");
        }
        if (!(first.exception() instanceof InterruptedException)) {
            throw new RuntimeException("sibling exception=" + first.exception());
        }

        boolean threw = false;
        ExecutionException failure = null;
        try {
            scope.throwIfFailed();
        } catch (ExecutionException expected) {
            threw = true;
            failure = expected;
        }
        if (!threw) {
            throw new RuntimeException("throwIfFailed did not report the failure");
        }
        if (failure.getMessage() == null || failure.getMessage().indexOf("boom") < 0) {
            throw new RuntimeException("first failure lost: " + failure.getMessage());
        }
        if (suppressedCount(failure) < 2) {
            throw new RuntimeException("expected suppressed sibling failures, got "
                    + suppressedCount(failure));
        }
        scope.close();
        if (scope.liveSubtasks() != 0) {
            throw new RuntimeException("children survived close");
        }
    }

    private static void successFirstWins() throws Exception {
        final StructuredTaskScope.ShutdownOnSuccess scope =
                new StructuredTaskScope.ShutdownOnSuccess();
        StructuredTaskScope.Subtask slow = scope.fork(new Callable() {
            public Object call() throws Exception {
                Thread.sleep(400);
                return "slow";
            }
        });
        StructuredTaskScope.Subtask fast = scope.fork(new Callable() {
            public Object call() throws Exception {
                Thread.sleep(20);
                return "fast";
            }
        });
        StructuredTaskScope.Subtask medium = scope.fork(new Callable() {
            public Object call() throws Exception {
                Thread.sleep(200);
                return "medium";
            }
        });

        long started = System.currentTimeMillis();
        scope.join();
        long elapsed = System.currentTimeMillis() - started;
        Object result = scope.result();
        if (!"fast".equals(result)) {
            throw new RuntimeException("first success=" + result);
        }
        if (elapsed > 1000) {
            throw new RuntimeException("success did not cancel siblings, elapsed=" + elapsed);
        }
        if (fast.state() != StructuredTaskScope.Subtask.State.SUCCESS
                || !"fast".equals(fast.get())) {
            throw new RuntimeException("fast subtask state=" + fast.state());
        }
        if (slow.state() != StructuredTaskScope.Subtask.State.FAILED
                || medium.state() != StructuredTaskScope.Subtask.State.FAILED) {
            throw new RuntimeException("siblings not cancelled after success");
        }
        if (!fast.thread().isVirtual()) {
            throw new RuntimeException("subtask did not run on a virtual thread");
        }
        scope.close();
        if (scope.liveSubtasks() != 0) {
            throw new RuntimeException("children survived close after success");
        }
    }

    private static void deadlineTimesOut() throws Exception {
        final StructuredTaskScope.ShutdownOnFailure scope =
                new StructuredTaskScope.ShutdownOnFailure();
        scope.fork(new Callable() {
            public Object call() throws Exception {
                Thread.sleep(5000);
                return "late";
            }
        });
        boolean timedOut = false;
        long started = System.currentTimeMillis();
        try {
            scope.joinUntil(Instant.now().plusMillis(80));
        } catch (TimeoutException expected) {
            timedOut = true;
        }
        long elapsed = System.currentTimeMillis() - started;
        if (!timedOut) {
            throw new RuntimeException("joinUntil did not time out");
        }
        if (elapsed > 3000) {
            throw new RuntimeException("joinUntil took too long: " + elapsed);
        }
        scope.close();
        if (scope.liveSubtasks() != 0) {
            throw new RuntimeException("timed-out child survived close");
        }
    }

    private static void leakAndCloseRejectFork() throws Exception {
        final StructuredTaskScope.ShutdownOnFailure scope =
                new StructuredTaskScope.ShutdownOnFailure();
        scope.fork(new Callable() {
            public Object call() throws Exception {
                Thread.sleep(5000);
                return "sleepy";
            }
        });
        scope.shutdown();
        // With more than one carrier the interrupted child may already have
        // finished; it must never still be counted twice.
        if (scope.liveSubtasks() > 1) {
            throw new RuntimeException("unexpected live subtasks after shutdown: "
                    + scope.liveSubtasks());
        }
        scope.join();
        if (scope.liveSubtasks() != 0) {
            throw new RuntimeException("live subtasks after join: " + scope.liveSubtasks());
        }
        scope.close();
        if (scope.liveSubtasks() != 0) {
            throw new RuntimeException("live subtasks after close: " + scope.liveSubtasks());
        }
        boolean rejected = false;
        try {
            scope.fork(new Callable() {
                public Object call() {
                    return "late";
                }
            });
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        if (!rejected) {
            throw new RuntimeException("fork after close was accepted");
        }
    }

    private static void leakDetectionThrows() throws Exception {
        final StructuredTaskScope.ShutdownOnFailure scope =
                new StructuredTaskScope.ShutdownOnFailure();
        scope.fork(new Callable() {
            public Object call() throws Exception {
                while (!ignoreInterrupts) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        // Deliberately unresponsive to cancellation.
                    }
                }
                return "done";
            }
        });
        System.setProperty("darks.scope.closeTimeout", "50");
        boolean detected = false;
        try {
            scope.close();
        } catch (IllegalStateException expected) {
            detected = expected.getMessage() != null
                    && expected.getMessage().indexOf("live subtasks") >= 0;
        } finally {
            System.setProperty("darks.scope.closeTimeout", "30000");
            ignoreInterrupts = true;
        }
        if (!detected) {
            throw new RuntimeException("close did not report a leaked subtask");
        }
        long deadline = System.currentTimeMillis() + 3000;
        while (scope.liveSubtasks() != 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        if (scope.liveSubtasks() != 0) {
            throw new RuntimeException("leaked subtask never terminated");
        }
    }

    private static void nestedScopes() throws Exception {
        final StructuredTaskScope.ShutdownOnFailure outer =
                new StructuredTaskScope.ShutdownOnFailure();
        StructuredTaskScope.Subtask nested = outer.fork(new Callable() {
            public Object call() throws Exception {
                if (StructuredTaskScope.currentScope() != outer) {
                    throw new RuntimeException("parent scope chain is broken");
                }
                final StructuredTaskScope.ShutdownOnSuccess inner =
                        new StructuredTaskScope.ShutdownOnSuccess();
                try {
                    inner.fork(new Callable() {
                        public Object call() throws Exception {
                            Thread.sleep(10);
                            return Integer.valueOf(41);
                        }
                    });
                    inner.fork(new Callable() {
                        public Object call() throws Exception {
                            Thread.sleep(300);
                            return Integer.valueOf(42);
                        }
                    });
                    inner.join();
                    return "inner=" + inner.result();
                } finally {
                    inner.close();
                }
            }
        });
        outer.join();
        outer.throwIfFailed();
        Object value = nested.get();
        if (!"inner=41".equals(value)) {
            throw new RuntimeException("nested result=" + value);
        }
        if (nested.thread().isVirtual() == false) {
            throw new RuntimeException("nested task not virtual");
        }
        outer.close();
        if (outer.liveSubtasks() != 0) {
            throw new RuntimeException("outer scope leaked children");
        }
    }

    private static void ownerConfinement() throws Exception {
        final StructuredTaskScope.ShutdownOnFailure scope =
                new StructuredTaskScope.ShutdownOnFailure();
        StructuredTaskScope.Subtask attempt = scope.fork(new Callable() {
            public Object call() {
                try {
                    scope.fork(new Callable() {
                        public Object call() {
                            return "nested-fork";
                        }
                    });
                } catch (IllegalStateException expected) {
                    return "rejected";
                }
                return "accepted";
            }
        });
        scope.join();
        scope.throwIfFailed();
        Object value = attempt.get();
        if (!"rejected".equals(value)) {
            throw new RuntimeException("fork from non-owner was " + value);
        }
        scope.close();
    }
}
