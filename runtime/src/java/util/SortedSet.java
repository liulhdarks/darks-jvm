package java.util;

// Sorted set contract.  Range views are not implemented by the VM's TreeSet
// yet, so the default implementations report them as unsupported.
public interface SortedSet<E> extends Set<E>
{
    Comparator<? super E> comparator();

    E first();

    E last();

    default SortedSet<E> subSet(E fromElement, E toElement)
    {
        throw new UnsupportedOperationException();
    }

    default SortedSet<E> headSet(E toElement)
    {
        throw new UnsupportedOperationException();
    }

    default SortedSet<E> tailSet(E fromElement)
    {
        throw new UnsupportedOperationException();
    }
}
