public class M3SemanticsChecks
{
    private static int checks;
    private static int failures;

    private static void check(boolean condition, String message)
    {
        checks++;
        if (condition)
        {
            System.out.println("ok " + message);
        }
        else
        {
            failures++;
            System.out.println("FAIL " + message);
        }
    }

    private static Throwable capture(String name, ClassLoader loader)
    {
        try
        {
            Class.forName(name, true, loader);
        }
        catch (Throwable t)
        {
            return t;
        }
        return null;
    }

    public static void main(String[] args) throws Exception
    {
        ClassLoader loader = M3SemanticsChecks.class.getClassLoader();

        // ---- non-Error throwable from <clinit>: ExceptionInInitializerError ----
        Class<?> fail = Class.forName("M3SemFail", false, loader);
        check(fail != null && fail.getName().equals("M3SemFail"),
            "forName(false) resolves a class whose <clinit> will fail");
        check(M3SemLog.runs == 0, "forName(false) does not run <clinit>");

        Throwable first = capture("M3SemFail", loader);
        check(first != null && first.getClass().getName().equals("java.lang.ExceptionInInitializerError"),
            "first initialization failure throws ExceptionInInitializerError");
        check(first != null && first.getMessage() == null,
            "ExceptionInInitializerError carries no message");
        check(first != null && first.getCause() != null &&
            first.getCause().getClass().getName().equals("java.lang.IllegalStateException"),
            "ExceptionInInitializerError cause is the throwable from <clinit>");
        check(first != null && first.getCause() != null &&
            "boom".equals(first.getCause().getMessage()),
            "ExceptionInInitializerError cause keeps its message");
        check(M3SemLog.runs == 1, "failed <clinit> ran exactly once");

        Throwable second = capture("M3SemFail", loader);
        check(second != null && second.getClass().getName().equals("java.lang.NoClassDefFoundError"),
            "second active use throws NoClassDefFoundError");
        check(second != null &&
            "Could not initialize class M3SemFail".equals(second.getMessage()),
            "NoClassDefFoundError names the erroneous class");
        check(M3SemLog.runs == 1, "erroneous class never runs <clinit> again");

        Class<?> again = Class.forName("M3SemFail", false, loader);
        check(again == fail, "forName(false) still returns the erroneous Class");

        boolean staticUseFailed = false;
        try
        {
            int ignored = M3SemFail.value;
            check(ignored == 42, "getstatic on an erroneous class must throw");
        }
        catch (NoClassDefFoundError e)
        {
            staticUseFailed = "Could not initialize class M3SemFail".equals(e.getMessage());
        }
        check(staticUseFailed, "getstatic on an erroneous class throws NoClassDefFoundError");

        boolean invokeFailed = false;
        try
        {
            M3SemFail.touch();
        }
        catch (NoClassDefFoundError e)
        {
            invokeFailed = "Could not initialize class M3SemFail".equals(e.getMessage());
        }
        check(invokeFailed, "invokestatic on an erroneous class throws NoClassDefFoundError");

        boolean reflectFailed = false;
        try
        {
            fail.getDeclaredMethod("touch", new Class[0]).invoke(null, new Object[0]);
        }
        catch (NoClassDefFoundError e)
        {
            reflectFailed = "Could not initialize class M3SemFail".equals(e.getMessage());
        }
        catch (Throwable ignored)
        {
        }
        check(reflectFailed, "reflective static invocation on an erroneous class throws NoClassDefFoundError");
        check(M3SemLog.runs == 1, "active uses never re-run a failed <clinit>");

        // ---- Error thrown by <clinit> propagates unchanged ----
        Throwable error = capture("M3SemError", loader);
        check(error != null && error.getClass().getName().equals("java.lang.AssertionError"),
            "Error from <clinit> propagates unchanged");
        check(error != null && "bad-init".equals(error.getMessage()),
            "propagated Error keeps its message");
        check(error == null || error.getCause() == null,
            "propagated Error is not wrapped in ExceptionInInitializerError");
        Throwable errorSecond = capture("M3SemError", loader);
        check(errorSecond != null &&
            errorSecond.getClass().getName().equals("java.lang.NoClassDefFoundError") &&
            "Could not initialize class M3SemError".equals(errorSecond.getMessage()),
            "class whose <clinit> threw an Error is erroneous afterwards");

        // ---- superclass failure propagates; subclass <clinit> never runs ----
        Throwable child = capture("M3SemChild", loader);
        check(child != null &&
            child.getClass().getName().equals("java.lang.ExceptionInInitializerError"),
            "subclass of a failing superclass throws ExceptionInInitializerError");
        check(child != null && child.getCause() != null &&
            child.getCause().getClass().getName().equals("java.lang.RuntimeException") &&
            "base-boom".equals(child.getCause().getMessage()),
            "subclass failure keeps the original superclass cause");
        check(M3SemLog.childRuns == 0, "subclass <clinit> does not run when its superclass fails");

        boolean childFieldFailed = false;
        try
        {
            int ignored = M3SemChild.childValue;
            check(ignored == 7, "subclass getstatic must throw");
        }
        catch (NoClassDefFoundError e)
        {
            childFieldFailed = "Could not initialize class M3SemChild".equals(e.getMessage());
        }
        check(childFieldFailed, "subclass getstatic throws NoClassDefFoundError");

        boolean childNewFailed = false;
        try
        {
            Object ignored = new M3SemChild();
            check(ignored != null, "new on an erroneous subclass must throw");
        }
        catch (NoClassDefFoundError e)
        {
            childNewFailed = "Could not initialize class M3SemChild".equals(e.getMessage());
        }
        check(childNewFailed, "new on an erroneous subclass throws NoClassDefFoundError");

        // ---- failure triggered by an active use other than forName ----
        Throwable lazy = null;
        try
        {
            int ignored = M3SemLazy.value;
            check(ignored == 5, "getstatic must trigger the failing <clinit>");
        }
        catch (Throwable t)
        {
            lazy = t;
        }
        check(lazy != null &&
            lazy.getClass().getName().equals("java.lang.ExceptionInInitializerError") &&
            lazy.getCause() != null &&
            lazy.getCause().getClass().getName().equals("java.lang.UnsupportedOperationException"),
            "getstatic triggers <clinit> and reports its failure");
        check(M3SemLog.lazyRuns == 1, "lazy <clinit> ran exactly once");
        boolean lazySecond = false;
        try
        {
            int ignored = M3SemLazy.value;
            check(ignored == 5, "repeated getstatic must throw");
        }
        catch (NoClassDefFoundError e)
        {
            lazySecond = "Could not initialize class M3SemLazy".equals(e.getMessage());
        }
        check(lazySecond, "repeated getstatic reports NoClassDefFoundError");
        check(M3SemLog.lazyRuns == 1, "lazy <clinit> is not retried");

        // ---- successful initialisation still runs once ----
        int ok = M3SemOk.value;
        check(ok == 9 && M3SemLog.okRuns == 1, "successful <clinit> runs once and field is visible");
        Class.forName("M3SemOk", true, loader);
        int ok2 = M3SemOk.value;
        check(ok2 == 9 && M3SemLog.okRuns == 1, "forName(true) on an initialized class is a no-op");

        System.out.println("m3 init checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            throw new RuntimeException("m3 init checks failed");
        }
        System.out.println("m3 init: ok");
    }
}
