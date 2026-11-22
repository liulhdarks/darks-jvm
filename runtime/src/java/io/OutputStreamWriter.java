package java.io;

public class OutputStreamWriter extends Writer
{
    private OutputStream out;
    private java.nio.charset.Charset charset;

    public OutputStreamWriter(OutputStream out)
    {
        this.out = out;
    }

    public OutputStreamWriter(OutputStream out,
                              java.nio.charset.Charset charset)
    {
        this.out = out;
        this.charset = charset;
    }

    public OutputStreamWriter(OutputStream out, String charsetName)
        throws UnsupportedEncodingException
    {
        this.out = out;
        this.charset = java.nio.charset.Charset.forName(charsetName);
    }

    public String getEncoding()
    {
        return charset == null ? "UTF-8" : charset.name();
    }

    public void write(String text) throws IOException
    {
        byte[] bytes = charset == null ? text.getBytes() : text.getBytes(charset);
        out.write(bytes);
    }

    public void flush() throws IOException
    {
        out.flush();
    }

    public void close() throws IOException
    {
        out.close();
    }
}
