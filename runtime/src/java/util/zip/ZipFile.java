package java.util.zip;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class ZipFile implements Closeable
{
    public static final int OPEN_READ = 1;
    public static final int OPEN_DELETE = 4;

    private final String name;
    private long handle;
    private boolean open;
    private HashMap<String, ZipEntry> entries;

    public ZipFile(String name) throws IOException
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        this.name = name;
    }

    public ZipFile(File file) throws IOException
    {
        this(file == null ? null : file.getPath());
    }

    public ZipFile(File file, int mode) throws IOException
    {
        this(file);
    }

    public ZipFile(String name, boolean verify) throws IOException
    {
        this(name);
    }

    public String getName()
    {
        return name;
    }

    private void ensureOpen() throws IOException
    {
        if (open)
        {
            return;
        }
        if (handle != 0)
        {
            throw new IOException("zip file closed");
        }
        handle = open0(name);
        if (handle == 0)
        {
            throw new ZipException("cannot open archive: " + name);
        }
        open = true;
        entries = null;
    }

    private HashMap<String, ZipEntry> entryMap() throws IOException
    {
        ensureOpen();
        if (entries != null)
        {
            return entries;
        }
        entries = new HashMap<String, ZipEntry>();
        String[] names = names0(handle);
        for (int i = 0; i < names.length; i++)
        {
            String entryName = names[i];
            if (entryName == null || entryName.length() == 0
                || entryName.charAt(entryName.length() - 1) == '/')
            {
                continue;
            }
            ZipEntry entry = new ZipEntry(entryName);
            entry.setSize(size0(handle, entryName));
            entry.setCompressedSize(compressedSize0(handle, entryName));
            entry.setMethod(method0(handle, entryName));
            entries.put(entryName, entry);
        }
        return entries;
    }

    public ZipEntry getEntry(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        try
        {
            return entryMap().get(name);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public InputStream getInputStream(ZipEntry entry) throws IOException
    {
        if (entry == null)
        {
            throw new NullPointerException("entry");
        }
        ensureOpen();
        byte[] data = read0(handle, entry.getName());
        if (data == null)
        {
            throw new ZipException("entry not found: " + entry.getName());
        }
        return new ByteArrayInputStream(data);
    }

    public int size()
    {
        try
        {
            return entryMap().size();
        }
        catch (IOException e)
        {
            return 0;
        }
    }

    public Enumeration<? extends ZipEntry> entries()
    {
        final ArrayList<ZipEntry> list = new ArrayList<ZipEntry>();
        try
        {
            HashMap<String, ZipEntry> map = entryMap();
            java.util.Iterator<String> it = map.keySet().iterator();
            while (it.hasNext())
            {
                list.add(map.get(it.next()));
            }
        }
        catch (IOException e)
        {
            // Fall through with an empty enumeration; callers see size() == 0.
        }
        return new Enumeration<ZipEntry>()
        {
            private int index;

            public boolean hasMoreElements()
            {
                return index < list.size();
            }

            public ZipEntry nextElement()
            {
                if (index >= list.size())
                {
                    throw new NoSuchElementException();
                }
                return list.get(index++);
            }
        };
    }

    public void close() throws IOException
    {
        if (open)
        {
            close0(handle);
            handle = 0;
            open = false;
            entries = null;
        }
    }

    private static native long open0(String path);

    private static native long openMemory0(byte[] data);

    private static native String[] names0(long handle);

    private static native byte[] read0(long handle, String name);

    private static native long size0(long handle, String name);

    private static native long compressedSize0(long handle, String name);

    private static native int method0(long handle, String name);

    private static native void close0(long handle);
}
