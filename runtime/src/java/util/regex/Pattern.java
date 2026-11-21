package java.util.regex;

public final class Pattern
{
    public static final int UNIX_LINES = 1;
    public static final int CASE_INSENSITIVE = 2;
    public static final int COMMENTS = 4;
    public static final int MULTILINE = 8;
    public static final int LITERAL = 16;
    public static final int DOTALL = 32;
    public static final int UNICODE_CASE = 64;
    public static final int CANON_EQ = 128;

    private final String regex;
    private final int flags;
    private final long handle;
    private final int groups;

    private Pattern(String regex, int flags)
    {
        this.regex = regex;
        this.flags = flags;
        long compiled = nativeCompile(regex, flags);
        this.handle = compiled;
        this.groups = compiled == 0 ? 0 : nativeGroupCount(compiled);
    }

    public static Pattern compile(String regex)
    {
        return compile(regex, 0);
    }

    public static Pattern compile(String regex, int flags)
    {
        if (regex == null)
        {
            throw new NullPointerException("regex");
        }
        return new Pattern(regex, flags);
    }

    public static boolean matches(String regex, CharSequence input)
    {
        return compile(regex).matcher(input).matches();
    }

    public String pattern()
    {
        return regex;
    }

    public int flags()
    {
        return flags;
    }

    public int groupCount()
    {
        return groups;
    }

    public Matcher matcher(CharSequence input)
    {
        if (input == null)
        {
            throw new NullPointerException("input");
        }
        return new Matcher(this, input.toString());
    }

    public String[] split(CharSequence input)
    {
        return split(input, 0);
    }

    public String[] split(CharSequence input, int limit)
    {
        if (input == null)
        {
            throw new NullPointerException("input");
        }
        String text = input.toString();
        if (regex.length() == 0)
        {
            return splitEmpty(text, limit);
        }
        Matcher matcher = matcher(text);
        java.util.ArrayList<String> parts = new java.util.ArrayList<String>();
        int last = 0;
        while (matcher.find())
        {
            if (limit > 0 && parts.size() == limit - 1)
            {
                break;
            }
            parts.add(text.substring(last, matcher.start()));
            last = matcher.end();
            if (matcher.start() == matcher.end() && matcher.end() == text.length())
            {
                break;
            }
        }
        parts.add(text.substring(last));
        int size = parts.size();
        if (limit == 0)
        {
            while (size > 0 && parts.get(size - 1).length() == 0)
            {
                size = size - 1;
            }
        }
        String[] result = new String[size];
        int i = 0;
        while (i < size)
        {
            result[i] = parts.get(i);
            i = i + 1;
        }
        return result;
    }

    private static String[] splitEmpty(String text, int limit)
    {
        int length = text.length();
        if (limit == 1)
        {
            String[] single = new String[1];
            single[0] = text;
            return single;
        }
        if (length == 0)
        {
            String[] empty = new String[1];
            empty[0] = "";
            return empty;
        }
        if (limit > 0)
        {
            int head = length < limit - 1 ? length : limit - 1;
            String[] result = new String[head + 1];
            int i = 0;
            while (i < head)
            {
                result[i] = text.substring(i, i + 1);
                i = i + 1;
            }
            result[head] = text.substring(head);
            return result;
        }
        String[] result = new String[length];
        int i = 0;
        while (i < length)
        {
            result[i] = text.substring(i, i + 1);
            i = i + 1;
        }
        return result;
    }

    public static String quote(String text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        StringBuilder escaped = new StringBuilder();
        int i = 0;
        while (i < text.length())
        {
            char c = text.charAt(i);
            if ("\\^$|()[]{}.*+?-".indexOf(c) >= 0)
            {
                escaped.append('\\');
            }
            escaped.append(c);
            i = i + 1;
        }
        return escaped.toString();
    }

    public String toString()
    {
        return regex;
    }

    long nativeHandle()
    {
        return handle;
    }

    long[] nativeMatch(String input, int start, boolean anchored)
    {
        return nativeMatch(handle, input, start, anchored);
    }

    private static native long nativeCompile(String regex, int flags);

    private static native void nativeFree(long handle);

    private static native int nativeGroupCount(long handle);

    private static native long[] nativeMatch(long handle, String input, int start,
                                             boolean anchored);
}
