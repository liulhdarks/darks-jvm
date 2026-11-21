import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import sdkcl.Plugin;

public class SdkClassLoaderTest
{
    private static int checks;
    private static int failures;

    static class ByteLoader extends ClassLoader
    {
        ByteLoader()
        {
            super(null);
        }

        static boolean parallelCapable()
        {
            return registerAsParallelCapable();
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException
        {
            String path = name.replace('.', '/') + ".class";
            InputStream input = ClassLoader.getSystemResourceAsStream(path);
            if (input == null)
            {
                throw new ClassNotFoundException(name);
            }
            try
            {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) > 0)
                {
                    output.write(buffer, 0, count);
                }
                input.close();
                byte[] bytes = output.toByteArray();
                return defineClass(name, bytes, 0, bytes.length);
            }
            catch (java.io.IOException error)
            {
                throw new ClassNotFoundException(name);
            }
        }
    }

    private static void check(String name, boolean condition)
    {
        checks++;
        if (!condition)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    private static String readLine(InputStream input) throws java.io.IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int value = input.read();
        while (value >= 0 && value != '\n')
        {
            output.write(value);
            value = input.read();
        }
        input.close();
        return new String(output.toByteArray());
    }

    public static void main(String[] args) throws Exception
    {
        URL resource = ClassLoader.getSystemResource("sdkcl/resource.txt");
        check("resource-url", resource != null);
        check("resource-protocol", resource != null && "file".equals(resource.getProtocol()));
        check("resource-stream",
            "resource-content".equals(readLine(resource.openStream())));

        InputStream direct = ClassLoader.getSystemResourceAsStream("sdkcl/resource.txt");
        check("resource-stream-direct",
            direct != null && "resource-content".equals(readLine(direct)));
        check("resource-leading-slash",
            ClassLoader.getSystemResource("/sdkcl/resource.txt") == null);
        check("resource-missing",
            ClassLoader.getSystemResource("sdkcl/missing.txt") == null);

        Enumeration<URL> all = ClassLoader.getSystemResources("sdkcl/resource.txt");
        int count = 0;
        while (all.hasMoreElements())
        {
            all.nextElement();
            count++;
        }
        check("resource-enumeration", count == 1);

        ByteLoader loader = new ByteLoader();
        Class<?> isolated = loader.loadClass("sdkcl.PluginImpl");
        Class<?> mainstream = Class.forName("sdkcl.PluginImpl");
        check("isolated-identity", isolated != mainstream);
        check("isolated-loader", isolated.getClassLoader() == loader);
        check("system-loader", mainstream.getClassLoader()
            == ClassLoader.getSystemClassLoader());
        check("isolated-name", "sdkcl.PluginImpl".equals(isolated.getName()));

        Object instance = isolated.getDeclaredConstructor().newInstance();
        check("isolated-instance", instance instanceof Plugin);
        check("isolated-plugin", "plugin-impl".equals(((Plugin)instance).name()));
        check("isolated-classloader-stream",
            ClassLoader.getSystemResource("sdkcl/resource.txt") != null);

        Class<?> again = Class.forName("sdkcl.PluginImpl", false, loader);
        check("forname-loader-identity", again == isolated);
        Class<?> initialized = Class.forName("sdkcl.PluginImpl", true, null);
        check("forname-init-system", initialized == mainstream);
        check("parallel-capable", ByteLoader.parallelCapable());

        System.out.println("classloader checks=" + checks + " failures=" + failures);
        if (failures == 0)
        {
            System.out.println("sdk classloader: ok");
        }
        if (failures != 0)
        {
            System.exit(1);
        }
    }
}
