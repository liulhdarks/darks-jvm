package java.io;

public class File
{
    public static final String separator = System.getProperty("file.separator");
    public static final char separatorChar = separator.charAt(0);
    public static final String pathSeparator = System.getProperty("path.separator");
    public static final char pathSeparatorChar = pathSeparator.charAt(0);

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

    public File(java.net.URI uri)
    {
        if (uri == null)
        {
            throw new NullPointerException("uri");
        }
        String value = uri.getPath();
        if (value == null)
        {
            value = uri.getSchemeSpecificPart();
        }
        this.path = value;
    }

    public java.net.URI toURI()
    {
        String absolute = getAbsolutePath();
        if (isDirectory() && !absolute.endsWith("/"))
        {
            absolute = absolute + "/";
        }
        return java.net.URI.create("file:" + absolute);
    }

    public File getAbsoluteFile()
    {
        return new File(getAbsolutePath());
    }

    public String getCanonicalPath() throws java.io.IOException
    {
        return getAbsolutePath();
    }

    public File getCanonicalFile() throws java.io.IOException
    {
        return getAbsoluteFile();
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

    public boolean isAbsolute()
    {
        return path.length() > 0 && path.charAt(0) == '/';
    }

    public File getParentFile()
    {
        String parent = getParent();
        return parent == null ? null : new File(parent);
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

    public boolean canRead()
    {
        return canRead0();
    }

    public boolean canWrite()
    {
        return canWrite0();
    }

    public boolean canExecute()
    {
        return canExecute0();
    }

    public long length()
    {
        return length0();
    }

    public long getTotalSpace()
    {
        return getTotalSpace0();
    }

    public long getFreeSpace()
    {
        return getFreeSpace0();
    }

    public long getUsableSpace()
    {
        return getUsableSpace0();
    }

    public boolean delete()
    {
        return delete0();
    }

    public boolean mkdirs()
    {
        return mkdirs0();
    }

    public boolean mkdir()
    {
        return mkdir0();
    }

    public String[] list()
    {
        return list0();
    }

    public String[] list(FilenameFilter filter)
    {
        String[] names = list();
        if (names == null || filter == null)
        {
            return names;
        }
        java.util.ArrayList<String> kept = new java.util.ArrayList<String>();
        int i = 0;
        while (i < names.length)
        {
            if (filter.accept(this, names[i]))
            {
                kept.add(names[i]);
            }
            i = i + 1;
        }
        return kept.toArray(new String[kept.size()]);
    }

    public File[] listFiles()
    {
        String[] names = list();
        if (names == null)
        {
            return null;
        }
        File[] files = new File[names.length];
        int i = 0;
        while (i < names.length)
        {
            files[i] = new File(this, names[i]);
            i = i + 1;
        }
        return files;
    }

    public File[] listFiles(FilenameFilter filter)
    {
        String[] names = list(filter);
        if (names == null)
        {
            return null;
        }
        File[] files = new File[names.length];
        int i = 0;
        while (i < names.length)
        {
            files[i] = new File(this, names[i]);
            i = i + 1;
        }
        return files;
    }

    public File[] listFiles(FileFilter filter)
    {
        String[] names = list();
        if (names == null)
        {
            return null;
        }
        java.util.ArrayList<File> kept = new java.util.ArrayList<File>();
        int i = 0;
        while (i < names.length)
        {
            File candidate = new File(this, names[i]);
            if (filter == null || filter.accept(candidate))
            {
                kept.add(candidate);
            }
            i = i + 1;
        }
        return kept.toArray(new File[kept.size()]);
    }

    public boolean renameTo(File dest)
    {
        if (dest == null)
        {
            throw new NullPointerException("dest");
        }
        return rename0(dest.getPath());
    }

    /**
     * Returns the roots available on this filesystem.  The JDK enumerates
     * drive letters on Windows; this runtime reports the single filesystem
     * root, as its filesystem layer does not expose a volume list.
     */
    public static File[] listRoots()
    {
        File[] roots = new File[1];
        roots[0] = new File("/");
        return roots;
    }

    public static File createTempFile(String prefix, String suffix)
        throws java.io.IOException
    {
        return createTempFile(prefix, suffix, null);
    }

    public static File createTempFile(String prefix, String suffix,
                                      File directory)
        throws java.io.IOException
    {
        if (prefix == null)
        {
            throw new NullPointerException("prefix");
        }
        if (prefix.length() < 3)
        {
            throw new IllegalArgumentException("Prefix string too short");
        }
        String ending = suffix == null ? ".tmp" : suffix;
        File dir = directory == null
            ? new File(System.getProperty("java.io.tmpdir", "."))
            : directory;
        long seed = System.nanoTime();
        int attempt = 0;
        while (attempt < 100)
        {
            String name = prefix + Long.toHexString(seed + attempt) + ending;
            File candidate = new File(dir, name);
            if (candidate.createNewFile())
            {
                return candidate;
            }
            attempt = attempt + 1;
        }
        throw new java.io.IOException("Unable to create temporary file in " +
            dir.getPath());
    }

    public boolean isHidden()
    {
        return hidden0();
    }

    public long lastModified()
    {
        return lastModified0();
    }

    public boolean setLastModified(long time)
    {
        if (time < 0)
        {
            throw new IllegalArgumentException("Negative time");
        }
        return setLastModified0(time);
    }

    public boolean setReadOnly()
    {
        return setWritable(false, false);
    }

    public boolean setReadable(boolean readable)
    {
        return setReadable(readable, true);
    }

    public boolean setReadable(boolean readable, boolean ownerOnly)
    {
        return setPermission0(4, readable, ownerOnly);
    }

    public boolean setWritable(boolean writable)
    {
        return setWritable(writable, true);
    }

    public boolean setWritable(boolean writable, boolean ownerOnly)
    {
        return setPermission0(2, writable, ownerOnly);
    }

    public boolean setExecutable(boolean executable)
    {
        return setExecutable(executable, true);
    }

    public boolean setExecutable(boolean executable, boolean ownerOnly)
    {
        return setPermission0(1, executable, ownerOnly);
    }

    public int compareTo(File other)
    {
        return path.compareTo(other.path);
    }

    @Deprecated
    public java.net.URL toURL() throws java.net.MalformedURLException
    {
        return toURI().toURL();
    }

    private native boolean rename0(String target);

    private native boolean hidden0();

    private native long lastModified0();

    private native boolean setLastModified0(long time);

    private native boolean setPermission0(int bits, boolean enable,
                                          boolean ownerOnly);

    private native boolean canRead0();

    private native boolean canWrite0();

    private native boolean canExecute0();

    public boolean createNewFile() throws java.io.IOException
    {
        if (exists())
        {
            return false;
        }
        java.io.FileOutputStream stream = new java.io.FileOutputStream(this);
        stream.close();
        return true;
    }

    public java.nio.file.Path toPath()
    {
        return java.nio.file.Paths.get(path);
    }

    public void deleteOnExit()
    {
        deleteOnExit0();
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

    private native long getTotalSpace0();

    private native long getFreeSpace0();

    private native long getUsableSpace0();

    private native boolean delete0();

    private native boolean mkdirs0();

    private native boolean mkdir0();

    private native String[] list0();

    private native void deleteOnExit0();
}
