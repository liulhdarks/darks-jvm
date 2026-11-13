import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class FileIoDemo
{
    static int checks = 0;
    static int failures = 0;

    static void check(String name, boolean ok)
    {
        checks++;
        if (!ok)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args) throws Exception
    {
        String base = System.getProperty("java.io.tmpdir");
        File dir = new File(base + "/darks-fileio-demo");
        check("mkdirs", dir.mkdirs() || dir.isDirectory());
        File file = new File(dir, "data.txt");
        if (file.exists())
        {
            file.delete();
        }
        PrintWriter writer = new PrintWriter(new FileWriter(file));
        writer.println("first line");
        writer.println("second line");
        writer.close();
        check("exists", file.exists());
        check("isFile", file.isFile());
        check("length", file.length() > 0);

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(file)));
        String first = reader.readLine();
        String second = reader.readLine();
        String third = reader.readLine();
        reader.close();
        check("readLine", "first line".equals(first) && "second line".equals(second) && third == null);

        FileWriter appender = new FileWriter(file, true);
        appender.write("third line\n");
        appender.close();
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        int lines = 0;
        while (reader.readLine() != null)
        {
            lines++;
        }
        reader.close();
        check("append", lines == 3);

        String[] entries = dir.list();
        check("list", entries != null && entries.length == 1 && entries[0].equals("data.txt"));
        check("delete", file.delete() && !file.exists());
        check("delete-dir", dir.delete() && !dir.exists());

        System.out.println("file-io checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
    }
}
