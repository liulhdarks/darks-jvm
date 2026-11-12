package java.lang;

public class Throwable
{
    private String detailMessage;
    private Throwable[] suppressed;
    private int suppressedCount;

    public Throwable()
    {
    }

    public Throwable(String message)
    {
        detailMessage = message;
    }

    public String getMessage()
    {
        return detailMessage;
    }

    public String toString()
    {
        String name = getClass().getName();
        if (detailMessage == null)
        {
            return name;
        }
        return name + ": " + detailMessage;
    }

    public void printStackTrace()
    {
        printStackTrace0(toString());
    }

    public final synchronized void addSuppressed(Throwable exception)
    {
        if (exception == this)
        {
            throw new IllegalArgumentException("self-suppression not permitted");
        }
        if (exception == null)
        {
            throw new NullPointerException("suppressed exception");
        }
        if (suppressed == null)
        {
            suppressed = new Throwable[4];
        }
        else if (suppressedCount == suppressed.length)
        {
            Throwable[] grown = new Throwable[suppressed.length * 2];
            for (int i = 0; i < suppressedCount; i++)
            {
                grown[i] = suppressed[i];
            }
            suppressed = grown;
        }
        suppressed[suppressedCount++] = exception;
    }

    public final Throwable[] getSuppressed()
    {
        Throwable[] result = new Throwable[suppressedCount];
        for (int i = 0; i < suppressedCount; i++)
        {
            result[i] = suppressed[i];
        }
        return result;
    }

    private native void printStackTrace0(String text);
}
