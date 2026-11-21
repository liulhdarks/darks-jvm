package java.net;

import java.io.IOException;

public abstract class URLStreamHandler
{
    protected abstract URLConnection openConnection(URL url) throws IOException;
}
