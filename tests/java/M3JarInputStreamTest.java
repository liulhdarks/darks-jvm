import java.io.FileInputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

// Exercises java.util.jar.JarInputStream against a real archive produced by
// the jar tool: the manifest, the META-INF directory entry and a class entry.
public class M3JarInputStreamTest
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

    public static void main(String[] args) throws Exception
    {
        JarInputStream jar = new JarInputStream(new FileInputStream(args[0]));
        Manifest manifest = jar.getManifest();
        check("manifest present", manifest != null);
        Attributes main = manifest == null ? null : manifest.getMainAttributes();
        check("main class", main != null && "Hello".equals(main.getValue("Main-Class")));

        JarEntry entry = jar.getNextJarEntry();
        boolean found = false;
        while (entry != null)
        {
            if ("Hello.class".equals(entry.getName()))
            {
                found = true;
                byte[] header = new byte[4];
                int read = 0;
                while (read < header.length)
                {
                    int count = jar.read(header, read, header.length - read);
                    if (count < 0)
                    {
                        break;
                    }
                    read += count;
                }
                check("class header length", read == 4);
                check("class magic",
                    header[0] == (byte)0xca && header[1] == (byte)0xfe
                        && header[2] == (byte)0xba && header[3] == (byte)0xbe);
            }
            entry = jar.getNextJarEntry();
        }
        check("class entry found", found);
        jar.close();

        System.out.println("m3 jar input checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
        System.out.println("m3 jar input: ok");
    }
}
