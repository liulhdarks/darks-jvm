package java.nio.file;

import java.util.Set;

public abstract class FileSystem
{
    public abstract Set<String> supportedFileAttributeViews();

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
