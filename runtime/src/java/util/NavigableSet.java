package java.util;

public interface NavigableSet<E> extends SortedSet<E>
{
    default E lower(E value)
    {
        throw new UnsupportedOperationException();
    }

    default E floor(E value)
    {
        throw new UnsupportedOperationException();
    }

    default E ceiling(E value)
    {
        throw new UnsupportedOperationException();
    }

    default E higher(E value)
    {
        throw new UnsupportedOperationException();
    }

    default E pollFirst()
    {
        throw new UnsupportedOperationException();
    }

    default E pollLast()
    {
        throw new UnsupportedOperationException();
    }

    default NavigableSet<E> descendingSet()
    {
        throw new UnsupportedOperationException();
    }

    default Iterator<E> descendingIterator()
    {
        throw new UnsupportedOperationException();
    }

    default NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
        E toElement, boolean toInclusive)
    {
        throw new UnsupportedOperationException();
    }

    default NavigableSet<E> headSet(E toElement, boolean inclusive)
    {
        throw new UnsupportedOperationException();
    }

    default NavigableSet<E> tailSet(E fromElement, boolean inclusive)
    {
        throw new UnsupportedOperationException();
    }
}
