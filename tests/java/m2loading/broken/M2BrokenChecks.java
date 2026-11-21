public class M2BrokenChecks
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

    private static void checkLinkFailure(ClassLoader loader, String name,
        String missing, boolean useLoadClass)
    {
        try
        {
            if (useLoadClass)
            {
                loader.loadClass(name);
            }
            else
            {
                Class.forName(name, false, loader);
            }
            check(false, name + ": expected NoClassDefFoundError");
        }
        catch (NoClassDefFoundError error)
        {
            Throwable cause = error.getCause();
            check(cause instanceof ClassNotFoundException,
                name + ": cause is ClassNotFoundException");
            check(cause != null && missing.equals(cause.getMessage()),
                name + ": cause names " + missing);
        }
        catch (Throwable other)
        {
            System.out.println("FAIL " + name + ": unexpected " + other);
            failures++;
            checks++;
        }
    }

    public static void main(String[] args) throws Exception
    {
        ClassLoader loader = M2BrokenChecks.class.getClassLoader();
        M2InitLog.reset();

        checkLinkFailure(loader, "M2BadSuper", "M2MissingBase", false);
        checkLinkFailure(loader, "M2BadSuper", "M2MissingBase", true);
        checkLinkFailure(loader, "M2BadChild", "M2MissingBase", false);
        checkLinkFailure(loader, "M2BadChild", "M2MissingBase", true);
        checkLinkFailure(loader, "M2BadImpl", "M2MissingIface", false);
        checkLinkFailure(loader, "M2BadImpl", "M2MissingIface", true);
        check(M2InitLog.inits() == 0,
            "failed linking does not run any <clinit>");

        try
        {
            Class.forName("M2MissingBase", false, loader);
            check(false, "missing class: expected ClassNotFoundException");
        }
        catch (ClassNotFoundException expected)
        {
            check(expected.getCause() == null,
                "a directly missing class throws ClassNotFoundException");
        }

        Class<?> good = Class.forName("M2GoodChild", false, loader);
        check(good != null && good.getSuperclass() != null &&
            good.getSuperclass().getName().equals("M2Base"),
            "a fully resolvable class still links");

        try
        {
            Class.forName("M2BadSuper", true, loader);
            check(false, "forName(true): expected NoClassDefFoundError");
        }
        catch (NoClassDefFoundError expected)
        {
            check(expected.getCause() instanceof ClassNotFoundException,
                "forName(true) fails at linking before initialization");
        }
        check(M2InitLog.inits() == 0,
            "forName(true) did not initialize the broken class");

        System.out.println("m2 broken-load checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            throw new RuntimeException("m2 broken-load checks failed");
        }
        System.out.println("m2 broken loads: ok");
    }
}
