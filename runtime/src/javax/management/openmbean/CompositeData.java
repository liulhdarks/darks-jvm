package javax.management.openmbean;

public interface CompositeData
{
    CompositeType getCompositeType();

    Object get(String key);

    Object[] getAll(String[] keys);

    boolean containsKey(String key);

    boolean containsValue(Object value);

    java.util.Set<String> keySet();

    java.util.Collection<?> values();

    int size();
}
