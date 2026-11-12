package java.io;

public class PrintStream
{
    public PrintStream()
    {
    }

    public void print(String s)
    {
        if (s != null)
        {
            write0(s);
        }
    }

    public void print(int v)
    {
        write0(java.lang.Integer.toString(v));
    }

    public void print(long v)
    {
        write0(java.lang.Long.toString(v));
    }

    public void print(double v)
    {
        write0(java.lang.Double.toString(v));
    }

    public void print(float v)
    {
        write0(java.lang.Double.toString((double)v));
    }

    public void print(boolean v)
    {
        write0(java.lang.Boolean.toString(v));
    }

    public void print(char v)
    {
        write0(java.lang.Character.toString(v));
    }

    public void print(Object v)
    {
        write0(java.lang.String.valueOf(v));
    }

    public void println(String s)
    {
        print(s);
        write0("\n");
    }

    public void println(int v)
    {
        print(v);
        write0("\n");
    }

    public void println(long v)
    {
        print(v);
        write0("\n");
    }

    public void println(boolean v)
    {
        print(v);
        write0("\n");
    }

    public void println(char v)
    {
        print(v);
        write0("\n");
    }

    public void println(double v)
    {
        print(v);
        write0("\n");
    }

    public void println(float v)
    {
        print(v);
        write0("\n");
    }

    public void println(Object v)
    {
        print(v);
        write0("\n");
    }

    public void println()
    {
        write0("\n");
    }

    private native void write0(String s);
}
