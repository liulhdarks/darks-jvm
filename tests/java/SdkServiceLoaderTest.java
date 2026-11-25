import java.util.ServiceLoader;
import sdkcl.Plugin;

public class SdkServiceLoaderTest
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

    static class LengthValue extends ClassValue<Integer>
    {
        protected Integer computeValue(Class<?> type)
        {
            return Integer.valueOf(1000 + type.getName().length());
        }
    }

    public static void main(String[] args) throws Exception
    {
        ServiceLoader<Plugin> services = ServiceLoader.load(Plugin.class);
        int count = 0;
        Class<?> providerClass = null;
        for (Plugin plugin : services)
        {
            count++;
            check("service-name", "plugin-impl".equals(plugin.name()));
            providerClass = plugin.getClass();
        }
        check("service-count", count == 1);
        check("service-class", providerClass != null
            && "sdkcl.PluginImpl".equals(providerClass.getName()));

        ServiceLoader<Plugin> explicit = ServiceLoader.load(Plugin.class,
            ClassLoader.getSystemClassLoader());
        count = 0;
        for (Plugin plugin : explicit)
        {
            count++;
            check("explicit-service-name", "plugin-impl".equals(plugin.name()));
        }
        check("explicit-service-count", count == 1);

        LengthValue value = new LengthValue();
        Integer first = value.get(SdkServiceLoaderTest.class);
        Integer second = value.get(SdkServiceLoaderTest.class);
        check("classvalue-stable", first == second);
        check("classvalue-computed",
            first.intValue() == 1000 + "SdkServiceLoaderTest".length());
        value.remove(SdkServiceLoaderTest.class);
        check("classvalue-remove", value.get(SdkServiceLoaderTest.class) != first);

        System.out.println("service checks=" + checks + " failures=" + failures);
        if (failures == 0)
        {
            System.out.println("sdk service loader: ok");
        }
        if (failures != 0)
        {
            System.exit(1);
        }
    }
}
