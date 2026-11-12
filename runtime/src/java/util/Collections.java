package java.util;

public class Collections
{
    private Collections()
    {
    }

    public static <T extends Comparable<? super T>> void sort(List<T> list)
    {
        sort(list, null);
    }

    public static <T> void sort(List<T> list, Comparator<? super T> comparator)
    {
        int size = list.size();
        for (int i = 1; i < size; i++)
        {
            T value = list.get(i);
            int j = i - 1;
            while (j >= 0)
            {
                T other = list.get(j);
                int result;
                if (comparator == null)
                {
                    result = ((Comparable)other).compareTo(value);
                }
                else
                {
                    result = comparator.compare(other, value);
                }
                if (result <= 0)
                {
                    break;
                }
                list.set(j + 1, other);
                j--;
            }
            list.set(j + 1, value);
        }
    }

    public static void reverse(List<?> list)
    {
        int size = list.size();
        for (int i = 0, j = size - 1; i < j; i++, j--)
        {
            swap(list, i, j);
        }
    }

    public static void swap(List<?> list, int i, int j)
    {
        List<Object> target = (List<Object>)list;
        Object value = target.get(i);
        target.set(i, target.get(j));
        target.set(j, value);
    }

    public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> values)
    {
        Iterator<? extends T> it = values.iterator();
        T best = it.next();
        while (it.hasNext())
        {
            T value = it.next();
            if (((Comparable)value).compareTo(best) > 0)
            {
                best = value;
            }
        }
        return best;
    }

    public static <T> T max(Collection<? extends T> values, Comparator<? super T> comparator)
    {
        Iterator<? extends T> it = values.iterator();
        T best = it.next();
        while (it.hasNext())
        {
            T value = it.next();
            if (comparator.compare(value, best) > 0)
            {
                best = value;
            }
        }
        return best;
    }

    public static <T extends Object & Comparable<? super T>> T min(Collection<? extends T> values)
    {
        Iterator<? extends T> it = values.iterator();
        T best = it.next();
        while (it.hasNext())
        {
            T value = it.next();
            if (((Comparable)value).compareTo(best) < 0)
            {
                best = value;
            }
        }
        return best;
    }
}
