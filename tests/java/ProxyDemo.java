import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// java.lang.reflect.Proxy and InvocationHandler: interface dispatch, boxing
// of primitive arguments/returns, Object method interception, exception
// propagation through the interpreter and Method.invoke on a proxy.
public class ProxyDemo
{
    public interface Greeter
    {
        String greet(String name);

        int add(int a, int b);

        long widen(long value);

        void touch();
    }

    public interface Named
    {
        String name();
    }

    public interface Boom
    {
        void boom();
    }

    public static class Handler implements InvocationHandler
    {
        int calls;
        Class lastDeclaringClass;
        String lastMethodName;
        Class lastReturnType;
        Class lastArgumentType;

        public Object invoke(Object proxy, Method method, Object[] args)
        {
            calls++;
            lastDeclaringClass = method.getDeclaringClass();
            lastMethodName = method.getName();
            lastReturnType = method.getReturnType();
            if (args != null && args.length > 0)
            {
                lastArgumentType = args[0].getClass();
            }
            String name = method.getName();
            if ("greet".equals(name))
            {
                return "hi " + args[0];
            }
            if ("add".equals(name))
            {
                return Integer.valueOf(((Integer)args[0]).intValue() +
                    ((Integer)args[1]).intValue());
            }
            if ("widen".equals(name))
            {
                return Long.valueOf(((Long)args[0]).longValue() + 1L);
            }
            if ("name".equals(name))
            {
                return "named";
            }
            if ("boom".equals(name))
            {
                throw new IllegalStateException("handler-boom");
            }
            if ("toString".equals(name))
            {
                return "proxy!";
            }
            if ("hashCode".equals(name))
            {
                return Integer.valueOf(42);
            }
            if ("equals".equals(name))
            {
                return Boolean.valueOf(proxy == args[0]);
            }
            return null;
        }
    }

    private static int checks;
    private static int failures;

    private static void check(String name, boolean ok)
    {
        checks++;
        if (!ok)
        {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    public static void main(String[] args) throws Exception
    {
        Handler handler = new Handler();
        Class<?>[] interfaces = new Class<?>[] { Greeter.class, Named.class };
        Object proxy = Proxy.newProxyInstance(
            ProxyDemo.class.getClassLoader(), interfaces, handler);

        check("proxy-created", proxy != null);
        check("proxy-interface", proxy instanceof Greeter);
        check("proxy-second-interface", proxy instanceof Named);
        check("proxy-class-flag", Proxy.isProxyClass(proxy.getClass()));
        check("proxy-class-name",
            proxy.getClass().getName().startsWith("darks.Proxy$"));
        check("proxy-handler", Proxy.getInvocationHandler(proxy) == handler);
        check("proxy-not-string", !Proxy.isProxyClass(String.class));

        Greeter greeter = (Greeter)proxy;
        check("proxy-greet", "hi bob".equals(greeter.greet("bob")));
        check("proxy-method-name", "greet".equals(handler.lastMethodName));
        check("proxy-declaring-class",
            handler.lastDeclaringClass == Greeter.class);
        check("proxy-return-type",
            handler.lastReturnType == String.class);
        check("proxy-argument-type", handler.lastArgumentType == String.class);

        check("proxy-int-args", greeter.add(2, 3) == 5);
        check("proxy-long-return", greeter.widen(41L) == 42L);
        check("proxy-long-argument", handler.lastArgumentType == Long.class);
        greeter.touch();
        check("proxy-void", "touch".equals(handler.lastMethodName));
        check("proxy-second-method", "named".equals(((Named)proxy).name()));
        check("proxy-call-count", handler.calls == 5);

        check("proxy-toString", "proxy!".equals(proxy.toString()));
        check("proxy-hashCode", proxy.hashCode() == 42);
        check("proxy-equals", proxy.equals(proxy));

        Method greet = Greeter.class.getMethod("greet", String.class);
        check("method-invoke-proxy",
            "hi reflect".equals(greet.invoke(proxy, new Object[] { "reflect" })));
        Method add = Greeter.class.getMethod("add", int.class, int.class);
        check("method-invoke-proxy-int",
            ((Integer)add.invoke(proxy, new Object[] { Integer.valueOf(4),
                Integer.valueOf(5) })).intValue() == 9);

        boolean boom = false;
        try
        {
            ((Boom)Proxy.newProxyInstance(ProxyDemo.class.getClassLoader(),
                new Class<?>[] { Boom.class }, handler)).boom();
        }
        catch (IllegalStateException expected)
        {
            boom = "handler-boom".equals(expected.getMessage());
        }
        check("proxy-exception", boom);

        boolean npe = false;
        try
        {
            Proxy.newProxyInstance(null, new Class<?>[] { Greeter.class }, null);
        }
        catch (NullPointerException expected)
        {
            npe = true;
        }
        check("proxy-null-handler", npe);

        boolean notInterface = false;
        try
        {
            Proxy.newProxyInstance(null, new Class<?>[] { String.class }, handler);
        }
        catch (IllegalArgumentException expected)
        {
            notInterface = true;
        }
        check("proxy-not-interface", notInterface);

        boolean notProxy = false;
        try
        {
            Proxy.getInvocationHandler("plain");
        }
        catch (IllegalArgumentException expected)
        {
            notProxy = true;
        }
        check("proxy-handler-non-proxy", notProxy);

        System.out.println("proxy checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
        System.out.println("dynamic proxy: ok");
    }
}
