package java.util;

/**
 * Minimal ISO-4217 currency value object.  The runtime has no locale
 * database, so symbols and fraction digits use the common defaults.
 */
public final class Currency implements java.io.Serializable
{
    private final String currencyCode;

    private Currency(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    public static Currency getInstance(String currencyCode)
    {
        if (currencyCode == null)
        {
            throw new NullPointerException("currencyCode");
        }
        if (currencyCode.length() != 3)
        {
            throw new IllegalArgumentException("Invalid ISO 4217 code: " + currencyCode);
        }
        return new Currency(currencyCode.toUpperCase());
    }

    public static Currency getInstance(Locale locale)
    {
        if (locale == null)
        {
            throw new NullPointerException("locale");
        }
        if ("GB".equals(locale.getCountry()))
        {
            return getInstance("GBP");
        }
        if ("CN".equals(locale.getCountry()))
        {
            return getInstance("CNY");
        }
        if ("JP".equals(locale.getCountry()))
        {
            return getInstance("JPY");
        }
        if ("DE".equals(locale.getCountry()) || "FR".equals(locale.getCountry()))
        {
            return getInstance("EUR");
        }
        return getInstance("USD");
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public String getSymbol()
    {
        return getSymbol(Locale.getDefault());
    }

    public String getSymbol(Locale locale)
    {
        if ("USD".equals(currencyCode))
        {
            return "$";
        }
        if ("EUR".equals(currencyCode))
        {
            return "\u20ac";
        }
        if ("GBP".equals(currencyCode))
        {
            return "\u00a3";
        }
        if ("JPY".equals(currencyCode) || "CNY".equals(currencyCode))
        {
            return "\u00a5";
        }
        return currencyCode;
    }

    public int getDefaultFractionDigits()
    {
        if ("JPY".equals(currencyCode))
        {
            return 0;
        }
        return 2;
    }

    public int getNumericCode()
    {
        return 0;
    }

    public String getDisplayName()
    {
        return getDisplayName(Locale.getDefault());
    }

    public String getDisplayName(Locale locale)
    {
        return currencyCode;
    }

    public String toString()
    {
        return currencyCode;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Currency))
        {
            return false;
        }
        return currencyCode.equals(((Currency)other).currencyCode);
    }

    public int hashCode()
    {
        return currencyCode.hashCode();
    }
}
