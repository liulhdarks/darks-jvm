package java.lang;

public final class System
{
    public static java.io.PrintStream out;

    static
    {
        out = new java.io.PrintStream();
    }

    public static native long currentTimeMillis();

    public static native long nanoTime();
}
