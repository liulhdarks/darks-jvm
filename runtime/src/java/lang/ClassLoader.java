package java.lang;

public class ClassLoader
{
    private static ClassLoader system;

    public static ClassLoader getSystemClassLoader()
    {
        if (system == null)
        {
            system = new ClassLoader();
        }
        return system;
    }

    public java.io.InputStream getResourceAsStream(String name)
    {
        return getSystemResourceAsStream(name);
    }

    public static java.io.InputStream getSystemResourceAsStream(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        String absolute = name.startsWith("/") ? name.substring(1) : name;
        byte[] data = getResourceBytes0(absolute);
        return data == null ? null : new java.io.ByteArrayInputStream(data);
    }

    private static native byte[] getResourceBytes0(String name);
}
