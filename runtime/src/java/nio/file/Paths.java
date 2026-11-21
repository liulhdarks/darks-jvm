package java.nio.file;

public final class Paths
{
    private Paths()
    {
    }

    public static Path get(String first, String... more)
    {
        StringBuilder builder = new StringBuilder(first);
        for (int i = 0; i < more.length; i++)
        {
            if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '/')
            {
                builder.append('/');
            }
            builder.append(more[i]);
        }
        return new SimplePath(builder.toString());
    }

    public static Path get(java.net.URI uri)
    {
        String scheme = uri.getScheme();
        if (scheme != null && !scheme.equalsIgnoreCase("file"))
        {
            throw new IllegalArgumentException("URI scheme is not \"file\"");
        }
        if (uri.getAuthority() != null)
        {
            throw new IllegalArgumentException("URI has an authority component");
        }
        String path = uri.getPath();
        if (path == null)
        {
            throw new IllegalArgumentException("URI path is not defined");
        }
        return new SimplePath(path);
    }
}
