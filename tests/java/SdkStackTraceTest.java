public class SdkStackTraceTest
{
    private static int checks;
    private static int failures;

    private static void check(String name, boolean condition)
    {
        checks++;
        if (!condition)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    static Throwable failure()
    {
        return new IllegalStateException("boom");
    }

    static StackTraceElement find(StackTraceElement[] frames, String method)
    {
        for (int i = 0; i < frames.length; i++)
        {
            if (method.equals(frames[i].getMethodName())
                && "SdkStackTraceTest".equals(frames[i].getClassName()))
            {
                return frames[i];
            }
        }
        return null;
    }

    public static void main(String[] args)
    {
        Throwable error = failure();
        StackTraceElement[] frames = error.getStackTrace();
        check("stack-nonempty", frames.length > 0);

        StackTraceElement top = frames[0];
        check("stack-top-class", "SdkStackTraceTest".equals(top.getClassName()));
        check("stack-top-method", "failure".equals(top.getMethodName()));
        check("stack-file", "SdkStackTraceTest.java".equals(top.getFileName()));
        check("stack-line", top.getLineNumber() > 0);

        StackTraceElement caller = find(frames, "main");
        check("stack-main", caller != null && caller.getLineNumber() > 0);
        check("stack-rendered",
            top.toString().indexOf("SdkStackTraceTest.failure(SdkStackTraceTest.java:") == 0);

        StackTraceElement fake = new StackTraceElement("custom.Fake", "run", "Fake.java", 7);
        error.setStackTrace(new StackTraceElement[] { fake });
        StackTraceElement[] replaced = error.getStackTrace();
        check("stack-set", replaced.length == 1 && fake.equals(replaced[0]));

        Throwable chained = new RuntimeException("outer",
            new IllegalStateException("inner"));
        chained.fillInStackTrace();
        check("stack-cause", chained.getCause() instanceof IllegalStateException);

        System.out.println("stack checks=" + checks + " failures=" + failures);
        failure().printStackTrace();
        chained.printStackTrace();
        if (failures == 0)
        {
            System.out.println("sdk stack trace: ok");
        }
        if (failures != 0)
        {
            System.exit(1);
        }
    }
}
