public class M3InheritableChecks
{
    private static final InheritableThreadLocal<String> PLAIN =
        new InheritableThreadLocal<String>();

    private static final InheritableThreadLocal<String> TAGGED =
        new InheritableThreadLocal<String>()
        {
            protected String childValue(String parentValue)
            {
                return parentValue + "-child";
            }
        };

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
        PLAIN.set("parent");
        TAGGED.set("tag");

        final String[] seen = new String[2];
        Thread child = new Thread(new Runnable()
        {
            public void run()
            {
                seen[0] = PLAIN.get();
                seen[1] = TAGGED.get();
                TAGGED.set("changed");
            }
        });
        child.start();
        child.join();

        check("parent".equals(seen[0]), "child inherits the parent value");
        check("tag-child".equals(seen[1]), "childValue transforms the inherited value");
        check("tag".equals(TAGGED.get()), "child writes do not leak to the parent");

        TAGGED.set("late");
        final String[] later = new String[1];
        Thread second = new Thread(new Runnable()
        {
            public void run()
            {
                later[0] = TAGGED.get();
            }
        });
        second.start();
        second.join();
        check("late-child".equals(later[0]), "later thread sees the updated parent value");

        System.out.println("m3 inheritable checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            throw new RuntimeException("m3 inheritable checks failed");
        }
        System.out.println("m3 inheritable: ok");
    }
}
