public class M2LoadingChecks
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

    public static void main(String[] args) throws Exception
    {
        ClassLoader loader = M2LoadingChecks.class.getClassLoader();

        M2InitLog.reset();
        Class<?> target = Class.forName("M2InitTarget", false, loader);
        check(target != null && target.getName().equals("M2InitTarget"),
            "forName(false) resolves the class");
        check(M2InitLog.inits() == 0, "forName(false) does not run <clinit>");

        Class<?> loaded = loader.loadClass("M2InitTarget");
        check(loaded == target, "loadClass returns the cached class");
        check(M2InitLog.inits() == 0, "loadClass does not run <clinit>");

        int value = M2InitTarget.value;
        check(value == 7, "getstatic returns the initialized value");
        check(M2InitLog.inits() == 1, "first active use runs <clinit> exactly once");

        Class.forName("M2InitTarget", true, loader);
        check(M2InitLog.inits() == 1, "forName(true) does not repeat <clinit>");

        M2InitLog.reset();
        Class<?> target2 = Class.forName("M2InitTarget2", true, loader);
        check(target2 != null && M2InitLog.inits() == 1,
            "forName(true) initializes a fresh class once");

        Class<?> base = Class.forName("M2GoodChild", false, loader);
        check(base.getSuperclass() != null &&
            base.getSuperclass().getName().equals("M2Base"),
            "superclass is resolved during linking");
        check(M2InitLog.inits() == 1, "linking superclasses does not run their <clinit>");

        System.out.println("m2 loading checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            throw new RuntimeException("m2 loading checks failed");
        }
        System.out.println("m2 loading: ok");
    }
}
