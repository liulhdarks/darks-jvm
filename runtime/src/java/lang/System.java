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

    public static native void arraycopy(Object src, int srcPos, Object dest, int destPos, int length);

    public static native void exit(int code);

    public static native String getProperty(String key);

    public static native void loadLibrary(String name);

    public static native void load(String path);

    public static void gc()
    {
    }
}
