package java.util;

public class Arrays
{
    private Arrays()
    {
    }

    public static <T> List<T> asList(T... values)
    {
        return new ArrayAsList<T>(values);
    }

    public static <T> java.util.stream.Stream<T> stream(T[] values)
    {
        return java.util.stream.Stream.of(values);
    }

    public static <T> java.util.stream.Stream<T> stream(T[] values, int start, int end)
    {
        int length = end - start;
        if (length < 0)
        {
            throw new IllegalArgumentException("start > end");
        }
        T[] slice = (T[])new Object[length];
        for (int i = 0; i < length; i++)
        {
            slice[i] = values[start + i];
        }
        return java.util.stream.Stream.of(slice);
    }

    public static java.util.stream.IntStream stream(int[] values)
    {
        return java.util.stream.IntStream.of(values);
    }

    public static void fill(Object[] values, Object value)
    {
        for (int i = 0; i < values.length; i++)
        {
            values[i] = value;
        }
    }

    public static void fill(Object[] values, int start, int end, Object value)
    {
        for (int i = start; i < end && i < values.length; i++)
        {
            values[i] = value;
        }
    }

    public static void fill(boolean[] values, boolean value)
    {
        for (int i = 0; i < values.length; i++)
        {
            values[i] = value;
        }
    }

    public static void fill(byte[] values, byte value)
    {
        for (int i = 0; i < values.length; i++)
        {
            values[i] = value;
        }
    }

    public static void fill(char[] values, char value)
    {
        for (int i = 0; i < values.length; i++)
        {
            values[i] = value;
        }
    }

    public static void fill(short[] values, short value)
    {
        for (int i = 0; i < values.length; i++)
        {
            values[i] = value;
        }
    }

    public static void fill(int[] values, int value)
    {
        for (int i = 0; i < values.length; i++)
        {
            values[i] = value;
        }
    }

    public static void fill(int[] values, int start, int end, int value)
    {
        for (int i = start; i < end && i < values.length; i++)
        {
            values[i] = value;
        }
    }

    public static void fill(long[] values, long value)
    {
        for (int i = 0; i < values.length; i++)
        {
            values[i] = value;
        }
    }

    public static void fill(float[] values, float value)
    {
        for (int i = 0; i < values.length; i++)
        {
            values[i] = value;
        }
    }

    public static void fill(double[] values, double value)
    {
        for (int i = 0; i < values.length; i++)
        {
            values[i] = value;
        }
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
        // Keep the source array's component type: callers like
        // AttributeMethods depend on getting Method[] back.
        Object[] result = (Object[]) java.lang.reflect.Array.newInstance(
            values.getClass().getComponentType(), length);
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

    public static int hashCode(Object[] values)
    {
        if (values == null)
        {
            return 0;
        }
        int result = 1;
        for (int i = 0; i < values.length; i++)
        {
            Object value = values[i];
            result = 31 * result + (value == null ? 0 : value.hashCode());
        }
        return result;
    }

    public static int hashCode(int[] values)
    {
        if (values == null)
        {
            return 0;
        }
        int result = 1;
        for (int i = 0; i < values.length; i++)
        {
            result = 31 * result + values[i];
        }
        return result;
    }

    public static int hashCode(long[] values)
    {
        if (values == null)
        {
            return 0;
        }
        int result = 1;
        for (int i = 0; i < values.length; i++)
        {
            long value = values[i];
            result = 31 * result + (int)(value ^ (value >>> 32));
        }
        return result;
    }

    public static int hashCode(byte[] values)
    {
        if (values == null)
        {
            return 0;
        }
        int result = 1;
        for (int i = 0; i < values.length; i++)
        {
            result = 31 * result + values[i];
        }
        return result;
    }

    public static int hashCode(char[] values)
    {
        if (values == null)
        {
            return 0;
        }
        int result = 1;
        for (int i = 0; i < values.length; i++)
        {
            result = 31 * result + values[i];
        }
        return result;
    }

    public static int hashCode(short[] values)
    {
        if (values == null)
        {
            return 0;
        }
        int result = 1;
        for (int i = 0; i < values.length; i++)
        {
            result = 31 * result + values[i];
        }
        return result;
    }

    public static int hashCode(boolean[] values)
    {
        if (values == null)
        {
            return 0;
        }
        int result = 1;
        for (int i = 0; i < values.length; i++)
        {
            result = 31 * result + (values[i] ? 1231 : 1237);
        }
        return result;
    }

    public static int hashCode(float[] values)
    {
        if (values == null)
        {
            return 0;
        }
        int result = 1;
        for (int i = 0; i < values.length; i++)
        {
            result = 31 * result + Float.floatToIntBits(values[i]);
        }
        return result;
    }

    public static int hashCode(double[] values)
    {
        if (values == null)
        {
            return 0;
        }
        int result = 1;
        for (int i = 0; i < values.length; i++)
        {
            long bits = Double.doubleToLongBits(values[i]);
            result = 31 * result + (int)(bits ^ (bits >>> 32));
        }
        return result;
    }

    public static int deepHashCode(Object[] values)
    {
        if (values == null)
        {
            return 0;
        }
        int result = 1;
        for (int i = 0; i < values.length; i++)
        {
            Object value = values[i];
            int elementHash;
            if (value == null)
            {
                elementHash = 0;
            }
            else if (value instanceof Object[])
            {
                elementHash = deepHashCode((Object[])value);
            }
            else if (value instanceof int[])
            {
                elementHash = hashCode((int[])value);
            }
            else if (value instanceof long[])
            {
                elementHash = hashCode((long[])value);
            }
            else if (value instanceof byte[])
            {
                elementHash = hashCode((byte[])value);
            }
            else if (value instanceof char[])
            {
                elementHash = hashCode((char[])value);
            }
            else if (value instanceof short[])
            {
                elementHash = hashCode((short[])value);
            }
            else if (value instanceof boolean[])
            {
                elementHash = hashCode((boolean[])value);
            }
            else if (value instanceof float[])
            {
                elementHash = hashCode((float[])value);
            }
            else if (value instanceof double[])
            {
                elementHash = hashCode((double[])value);
            }
            else
            {
                elementHash = value.hashCode();
            }
            result = 31 * result + elementHash;
        }
        return result;
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

    // Fixed-size list view over an array.  Element writes are visible through
    // the backing array; structural changes are rejected.
    private static final class ArrayAsList<E> implements List<E>
    {
        private final Object[] values;

        ArrayAsList(Object[] values)
        {
            this.values = values;
        }

        public int size()
        {
            return values.length;
        }

        public boolean isEmpty()
        {
            return values.length == 0;
        }

        public E get(int index)
        {
            if (index < 0 || index >= values.length)
            {
                throw new IndexOutOfBoundsException("index " + index + ", size " + values.length);
            }
            return (E)values[index];
        }

        public E set(int index, E value)
        {
            if (index < 0 || index >= values.length)
            {
                throw new IndexOutOfBoundsException("index " + index + ", size " + values.length);
            }
            E old = (E)values[index];
            values[index] = value;
            return old;
        }

        public boolean add(E value)
        {
            throw new UnsupportedOperationException();
        }

        public void add(int index, E value)
        {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection<? extends E> values)
        {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(int index, Collection<? extends E> values)
        {
            throw new UnsupportedOperationException();
        }

        public E remove(int index)
        {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object value)
        {
            throw new UnsupportedOperationException();
        }

        public boolean contains(Object value)
        {
            return indexOf(value) >= 0;
        }

        public int indexOf(Object value)
        {
            for (int i = 0; i < values.length; i++)
            {
                if (value == null ? values[i] == null : value.equals(values[i]))
                {
                    return i;
                }
            }
            return -1;
        }

        public int lastIndexOf(Object value)
        {
            for (int i = values.length - 1; i >= 0; i--)
            {
                if (value == null ? values[i] == null : value.equals(values[i]))
                {
                    return i;
                }
            }
            return -1;
        }

        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        public Object[] toArray()
        {
            Object[] result = new Object[values.length];
            for (int i = 0; i < values.length; i++)
            {
                result[i] = values[i];
            }
            return result;
        }

        public Iterator<E> iterator()
        {
            return new Itr();
        }

        public String toString()
        {
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

        private class Itr implements Iterator<E>
        {
            private int cursor;

            public boolean hasNext()
            {
                return cursor < values.length;
            }

            public E next()
            {
                if (cursor >= values.length)
                {
                    throw new NoSuchElementException();
                }
                return (E)values[cursor++];
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        }
    }

    public static int binarySearch(int[] values, int key)
    {
        return binarySearch(values, 0, values.length, key);
    }

    public static int binarySearch(int[] values, int fromIndex, int toIndex, int key)
    {
        if (fromIndex > toIndex)
        {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high)
        {
            int mid = (low + high) >>> 1;
            int midValue = values[mid];
            if (midValue < key)
            {
                low = mid + 1;
            }
            else if (midValue > key)
            {
                high = mid - 1;
            }
            else
            {
                return mid;
            }
        }
        return -(low + 1);
    }

    public static int binarySearch(long[] values, long key)
    {
        return binarySearch(values, 0, values.length, key);
    }

    public static int binarySearch(long[] values, int fromIndex, int toIndex, long key)
    {
        if (fromIndex > toIndex)
        {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        int low = fromIndex;
        int high = toIndex - 1;
        while (low <= high)
        {
            int mid = (low + high) >>> 1;
            long midValue = values[mid];
            if (midValue < key)
            {
                low = mid + 1;
            }
            else if (midValue > key)
            {
                high = mid - 1;
            }
            else
            {
                return mid;
            }
        }
        return -(low + 1);
    }
}
