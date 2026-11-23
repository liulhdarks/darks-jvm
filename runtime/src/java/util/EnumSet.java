package java.util;

public class EnumSet<E extends Enum<E>> extends AbstractSet<E>
    implements Cloneable, java.io.Serializable
{
    private final Class<E> elementType;
    private final HashSet<E> elements;

    EnumSet(Class<E> elementType)
    {
        this.elementType = elementType;
        this.elements = new HashSet<E>();
    }

    public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType)
    {
        if (elementType == null)
        {
            throw new NullPointerException("elementType");
        }
        return new EnumSet<E>(elementType);
    }

    public static <E extends Enum<E>> EnumSet<E> allOf(Class<E> elementType)
    {
        EnumSet<E> set = noneOf(elementType);
        E[] constants = elementType.getEnumConstants();
        if (constants == null)
        {
            throw new IllegalArgumentException("not an enum type");
        }
        int i = 0;
        while (i < constants.length)
        {
            set.add(constants[i]);
            i = i + 1;
        }
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> set)
    {
        if (set == null)
        {
            throw new NullPointerException("set");
        }
        EnumSet<E> result = noneOf(set.elementType);
        E[] constants = set.elementType.getEnumConstants();
        if (constants == null)
        {
            throw new IllegalArgumentException("not an enum type");
        }
        for (int i = 0; i < constants.length; i++)
        {
            if (!set.contains(constants[i]))
            {
                result.add(constants[i]);
            }
        }
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E first)
    {
        EnumSet<E> set = noneOf(elementTypeOf(first));
        set.add(first);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E first, E second)
    {
        EnumSet<E> set = noneOf(elementTypeOf(first));
        set.add(first);
        set.add(second);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E first, E second, E third)
    {
        EnumSet<E> set = noneOf(elementTypeOf(first));
        set.add(first);
        set.add(second);
        set.add(third);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E first, E second, E third, E fourth)
    {
        EnumSet<E> set = noneOf(elementTypeOf(first));
        set.add(first);
        set.add(second);
        set.add(third);
        set.add(fourth);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E first, E second, E third, E fourth,
        E fifth)
    {
        EnumSet<E> set = noneOf(elementTypeOf(first));
        set.add(first);
        set.add(second);
        set.add(third);
        set.add(fourth);
        set.add(fifth);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E first, E... rest)
    {
        EnumSet<E> set = noneOf(elementTypeOf(first));
        set.add(first);
        if (rest != null)
        {
            for (int i = 0; i < rest.length; i++)
            {
                set.add(rest[i]);
            }
        }
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> values)
    {
        if (values == null)
        {
            throw new NullPointerException("values");
        }
        Iterator<E> it = values.iterator();
        if (!it.hasNext())
        {
            throw new IllegalArgumentException("collection is empty");
        }
        E first = it.next();
        EnumSet<E> set = noneOf(elementTypeOf(first));
        set.add(first);
        while (it.hasNext())
        {
            set.add(it.next());
        }
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> copyOf(EnumSet<E> set)
    {
        if (set == null)
        {
            throw new NullPointerException("set");
        }
        EnumSet<E> copy = new EnumSet<E>(set.elementType);
        copy.elements.addAll(set.elements);
        return copy;
    }

    private static <E extends Enum<E>> Class<E> elementTypeOf(E value)
    {
        if (value == null)
        {
            throw new NullPointerException();
        }
        return (Class<E>)value.getClass();
    }

    public EnumSet<E> clone()
    {
        return copyOf(this);
    }

    public Iterator<E> iterator()
    {
        return elements.iterator();
    }

    public int size()
    {
        return elements.size();
    }

    public boolean isEmpty()
    {
        return elements.isEmpty();
    }

    public boolean contains(Object value)
    {
        return elements.contains(value);
    }

    public boolean add(E value)
    {
        return elements.add(value);
    }

    public boolean remove(Object value)
    {
        return elements.remove(value);
    }

    public void clear()
    {
        elements.clear();
    }
}
