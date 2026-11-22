package java.text;

/**
 * Pattern based number format.  Supported placeholders: 0 # . , % and quoted
 * literals, with an optional negative subpattern after ';'.
 */
public class DecimalFormat extends NumberFormat
{
    private String pattern;

    public DecimalFormat()
    {
        this("#,##0.###");
    }

    public DecimalFormat(String pattern)
    {
        applyPattern(pattern);
    }

    public DecimalFormat(String pattern, DecimalFormatSymbols symbols)
    {
        this(pattern);
    }

    public void applyPattern(String pattern)
    {
        if (pattern == null)
        {
            throw new NullPointerException("pattern");
        }
        this.pattern = pattern;
        String positive = pattern;
        String negative = null;
        int semi = pattern.indexOf(';');
        if (semi >= 0)
        {
            positive = pattern.substring(0, semi);
            negative = pattern.substring(semi + 1);
        }
        applySubPattern(positive, false);
        if (negative != null)
        {
            applySubPattern(negative, true);
        }
        else
        {
            negativePrefix = "-" + prefix;
            negativeSuffix = suffix;
        }
    }

    private void applySubPattern(String sub, boolean negative)
    {
        StringBuilder prefixBuf = new StringBuilder();
        StringBuilder suffixBuf = new StringBuilder();
        StringBuilder integerDigits = new StringBuilder();
        StringBuilder fractionDigits = new StringBuilder();
        boolean inFraction = false;
        boolean seenDigit = false;
        boolean afterDigit = false;
        int minInteger = 0;
        int minFraction = 0;
        int maxFraction = 0;
        boolean grouping = false;
        int groupingSize = 0;
        int currentGroup = 0;
        int multiplierValue = 1;
        int i = 0;
        while (i < sub.length())
        {
            char c = sub.charAt(i);
            if (c == '\'')
            {
                i = i + 1;
                if (i < sub.length() && sub.charAt(i) == '\'')
                {
                    appendLiteral(prefixBuf, suffixBuf, seenDigit, afterDigit, '\'');
                    i = i + 1;
                    continue;
                }
                while (i < sub.length() && sub.charAt(i) != '\'')
                {
                    appendLiteral(prefixBuf, suffixBuf, seenDigit, afterDigit, sub.charAt(i));
                    i = i + 1;
                }
                i = i + 1;
                continue;
            }
            if (c == '0' || c == '#')
            {
                seenDigit = true;
                afterDigit = true;
                if (inFraction)
                {
                    fractionDigits.append(c);
                    maxFraction = maxFraction + 1;
                }
                else
                {
                    integerDigits.append(c);
                    if (c == '0')
                    {
                        minInteger = minInteger + 1;
                    }
                    currentGroup = currentGroup + 1;
                }
            }
            else if (c == '.')
            {
                seenDigit = true;
                afterDigit = true;
                inFraction = true;
            }
            else if (c == ',')
            {
                grouping = true;
                if (!inFraction)
                {
                    currentGroup = 0;
                }
            }
            else if (c == '%')
            {
                multiplierValue = 100;
                appendLiteral(prefixBuf, suffixBuf, seenDigit, afterDigit, '%');
            }
            else
            {
                appendLiteral(prefixBuf, suffixBuf, seenDigit, afterDigit, c);
            }
            i = i + 1;
        }
        int lastZero = -1;
        int k = 0;
        while (k < fractionDigits.length())
        {
            if (fractionDigits.charAt(k) == '0')
            {
                lastZero = k;
            }
            else if (fractionDigits.charAt(k) == '#')
            {
                if (lastZero >= 0)
                {
                    minFraction = lastZero + 1;
                }
            }
            k = k + 1;
        }
        if (fractionDigits.length() > 0 && fractionDigits.charAt(0) == '0')
        {
            int zeroRun = 0;
            while (zeroRun < fractionDigits.length() &&
                   fractionDigits.charAt(zeroRun) == '0')
            {
                zeroRun = zeroRun + 1;
            }
            if (zeroRun > minFraction)
            {
                minFraction = zeroRun;
            }
        }
        if (minInteger == 0)
        {
            minInteger = 1;
        }
        if (grouping)
        {
            groupingSize = currentGroup;
        }
        if (groupingSize == 0)
        {
            groupingSize = 3;
        }
        groupingUsed = grouping;
        this.groupingSize = groupingSize;
        this.minimumIntegerDigits = minInteger;
        this.maximumFractionDigits = maxFraction;
        this.minimumFractionDigits = minFraction;
        this.multiplier = multiplierValue;
        if (negative)
        {
            negativePrefix = prefixBuf.toString();
            negativeSuffix = suffixBuf.toString();
        }
        else
        {
            prefix = prefixBuf.toString();
            suffix = suffixBuf.toString();
        }
    }

    private static void appendLiteral(StringBuilder prefixBuf, StringBuilder suffixBuf,
                                      boolean seenDigit, boolean afterDigit, char c)
    {
        if (!seenDigit)
        {
            prefixBuf.append(c);
        }
        else
        {
            suffixBuf.append(c);
        }
    }

    public String toPattern()
    {
        return pattern;
    }

    public void setGroupingSize(int size)
    {
        groupingSize = size;
    }

    public int getGroupingSize()
    {
        return groupingSize;
    }
}
