import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourceDemo
{
    public static void main(String[] args) throws Exception
    {
        int checks = 0;
        int failures = 0;

        InputStream direct = ResourceDemo.class.getResourceAsStream("/compat/message.txt");
        checks++;
        if (direct == null)
        {
            failures++;
            System.out.println("FAIL direct-null");
        }
        else
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(direct));
            checks++;
            if (!"resource-from-dir".equals(reader.readLine())) failures++;
            reader.close();
        }

        InputStream loader = ClassLoader.getSystemResourceAsStream("compat/message.txt");
        checks++;
        if (loader == null)
        {
            failures++;
            System.out.println("FAIL loader-null");
        }
        else
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(loader));
            checks++;
            if (!"resource-from-dir".equals(reader.readLine())) failures++;
            reader.close();
        }

        checks++;
        if (ResourceDemo.class.getResourceAsStream("/compat/missing.txt") != null)
        {
            failures++;
        }

        System.out.println("resource checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
    }
}
