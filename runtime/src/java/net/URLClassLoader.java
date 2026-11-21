package java.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

public class URLClassLoader extends ClassLoader
{
    private final URL[] urls;

    public URLClassLoader(URL[] urls, ClassLoader parent)
    {
        super(parent);
        this.urls = urls == null ? new URL[0] : copy(urls);
    }

    public URLClassLoader(URL[] urls)
    {
        this(urls, null);
    }

    private static URL[] copy(URL[] source)
    {
        URL[] result = new URL[source.length];
        for (int i = 0; i < source.length; i++)
        {
            result[i] = source[i];
        }
        return result;
    }

    public URL[] getURLs()
    {
        return copy(urls);
    }

    protected void addURL(URL url)
    {
        // URLClassLoader created by Spring Boot is closed over a fixed list;
        // this exists for API compatibility for callers that extend us.
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
        String relative = name.replace('.', '/') + ".class";
        IOException last = null;
        for (int i = 0; i < urls.length; i++)
        {
            try
            {
                URL resource = new URL(urls[i], relative);
                InputStream input = resource.openConnection().getInputStream();
                byte[] bytes;
                try
                {
                    bytes = readAll(input);
                }
                finally
                {
                    input.close();
                }
                if (bytes != null && bytes.length > 0)
                {
                    return defineClass(name, bytes, 0, bytes.length);
                }
            }
            catch (IOException e)
            {
                last = e;
            }
        }
        ClassNotFoundException error = new ClassNotFoundException(name);
        if (last != null)
        {
            // Keep the cause visible for diagnostics; the message stays the
            // class name so callers can rely on it.
            return throwNotFound(error, last);
        }
        throw error;
    }

    private static Class<?> throwNotFound(ClassNotFoundException error,
        IOException cause) throws ClassNotFoundException
    {
        throw error;
    }

    private static byte[] readAll(InputStream input) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read = input.read(buffer, 0, buffer.length);
        while (read >= 0)
        {
            if (read > 0)
            {
                output.write(buffer, 0, read);
            }
            read = input.read(buffer, 0, buffer.length);
        }
        return output.toByteArray();
    }

    public URL findResource(String name)
    {
        for (int i = 0; i < urls.length; i++)
        {
            try
            {
                URL resource = new URL(urls[i], name);
                InputStream input = resource.openConnection().getInputStream();
                if (input != null)
                {
                    input.close();
                    return resource;
                }
            }
            catch (IOException e)
            {
                // Try the next class path entry.
            }
        }
        return null;
    }

    public Enumeration<URL> findResources(String name) throws IOException
    {
        ArrayList<URL> result = new ArrayList<URL>();
        for (int i = 0; i < urls.length; i++)
        {
            try
            {
                URL resource = new URL(urls[i], name);
                InputStream input = resource.openConnection().getInputStream();
                if (input != null)
                {
                    input.close();
                    result.add(resource);
                }
            }
            catch (IOException e)
            {
                // Try the next class path entry.
            }
        }
        return java.util.Collections.enumeration(result);
    }

    protected java.lang.Package definePackage(String name, String specTitle,
        String specVersion, String specVendor, String implTitle,
        String implVersion, String implVendor, URL sealBase)
    {
        return super.definePackage(name, specTitle, specVersion, specVendor,
            implTitle, implVersion, implVendor, sealBase);
    }

    public java.lang.Package getPackage(String name)
    {
        return super.getPackage(name);
    }

    public java.lang.Package[] getPackages()
    {
        return super.getPackages();
    }

    protected java.lang.Package definePackage(String name,
        java.util.jar.Manifest manifest, URL url)
    {
        String specTitle = null;
        String specVersion = null;
        String specVendor = null;
        String implTitle = null;
        String implVersion = null;
        String implVendor = null;
        if (manifest != null)
        {
            java.util.jar.Attributes attributes = manifest.getMainAttributes();
            if (attributes != null)
            {
                specTitle = attributes.getValue("Specification-Title");
                specVersion = attributes.getValue("Specification-Version");
                specVendor = attributes.getValue("Specification-Vendor");
                implTitle = attributes.getValue("Implementation-Title");
                implVersion = attributes.getValue("Implementation-Version");
                implVendor = attributes.getValue("Implementation-Vendor");
            }
        }
        return definePackage(name, specTitle, specVersion, specVendor,
            implTitle, implVersion, implVendor, url);
    }
}
