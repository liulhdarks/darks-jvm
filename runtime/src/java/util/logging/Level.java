package java.util.logging;

public class Level implements Comparable<Level>
{
    public static final Level OFF = new Level("OFF", Integer.MAX_VALUE);
    public static final Level SEVERE = new Level("SEVERE", 1000);
    public static final Level WARNING = new Level("WARNING", 900);
    public static final Level INFO = new Level("INFO", 800);
    public static final Level CONFIG = new Level("CONFIG", 700);
    public static final Level FINE = new Level("FINE", 500);
    public static final Level FINER = new Level("FINER", 400);
    public static final Level FINEST = new Level("FINEST", 300);
    public static final Level ALL = new Level("ALL", Integer.MIN_VALUE);

    private final String name;
    private final int value;

    protected Level(String name, int value)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        this.name = name;
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public int intValue()
    {
        return value;
    }

    public int compareTo(Level other)
    {
        return value - other.value;
    }

    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof Level))
        {
            return false;
        }
        Level that = (Level)other;
        return value == that.value && name.equals(that.name);
    }

    public int hashCode()
    {
        return name.hashCode() ^ value;
    }

    public String toString()
    {
        return name;
    }

    public static Level parse(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        String text = name.trim();
        if (text.length() == 0)
        {
            throw new IllegalArgumentException("empty level name");
        }
        if (text.equalsIgnoreCase(OFF.getName()))
        {
            return OFF;
        }
        if (text.equalsIgnoreCase(SEVERE.getName()))
        {
            return SEVERE;
        }
        if (text.equalsIgnoreCase(WARNING.getName()))
        {
            return WARNING;
        }
        if (text.equalsIgnoreCase(INFO.getName()))
        {
            return INFO;
        }
        if (text.equalsIgnoreCase(CONFIG.getName()))
        {
            return CONFIG;
        }
        if (text.equalsIgnoreCase(FINE.getName()))
        {
            return FINE;
        }
        if (text.equalsIgnoreCase(FINER.getName()))
        {
            return FINER;
        }
        if (text.equalsIgnoreCase(FINEST.getName()))
        {
            return FINEST;
        }
        if (text.equalsIgnoreCase(ALL.getName()))
        {
            return ALL;
        }
        try
        {
            return new Level(text, Integer.parseInt(text));
        }
        catch (NumberFormatException error)
        {
            throw new IllegalArgumentException("unknown level name: " + name);
        }
    }
}
