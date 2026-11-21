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

    public Path toRealPath(LinkOption... options) throws java.io.IOException
    {
        return toAbsolutePath().normalize();
    }

    public Path relativize(Path other)
    {
        if (other == null)
        {
            throw new NullPointerException("other");
        }
        if (isAbsolute() != other.isAbsolute())
        {
            throw new IllegalArgumentException("different roots");
        }
        String[] mine = parts(toAbsolutePath().normalize().toString());
        String[] theirs = parts(other.toAbsolutePath().normalize().toString());
        int common = 0;
        while (common < mine.length && common < theirs.length &&
               mine[common].equals(theirs[common]))
        {
            common++;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = common; i < mine.length; i++)
        {
            if (builder.length() > 0)
            {
                builder.append('/');
            }
            builder.append("..");
        }
        for (int i = common; i < theirs.length; i++)
        {
            if (builder.length() > 0)
            {
                builder.append('/');
            }
            builder.append(theirs[i]);
        }
        return new UnixPath(fileSystem, builder.toString());
    }

    private static String[] parts(String value)
    {
        java.util.ArrayList<String> list = new java.util.ArrayList<String>();
        int position = 0;
        while (position < value.length())
        {
            while (position < value.length() && value.charAt(position) == '/')
            {
                position++;
            }
            int start = position;
            while (position < value.length() && value.charAt(position) != '/')
            {
                position++;
            }
            if (start < position)
            {
                list.add(value.substring(start, position));
            }
        }
        return list.toArray(new String[list.size()]);
    }

    public Path resolveSibling(Path other)
    {
        Path parent = getParent();
        return parent == null ? other : parent.resolve(other);
    }

    public Path resolveSibling(String other)
    {
        Path parent = getParent();
        return parent == null ? new UnixPath(fileSystem, other) : parent.resolve(other);
    }

    public Path subpath(int beginIndex, int endIndex)
    {
        String[] parts = parts(path);
        if (beginIndex < 0 || endIndex > parts.length || beginIndex >= endIndex)
        {
            throw new IllegalArgumentException("range");
        }
        StringBuilder builder = new StringBuilder();
        for (int i = beginIndex; i < endIndex; i++)
        {
            if (builder.length() > 0)
            {
                builder.append('/');
            }
            builder.append(parts[i]);
        }
        return new UnixPath(fileSystem, builder.toString());
    }

    public boolean startsWith(Path other)
    {
        return other != null && startsWith(other.toString());
    }

    public boolean endsWith(Path other)
    {
        return other != null && endsWith(other.toString());
    }

    public Path getRoot()
    {
        return isAbsolute() ? new UnixPath(fileSystem, "/") : null;
    }

    public java.util.Iterator<Path> iterator()
    {
        final String[] parts = parts(path.startsWith("/") ? path.substring(1) : path);
        return new java.util.Iterator<Path>()
        {
            private int index;

            public boolean hasNext()
            {
                return index < parts.length;
            }

            public Path next()
            {
                if (index >= parts.length)
                {
                    throw new java.util.NoSuchElementException();
                }
                return new UnixPath(fileSystem, parts[index++]);
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }
}
