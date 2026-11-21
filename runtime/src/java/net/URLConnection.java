package java.net;

import java.io.IOException;

public abstract class URLConnection
{
    protected URL url;
    protected boolean connected;
    protected boolean doInput = true;
    protected boolean doOutput;
    protected boolean useCaches = true;
    protected long ifModifiedSince;

    protected URLConnection(URL url)
    {
        this.url = url;
    }

    public URL getURL()
    {
        return url;
    }

    public void connect() throws IOException
    {
        connected = true;
    }

    public boolean getDoInput()
    {
        return doInput;
    }

    public void setDoInput(boolean value)
    {
        doInput = value;
    }

    public boolean getDoOutput()
    {
        return doOutput;
    }

    public void setDoOutput(boolean value)
    {
        doOutput = value;
    }

    public boolean getUseCaches()
    {
        return useCaches;
    }

    public void setUseCaches(boolean value)
    {
        useCaches = value;
    }

    public long getIfModifiedSince()
    {
        return ifModifiedSince;
    }

    public void setIfModifiedSince(long value)
    {
        ifModifiedSince = value;
    }

    public String getContentType()
    {
        return null;
    }

    public int getContentLength()
    {
        return -1;
    }

    public long getContentLengthLong()
    {
        return -1;
    }

    public abstract java.io.InputStream getInputStream() throws IOException;
}
