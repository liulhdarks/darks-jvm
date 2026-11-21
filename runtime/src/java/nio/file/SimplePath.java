package java.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;

// String-backed Path.  All operations are lexical; nothing here touches the
// underlying filesystem except toFile()/toUri().
final class SimplePath implements Path
{
    private final String value;

    SimplePath(String value)
    {
        this.value = value;
    }

    public FileSystem getFileSystem()
    {
        return DefaultFileSystem.instance();
    }

    public boolean isAbsolute()
    {
        return value.length() > 0 && value.charAt(0) == '/';
    }

    private String withoutTrailingSlashes()
    {
        int end = value.length();
        while (end > 1 && value.charAt(end - 1) == '/')
        {
            end--;
        }
        return value.substring(0, end);
    }

    public Path getRoot()
    {
        return isAbsolute() ? new SimplePath("/") : null;
    }

    public Path getFileName()
    {
        String text = withoutTrailingSlashes();
        if (text.length() == 0 || text.equals("/"))
        {
            return null;
        }
        int slash = text.lastIndexOf('/');
        return new SimplePath(text.substring(slash + 1));
    }

    public Path getParent()
    {
        String text = withoutTrailingSlashes();
        if (text.length() == 0 || text.equals("/"))
        {
            return null;
        }
        int slash = text.lastIndexOf('/');
        if (slash < 0)
        {
            return null;
        }
        if (slash == 0)
        {
            return new SimplePath("/");
        }
        return new SimplePath(text.substring(0, slash));
    }

    private ArrayList<String> segments()
    {
        ArrayList<String> result = new ArrayList<String>();
        int i = 0;
        while (i < value.length())
        {
            while (i < value.length() && value.charAt(i) == '/')
            {
                i++;
            }
            int start = i;
            while (i < value.length() && value.charAt(i) != '/')
            {
                i++;
            }
            if (i > start)
            {
                result.add(value.substring(start, i));
            }
        }
        return result;
    }

    public int getNameCount()
    {
        return segments().size();
    }

    public Path getName(int index)
    {
        ArrayList<String> parts = segments();
        if (index < 0 || index >= parts.size())
        {
            throw new IllegalArgumentException("name index out of range");
        }
        return new SimplePath(parts.get(index));
    }

    public Path subpath(int beginIndex, int endIndex)
    {
        ArrayList<String> parts = segments();
        if (beginIndex < 0 || beginIndex >= parts.size() ||
            endIndex > parts.size() || endIndex <= beginIndex)
        {
            throw new IllegalArgumentException("subpath range out of bounds");
        }
        StringBuilder builder = new StringBuilder();
        for (int i = beginIndex; i < endIndex; i++)
        {
            if (builder.length() > 0)
            {
                builder.append('/');
            }
            builder.append(parts.get(i));
        }
        return new SimplePath(builder.toString());
    }

    public boolean startsWith(Path other)
    {
        return startsWith(other.toString());
    }

    public boolean startsWith(String other)
    {
        ArrayList<String> mine = segments();
        SimplePath candidate = new SimplePath(other);
        ArrayList<String> theirs = candidate.segments();
        if (candidate.isAbsolute() != isAbsolute())
        {
            return false;
        }
        if (theirs.size() > mine.size())
        {
            return false;
        }
        for (int i = 0; i < theirs.size(); i++)
        {
            if (!mine.get(i).equals(theirs.get(i)))
            {
                return false;
            }
        }
        return true;
    }

    public boolean endsWith(Path other)
    {
        return endsWith(other.toString());
    }

    public boolean endsWith(String other)
    {
        ArrayList<String> mine = segments();
        ArrayList<String> theirs = new SimplePath(other).segments();
        if (theirs.size() > mine.size())
        {
            return false;
        }
        int offset = mine.size() - theirs.size();
        for (int i = 0; i < theirs.size(); i++)
        {
            if (!mine.get(offset + i).equals(theirs.get(i)))
            {
                return false;
            }
        }
        return true;
    }

    public Path normalize()
    {
        ArrayList<String> out = new ArrayList<String>();
        ArrayList<String> parts = segments();
        for (int i = 0; i < parts.size(); i++)
        {
            String part = parts.get(i);
            if (part.equals("."))
            {
                continue;
            }
            if (part.equals(".."))
            {
                if (!out.isEmpty() && !out.get(out.size() - 1).equals(".."))
                {
                    out.remove(out.size() - 1);
                }
                else if (!isAbsolute())
                {
                    out.add("..");
                }
                continue;
            }
            out.add(part);
        }
        StringBuilder builder = new StringBuilder();
        if (isAbsolute())
        {
            builder.append('/');
        }
        for (int i = 0; i < out.size(); i++)
        {
            if (i > 0)
            {
                builder.append('/');
            }
            builder.append(out.get(i));
        }
        if (builder.length() == 0)
        {
            return new SimplePath("");
        }
        return new SimplePath(builder.toString());
    }

    public Path resolve(Path other)
    {
        if (other.isAbsolute())
        {
            return other;
        }
        return resolve(other.toString());
    }

    public Path resolve(String other)
    {
        if (other.length() == 0)
        {
            return this;
        }
        if (isAbsolute() && other.charAt(0) == '/')
        {
            return new SimplePath(other);
        }
        if (value.length() == 0)
        {
            return new SimplePath(other);
        }
        if (value.charAt(value.length() - 1) == '/')
        {
            return new SimplePath(value + other);
        }
        return new SimplePath(value + "/" + other);
    }

    public Path resolveSibling(Path other)
    {
        Path parent = getParent();
        return parent == null ? other : parent.resolve(other);
    }

    public Path resolveSibling(String other)
    {
        Path parent = getParent();
        return parent == null ? new SimplePath(other) : parent.resolve(other);
    }

    public Path relativize(Path other)
    {
        if (isAbsolute() != other.isAbsolute())
        {
            throw new IllegalArgumentException("different roots");
        }
        ArrayList<String> mine = segments();
        ArrayList<String> theirs = new SimplePath(other.toString()).segments();
        int common = 0;
        while (common < mine.size() && common < theirs.size() &&
               mine.get(common).equals(theirs.get(common)))
        {
            common++;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = common; i < mine.size(); i++)
        {
            if (mine.get(i).equals(".."))
            {
                throw new IllegalArgumentException("cannot relativize");
            }
            if (builder.length() > 0)
            {
                builder.append('/');
            }
            builder.append("..");
        }
        for (int i = common; i < theirs.size(); i++)
        {
            if (builder.length() > 0)
            {
                builder.append('/');
            }
            builder.append(theirs.get(i));
        }
        return new SimplePath(builder.toString());
    }

    public Path toAbsolutePath()
    {
        if (isAbsolute())
        {
            return this;
        }
        String dir = System.getProperty("user.dir");
        if (dir == null || dir.length() == 0)
        {
            dir = "/";
        }
        if (value.length() == 0)
        {
            return new SimplePath(dir);
        }
        if (dir.charAt(dir.length() - 1) == '/')
        {
            return new SimplePath(dir + value);
        }
        return new SimplePath(dir + "/" + value);
    }

    public Path toRealPath(LinkOption... options) throws IOException
    {
        return toAbsolutePath().normalize();
    }

    public URI toUri()
    {
        return URI.create("file:" + toAbsolutePath().normalize().toString());
    }

    public File toFile()
    {
        return new File(value);
    }

    public int compareTo(Path other)
    {
        return value.compareTo(other.toString());
    }

    public boolean equals(Object other)
    {
        return other instanceof Path && value.equals(other.toString());
    }

    public int hashCode()
    {
        return value.hashCode();
    }

    public String toString()
    {
        return value;
    }

    public Iterator<Path> iterator()
    {
        ArrayList<String> parts = segments();
        ArrayList<Path> paths = new ArrayList<Path>();
        for (int i = 0; i < parts.size(); i++)
        {
            paths.add(new SimplePath(parts.get(i)));
        }
        return paths.iterator();
    }
}
