package java.nio.file;

import java.io.File;
import java.net.URI;

final class UnixPath implements Path
{
    private final FileSystem fileSystem;
    private final String path;

    UnixPath(FileSystem fileSystem, String path)
    {
        if (path == null)
        {
            throw new NullPointerException("path");
        }
        this.fileSystem = fileSystem;
        this.path = path;
    }

    public FileSystem getFileSystem()
    {
        return fileSystem;
    }

    public boolean isAbsolute()
    {
        return path.length() > 0 && path.charAt(0) == '/';
    }

    public Path getFileName()
    {
        if (path.length() == 0)
        {
            return null;
        }
        int end = path.length();
        if (end > 1 && path.charAt(end - 1) == '/')
        {
            end--;
        }
        int slash = path.lastIndexOf('/', end - 1);
        String name = path.substring(slash + 1, end);
        if (name.length() == 0)
        {
            return null;
        }
        return new UnixPath(fileSystem, name);
    }

    public Path getParent()
    {
        int end = path.length();
        while (end > 0 && path.charAt(end - 1) == '/')
        {
            end--;
        }
        int slash = path.lastIndexOf('/', end - 1);
        if (slash < 0)
        {
            return null;
        }
        if (slash == 0)
        {
            return new UnixPath(fileSystem, "/");
        }
        return new UnixPath(fileSystem, path.substring(0, slash));
    }

    public int getNameCount()
    {
        int count = 0;
        int index = 0;
        while (index < path.length())
        {
            while (index < path.length() && path.charAt(index) == '/')
            {
                index++;
            }
            if (index < path.length())
            {
                count++;
                while (index < path.length() && path.charAt(index) != '/')
                {
                    index++;
                }
            }
        }
        return count;
    }

    public Path getName(int index)
    {
        int count = 0;
        int position = 0;
        while (position < path.length())
        {
            while (position < path.length() && path.charAt(position) == '/')
            {
                position++;
            }
            if (position >= path.length())
            {
                break;
            }
            int start = position;
            while (position < path.length() && path.charAt(position) != '/')
            {
                position++;
            }
            if (count == index)
            {
                return new UnixPath(fileSystem, path.substring(start, position));
            }
            count++;
        }
        throw new IllegalArgumentException("name index out of range: " + index);
    }

    public Path resolve(Path other)
    {
        return resolve(other.toString());
    }

    public Path resolve(String other)
    {
        if (other == null)
        {
            throw new NullPointerException("other");
        }
        if (other.length() == 0)
        {
            return this;
        }
        if (other.charAt(0) == '/')
        {
            return new UnixPath(fileSystem, other);
        }
        if (path.length() == 0)
        {
            return new UnixPath(fileSystem, other);
        }
        String prefix = path;
        while (prefix.length() > 1 && prefix.charAt(prefix.length() - 1) == '/')
        {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return new UnixPath(fileSystem, prefix + "/" + other);
    }

    public Path toAbsolutePath()
    {
        if (isAbsolute())
        {
            return this;
        }
        return new UnixPath(fileSystem, new File(path).getAbsolutePath());
    }

    public Path normalize()
    {
        boolean absolute = isAbsolute();
        java.util.ArrayList<String> parts = new java.util.ArrayList<String>();
        int position = 0;
        while (position < path.length())
        {
            while (position < path.length() && path.charAt(position) == '/')
            {
                position++;
            }
            int start = position;
            while (position < path.length() && path.charAt(position) != '/')
            {
                position++;
            }
            if (start == position)
            {
                continue;
            }
            String part = path.substring(start, position);
            if (part.equals("."))
            {
                continue;
            }
            if (part.equals(".."))
            {
                if (parts.size() > 0 && !parts.get(parts.size() - 1).equals(".."))
                {
                    parts.remove(parts.size() - 1);
                }
                else if (!absolute)
                {
                    parts.add(part);
                }
                continue;
            }
            parts.add(part);
        }
        StringBuilder builder = new StringBuilder();
        if (absolute)
        {
            builder.append('/');
        }
        for (int i = 0; i < parts.size(); i++)
        {
            if (i > 0)
            {
                builder.append('/');
            }
            builder.append(parts.get(i));
        }
        if (builder.length() == 0)
        {
            return new UnixPath(fileSystem, ".");
        }
        return new UnixPath(fileSystem, builder.toString());
    }

    public boolean startsWith(String other)
    {
        if (other == null)
        {
            throw new NullPointerException("other");
        }
        return path.equals(other) || path.startsWith(other.endsWith("/") ? other : other + "/");
    }

    public boolean endsWith(String other)
    {
        if (other == null)
        {
            throw new NullPointerException("other");
        }
        return path.equals(other) || path.endsWith("/" + other);
    }

    public File toFile()
    {
        return new File(path);
    }

    public URI toUri()
    {
        String absolute = new File(path).getAbsolutePath();
        return URI.create("file:" + absolute);
    }

    public int compareTo(Path other)
    {
        return path.compareTo(other.toString());
    }

    public boolean equals(Object other)
    {
        return other instanceof UnixPath && path.equals(((UnixPath)other).path);
    }

    public int hashCode()
    {
        return path.hashCode();
    }

    public String toString()
    {
        return path;
    }
}
