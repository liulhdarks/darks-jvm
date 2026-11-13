package java.io;

public class FileWriter extends Writer
{
    private FileOutputStream out;

    public FileWriter(String name) throws IOException
    {
        out = new FileOutputStream(name, false);
    }

    public FileWriter(String name, boolean append) throws IOException
    {
        out = new FileOutputStream(name, append);
    }

    public FileWriter(File file) throws IOException
    {
        out = new FileOutputStream(file, false);
    }

    public FileWriter(File file, boolean append) throws IOException
    {
        out = new FileOutputStream(file, append);
    }

    public void write(String text) throws IOException
    {
        byte[] bytes = text.getBytes();
        out.write(bytes, 0, bytes.length);
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
