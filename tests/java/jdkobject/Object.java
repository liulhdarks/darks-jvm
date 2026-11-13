package java.lang;

// JDK-style java.lang.Object used by the compatibility test: the reference
// implementation registers its natives from <clinit>.
public class Object
{
    private static native void registerNatives();

    static
    {
        registerNatives();
    }

    public Object()
    {
    }

    public boolean equals(Object other)
    {
        return this == other;
    }

    public native int hashCode();

    public final native Class getClass();

    public String toString()
    {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    public final native void wait() throws InterruptedException;

    public final native void wait(long millis) throws InterruptedException;

    public final native void wait(long millis, int nanos) throws InterruptedException;

    public final native void notify();

    public final native void notifyAll();
}
