import java.lang.reflect.Method;

interface M2A
{
    default String who()
    {
        return "A";
    }

    default String both()
    {
        return "A+" + who();
    }
}

interface M2B extends M2A
{
    default String who()
    {
        return "B";
    }
}

interface M2C
{
    default String who()
    {
        return "C";
    }
}

class M2ImplB implements M2B
{
}

class M2ImplAB implements M2A, M2B
{
}

class M2ImplAC implements M2A, M2C
{
    public String who()
    {
        return "AC";
    }
}

class M2Super
{
    public String who()
    {
        return "S";
    }
}

class M2ImplSuper extends M2Super implements M2A
{
}

public class M2DefaultMethodChecks
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
        check("B".equals(new M2ImplB().who()),
            "subinterface default shadows its superinterface");
        check("A+B".equals(new M2ImplB().both()),
            "default method calls dispatch virtually");
        check("B".equals(new M2ImplAB().who()),
            "most specific interface default wins for A, B");
        check("AC".equals(new M2ImplAC().who()),
            "class override wins over unrelated interface defaults");
        check("S".equals(new M2ImplSuper().who()),
            "superclass method wins over interface default");

        Method aWho = M2A.class.getMethod("who", new Class[0]);
        check("B".equals(aWho.invoke(new M2ImplB(), new Object[0])),
            "reflective invocation dispatches to the derived default");

        System.out.println("m2 default-method checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            throw new RuntimeException("m2 default-method checks failed");
        }
        System.out.println("m2 default methods: ok");
    }
}
