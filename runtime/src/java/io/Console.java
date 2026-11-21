package java.io;

// Console access is not wired to a real terminal.  System.console() always
// returns null, so this class only keeps signatures and simple stream
// fallbacks resolvable.
public final class Console
{
    private Console()
    {
    }

    public PrintWriter writer()
    {
        return new PrintWriter(new Writer()
        {
            public void write(String text) throws IOException
            {
                System.out.print(text);
            }

            public void flush()
            {
                System.out.flush();
            }
        });
    }

    public Reader reader()
    {
        return new InputStreamReader(System.in);
    }

    public Console printf(String format, Object... args)
    {
        System.out.print(String.format(format, args));
        return this;
    }

    public Console format(String format, Object... args)
    {
        return printf(format, args);
    }

    public String readLine()
    {
        return null;
    }

    public String readLine(String format, Object... args)
    {
        return null;
    }

    public char[] readPassword()
    {
        return null;
    }

    public char[] readPassword(String format, Object... args)
    {
        return null;
    }

    public void flush()
    {
        System.out.flush();
    }
}
