package java.net;

import java.io.IOException;

public abstract class URLStreamHandler
{
    protected abstract URLConnection openConnection(URL url) throws IOException;

    protected void parseURL(URL url, String spec, int start, int limit)
    {
        String value = spec.substring(start, limit);
        int colon = value.indexOf(':');
        if (colon > 0)
        {
            url.protocol = value.substring(0, colon);
            url.file = value.substring(colon + 1);
        }
        else
        {
            url.file = value;
        }
        int hash = url.file.indexOf('#');
        if (hash >= 0)
        {
            url.ref = url.file.substring(hash + 1);
            url.file = url.file.substring(0, hash);
        }
    }

    protected void setURL(URL url, String protocol, String host, int port,
        String authority, String userInfo, String path, String query, String ref)
    {
        // Like the JDK, the 9-argument setURL ignores its protocol argument
        // (a protocol change is not permitted); handlers such as Spring Boot's
        // pass "jar:" here for historical reasons.
        if (url.protocol == null)
        {
            url.protocol = protocol;
        }
        url.host = host;
        url.port = port;
        url.file = path;
        if (query != null)
        {
            url.file = (url.file == null ? "" : url.file) + "?" + query;
        }
        url.ref = ref;
    }

    protected String toExternalForm(URL url)
    {
        String value = url.getProtocol() + ":" + (url.getFile() == null ? "" : url.getFile());
        if (url.getRef() != null)
        {
            value = value + "#" + url.getRef();
        }
        return value;
    }

    protected boolean sameFile(URL first, URL second)
    {
        return first.toExternalForm().equals(second.toExternalForm());
    }

    protected int hashCode(URL url)
    {
        return url.toExternalForm().hashCode();
    }

    protected int getDefaultPort()
    {
        return -1;
    }

    protected boolean equals(URL first, URL second)
    {
        return sameFile(first, second);
    }

    protected String getHostAddress(URL url)
    {
        return null;
    }

    protected int getDefaultPort(String protocol)
    {
        return -1;
    }
}
