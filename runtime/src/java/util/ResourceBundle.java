package java.util;

import java.io.InputStream;
import java.io.IOException;

public abstract class ResourceBundle
{
    private ResourceBundle parent;
    private Locale locale = Locale.getDefault();

    public Locale getLocale()
    {
        return locale;
    }

    public void setLocale(Locale locale)
    {
        if (locale != null)
        {
            this.locale = locale;
        }
    }

    public boolean containsKey(String key)
    {
        try
        {
            return getObject(key) != null;
        }
        catch (MissingResourceException e)
        {
            return false;
        }
    }

    public final String getString(String key)
    {
        return (String)getObject(key);
    }

    public final String[] getStringArray(String key)
    {
        return (String[])getObject(key);
    }

    public abstract Object getObject(String key);

    public ResourceBundle getParent()
    {
        return parent;
    }

    public void setParent(ResourceBundle parent)
    {
        this.parent = parent;
    }

    public static ResourceBundle getBundle(String baseName)
    {
        return getBundle(baseName, Locale.getDefault(),
            Thread.currentThread() == null ? ClassLoader.getSystemClassLoader()
                : Thread.currentThread().getContextClassLoader());
    }

    public static ResourceBundle getBundle(String baseName, Locale locale)
    {
        ClassLoader loader = Thread.currentThread() == null
            ? ClassLoader.getSystemClassLoader()
            : Thread.currentThread().getContextClassLoader();
        return getBundle(baseName, locale, loader);
    }

    public static ResourceBundle getBundle(String baseName, Locale locale,
        ClassLoader loader)
    {
        if (loader == null)
        {
            loader = ClassLoader.getSystemClassLoader();
        }
        String language = locale == null ? "" : locale.getLanguage();
        String country = locale == null ? "" : locale.getCountry();
        ArrayList<String> candidates = new ArrayList<String>();
        if (language.length() > 0 && country.length() > 0)
        {
            candidates.add(baseName + "_" + language + "_" + country);
        }
        if (language.length() > 0)
        {
            candidates.add(baseName + "_" + language);
        }
        candidates.add(baseName);
        for (int i = 0; i < candidates.size(); i++)
        {
            String candidate = candidates.get(i);
            ResourceBundle bundle = loadClassBundle(candidate, loader);
            if (bundle == null)
            {
                bundle = loadPropertyBundle(candidate, loader);
            }
            if (bundle != null)
            {
                return bundle;
            }
        }
        throw new MissingResourceException("Can't find bundle for base name "
            + baseName + ", locale " + locale, baseName, "");
    }

    private static ResourceBundle loadClassBundle(String name, ClassLoader loader)
    {
        try
        {
            Class clazz = Class.forName(name, true, loader);
            if (ResourceBundle.class.isAssignableFrom(clazz))
            {
                return (ResourceBundle)clazz.getDeclaredConstructor().newInstance();
            }
        }
        catch (Throwable ignored)
        {
        }
        return null;
    }

    private static ResourceBundle loadPropertyBundle(String name, ClassLoader loader)
    {
        StringBuilder resourceName = new StringBuilder();
        for (int i = 0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            resourceName.append(c == '.' ? '/' : c);
        }
        String resource = resourceName.append(".properties").toString();
        InputStream stream = loader.getResourceAsStream(resource);
        if (stream == null)
        {
            return null;
        }
        try
        {
            return new PropertyResourceBundle(stream);
        }
        catch (IOException ignored)
        {
            return null;
        }
    }
}
