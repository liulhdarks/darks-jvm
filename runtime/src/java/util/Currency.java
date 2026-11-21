package java.util;

public final class Currency
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
        for (int i = 0; i < currencyCode.length(); i++)
        {
            char c = currencyCode.charAt(i);
            if (c < 'A' || c > 'Z')
            {
                throw new IllegalArgumentException("Invalid ISO 4217 code: " + currencyCode);
            }
        }
        return new Currency(currencyCode);
    }

    public static Currency getInstance(Locale locale)
    {
        if (locale == null)
        {
            throw new NullPointerException("locale");
        }
        String country = locale.getCountry();
        if (country != null && "US".equals(country))
        {
            return getInstance("USD");
        }
        if (country != null && "GB".equals(country))
        {
            return getInstance("GBP");
        }
        if (country != null && ("CN".equals(country) || "JP".equals(country)))
        {
            return getInstance("CN".equals(country) ? "CNY" : "JPY");
        }
        if (country != null && ("DE".equals(country) || "FR".equals(country)))
        {
            return getInstance("EUR");
        }
        return getInstance("XXX");
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public String getSymbol()
    {
        return currencyCode;
    }

    public String getSymbol(Locale locale)
    {
        return currencyCode;
    }

    public int getDefaultFractionDigits()
    {
        return 2;
    }

    public String getDisplayName()
    {
        return currencyCode;
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
