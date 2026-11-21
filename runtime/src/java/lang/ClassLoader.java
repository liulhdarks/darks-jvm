package java.lang;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

public class ClassLoader
{
    private static ClassLoader system;

    private HashMap<String, Class<?>> loadedClasses;
    private ClassLoader parent;
    private long vmLoader;

    private ClassLoader(boolean systemLoader)
    {
    }

    protected ClassLoader()
    {
        this(getSystemClassLoader());
    }

    protected ClassLoader(ClassLoader parent)
    {
        this.parent = parent;
    }

    public static synchronized ClassLoader getSystemClassLoader()
    {
        if (system == null)
        {
            system = new ClassLoader(true);
        }
        return system;
    }

    public ClassLoader getParent()
    {
        return parent;
    }

    protected static boolean registerAsParallelCapable()
    {
        return true;
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        return loadClass(name, false);
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        if (name.length() > 0 && name.charAt(0) == '[')
        {
            Class<?> array = loadClass0(name);
            if (array == null)
            {
                throw new ClassNotFoundException(name);
            }
            return array;
        }
        Class<?> cached = findLoadedClass(name);
        if (cached != null)
        {
            return cached;
        }
        if (parent != null)
        {
            try
            {
                return parent.loadClass(name, false);
            }
            catch (ClassNotFoundException ignored)
            {
            }
        }
        Class<?> clazz = findClass(name);
        cacheClass(name, clazz);
        return clazz;
    }

    protected final Class<?> findLoadedClass(String name)
    {
        if (loadedClasses == null)
        {
            return null;
        }
        return loadedClasses.get(name);
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
        if (vmLoader == 0)
        {
            Class<?> clazz = loadClass0(name);
            if (clazz == null)
            {
                throw new ClassNotFoundException(name);
            }
            return clazz;
        }
        byte[] bytes = findClassBytes0(name);
        if (bytes == null)
        {
            throw new ClassNotFoundException(name);
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    protected final Class<?> defineClass(String name, byte[] bytes, int offset,
        int length) throws ClassFormatError
    {
        if (bytes == null)
        {
            throw new NullPointerException("bytes");
        }
        if (offset < 0 || length < 0 || offset + length > bytes.length)
        {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0)
        {
            throw new ClassFormatError("empty class file");
        }
        if (vmLoader == 0)
        {
            vmLoader = createLoader0();
        }
        Class<?> clazz = defineClass0(vmLoader, name, bytes, offset, length);
        if (clazz == null)
        {
            throw new ClassFormatError(name);
        }
        return clazz;
    }

    public URL getResource(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        if (name.length() > 0 && name.charAt(0) == '/')
        {
            return null;
        }
        String[] specs = resourceUrls0(name);
        for (int i = 0; i < specs.length; i++)
        {
            try
            {
                return new URL(specs[i]);
            }
            catch (java.net.MalformedURLException ignored)
            {
            }
        }
        return null;
    }

    public InputStream getResourceAsStream(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        if (name.length() > 0 && name.charAt(0) == '/')
        {
            return null;
        }
        byte[] data = getResourceBytes0(name);
        return data == null ? null : new java.io.ByteArrayInputStream(data);
    }

    public Enumeration<URL> getResources(String name) throws java.io.IOException
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        ArrayList<URL> urls = new ArrayList<URL>();
        if (name.length() > 0 && name.charAt(0) == '/')
        {
            return Collections.enumeration(urls);
        }
        String[] specs = resourceUrls0(name);
        for (int i = 0; i < specs.length; i++)
        {
            urls.add(new URL(specs[i]));
        }
        return Collections.enumeration(urls);
    }

    public static URL getSystemResource(String name)
    {
        return getSystemClassLoader().getResource(name);
    }

    public static InputStream getSystemResourceAsStream(String name)
    {
        return getSystemClassLoader().getResourceAsStream(name);
    }

    public static Enumeration<URL> getSystemResources(String name)
        throws java.io.IOException
    {
        return getSystemClassLoader().getResources(name);
    }

    private void cacheClass(String name, Class<?> clazz)
    {
        if (loadedClasses == null)
        {
            loadedClasses = new HashMap<String, Class<?>>();
        }
        loadedClasses.put(name, clazz);
    }

    private static native Class<?> loadClass0(String name);

    private static native byte[] findClassBytes0(String name);

    private static native byte[] getResourceBytes0(String name);

    private static native String[] resourceUrls0(String name);

    private native long createLoader0();

    private static native Class<?> defineClass0(long handle, String name,
        byte[] bytes, int offset, int length);
}
