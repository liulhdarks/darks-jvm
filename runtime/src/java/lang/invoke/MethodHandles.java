package java.lang.invoke;

/**
 * Minimal MethodHandles surface. Call sites that only probe for the
 * java.lang.invoke API (for example cglib's ReflectUtils static initializer)
 * can now resolve the class and fall back to plain reflection. Method handle
 * constants and Lookup.defineClass are not implemented.
 */
public final class MethodHandles
{
    private MethodHandles()
    {
    }

    public static Lookup lookup()
    {
        return new Lookup(Object.class, Lookup.ALL_MODES);
    }

    public static Lookup publicLookup()
    {
        return new Lookup(Object.class, Lookup.PUBLIC);
    }

    public static final class Lookup
    {
        static final int PUBLIC = 1;
        static final int ALL_MODES = 15;

        private final Class<?> lookupClass;
        private final int modes;

        Lookup(Class<?> lookupClass, int modes)
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

        public Lookup in(Class<?> requestedLookupClass)
        {
            if (requestedLookupClass == null)
            {
                throw new NullPointerException("requestedLookupClass");
            }
            return new Lookup(requestedLookupClass, modes);
        }

        public Class<?> findClass(String targetName) throws ClassNotFoundException
        {
            if (targetName == null)
            {
                throw new NullPointerException("targetName");
            }
            return Class.forName(targetName, false, lookupClass.getClassLoader());
        }

        public String toString()
        {
            return "Lookup[" + lookupClass.getName() + "]";
        }
    }
}
