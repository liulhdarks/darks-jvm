package java.time.temporal;

/**
 * Strategy for querying a temporal object.
 */
public interface TemporalQuery<R>
{
    R queryFrom(TemporalAccessor temporal);
}
