package java.util;

public class Arrays
{
    private Arrays()
    {
    }

    public static <T> List<T> asList(T... values)
    {
        ArrayList<T> list = new ArrayList<T>(values.length > 0 ? values.length : 1);
        for (int i = 0; i < values.length; i++)
        {
            list.add(values[i]);
        }
        return list;
    }

    public static String toString(Object[] values)
    {
        if (values == null)
        {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(String.valueOf(values[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(int[] values)
    {
        if (values == null)
        {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(long[] values)
    {
        if (values == null)
        {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(short[] values)
    {
        if (values == null)
        {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(byte[] values)
    {
        if (values == null)
        {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(char[] values)
    {
        if (values == null)
        {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(boolean[] values)
    {
        if (values == null)
        {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(float[] values)
    {
        if (values == null)
        {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(double[] values)
    {
        if (values == null)
        {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static int[] copyOf(int[] values, int length)
    {
        int[] result = new int[length];
        int limit = values.length < length ? values.length : length;
        for (int i = 0; i < limit; i++)
        {
            result[i] = values[i];
        }
        return result;
    }

    public static Object[] copyOf(Object[] values, int length)
    {
        Object[] result = new Object[length];
        int limit = values.length < length ? values.length : length;
        for (int i = 0; i < limit; i++)
        {
            result[i] = values[i];
        }
        return result;
    }
}
