package java.net;

import java.io.IOException;

public abstract class URLConnection
{
    protected URL url;

    protected URLConnection(URL url)
    {
        this.url = url;
    }

    public URL getURL()
    {
        return url;
    }

    public abstract java.io.InputStream getInputStream() throws IOException;
}
