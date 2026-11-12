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

    public void println(String s)
    {
        print(s);
        write0("\n");
    }

    public void println()
    {
        write0("\n");
    }

    private native void write0(String s);
}
