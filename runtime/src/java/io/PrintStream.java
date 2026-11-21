package java.io;

public class PrintStream
{
    private final boolean err;

    public PrintStream()
    {
        this(false);
    }

    public PrintStream(boolean err)
    {
        this.err = err;
    }

    public void print(String s)
    {
        if (s != null)
        {
            write(s);
        }
    }

    public void print(int v)
    {
        write(java.lang.Integer.toString(v));
    }

    public void print(long v)
    {
        write(java.lang.Long.toString(v));
    }

    public void print(double v)
    {
        write(java.lang.Double.toString(v));
    }

    public void print(float v)
    {
        write(java.lang.Double.toString((double)v));
    }

    public void print(boolean v)
    {
        write(java.lang.Boolean.toString(v));
    }

    public void print(char v)
    {
        write(java.lang.Character.toString(v));
    }

    public void print(Object v)
    {
        write(java.lang.String.valueOf(v));
    }

    public void println(String s)
    {
        print(s);
        write("\n");
    }

    public void println(int v)
    {
        print(v);
        write("\n");
    }

    public void println(long v)
    {
        print(v);
        write("\n");
    }

    public void println(boolean v)
    {
        print(v);
        write("\n");
    }

    public void println(char v)
    {
        print(v);
        write("\n");
    }

    public void println(double v)
    {
        print(v);
        write("\n");
    }

    public void println(float v)
    {
        print(v);
        write("\n");
    }

    public void println(Object v)
    {
        print(v);
        write("\n");
    }

    public void println()
    {
        write("\n");
    }

    public void flush()
    {
        // Every write already flushes the underlying stdio stream.
    }

    private void write(String s)
    {
        if (err)
        {
            writeErr0(s);
        }
        else
        {
            write0(s);
        }
    }

    private native void write0(String s);

    private native void writeErr0(String s);
}
