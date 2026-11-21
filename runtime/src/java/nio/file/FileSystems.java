package java.nio.file;

public final class FileSystems
{
    private FileSystems()
    {
    }

    public static FileSystem getDefault()
    {
        return DefaultFileSystem.instance();
    }
}
