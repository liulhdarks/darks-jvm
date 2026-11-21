package java.net;

import java.io.IOException;
import java.io.InputStream;

public abstract class JarURLConnection extends URLConnection
{
    private String entryName;

    protected JarURLConnection(URL url) throws MalformedURLException
    {
        super(url);
    }

    public String getEntryName()
    {
        if (entryName == null)
        {
            String value = url == null ? null : url.getFile();
            if (value != null)
            {
                int marker = value.indexOf("!/");
                entryName = marker >= 0 ? value.substring(marker + 2) : null;
            }
        }
        return entryName;
    }

    public abstract java.util.jar.JarFile getJarFile() throws IOException;

    public java.util.jar.JarEntry getJarEntry() throws IOException
    {
        return null;
    }

    public java.net.URL getJarFileURL()
    {
        return null;
    }

    public void connect() throws IOException
    {
        connected = true;
    }

    public InputStream getInputStream() throws IOException
    {
        connect();
        java.util.jar.JarFile jar = getJarFile();
        if (jar == null)
        {
            throw new IOException("no jar file");
        }
        java.util.jar.JarEntry entry = getJarEntry();
        if (entry == null)
        {
            String name = getEntryName();
            if (name == null)
            {
                return jar.getInputStream(jar.getEntry(""));
            }
            entry = jar.getJarEntry(name);
        }
        if (entry == null)
        {
            throw new IOException("entry not found: " + getEntryName());
        }
        return jar.getInputStream(entry);
    }
}
