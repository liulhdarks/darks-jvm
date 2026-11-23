package java.util;

/**
 * Minimal locale value object.  The runtime is locale neutral; the class
 * exists so that the formatting APIs keep their standard signatures.
 */
public final class Locale
{
    public static final Locale ROOT = new Locale("", "");
    public static final Locale ENGLISH = new Locale("en", "");
    public static final Locale US = new Locale("en", "US");
    public static final Locale UK = new Locale("en", "GB");
    public static final Locale CHINA = new Locale("zh", "CN");
    public static final Locale CANADA = new Locale("en", "CA");
    public static final Locale CANADA_FRENCH = new Locale("fr", "CA");
    public static final Locale FRENCH = new Locale("fr", "");
    public static final Locale FRANCE = new Locale("fr", "FR");
    public static final Locale GERMAN = new Locale("de", "");
    public static final Locale GERMANY = new Locale("de", "DE");
    public static final Locale ITALIAN = new Locale("it", "");
    public static final Locale ITALY = new Locale("it", "IT");
    public static final Locale JAPANESE = new Locale("ja", "");
    public static final Locale JAPAN = new Locale("ja", "JP");
    public static final Locale KOREAN = new Locale("ko", "");
    public static final Locale KOREA = new Locale("ko", "KR");
    public static final Locale CHINESE = new Locale("zh", "");
    public static final Locale SIMPLIFIED_CHINESE = new Locale("zh", "CN");
    public static final Locale TRADITIONAL_CHINESE = new Locale("zh", "TW");
    public static final Locale TAIWAN = new Locale("zh", "TW");

    private final String language;
    private final String country;

    public Locale(String language, String country)
    {
        this.language = language == null ? "" : language;
        this.country = country == null ? "" : country;
    }

    public Locale(String language)
    {
        this(language, "");
    }

    public static Locale getDefault()
    {
        return formatLocale;
    }

    public enum Category
    {
        DISPLAY,
        FORMAT
    }

    private static Locale displayLocale = ROOT;
    private static Locale formatLocale = ROOT;

    public static Locale getDefault(Category category)
    {
        if (category == null)
        {
            throw new NullPointerException("category");
        }
        return category == Category.DISPLAY ? displayLocale : formatLocale;
    }

    public static void setDefault(Category category, Locale newLocale)
    {
        if (category == null)
        {
            throw new NullPointerException("category");
        }
        if (newLocale == null)
        {
            throw new NullPointerException("newLocale");
        }
        if (category == Category.DISPLAY)
        {
            displayLocale = newLocale;
        }
        else
        {
            formatLocale = newLocale;
        }
    }

    public String getLanguage()
    {
        return language;
    }

    public String getCountry()
    {
        return country;
    }

    public String getVariant()
    {
        return "";
    }

    public String getScript()
    {
        return "";
    }

    public String toLanguageTag()
    {
        if (language.length() == 0)
        {
            return "und";
        }
        StringBuilder tag = new StringBuilder(language);
        String script = getScript();
        if (script.length() > 0)
        {
            tag.append('-').append(script);
        }
        if (country.length() > 0)
        {
            tag.append('-').append(country);
        }
        String variant = getVariant();
        if (variant.length() > 0)
        {
            tag.append('-').append(variant);
        }
        return tag.toString();
    }

    public String getDisplayName()
    {
        return toString();
    }

    public String getDisplayLanguage()
    {
        return language;
    }

    public String getDisplayCountry()
    {
        return country;
    }

    public String getDisplayVariant()
    {
        return "";
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Locale))
        {
            return false;
        }
        Locale o = (Locale)other;
        return language.equals(o.language) && country.equals(o.country);
    }

    public int hashCode()
    {
        return language.hashCode() * 31 + country.hashCode();
    }

    public String toString()
    {
        if (language.length() == 0 && country.length() == 0)
        {
            return "";
        }
        if (country.length() == 0)
        {
            return language;
        }
        if (language.length() == 0)
        {
            return country;
        }
        return language + "_" + country;
    }
}
