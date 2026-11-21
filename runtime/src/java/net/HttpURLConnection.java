package java.net;

import java.io.IOException;

public abstract class HttpURLConnection extends URLConnection
{
    protected String method = "GET";
    protected int responseCode = -1;

    protected HttpURLConnection(URL url)
    {
        super(url);
    }

    public void setRequestMethod(String method)
    {
        this.method = method;
    }

    public String getRequestMethod()
    {
        return method;
    }

    public int getResponseCode() throws IOException
    {
        connect();
        return responseCode;
    }

    public String getResponseMessage() throws IOException
    {
        return null;
    }

    public java.io.InputStream getErrorStream()
    {
        return null;
    }

    public void disconnect()
    {
    }

    public boolean usingProxy()
    {
        return false;
    }

    public abstract void connect() throws IOException;

    public static void setFollowRedirects(boolean follow)
    {
    }

    public static boolean getFollowRedirects()
    {
        return true;
    }
}
