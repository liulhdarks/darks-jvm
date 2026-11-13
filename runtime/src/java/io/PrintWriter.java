package java.io;

public class PrintWriter extends Writer
{
    private Writer out;
    private boolean error;

    public PrintWriter(Writer out)
    {
        this.out = out;
    }

    public PrintWriter(String name) throws IOException
    {
        out = new FileWriter(name, false);
    }

    public PrintWriter(File file) throws IOException
    {
        out = new FileWriter(file);
    }

    public void write(String text)
    {
        try
        {
            out.write(text);
        }
        catch (IOException e)
        {
            error = true;
        }
    }

    public void print(String text)
    {
        write(text == null ? "null" : text);
    }

    public void print(int value)
    {
        write(Integer.toString(value));
    }

    public void print(long value)
    {
        write(Long.toString(value));
    }

    public void print(double value)
    {
        write(Double.toString(value));
    }

    public void print(boolean value)
    {
        write(Boolean.toString(value));
    }

    public void print(char value)
    {
        write(String.valueOf(value));
    }

    public void print(Object value)
    {
        write(String.valueOf(value));
    }

    public void println()
    {
        write("\n");
    }

    public void println(String text)
    {
        print(text);
        write("\n");
    }

    public void println(int value)
    {
        print(value);
        write("\n");
    }

    public void println(long value)
    {
        print(value);
        write("\n");
    }

    public void println(double value)
    {
        print(value);
        write("\n");
    }

    public void println(boolean value)
    {
        print(value);
        write("\n");
    }

    public void println(char value)
    {
        print(value);
        write("\n");
    }

    public void println(Object value)
    {
        print(value);
        write("\n");
    }

    public boolean checkError()
    {
        return error;
    }

    public void flush()
    {
        try
        {
            out.flush();
        }
        catch (IOException e)
        {
            error = true;
        }
    }

    public void close()
    {
        try
        {
            out.close();
        }
        catch (IOException e)
        {
            error = true;
        }
    }
}
