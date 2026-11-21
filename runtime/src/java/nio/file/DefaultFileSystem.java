package java.nio.file;

final class DefaultFileSystem extends FileSystem
{
    private static final DefaultFileSystem INSTANCE = new DefaultFileSystem();

    private DefaultFileSystem()
    {
    }

    static DefaultFileSystem instance()
    {
        return INSTANCE;
    }

    public Path getPath(String first, String... more)
    {
        return Paths.get(first, more);
    }
}
