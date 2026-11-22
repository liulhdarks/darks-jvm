package java.util;

public final class StringJoiner
{
    private final String prefix;
    private final String delimiter;
    private final String suffix;
    private String emptyValue;
    private StringBuilder builder;
    private boolean hasElements;

    public StringJoiner(CharSequence delimiter)
    {
        this(delimiter, "", "");
    }

    public StringJoiner(CharSequence delimiter, CharSequence prefix,
                        CharSequence suffix)
    {
        if (delimiter == null || prefix == null || suffix == null)
        {
            throw new NullPointerException();
        }
        this.prefix = prefix.toString();
        this.delimiter = delimiter.toString();
        this.suffix = suffix.toString();
        this.emptyValue = this.prefix + this.suffix;
        this.builder = new StringBuilder();
        this.builder.append(this.prefix);
    }

    public StringJoiner setEmptyValue(CharSequence emptyValue)
    {
        if (emptyValue == null)
        {
            throw new NullPointerException();
        }
        this.emptyValue = emptyValue.toString();
        return this;
    }

    public StringJoiner add(CharSequence newElement)
    {
        if (newElement == null)
        {
            newElement = "null";
        }
        if (hasElements)
        {
            builder.append(delimiter);
        }
        builder.append(newElement.toString());
        hasElements = true;
        return this;
    }

    public StringJoiner merge(StringJoiner other)
    {
        if (other == null || !other.hasElements)
        {
            return this;
        }
        add(other.builder.substring(other.prefix.length(),
                                    other.builder.length()));
        return this;
    }

    public int length()
    {
        if (!hasElements)
        {
            return emptyValue.length();
        }
        return builder.length() + suffix.length();
    }

    public String toString()
    {
        if (!hasElements)
        {
            return emptyValue;
        }
        StringBuilder out = new StringBuilder();
        out.append(builder.toString());
        out.append(suffix);
        return out.toString();
    }
}
