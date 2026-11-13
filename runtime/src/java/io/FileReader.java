package java.io;

public class FileReader extends InputStreamReader
{
    public FileReader(String name) throws FileNotFoundException
    {
        super(new FileInputStream(name));
    }

    public FileReader(File file) throws FileNotFoundException
    {
        super(new FileInputStream(file));
    }
}
