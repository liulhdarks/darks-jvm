package java.util.regex;

public final class Matcher
{
    private final Pattern pattern;
    private String text;
    private int searchFrom;
    private int[] bounds;
    private int appendPosition;

    Matcher(Pattern pattern, String text)
    {
        this.pattern = pattern;
        this.text = text;
        this.searchFrom = 0;
        this.bounds = new int[0];
        this.appendPosition = 0;
    }

    public Pattern pattern()
    {
        return pattern;
    }

    public Matcher reset()
    {
        searchFrom = 0;
        bounds = new int[0];
        appendPosition = 0;
        return this;
    }

    public Matcher reset(CharSequence input)
    {
        if (input == null)
        {
            throw new NullPointerException("input");
        }
        text = input.toString();
        return reset();
    }

    public String input()
    {
        return text;
    }

    public boolean find()
    {
        while (searchFrom <= text.length())
        {
            long[] match = pattern.nativeMatch(text, searchFrom, false);
            if (match == null)
            {
                searchFrom = text.length() + 1;
                return false;
            }
            // Keep the native result conversion inline: passing a long[]
            // to a helper would hit the VM's trivial-getter slot accounting.
            bounds = new int[match.length];
            int k = 0;
            while (k < match.length)
            {
                bounds[k] = (int)match[k];
                k = k + 1;
            }
            int start = start();
            int end = end();
            searchFrom = end > start ? end : end + 1;
            return true;
        }
        return false;
    }

    public boolean find(int start)
    {
        if (start < 0 || start > text.length())
        {
            throw new IndexOutOfBoundsException("start");
        }
        searchFrom = start;
        return find();
    }

    public boolean matches()
    {
        long[] match = pattern.nativeMatch(text, 0, true);
        if (match == null || match[1] != text.length())
        {
            return false;
        }
        bounds = new int[match.length];
        int k = 0;
        while (k < match.length)
        {
            bounds[k] = (int)match[k];
            k = k + 1;
        }
        searchFrom = end();
        return true;
    }

    public boolean lookingAt()
    {
        long[] match = pattern.nativeMatch(text, 0, true);
        if (match == null)
        {
            return false;
        }
        bounds = new int[match.length];
        int k = 0;
        while (k < match.length)
        {
            bounds[k] = (int)match[k];
            k = k + 1;
        }
        searchFrom = end();
        return true;
    }

    public int groupCount()
    {
        return pattern.groupCount();
    }

    public int start()
    {
        return start(0);
    }

    public int start(int group)
    {
        checkGroup(group);
        return bounds[group * 2];
    }

    public int end()
    {
        return end(0);
    }

    public int end(int group)
    {
        checkGroup(group);
        return bounds[group * 2 + 1];
    }

    public String group()
    {
        return group(0);
    }

    public String group(int group)
    {
        checkGroup(group);
        int start = bounds[group * 2];
        int end = bounds[group * 2 + 1];
        if (start < 0)
        {
            return null;
        }
        return text.substring(start, end);
    }

    private void checkGroup(int group)
    {
        if (group < 0 || group > groupCount())
        {
            throw new IndexOutOfBoundsException("group " + group);
        }
        if (bounds.length < (group + 1) * 2)
        {
            throw new IllegalStateException("no match found");
        }
    }

    public Matcher appendReplacement(StringBuilder target, String replacement)
    {
        if (bounds.length < 2)
        {
            throw new IllegalStateException("no match found");
        }
        int start = start();
        int end = end();
        target.append(text.substring(appendPosition, start));
        target.append(expandReplacement(replacement));
        appendPosition = end;
        return this;
    }

    public Matcher appendReplacement(StringBuffer target, String replacement)
    {
        if (bounds.length < 2)
        {
            throw new IllegalStateException("no match found");
        }
        int start = start();
        int end = end();
        target.append(text.substring(appendPosition, start));
        target.append(expandReplacement(replacement));
        appendPosition = end;
        return this;
    }

    public StringBuilder appendTail(StringBuilder target)
    {
        target.append(text.substring(appendPosition));
        return target;
    }

    public StringBuffer appendTail(StringBuffer target)
    {
        target.append(text.substring(appendPosition));
        return target;
    }

    public String replaceAll(String replacement)
    {
        return replace(replacement, true);
    }

    public String replaceFirst(String replacement)
    {
        return replace(replacement, false);
    }

    private String replace(String replacement, boolean all)
    {
        if (replacement == null)
        {
            throw new NullPointerException("replacement");
        }
        reset();
        if (groupCount() == 0)
        {
            return replaceFast(replacement, all);
        }
        StringBuilder result = new StringBuilder();
        int last = 0;
        boolean found = find();
        while (found)
        {
            result.append(text.substring(last, start()));
            result.append(expandReplacement(replacement));
            last = end();
            if (!all)
            {
                break;
            }
            if (start() == end())
            {
                if (end() < text.length())
                {
                    result.append(text.charAt(end()));
                    last = end() + 1;
                }
            }
            found = find();
        }
        result.append(text.substring(last));
        return result.toString();
    }

    // Fast path for patterns without capturing groups: avoids building
    // substrings for group expansion while still honouring "$" escapes.
    private String replaceFast(String replacement, boolean all)
    {
        StringBuilder result = new StringBuilder();
        int last = 0;
        boolean found = find();
        while (found)
        {
            result.append(text.substring(last, start()));
            result.append(expandReplacement(replacement));
            last = end();
            if (!all)
            {
                break;
            }
            if (start() == end() && end() < text.length())
            {
                result.append(text.charAt(end()));
                last = end() + 1;
            }
            found = find();
        }
        result.append(text.substring(last));
        return result.toString();
    }

    private String expandReplacement(String replacement)
    {
        StringBuilder target = new StringBuilder();
        int i = 0;
        while (i < replacement.length())
        {
            char c = replacement.charAt(i);
            if (c == '\\')
            {
                i = i + 1;
                if (i < replacement.length())
                {
                    target.append(replacement.charAt(i));
                    i = i + 1;
                }
                continue;
            }
            if (c == '$' && i + 1 < replacement.length() &&
                replacement.charAt(i + 1) >= '0' && replacement.charAt(i + 1) <= '9')
            {
                int group = replacement.charAt(i + 1) - '0';
                i = i + 2;
                if (i < replacement.length() && replacement.charAt(i) >= '0' &&
                    replacement.charAt(i) <= '9')
                {
                    int two = group * 10 + (replacement.charAt(i) - '0');
                    if (two <= groupCount())
                    {
                        group = two;
                        i = i + 1;
                    }
                }
                if (group > groupCount())
                {
                    throw new IndexOutOfBoundsException("group " + group);
                }
                String value = group(group);
                if (value != null)
                {
                    target.append(value);
                }
                continue;
            }
            target.append(c);
            i = i + 1;
        }
        return target.toString();
    }

    public static String quoteReplacement(String text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length())
        {
            char c = text.charAt(i);
            if (c == '\\' || c == '$')
            {
                result.append('\\');
            }
            result.append(c);
            i = i + 1;
        }
        return result.toString();
    }
}
