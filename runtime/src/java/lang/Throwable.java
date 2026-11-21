package java.lang;

public class Throwable
{
    private String detailMessage;
    private Throwable cause;
    private boolean causeInitialized;
    private StackTraceElement[] stackTrace;
    private Throwable[] suppressed;
    private int suppressedCount;

    public Throwable()
    {
        fillInStackTrace();
    }

    public Throwable(String message)
    {
        detailMessage = message;
        fillInStackTrace();
    }

    public Throwable(String message, Throwable cause)
    {
        detailMessage = message;
        this.cause = cause;
        causeInitialized = true;
        fillInStackTrace();
    }

    public Throwable(Throwable cause)
    {
        detailMessage = cause == null ? null : cause.toString();
        this.cause = cause;
        causeInitialized = true;
        fillInStackTrace();
    }

    public String getMessage()
    {
        return detailMessage;
    }

    public String getLocalizedMessage()
    {
        return detailMessage;
    }

    public Throwable getCause()
    {
        return cause == this ? null : cause;
    }

    public synchronized Throwable initCause(Throwable value)
    {
        if (causeInitialized)
        {
            throw new IllegalStateException("cause already initialized");
        }
        if (value == this)
        {
            throw new IllegalArgumentException("self-causation not permitted");
        }
        cause = value;
        causeInitialized = true;
        return this;
    }

    public String toString()
    {
        String name = getClass().getName();
        String message = getLocalizedMessage();
        if (message == null)
        {
            return name;
        }
        return name + ": " + message;
    }

    public Throwable fillInStackTrace()
    {
        String[] raw = captureStack0();
        if (raw == null || raw.length == 0)
        {
            stackTrace = new StackTraceElement[0];
            return this;
        }
        StackTraceElement[] frames = new StackTraceElement[raw.length];
        for (int i = 0; i < raw.length; i++)
        {
            frames[i] = parseFrame(raw[i]);
        }
        stackTrace = frames;
        return this;
    }

    public StackTraceElement[] getStackTrace()
    {
        if (stackTrace == null)
        {
            fillInStackTrace();
        }
        StackTraceElement[] copy = new StackTraceElement[stackTrace.length];
        for (int i = 0; i < stackTrace.length; i++)
        {
            copy[i] = stackTrace[i];
        }
        return copy;
    }

    public void setStackTrace(StackTraceElement[] frames)
    {
        if (frames == null)
        {
            throw new NullPointerException("stackTrace");
        }
        StackTraceElement[] copy = new StackTraceElement[frames.length];
        for (int i = 0; i < frames.length; i++)
        {
            if (frames[i] == null)
            {
                throw new NullPointerException("stackTrace[" + i + "]");
            }
            copy[i] = frames[i];
        }
        stackTrace = copy;
    }

    private static StackTraceElement parseFrame(String text)
    {
        int first = text.indexOf('\t');
        int second = first < 0 ? -1 : text.indexOf('\t', first + 1);
        int third = second < 0 ? -1 : text.indexOf('\t', second + 1);
        if (second < 0 || third < 0)
        {
            return new StackTraceElement("<unknown>", text, null, -1);
        }
        String className = text.substring(0, first);
        String methodName = text.substring(first + 1, second);
        String fileName = text.substring(second + 1, third);
        String lineText = text.substring(third + 1);
        int line = -1;
        try
        {
            line = Integer.parseInt(lineText);
        }
        catch (NumberFormatException ignored)
        {
        }
        if (fileName.length() == 0)
        {
            fileName = null;
        }
        return new StackTraceElement(className, methodName, fileName, line);
    }

    public void printStackTrace()
    {
        printStackTrace0(formatStackTrace());
    }

    public void printStackTrace(java.io.PrintStream stream)
    {
        if (stream == null)
        {
            throw new NullPointerException("s");
        }
        stream.print(formatStackTrace());
    }

    public void printStackTrace(java.io.PrintWriter writer)
    {
        if (writer == null)
        {
            throw new NullPointerException("s");
        }
        writer.print(formatStackTrace());
    }

    private String formatStackTrace()
    {
        StringBuilder buffer = new StringBuilder();
        Throwable current = this;
        while (current != null)
        {
            buffer.append(current.toString()).append('\n');
            StackTraceElement[] frames = current.getStackTrace();
            for (int i = 0; i < frames.length; i++)
            {
                buffer.append("\tat ").append(frames[i].toString()).append('\n');
            }
            Throwable next = current.getCause();
            if (next != null)
            {
                buffer.append("Caused by: ");
            }
            current = next;
        }
        return buffer.toString();
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

    private static native String[] captureStack0();

    private native void printStackTrace0(String text);
}
