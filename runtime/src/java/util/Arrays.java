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

    public static void sort(int[] values)
    {
        for (int i = 1; i < values.length; i++)
        {
            int value = values[i];
            int j = i - 1;
            while (j >= 0 && values[j] > value)
            {
                values[j + 1] = values[j];
                j--;
            }
            values[j + 1] = value;
        }
    }

    public static void sort(long[] values)
    {
        for (int i = 1; i < values.length; i++)
        {
            long value = values[i];
            int j = i - 1;
            while (j >= 0 && values[j] > value)
            {
                values[j + 1] = values[j];
                j--;
            }
            values[j + 1] = value;
        }
    }

    public static void sort(double[] values)
    {
        for (int i = 1; i < values.length; i++)
        {
            double value = values[i];
            int j = i - 1;
            while (j >= 0 && values[j] > value)
            {
                values[j + 1] = values[j];
                j--;
            }
            values[j + 1] = value;
        }
    }

    public static void sort(byte[] values)
    {
        for (int i = 1; i < values.length; i++)
        {
            byte value = values[i];
            int j = i - 1;
            while (j >= 0 && values[j] > value)
            {
                values[j + 1] = values[j];
                j--;
            }
            values[j + 1] = value;
        }
    }

    public static void sort(char[] values)
    {
        for (int i = 1; i < values.length; i++)
        {
            char value = values[i];
            int j = i - 1;
            while (j >= 0 && values[j] > value)
            {
                values[j + 1] = values[j];
                j--;
            }
            values[j + 1] = value;
        }
    }

    public static void sort(Object[] values)
    {
        for (int i = 1; i < values.length; i++)
        {
            Object value = values[i];
            int j = i - 1;
            while (j >= 0 && ((Comparable)values[j]).compareTo(value) > 0)
            {
                values[j + 1] = values[j];
                j--;
            }
            values[j + 1] = value;
        }
    }

    public static <T> void sort(T[] values, Comparator<? super T> comparator)
    {
        for (int i = 1; i < values.length; i++)
        {
            T value = values[i];
            int j = i - 1;
            while (j >= 0 && comparator.compare(values[j], value) > 0)
            {
                values[j + 1] = values[j];
                j--;
            }
            values[j + 1] = value;
        }
    }

    public static boolean equals(int[] a, int[] b)
    {
        if (a == b)
        {
            return true;
        }
        if (a == null || b == null || a.length != b.length)
        {
            return false;
        }
        for (int i = 0; i < a.length; i++)
        {
            if (a[i] != b[i])
            {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(long[] a, long[] b)
    {
        if (a == b)
        {
            return true;
        }
        if (a == null || b == null || a.length != b.length)
        {
            return false;
        }
        for (int i = 0; i < a.length; i++)
        {
            if (a[i] != b[i])
            {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(Object[] a, Object[] b)
    {
        if (a == b)
        {
            return true;
        }
        if (a == null || b == null || a.length != b.length)
        {
            return false;
        }
        for (int i = 0; i < a.length; i++)
        {
            Object x = a[i];
            Object y = b[i];
            if (!(x == null ? y == null : x.equals(y)))
            {
                return false;
            }
        }
        return true;
    }

    public static long[] copyOf(long[] values, int length)
    {
        long[] result = new long[length];
        int limit = values.length < length ? values.length : length;
        for (int i = 0; i < limit; i++)
        {
            result[i] = values[i];
        }
        return result;
    }

    public static double[] copyOf(double[] values, int length)
    {
        double[] result = new double[length];
        int limit = values.length < length ? values.length : length;
        for (int i = 0; i < limit; i++)
        {
            result[i] = values[i];
        }
        return result;
    }

    public static byte[] copyOf(byte[] values, int length)
    {
        byte[] result = new byte[length];
        int limit = values.length < length ? values.length : length;
        for (int i = 0; i < limit; i++)
        {
            result[i] = values[i];
        }
        return result;
    }

    public static char[] copyOf(char[] values, int length)
    {
        char[] result = new char[length];
        int limit = values.length < length ? values.length : length;
        for (int i = 0; i < limit; i++)
        {
            result[i] = values[i];
        }
        return result;
    }

    public static boolean[] copyOf(boolean[] values, int length)
    {
        boolean[] result = new boolean[length];
        int limit = values.length < length ? values.length : length;
        for (int i = 0; i < limit; i++)
        {
            result[i] = values[i];
        }
        return result;
    }
}
