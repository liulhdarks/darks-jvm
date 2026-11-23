package javax.naming;

import java.util.Vector;

public class CompositeName
{
    private final Vector<String> parts = new Vector<String>();

    public CompositeName(String name)
    {
        String value = name == null ? "" : name;
        int start = 0;
        for (int i = 0; i <= value.length(); i++)
        {
            if (i == value.length() || value.charAt(i) == '/')
            {
                if (i > start)
                {
                    parts.add(value.substring(start, i));
                }
                start = i + 1;
            }
        }
    }

    public int size()
    {
        return parts.size();
    }

    public String get(int index)
    {
        return parts.get(index);
    }

    public java.util.Enumeration<String> getAll()
    {
        return parts.elements();
    }

    public String toString()
    {
        String text = "";
        for (int i = 0; i < parts.size(); i++)
        {
            text = text + parts.get(i) + "/";
        }
        return text;
    }
}
