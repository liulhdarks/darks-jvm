package java.lang;

public final class System
{
    public static java.io.PrintStream out;

    // Single system property table shared by getProperty / setProperty /
    // clearProperty / getProperties.  Defaults come from the native
    // initProperties (real process environment plus -D overrides).
    private static java.util.Properties props;

    static
    {
        out = new java.io.PrintStream();
        props = initProperties(new java.util.Properties());
    }

    public static native long currentTimeMillis();

    public static native long nanoTime();

    public static native void arraycopy(Object src, int srcPos, Object dest, int destPos, int length);

    public static native void exit(int code);

    public static native int identityHashCode(Object object);

    public static String getProperty(String key)
    {
        return props.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue)
    {
        String value = props.getProperty(key);
        return value == null ? defaultValue : value;
    }

    public static String setProperty(String key, String value)
    {
        if (key == null)
        {
            throw new NullPointerException("key");
        }
        return (String)props.setProperty(key, value);
    }

    public static String clearProperty(String key)
    {
        if (key == null)
        {
            throw new NullPointerException("key");
        }
        return (String)props.remove(key);
    }

    public static java.util.Properties getProperties()
    {
        return props;
    }

    public static String getenv(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        return getenv0(name);
    }

    public static native java.util.Properties initProperties(java.util.Properties target);

    public static native String mapLibraryName(String name);

    public static void loadLibrary(String name)
    {
        loadLibrary0(name, getProperty("java.library.path"));
    }

    public static native void load(String path);

    private static native void loadLibrary0(String name, String libraryPath);

    private static native String getenv0(String name);

    private System()
    {
    }

    public static void gc()
    {
    }
}
