package java.io;

public class File
{
    public static final String separator = System.getProperty("file.separator");
    public static final char separatorChar = separator.charAt(0);

    private String path;

    public File(String path)
    {
        if (path == null)
        {
            throw new NullPointerException();
        }
        this.path = path;
    }

    public File(String parent, String child)
    {
        if (child == null)
        {
            throw new NullPointerException();
        }
        this.path = (parent == null || parent.length() == 0) ? child : parent + separator + child;
    }

    public File(File parent, String child)
    {
        this(parent == null ? null : parent.getPath(), child);
    }

    public String getPath()
    {
        return path;
    }

    public String getName()
    {
        int slash = path.lastIndexOf('/');
        if (slash >= 0)
        {
            return path.substring(slash + 1);
        }
        return path;
    }

    public String getParent()
    {
        int slash = path.lastIndexOf('/');
        if (slash <= 0)
        {
            return null;
        }
        return path.substring(0, slash);
    }

    public String getAbsolutePath()
    {
        if (path.length() > 0 && path.charAt(0) == '/')
        {
            return path;
        }
        String dir = System.getProperty("user.dir", ".");
        if (path.length() == 0 || path.equals("."))
        {
            return dir;
        }
        if (dir.endsWith("/"))
        {
            return dir + path;
        }
        return dir + "/" + path;
    }

    public boolean exists()
    {
        return exists0();
    }

    public boolean isFile()
    {
        return isFile0();
    }

    public boolean isDirectory()
    {
        return isDirectory0();
    }

    public long length()
    {
        return length0();
    }

    public boolean delete()
    {
        return delete0();
    }

    public boolean mkdirs()
    {
        return mkdirs0();
    }

    public String[] list()
    {
        return list0();
    }

    public String toString()
    {
        return path;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof File))
        {
            return false;
        }
        return path.equals(((File)other).path);
    }

    public int hashCode()
    {
        return path.hashCode();
    }

    private native boolean exists0();

    private native boolean isFile0();

    private native boolean isDirectory0();

    private native long length0();

    private native boolean delete0();

    private native boolean mkdirs0();

    private native String[] list0();
}
