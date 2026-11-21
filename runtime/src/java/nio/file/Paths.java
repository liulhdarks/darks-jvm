package java.nio.file;

public final class Paths
{
    private Paths()
    {
    }

    public static Path get(String first, String... more)
    {
        return DefaultFileSystem.INSTANCE.getPath(first, more);
    }
}
