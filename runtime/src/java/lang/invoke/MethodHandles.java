package java.lang.invoke;

/**
 * Minimal java.lang.invoke.MethodHandles support.
 *
 * <p>The VM has no first-class method handle implementation.  Libraries such
 * as cglib probe for the Java 9 {@code privateLookupIn}/{@code defineClass}
 * pair and fall back to {@link ClassLoader#defineClass} when it is absent, so
 * this class provides the lookup type without advertising those methods.</p>
 */
public class MethodHandles
{
    private MethodHandles()
    {
    }

    public static Lookup lookup()
    {
        return Lookup.IMPL_LOOKUP;
    }

    public static Lookup publicLookup()
    {
        return Lookup.PUBLIC_LOOKUP;
    }

    public static final class Lookup
    {
        private final Class<?> lookupClass;
        private final int modes;

        static final Lookup IMPL_LOOKUP = new Lookup(Object.class, -1);
        static final Lookup PUBLIC_LOOKUP = new Lookup(Object.class, 0);

        private Lookup(Class<?> lookupClass, int modes)
        {
            this.lookupClass = lookupClass;
            this.modes = modes;
        }

        public Class<?> lookupClass()
        {
            return lookupClass;
        }

        public int lookupModes()
        {
            return modes;
        }
    }
}
