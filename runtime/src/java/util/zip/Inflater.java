package java.util.zip;

public class Inflater
{
    private long handle;
    private boolean nowrap;
    private boolean finished;

    public Inflater()
    {
        this(false);
    }

    public Inflater(boolean nowrap)
    {
        this.nowrap = nowrap;
        this.handle = init0(nowrap);
    }

    public void setInput(byte[] input, int offset, int length)
    {
        if (input == null)
        {
            throw new NullPointerException("input");
        }
        if (offset < 0 || length < 0 || offset + length > input.length)
        {
            throw new IndexOutOfBoundsException();
        }
        ensureOpen();
        setInput0(handle, input, offset, length);
    }

    public void setInput(byte[] input)
    {
        setInput(input, 0, input.length);
    }

    public int inflate(byte[] output, int offset, int length) throws DataFormatException
    {
        if (output == null)
        {
            throw new NullPointerException("output");
        }
        if (offset < 0 || length < 0 || offset + length > output.length)
        {
            throw new IndexOutOfBoundsException();
        }
        ensureOpen();
        int produced = inflate0(handle, output, offset, length);
        if (produced < 0)
        {
            throw new DataFormatException("invalid deflate data");
        }
        if (produced == 0 && finished0(handle))
        {
            finished = true;
        }
        return produced;
    }

    public int inflate(byte[] output) throws DataFormatException
    {
        return inflate(output, 0, output.length);
    }

    public boolean needsInput()
    {
        return handle == 0 || needsInput0(handle);
    }

    public boolean needsDictionary()
    {
        return handle != 0 && needsDictionary0(handle);
    }

    public boolean finished()
    {
        return finished || (handle != 0 && finished0(handle));
    }

    public int getRemaining()
    {
        return handle == 0 ? 0 : remaining0(handle);
    }

    public int getTotalIn()
    {
        return 0;
    }

    public int getTotalOut()
    {
        return 0;
    }

    public void reset()
    {
        ensureOpen();
        reset0(handle);
        finished = false;
    }

    public void end()
    {
        if (handle != 0)
        {
            end0(handle);
            handle = 0;
        }
    }

    private void ensureOpen()
    {
        if (handle == 0)
        {
            handle = init0(nowrap);
        }
    }

    private static native long init0(boolean nowrap);

    private static native void setInput0(long handle, byte[] input, int offset, int length);

    private static native int inflate0(long handle, byte[] output, int offset, int length);

    private static native boolean needsInput0(long handle);

    private static native boolean needsDictionary0(long handle);

    private static native boolean finished0(long handle);

    private static native int remaining0(long handle);

    private static native void reset0(long handle);

    private static native void end0(long handle);
}
