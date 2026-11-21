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

    String protocol;
    String host;
    int port = -1;
    String file;
    String ref;
    URLStreamHandler handler;

    public URL(String spec) throws MalformedURLException
    {
        this(null, spec);
    }

    public URL(URL context, String spec) throws MalformedURLException
    {
        if (spec == null)
        {
            throw new NullPointerException("spec");
        }
        URLStreamHandler parentHandler = context == null ? null : context.handler;
        if (parentHandler != null)
        {
            handler = parentHandler;
            inherit(context);
            parentHandler.parseURL(this, spec, 0, spec.length());
            return;
        }
        parseSpec(spec);
    }

    public URL(URL context, String spec, URLStreamHandler handler)
        throws MalformedURLException
    {
        if (handler == null)
        {
            throw new NullPointerException("handler");
        }
        if (spec == null)
        {
            throw new NullPointerException("spec");
        }
        this.handler = handler;
        if (context != null)
        {
            inherit(context);
        }
        handler.parseURL(this, spec, 0, spec.length());
    }

    private void inherit(URL context)
    {
        protocol = context.protocol;
        host = context.host;
        port = context.port;
        file = context.file;
        ref = context.ref;
    }

    public URL(String protocol, String host, int port, String file)
        throws MalformedURLException
    {
        this(protocol, host, port, file, null);
    }

    public URL(String protocol, String host, int port, String file,
        URLStreamHandler handler) throws MalformedURLException
    {
        if (protocol == null)
        {
            throw new NullPointerException("protocol");
        }
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.file = file;
        this.handler = handler;
    }

    private void parseSpec(String spec) throws MalformedURLException
    {
        int colon = spec.indexOf(':');
        if (colon <= 0)
        {
            throw new MalformedURLException("no protocol: " + spec);
        }
        protocol = spec.substring(0, colon);
        file = spec.substring(colon + 1);
        int hash = file.indexOf('#');
        if (hash >= 0)
        {
            ref = file.substring(hash + 1);
            file = file.substring(0, hash);
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

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public int getDefaultPort()
    {
        return handler == null ? -1 : handler.getDefaultPort();
    }

    public String getFile()
    {
        return file;
    }

    public String getPath()
    {
        if (file == null)
        {
            return null;
        }
        int query = file.indexOf('?');
        return query < 0 ? file : file.substring(0, query);
    }

    public String getQuery()
    {
        if (file == null)
        {
            return null;
        }
        int query = file.indexOf('?');
        return query < 0 ? null : file.substring(query + 1);
    }

    public String getRef()
    {
        return ref;
    }

    public String getAuthority()
    {
        return host == null ? null : host;
    }

    public String getUserInfo()
    {
        return null;
    }

    public String toExternalForm()
    {
        if (handler != null)
        {
            return handler.toExternalForm(this);
        }
        String value = protocol + ":" + (file == null ? "" : file);
        if (ref != null)
        {
            value = value + "#" + ref;
        }
        return value;
    }

    public String toString()
    {
        return toExternalForm();
    }

    public URI toURI() throws URISyntaxException
    {
        try
        {
            return URI.parse(toExternalForm());
        }
        catch (IllegalArgumentException e)
        {
            throw new URISyntaxException(toExternalForm(), e.getMessage());
        }
    }

    public InputStream openStream() throws IOException
    {
        return openConnection().getInputStream();
    }

    public URLConnection openConnection() throws IOException
    {
        if (handler != null)
        {
            return handler.openConnection(this);
        }
        if ("file".equals(protocol))
        {
            return new FileURLConnection(this, getPath());
        }
        if ("jar".equals(protocol))
        {
            return new ClasspathURLConnection(this, file);
        }
        URLStreamHandler resolved = handlerFor(protocol);
        if (resolved == null)
        {
            throw new IOException("unknown protocol: " + protocol);
        }
        return resolved.openConnection(this);
    }

    private static URLStreamHandler handlerFor(String protocol)
    {
        URLStreamHandler value = handlers.get(protocol);
        if (value != null)
        {
            return value;
        }
        if (factory != null)
        {
            value = factory.createURLStreamHandler(protocol);
            if (value != null)
            {
                handlers.put(protocol, value);
            }
        }
        return value;
    }

    public boolean sameFile(URL other)
    {
        if (other == null)
        {
            return false;
        }
        if (handler != null)
        {
            return handler.sameFile(this, other);
        }
        return toExternalForm().equals(other.toExternalForm());
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
        return sameFile((URL)other);
    }

    public int hashCode()
    {
        if (handler != null)
        {
            return handler.hashCode(this);
        }
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
    private final String location;

    ClasspathURLConnection(URL url, String location)
    {
        super(url);
        this.location = location;
    }

    public InputStream getInputStream() throws IOException
    {
        String value = location == null ? "" : location;
        int marker = value.lastIndexOf("!/");
        if (marker < 0)
        {
            throw new IOException("not an archive entry: " + value);
        }
        String archive = value.substring(0, marker);
        String entry = value.substring(marker + 2);
        if (archive.startsWith("file:"))
        {
            archive = archive.substring(5);
            if (archive.startsWith("//"))
            {
                archive = archive.substring(2);
            }
        }
        java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive);
        try
        {
            java.util.zip.ZipEntry item = zip.getEntry(entry);
            if (item == null)
            {
                throw new IOException("resource not found: " + entry);
            }
            return zip.getInputStream(item);
        }
        finally
        {
            zip.close();
        }
    }
}
