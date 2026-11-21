package java.nio.file;

import java.util.HashSet;
import java.util.Set;

final class DefaultFileSystem extends FileSystem
{
    static final DefaultFileSystem INSTANCE = new DefaultFileSystem();

    static DefaultFileSystem instance()
    {
        return INSTANCE;
    }

    private DefaultFileSystem()
    {
    }

    public Set<String> supportedFileAttributeViews()
    {
        // Only the basic view is implemented; posix/dos owner views are not.
        Set<String> views = new HashSet<String>();
        views.add("basic");
        return views;
    }

    public Path getPath(String first, String... more)
    {
        StringBuilder builder = new StringBuilder(first == null ? "" : first);
        for (int i = 0; more != null && i < more.length; i++)
        {
            if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '/')
            {
                builder.append('/');
            }
            builder.append(more[i]);
        }
        return new UnixPath(this, builder.toString());
    }
}
