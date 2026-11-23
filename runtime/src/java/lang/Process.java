package java.lang;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public abstract class Process
{
    public abstract OutputStream getOutputStream();

    public abstract InputStream getInputStream();

    public abstract InputStream getErrorStream();

    public abstract int waitFor() throws InterruptedException;

    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException
    {
        throw new UnsupportedOperationException("process control is not supported");
    }

    public abstract int exitValue();

    public abstract void destroy();

    public Process destroyForcibly()
    {
        destroy();
        return this;
    }

    public boolean isAlive()
    {
        throw new UnsupportedOperationException("process control is not supported");
    }

    public long pid()
    {
        throw new UnsupportedOperationException("process control is not supported");
    }
}
