package java.net;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public final class URL
{
    private static final HashMap<String, URLStreamHandler> handlers =
        new HashMap<String, URLStreamHandler>();
    private static URLStreamHandlerFactory factory;

    private final String protocol;
    private final String file;
    private final String path;

    public URL(String spec) throws MalformedURLException
    {
        if (spec == null)
        {
            throw new NullPointerException("spec");
        }
        int colon = spec.indexOf(':');
        if (colon <= 0)
        {
            throw new MalformedURLException("no protocol: " + spec);
        }
        protocol = spec.substring(0, colon);
        file = spec.substring(colon + 1);
        if ("file".equals(protocol))
        {
            path = file;
        }
        else
        {
            int marker = file.lastIndexOf("!/");
            path = marker >= 0 ? file.substring(marker + 2) : file;
        }
    }

    public static void setURLStreamHandlerFactory(URLStreamHandlerFactory value)
    {
        if (factory != null)
        {
            throw new Error("factory already defined");
        }
        factory = value;
        handlers.clear();
    }

    public String getProtocol()
    {
        return protocol;
    }

    public String getPath()
    {
        return path;
    }

    public String getFile()
    {
        return file;
    }

    public String toExternalForm()
    {
        return protocol + ":" + file;
    }

    public String toString()
    {
        return toExternalForm();
    }

    public InputStream openStream() throws IOException
    {
        return openConnection().getInputStream();
    }

    public URLConnection openConnection() throws IOException
    {
        if ("file".equals(protocol))
        {
            return new FileURLConnection(this, path);
        }
        if ("jar".equals(protocol))
        {
            return new ClasspathURLConnection(this, path);
        }
        URLStreamHandler handler = handlerFor(protocol);
        if (handler == null)
        {
            throw new IOException("unknown protocol: " + protocol);
        }
        return handler.openConnection(this);
    }

    private static URLStreamHandler handlerFor(String protocol)
    {
        URLStreamHandler handler = handlers.get(protocol);
        if (handler != null)
        {
            return handler;
        }
        if (factory != null)
        {
            handler = factory.createURLStreamHandler(protocol);
            if (handler != null)
            {
                handlers.put(protocol, handler);
            }
        }
        return handler;
    }

    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof URL))
        {
            return false;
        }
        return toExternalForm().equals(((URL)other).toExternalForm());
    }

    public int hashCode()
    {
        return toExternalForm().hashCode();
    }
}

final class FileURLConnection extends URLConnection
{
    private final String path;

    FileURLConnection(URL url, String path)
    {
        super(url);
        this.path = path;
    }

    public InputStream getInputStream() throws IOException
    {
        return new FileInputStream(path);
    }
}

final class ClasspathURLConnection extends URLConnection
{
    private final String entry;

    ClasspathURLConnection(URL url, String entry)
    {
        super(url);
        this.entry = entry;
    }

    public InputStream getInputStream() throws IOException
    {
        InputStream stream = ClassLoader.getSystemResourceAsStream(entry);
        if (stream == null)
        {
            throw new IOException("resource not found: " + entry);
        }
        return stream;
    }
}
