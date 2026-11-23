package java.text;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Number formatting/parsing with grouping, integer/fraction digit limits and
 * percentage scaling.  DecimalFormat adds pattern support on top.
 */
public class NumberFormat
{
    protected boolean groupingUsed = true;
    protected int minimumIntegerDigits = 1;
    protected int minimumFractionDigits = 0;
    protected int maximumFractionDigits = 3;
    protected boolean parseIntegerOnly = false;
    protected String prefix = "";
    protected String suffix = "";
    protected String negativePrefix = "-";
    protected String negativeSuffix = "";
    protected String decimalSeparator = ".";
    protected String groupingSeparator = ",";
    protected int groupingSize = 3;
    protected int multiplier = 1;

    public NumberFormat()
    {
    }

    public static NumberFormat getInstance()
    {
        return new NumberFormat();
    }

    public static NumberFormat getNumberInstance()
    {
        return new NumberFormat();
    }

    public static NumberFormat getIntegerInstance()
    {
        NumberFormat format = new NumberFormat();
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        format.setParseIntegerOnly(true);
        return format;
    }

    public static NumberFormat getPercentInstance()
    {
        NumberFormat format = new NumberFormat();
        format.multiplier = 100;
        format.suffix = "%";
        return format;
    }

    public static NumberFormat getCurrencyInstance()
    {
        NumberFormat format = new NumberFormat();
        format.prefix = "$";
        format.minimumFractionDigits = 2;
        format.maximumFractionDigits = 2;
        return format;
    }

    public static NumberFormat getInstance(Locale locale)
    {
        return getInstance();
    }

    public static NumberFormat getNumberInstance(Locale locale)
    {
        return getNumberInstance();
    }

    public static NumberFormat getIntegerInstance(Locale locale)
    {
        return getIntegerInstance();
    }

    public static NumberFormat getPercentInstance(Locale locale)
    {
        // Locale-specific symbols need JDK locale data; the default symbols
        // are used instead.
        return getPercentInstance();
    }

    public static NumberFormat getCurrencyInstance(Locale locale)
    {
        return getCurrencyInstance();
    }

    public boolean isGroupingUsed()
    {
        return groupingUsed;
    }

    public void setGroupingUsed(boolean value)
    {
        groupingUsed = value;
    }

    public int getMinimumIntegerDigits()
    {
        return minimumIntegerDigits;
    }

    public void setMinimumIntegerDigits(int value)
    {
        minimumIntegerDigits = value < 0 ? 0 : value;
    }

    public int getMaximumIntegerDigits()
    {
        return 40;
    }

    public void setMaximumIntegerDigits(int value)
    {
    }

    public int getMinimumFractionDigits()
    {
        return minimumFractionDigits;
    }

    public void setMinimumFractionDigits(int value)
    {
        minimumFractionDigits = value < 0 ? 0 : value;
        if (maximumFractionDigits < minimumFractionDigits)
        {
            maximumFractionDigits = minimumFractionDigits;
        }
    }

    public int getMaximumFractionDigits()
    {
        return maximumFractionDigits;
    }

    public void setMaximumFractionDigits(int value)
    {
        maximumFractionDigits = value < 0 ? 0 : value;
        if (minimumFractionDigits > maximumFractionDigits)
        {
            minimumFractionDigits = maximumFractionDigits;
        }
    }

    public boolean isParseIntegerOnly()
    {
        return parseIntegerOnly;
    }

    public void setParseIntegerOnly(boolean value)
    {
        parseIntegerOnly = value;
    }

    public String format(long value)
    {
        return formatWhole(Long.toString(value));
    }

    public String format(double value)
    {
        if (value != value)
        {
            return "NaN";
        }
        if (value == Double.POSITIVE_INFINITY)
        {
            return "\u221e";
        }
        if (value == Double.NEGATIVE_INFINITY)
        {
            return "-\u221e";
        }
        boolean negative = value < 0;
        BigDecimal decimal = BigDecimal.valueOf(negative ? -value : value)
            .multiply(BigDecimal.valueOf(multiplier))
            .setScale(maximumFractionDigits, RoundingMode.HALF_UP);
        String plain = decimal.toPlainString();
        int dot = plain.indexOf('.');
        String integerPart;
        String fractionPart;
        if (dot < 0)
        {
            integerPart = plain;
            fractionPart = "";
        }
        else
        {
            integerPart = plain.substring(0, dot);
            fractionPart = plain.substring(dot + 1);
        }
        while (fractionPart.length() > minimumFractionDigits &&
               fractionPart.charAt(fractionPart.length() - 1) == '0')
        {
            fractionPart = fractionPart.substring(0, fractionPart.length() - 1);
        }
        while (fractionPart.length() < minimumFractionDigits)
        {
            fractionPart = fractionPart + "0";
        }
        String result = formatInteger(integerPart);
        if (fractionPart.length() > 0)
        {
            result = result + decimalSeparator + fractionPart;
        }
        if (negative)
        {
            return negativePrefix + result + negativeSuffix;
        }
        return prefix + result + suffix;
    }

    public String format(Object value)
    {
        if (value instanceof Number)
        {
            if (value instanceof Double || value instanceof Float)
            {
                return format(((Number)value).doubleValue());
            }
            return format(((Number)value).longValue());
        }
        throw new IllegalArgumentException("Cannot format given Object as a Number");
    }

    private String formatWhole(String digits)
    {
        boolean negative = digits.length() > 0 && digits.charAt(0) == '-';
        if (negative)
        {
            digits = digits.substring(1);
        }
        if (multiplier != 1)
        {
            BigDecimal scaled = new BigDecimal(digits)
                .multiply(BigDecimal.valueOf(multiplier))
                .setScale(minimumFractionDigits, RoundingMode.HALF_UP);
            return format(scaled.doubleValue() * (negative ? -1 : 1));
        }
        StringBuilder fraction = new StringBuilder();
        while (fraction.length() < minimumFractionDigits)
        {
            fraction.append('0');
        }
        String result = formatInteger(digits);
        if (fraction.length() > 0)
        {
            result = result + decimalSeparator + fraction.toString();
        }
        if (negative)
        {
            return negativePrefix + result + negativeSuffix;
        }
        return prefix + result + suffix;
    }

    protected String formatInteger(String digits)
    {
        StringBuilder out = new StringBuilder();
        int i = digits.length();
        while (i < minimumIntegerDigits)
        {
            out.append('0');
            i = i + 1;
        }
        out.append(digits);
        String padded = out.toString();
        if (!groupingUsed || groupingSize <= 0 || padded.length() <= groupingSize)
        {
            return padded;
        }
        StringBuilder grouped = new StringBuilder();
        int first = padded.length() % groupingSize;
        if (first == 0)
        {
            first = groupingSize;
        }
        grouped.append(padded.substring(0, first));
        int pos = first;
        while (pos < padded.length())
        {
            grouped.append(groupingSeparator);
            grouped.append(padded.substring(pos, pos + groupingSize));
            pos = pos + groupingSize;
        }
        return grouped.toString();
    }

    public Number parse(String text) throws ParseException
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        String s = text.trim();
        boolean negative = false;
        if (s.startsWith(negativePrefix) && negativePrefix.length() > 0)
        {
            negative = true;
            s = s.substring(negativePrefix.length());
        }
        else if (prefix.length() > 0 && s.startsWith(prefix))
        {
            s = s.substring(prefix.length());
        }
        if (negative && negativeSuffix.length() > 0 && s.endsWith(negativeSuffix))
        {
            s = s.substring(0, s.length() - negativeSuffix.length());
        }
        else if (!negative && suffix.length() > 0 && s.endsWith(suffix))
        {
            s = s.substring(0, s.length() - suffix.length());
        }
        StringBuilder cleaned = new StringBuilder();
        int i = 0;
        while (i < s.length())
        {
            char c = s.charAt(i);
            if (groupingUsed && c == groupingSeparator.charAt(0))
            {
                i = i + 1;
                continue;
            }
            if (c == decimalSeparator.charAt(0))
            {
                cleaned.append('.');
                i = i + 1;
                continue;
            }
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == 'e' || c == 'E')
            {
                cleaned.append(c);
                i = i + 1;
                continue;
            }
            throw new ParseException("Unparseable number: \"" + text + "\"", i);
        }
        if (cleaned.length() == 0)
        {
            throw new ParseException("Unparseable number: \"" + text + "\"", 0);
        }
        BigDecimal value = new BigDecimal(cleaned.toString());
        if (negative)
        {
            value = value.negate();
        }
        if (multiplier != 1)
        {
            value = value.divide(BigDecimal.valueOf(multiplier), 10, RoundingMode.HALF_UP);
        }
        if (parseIntegerOnly || value.scale() <= 0)
        {
            return Long.valueOf(value.longValue());
        }
        return Double.valueOf(value.doubleValue());
    }

    public Number parse(String text, ParsePosition pos)
    {
        if (text == null || pos == null)
        {
            throw new NullPointerException();
        }
        int start = pos.getIndex();
        try
        {
            Number value = parse(text.substring(start));
            pos.setIndex(text.length());
            return value;
        }
        catch (ParseException error)
        {
            pos.setErrorIndex(start);
            return null;
        }
    }
}
