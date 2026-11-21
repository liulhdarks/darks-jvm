package java.net;

import java.io.IOException;

public abstract class URLConnection
{
    protected URL url;
    private boolean useCaches = true;
    private boolean doInput = true;
    private boolean doOutput = false;
    private int connectTimeout = 0;
    private int readTimeout = 0;
    private long ifModifiedSince = 0;

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
    }

    public boolean getUseCaches()
    {
        return useCaches;
    }

    public void setUseCaches(boolean useCaches)
    {
        this.useCaches = useCaches;
    }

    public boolean getDoInput()
    {
        return doInput;
    }

    public void setDoInput(boolean doInput)
    {
        this.doInput = doInput;
    }

    public boolean getDoOutput()
    {
        return doOutput;
    }

    public void setDoOutput(boolean doOutput)
    {
        this.doOutput = doOutput;
    }

    public int getConnectTimeout()
    {
        return connectTimeout;
    }

    public void setConnectTimeout(int timeout)
    {
        this.connectTimeout = timeout;
    }

    public int getReadTimeout()
    {
        return readTimeout;
    }

    public void setReadTimeout(int timeout)
    {
        this.readTimeout = timeout;
    }

    public long getIfModifiedSince()
    {
        return ifModifiedSince;
    }

    public void setIfModifiedSince(long ifModifiedSince)
    {
        this.ifModifiedSince = ifModifiedSince;
    }

    public long getLastModified()
    {
        return 0;
    }

    public int getContentLength()
    {
        return -1;
    }

    public long getContentLengthLong()
    {
        return -1;
    }

    public String getContentType()
    {
        return null;
    }

    public String getHeaderField(String name)
    {
        return null;
    }

    public String getHeaderFieldKey(int index)
    {
        return null;
    }

    public String getHeaderField(int index)
    {
        return null;
    }

    public java.util.Map<String, java.util.List<String>> getHeaderFields()
    {
        return new java.util.HashMap<String, java.util.List<String>>();
    }

    public Object getContent() throws IOException
    {
        return getInputStream();
    }

    public abstract java.io.InputStream getInputStream() throws IOException;
}
