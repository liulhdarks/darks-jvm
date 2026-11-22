package javax.management.openmbean;

public interface TabularData
{
    TabularType getTabularType();

    Object[] calculateIndex(CompositeData value);

    int size();

    boolean isEmpty();

    boolean containsKey(Object[] key);

    boolean containsValue(CompositeData value);

    CompositeData get(Object[] key);

    void put(CompositeData value);

    CompositeData remove(Object[] key);

    void clear();

    java.util.Set<java.util.List<?>> keySet();

    java.util.Collection<CompositeData> values();
}
