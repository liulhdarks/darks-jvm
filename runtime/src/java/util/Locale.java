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
        return ROOT;
    }

    public String getLanguage()
    {
        return language;
    }

    public String getCountry()
    {
        return country;
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
