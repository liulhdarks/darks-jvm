package java.util;

public class EnumSet<E extends Enum<E>> extends HashSet<E>
{
    public EnumSet()
    {
        super();
    }

    public EnumSet(Collection<? extends E> values)
    {
        super(values);
    }

    public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType)
    {
        return new EnumSet<E>();
    }

    public static <E extends Enum<E>> EnumSet<E> of(E value)
    {
        EnumSet<E> set = new EnumSet<E>();
        set.add(value);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E first, E second)
    {
        EnumSet<E> set = of(first);
        set.add(second);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E first, E second, E third)
    {
        EnumSet<E> set = of(first, second);
        set.add(third);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E first, E second, E third, E fourth)
    {
        EnumSet<E> set = of(first, second, third);
        set.add(fourth);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E first, E second, E third, E fourth, E fifth)
    {
        EnumSet<E> set = of(first, second, third, fourth);
        set.add(fifth);
        return set;
    }

    @SafeVarargs
    public static <E extends Enum<E>> EnumSet<E> of(E first, E... rest)
    {
        EnumSet<E> set = of(first);
        for (int i = 0; i < rest.length; i++)
        {
            set.add(rest[i]);
        }
        return set;
    }

    @SafeVarargs
    public static <E extends Enum<E>> EnumSet<E> copyOf(E... values)
    {
        EnumSet<E> set = new EnumSet<E>();
        for (int i = 0; i < values.length; i++)
        {
            set.add(values[i]);
        }
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> values)
    {
        return new EnumSet<E>(values);
    }

    public static <E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> other)
    {
        EnumSet<E> set = new EnumSet<E>();
        Iterator<E> iterator = other.iterator();
        while (iterator.hasNext())
        {
            set.add(iterator.next());
        }
        return set;
    }

    public EnumSet<E> clone()
    {
        return new EnumSet<E>(this);
    }
}
