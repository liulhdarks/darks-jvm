import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

public class SdkJarResourceTest
{
    private static int checks;
    private static int failures;

    private static void check(String name, boolean condition)
    {
        checks++;
        if (!condition)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    private static String read(InputStream input) throws java.io.IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int value = input.read();
        while (value >= 0)
        {
            output.write(value);
            value = input.read();
        }
        input.close();
        return new String(output.toByteArray()).trim();
    }

    public static void main(String[] args) throws Exception
    {
        URL resource = ClassLoader.getSystemResource("sdkcl/resource.txt");
        check("jar-url", resource != null);
        check("jar-protocol", resource != null && "jar".equals(resource.getProtocol()));
        check("jar-open", resource != null
            && "resource-content".equals(read(resource.openStream())));

        InputStream direct = ClassLoader.getSystemResourceAsStream("sdkcl/resource.txt");
        check("jar-stream", direct != null
            && "resource-content".equals(read(direct)));

        Enumeration<URL> all = ClassLoader.getSystemResources("sdkcl/resource.txt");
        int count = 0;
        while (all.hasMoreElements())
        {
            check("jar-enumerated-protocol",
                "jar".equals(all.nextElement().getProtocol()));
            count++;
        }
        check("jar-enumeration-count", count == 1);

        System.out.println("jar resource checks=" + checks + " failures=" + failures);
        if (failures == 0)
        {
            System.out.println("sdk jar resource: ok");
        }
        if (failures != 0)
        {
            System.exit(1);
        }
    }
}
