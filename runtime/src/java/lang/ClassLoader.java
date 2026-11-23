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
    private HashMap<String, Package> packages;
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

    protected Package getPackage(String name)
    {
        if (packages == null || name == null)
        {
            return null;
        }
        return packages.get(name);
    }

    protected Package[] getPackages()
    {
        if (packages == null)
        {
            return new Package[0];
        }
        Package[] result = new Package[packages.size()];
        int index = 0;
        java.util.Iterator<java.util.Map.Entry<String, Package>> iterator =
            packages.entrySet().iterator();
        while (iterator.hasNext())
        {
            result[index++] = iterator.next().getValue();
        }
        return result;
    }

    protected Package definePackage(String name, String specTitle,
        String specVersion, String specVendor, String implTitle,
        String implVersion, String implVendor, URL sealBase)
    {
        if (packages == null)
        {
            packages = new HashMap<String, Package>();
        }
        if (packages.get(name) != null)
        {
            throw new IllegalArgumentException(name);
        }
        Package pkg = new Package(name, specTitle, specVersion, specVendor,
            implTitle, implVersion, implVendor);
        packages.put(name, pkg);
        return pkg;
    }

    protected static boolean registerAsParallelCapable()
    {
        return true;
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        return loadClass(name, false);
    }

    protected Object getClassLoadingLock(String className)
    {
        return this;
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
        if (loadedClasses != null)
        {
            Class<?> cached = loadedClasses.get(name);
            if (cached != null)
            {
                return cached;
            }
        }
        if (vmLoader != 0)
        {
            // Classes are registered in the native loader during defineClass,
            // before their static initializer runs, so a <clinit> that asks for
            // its own class (cglib's generated methods do) resolves here.
            Class<?> defined = findLoadedClass0(vmLoader, name);
            if (defined != null)
            {
                cacheClass(name, defined);
                return defined;
            }
        }
        return null;
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
        return defineClass(name, bytes, offset, length, null);
    }

    public static Class<?> defineClassForUnsafe(ClassLoader loader, String name,
        byte[] bytes, int offset, int length,
        java.security.ProtectionDomain protectionDomain) throws ClassFormatError
    {
        ClassLoader target = loader != null ? loader : getSystemClassLoader();
        return target.defineClass(name, bytes, offset, length, protectionDomain);
    }

    protected final Class<?> defineClass(String name, byte[] bytes, int offset,
        int length, java.security.ProtectionDomain protectionDomain)
        throws ClassFormatError
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
        if (name != null)
        {
            // Record the definition so findLoadedClass (and therefore
            // Class.forName with this loader) sees it before delegating.
            cacheClass(name, clazz);
        }
        return clazz;
    }

    protected final Class<?> defineClass(String name, byte[] bytes,
        java.security.ProtectionDomain protectionDomain) throws ClassFormatError
    {
        return defineClass(name, bytes, 0, bytes.length, protectionDomain);
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
        if (parent != null)
        {
            URL fromParent = parent.getResource(name);
            if (fromParent != null)
            {
                return fromParent;
            }
        }
        if (this == system)
        {
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
        return findResource(name);
    }

    protected URL findResource(String name)
    {
        return null;
    }

    protected Enumeration<URL> findResources(String name) throws java.io.IOException
    {
        return Collections.enumeration(new ArrayList<URL>());
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
        URL resource = getResource(name);
        if (resource == null)
        {
            return null;
        }
        try
        {
            return resource.openStream();
        }
        catch (java.io.IOException e)
        {
            return null;
        }
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
        if (parent != null)
        {
            Enumeration<URL> fromParent = parent.getResources(name);
            while (fromParent != null && fromParent.hasMoreElements())
            {
                urls.add(fromParent.nextElement());
            }
        }
        if (this == system)
        {
            String[] specs = resourceUrls0(name);
            for (int i = 0; i < specs.length; i++)
            {
                urls.add(new URL(specs[i]));
            }
        }
        else
        {
            Enumeration<URL> own = findResources(name);
            while (own != null && own.hasMoreElements())
            {
                urls.add(own.nextElement());
            }
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

    private static native Class<?> findLoadedClass0(long handle, String name);

    private static native byte[] findClassBytes0(String name);

    private static native byte[] getResourceBytes0(String name);

    private static native String[] resourceUrls0(String name);

    private native long createLoader0();

    private static native Class<?> defineClass0(long handle, String name,
        byte[] bytes, int offset, int length);
}
