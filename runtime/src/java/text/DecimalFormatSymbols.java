package java.text;

import java.util.Locale;

/**
 * Locale dependent symbols used by {@link DecimalFormat}.  The runtime only
 * needs a single symbol set, so every locale maps to the same values.
 */
public class DecimalFormatSymbols implements Cloneable
{
    private char zeroDigit = '0';
    private char decimalSeparator = '.';
    private char groupingSeparator = ',';
    private char minusSign = '-';
    private char percent = '%';
    private char perMill = '\u2030';
    private char digit = '#';
    private char patternSeparator = ';';
    private String exponentSeparator = "E";
    private String infinity = "\u221E";
    private String nan = "NaN";
    private String currencySymbol = "\u00A4";
    private String internationalCurrencySymbol = "XXX";
    private char monetaryDecimalSeparator = '.';

    public DecimalFormatSymbols()
    {
    }

    public DecimalFormatSymbols(Locale locale)
    {
        if (locale == null)
        {
            throw new NullPointerException("locale");
        }
    }

    public static DecimalFormatSymbols getInstance()
    {
        return new DecimalFormatSymbols();
    }

    public static DecimalFormatSymbols getInstance(Locale locale)
    {
        return new DecimalFormatSymbols(locale);
    }

    public char getZeroDigit()
    {
        return zeroDigit;
    }

    public void setZeroDigit(char value)
    {
        zeroDigit = value;
    }

    public char getDecimalSeparator()
    {
        return decimalSeparator;
    }

    public void setDecimalSeparator(char value)
    {
        decimalSeparator = value;
    }

    public char getGroupingSeparator()
    {
        return groupingSeparator;
    }

    public void setGroupingSeparator(char value)
    {
        groupingSeparator = value;
    }

    public char getMinusSign()
    {
        return minusSign;
    }

    public void setMinusSign(char value)
    {
        minusSign = value;
    }

    public char getPercent()
    {
        return percent;
    }

    public void setPercent(char value)
    {
        percent = value;
    }

    public char getPerMill()
    {
        return perMill;
    }

    public void setPerMill(char value)
    {
        perMill = value;
    }

    public char getDigit()
    {
        return digit;
    }

    public void setDigit(char value)
    {
        digit = value;
    }

    public char getPatternSeparator()
    {
        return patternSeparator;
    }

    public void setPatternSeparator(char value)
    {
        patternSeparator = value;
    }

    public String getExponentSeparator()
    {
        return exponentSeparator;
    }

    public void setExponentSeparator(String value)
    {
        exponentSeparator = value;
    }

    public String getInfinity()
    {
        return infinity;
    }

    public void setInfinity(String value)
    {
        infinity = value;
    }

    public String getNaN()
    {
        return nan;
    }

    public void setNaN(String value)
    {
        nan = value;
    }

    public String getCurrencySymbol()
    {
        return currencySymbol;
    }

    public void setCurrencySymbol(String value)
    {
        currencySymbol = value;
    }

    public String getInternationalCurrencySymbol()
    {
        return internationalCurrencySymbol;
    }

    public void setInternationalCurrencySymbol(String value)
    {
        internationalCurrencySymbol = value;
    }

    public char getMonetaryDecimalSeparator()
    {
        return monetaryDecimalSeparator;
    }

    public void setMonetaryDecimalSeparator(char value)
    {
        monetaryDecimalSeparator = value;
    }
}
