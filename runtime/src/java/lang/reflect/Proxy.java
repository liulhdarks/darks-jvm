package java.lang.reflect;

/**
 * Minimal JDK dynamic proxy support.
 *
 * Proxies are instances of a VM-synthesised class that implements the
 * requested interfaces.  Network of methods is not generated: the interpreter
 * routes every interface call on a proxy object to the InvocationHandler
 * stored with the object.
 */
public class Proxy
{
    protected InvocationHandler h;

    protected Proxy(InvocationHandler h)
    {
        this.h = h;
    }

    public static Object newProxyInstance(ClassLoader loader,
        Class<?>[] interfaces, InvocationHandler h)
    {
        if (h == null)
        {
            throw new NullPointerException("handler");
        }
        if (interfaces == null)
        {
            throw new NullPointerException("interfaces");
        }
        return newProxyInstance0(loader, interfaces, h);
    }

    private static native Object newProxyInstance0(ClassLoader loader,
        Class<?>[] interfaces, InvocationHandler h);

    public static boolean isProxyClass(Class<?> cl)
    {
        if (cl == null)
        {
            throw new NullPointerException("class");
        }
        return isProxyClass0(cl);
    }

    private static native boolean isProxyClass0(Class<?> cl);

    public static InvocationHandler getInvocationHandler(Object proxy)
    {
        if (proxy == null)
        {
            throw new NullPointerException("proxy");
        }
        return (InvocationHandler)getInvocationHandler0(proxy);
    }

    private static native Object getInvocationHandler0(Object proxy);
}
