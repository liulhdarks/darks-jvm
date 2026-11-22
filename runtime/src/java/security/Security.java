package java.security;

import java.util.Properties;

/**
 * Minimal security properties table.  Only the lookups performed while
 * initialising JASPIC providers are supported; providers are not modelled.
 */
public final class Security
{
    private static final Properties PROPS = new Properties();

    private Security()
    {
    }

    public static String getProperty(String key)
    {
        return PROPS.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue)
    {
        return PROPS.getProperty(key, defaultValue);
    }

    public static void setProperty(String key, String value)
    {
        PROPS.setProperty(key, value);
    }

    public static String getAlgorithmProperty(String algName, String propName)
    {
        return null;
    }

    public static int getInteger(String key, int defaultValue)
    {
        try
        {
            String value = PROPS.getProperty(key);
            return value == null ? defaultValue : Integer.parseInt(value);
        }
        catch (RuntimeException ignored)
        {
            return defaultValue;
        }
    }
}
