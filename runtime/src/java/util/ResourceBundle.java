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

    public Enumeration<String> getKeys()
    {
        return Collections.enumeration(new ArrayList<String>());
    }

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

    public static ResourceBundle getBundle(String baseName, Locale locale,
        ClassLoader loader, Control control)
    {
        if (control == null)
        {
            return getBundle(baseName, locale, loader);
        }
        if (loader == null)
        {
            loader = ClassLoader.getSystemClassLoader();
        }
        List<Locale> candidates = control.getCandidateLocales(baseName, locale);
        for (int i = 0; i < candidates.size(); i++)
        {
            Locale candidate = candidates.get(i);
            try
            {
                ResourceBundle bundle = control.newBundle(baseName, candidate,
                    "java.properties", loader, false);
                if (bundle != null)
                {
                    return bundle;
                }
            }
            catch (Exception ignored)
            {
            }
        }
        return getBundle(baseName, locale, loader);
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

    public static class Control
    {
        public static final List<String> FORMAT_DEFAULT;
        public static final List<String> FORMAT_CLASS;
        public static final List<String> FORMAT_PROPERTIES;
        public static final long TTL_DEFAULT = 60000L;
        public static final long TTL_NO_EXPIRATION_CONTROL = -1L;

        static
        {
            ArrayList<String> defaults = new ArrayList<String>();
            defaults.add("java.class");
            defaults.add("java.properties");
            FORMAT_DEFAULT = defaults;

            ArrayList<String> classOnly = new ArrayList<String>();
            classOnly.add("java.class");
            FORMAT_CLASS = classOnly;

            ArrayList<String> propertiesOnly = new ArrayList<String>();
            propertiesOnly.add("java.properties");
            FORMAT_PROPERTIES = propertiesOnly;
        }

        protected Control()
        {
        }

        public static Control getControl(List<String> formats)
        {
            return new Control();
        }

        public static Control getNoFallbackControl(List<String> formats)
        {
            return new Control();
        }

        public List<String> getFormats(String baseName)
        {
            return FORMAT_DEFAULT;
        }

        public List<Locale> getCandidateLocales(String baseName, Locale locale)
        {
            ArrayList<Locale> result = new ArrayList<Locale>();
            if (locale == null)
            {
                result.add(Locale.ROOT);
                return result;
            }
            String language = locale.getLanguage();
            String country = locale.getCountry();
            String variant = locale.getVariant();
            if (variant != null && variant.length() > 0)
            {
                result.add(locale);
            }
            if (language != null && language.length() > 0)
            {
                if (country != null && country.length() > 0)
                {
                    result.add(new Locale(language, country));
                }
                result.add(new Locale(language, ""));
            }
            result.add(Locale.ROOT);
            return result;
        }

        public Locale getFallbackLocale(String baseName, Locale locale)
        {
            Locale fallback = Locale.getDefault();
            if (fallback.equals(locale))
            {
                return null;
            }
            return fallback;
        }

        public ResourceBundle newBundle(String baseName, Locale locale, String format,
            ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, IOException
        {
            if (format == null)
            {
                throw new NullPointerException("format");
            }
            if (format.equals("java.class"))
            {
                String bundleName = toBundleName(baseName, locale);
                try
                {
                    Class clazz = Class.forName(bundleName, true, loader);
                    if (ResourceBundle.class.isAssignableFrom(clazz))
                    {
                        return (ResourceBundle)clazz.newInstance();
                    }
                }
                catch (ClassNotFoundException ignored)
                {
                }
                return null;
            }
            if (format.equals("java.properties"))
            {
                String resourceName = toResourceName(toBundleName(baseName, locale),
                    "properties");
                InputStream stream = loader.getResourceAsStream(resourceName);
                if (stream == null)
                {
                    return null;
                }
                try
                {
                    return new PropertyResourceBundle(stream);
                }
                finally
                {
                    stream.close();
                }
            }
            throw new IllegalArgumentException("unknown format: " + format);
        }

        public String toBundleName(String baseName, Locale locale)
        {
            if (locale == null || locale == Locale.ROOT)
            {
                return baseName;
            }
            String language = locale.getLanguage();
            String country = locale.getCountry();
            String variant = locale.getVariant();
            if (language.length() == 0 && country.length() == 0
                && variant.length() == 0)
            {
                return baseName;
            }
            StringBuilder name = new StringBuilder(baseName);
            name.append('_').append(language);
            if (country.length() > 0)
            {
                name.append('_').append(country);
            }
            if (variant.length() > 0)
            {
                name.append('_').append(variant);
            }
            return name.toString();
        }

        public String toResourceName(String bundleName, String suffix)
        {
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < bundleName.length(); i++)
            {
                char c = bundleName.charAt(i);
                name.append(c == '.' ? '/' : c);
            }
            name.append('.').append(suffix);
            return name.toString();
        }

        public long getTimeToLive(String baseName, Locale locale)
        {
            return TTL_DEFAULT;
        }

        public boolean needsReload(String baseName, Locale locale, String format,
            ClassLoader loader, ResourceBundle bundle, long loadTime)
        {
            return false;
        }
    }
}
