package java.lang;

/**
 * Thread-local variable whose value is inherited by threads created after the
 * value was set.  The parent's table is copied in Thread.init(); childValue()
 * is the hook subclasses use to transform the inherited value.
 */
public class InheritableThreadLocal<T> extends ThreadLocal<T>
{
    protected T childValue(T parentValue)
    {
        return parentValue;
    }
}
