import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

// Reads config.properties from the jar with getResourceAsStream, applies
// -Dconfig.<key>=value overrides and prints the merged configuration.
public class ConfigReader
{
    public static void main(String[] args) throws Exception
    {
        InputStream input = ConfigReader.class.getResourceAsStream("/config.properties");
        if (input == null)
        {
            System.out.println("config-missing");
            return;
        }
        HashMap<String, String> values = new HashMap<String, String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line;
        while ((line = reader.readLine()) != null)
        {
            line = line.trim();
            if (line.length() == 0 || line.charAt(0) == '#')
            {
                continue;
            }
            int equals = line.indexOf('=');
            if (equals > 0)
            {
                values.put(line.substring(0, equals), line.substring(equals + 1));
            }
        }
        reader.close();

        String[] order = {"app.name", "app.version", "owner", "mode"};
        for (int i = 0; i < order.length; i++)
        {
            String key = order[i];
            String value = System.getProperty("config." + key, values.get(key));
            System.out.println(key + "=" + value);
        }
        System.out.println("keys=" + values.size());
        System.out.println("bigOk=" + (readAll("/big.txt").length > 65536));
        System.out.println("empty=" + readAll("/empty.txt").length);
    }

    static byte[] readAll(String name) throws Exception
    {
        InputStream input = ConfigReader.class.getResourceAsStream(name);
        if (input == null)
        {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer, 0, buffer.length)) > 0)
        {
            out.write(buffer, 0, count);
        }
        input.close();
        return out.toByteArray();
    }
}
