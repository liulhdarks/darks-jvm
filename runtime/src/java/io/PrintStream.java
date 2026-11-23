package java.io;

public class PrintStream
{
    private final boolean err;
    private final java.io.OutputStream out;
    private final boolean autoFlush;
    private final java.nio.charset.Charset charset;
    private boolean trouble;

    public PrintStream()
    {
        this(null, false, false);
    }

    public PrintStream(boolean err)
    {
        this(null, err, false);
    }

    public PrintStream(OutputStream out)
    {
        this(out, false, false);
    }

    public PrintStream(OutputStream out, boolean autoFlush)
    {
        this(out, false, autoFlush);
    }

    private PrintStream(OutputStream out, boolean err, boolean autoFlush)
    {
        this(out, err, autoFlush, null);
    }

    private PrintStream(OutputStream out, boolean err, boolean autoFlush,
                        java.nio.charset.Charset charset)
    {
        this.out = out;
        this.err = err;
        this.autoFlush = autoFlush;
        this.charset = charset;
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
        if (out != null)
        {
            try
            {
                out.flush();
            }
            catch (java.io.IOException e)
            {
                // PrintStream swallows transport errors.
            }
        }
        // The stdio-backed variants flush on every write.
    }

    public void write(int b)
    {
        write(new byte[] { (byte)b }, 0, 1);
    }

    public void write(byte[] bytes)
    {
        if (bytes == null)
        {
            throw new NullPointerException("bytes");
        }
        write(bytes, 0, bytes.length);
    }

    public void write(byte[] bytes, int offset, int length)
    {
        if (bytes == null)
        {
            throw new NullPointerException("bytes");
        }
        if (offset < 0 || length < 0 || offset > bytes.length - length)
        {
            throw new IndexOutOfBoundsException();
        }
        if (out != null)
        {
            try
            {
                out.write(bytes, offset, length);
                if (autoFlush)
                {
                    out.flush();
                }
            }
            catch (java.io.IOException e)
            {
                trouble = true;
            }
            return;
        }
        if (err)
        {
            writeErrBytes0(bytes, offset, length);
        }
        else
        {
            writeBytes0(bytes, offset, length);
        }
    }

    public PrintStream(String fileName) throws java.io.FileNotFoundException
    {
        this(new java.io.FileOutputStream(fileName));
    }

    public PrintStream(String fileName, String csn)
        throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException
    {
        this(new java.io.FileOutputStream(fileName), false, csn);
    }

    public PrintStream(java.io.File file) throws java.io.FileNotFoundException
    {
        this(new java.io.FileOutputStream(file));
    }

    public PrintStream(java.io.File file, String csn)
        throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException
    {
        this(new java.io.FileOutputStream(file), false, csn);
    }

    public PrintStream(java.io.OutputStream out, boolean autoFlush,
                       String encoding)
        throws java.io.UnsupportedEncodingException
    {
        this(out, false, autoFlush, java.nio.charset.Charset.forName(encoding));
    }

    public PrintStream(java.io.OutputStream out, boolean autoFlush,
                       java.nio.charset.Charset charset)
    {
        this(out, false, autoFlush, charset);
    }

    public void print(char[] chars)
    {
        write(new String(chars));
    }

    public void println(char[] chars)
    {
        print(chars);
        write("\n");
    }

    public PrintStream append(CharSequence text)
    {
        print(text == null ? "null" : text.toString());
        return this;
    }

    public PrintStream append(CharSequence text, int start, int end)
    {
        CharSequence piece = text == null ? "null" : text.subSequence(start, end);
        print(piece.toString());
        return this;
    }

    public PrintStream append(char value)
    {
        print(value);
        return this;
    }

    public PrintStream format(String format, Object... args)
    {
        return format(java.util.Locale.getDefault(), format, args);
    }

    public PrintStream format(java.util.Locale locale, String format,
                              Object... args)
    {
        print(String.format(locale, format, args));
        return this;
    }

    public PrintStream printf(String format, Object... args)
    {
        return format(format, args);
    }

    public PrintStream printf(java.util.Locale locale, String format,
                              Object... args)
    {
        return format(locale, format, args);
    }

    public boolean checkError()
    {
        flush();
        return trouble;
    }

    public void close()
    {
        flush();
        if (out != null)
        {
            try
            {
                out.close();
            }
            catch (java.io.IOException e)
            {
                trouble = true;
            }
        }
    }

    private native void writeBytes0(byte[] bytes, int offset, int length);

    private native void writeErrBytes0(byte[] bytes, int offset, int length);

    private void write(String s)
    {
        if (out != null)
        {
            byte[] bytes = charset == null ? s.getBytes() : s.getBytes(charset);
            write(bytes, 0, bytes.length);
            return;
        }
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
