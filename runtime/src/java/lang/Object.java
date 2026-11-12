package java.lang;

public class Object
{
    public Object()
    {
    }

    public boolean equals(Object other)
    {
        return this == other;
    }

    public native int hashCode();

    public String toString()
    {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    public final native Class getClass();

    // Monitor operations.  These are native entry points intercepted by the
    // coroutine scheduler; the declarations keep the standard Java API.
    public final native void wait() throws InterruptedException;

    public final native void wait(long millis) throws InterruptedException;

    public final native void wait(long millis, int nanos) throws InterruptedException;

    public final native void notify();

    public final native void notifyAll();
}
