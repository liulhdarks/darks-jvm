package java.nio.file;

// Minimal FileSystem: only the path factory Spring uses.  There is no
// provider, watch service or attribute view support here.
public abstract class FileSystem
{
    protected FileSystem()
    {
    }

    public abstract Path getPath(String first, String... more);

    public String getSeparator()
    {
        return java.io.File.separator;
    }

    public boolean isOpen()
    {
        return true;
    }

    public void close() throws java.io.IOException
    {
    }
}
