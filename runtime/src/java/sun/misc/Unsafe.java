package sun.misc;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Minimal Unsafe surface for libraries that use field/array offsets for
 * volatile access and CAS. The VM does not expose a CAS instruction to Java,
 * so updates are guarded by the target object's monitor, matching the
 * monitor-based AtomicInteger implementation.
 */
public final class Unsafe
{
    private static final Unsafe theUnsafe = new Unsafe();
    private static final Object LOCK = new Object();
    private static final HashMap<Field, Long> FIELD_OFFSETS = new HashMap<Field, Long>();
    private static final HashMap<Long, Field> OFFSET_FIELDS = new HashMap<Long, Field>();
    private static final int ARRAY_BASE = 16;
    private static long nextOffset = 4096L;

    private Unsafe()
    {
    }

    public static Unsafe getUnsafe()
    {
        return theUnsafe;
    }

    public Class<?> defineClass(String name, byte[] bytes, int offset, int length,
        ClassLoader loader, java.security.ProtectionDomain protectionDomain)
    {
        return ClassLoader.defineClassForUnsafe(loader, name, bytes, offset, length,
            protectionDomain);
    }

    public long objectFieldOffset(Field field)
    {
        if (field == null)
        {
            throw new NullPointerException("field");
        }
        synchronized (LOCK)
        {
            Long existing = FIELD_OFFSETS.get(field);
            if (existing != null)
            {
                return existing.longValue();
            }
            field.setAccessible(true);
            long offset = nextOffset;
            nextOffset += 8;
            Long key = Long.valueOf(offset);
            FIELD_OFFSETS.put(field, key);
            OFFSET_FIELDS.put(key, field);
            return offset;
        }
    }

    private static Field fieldFor(long offset)
    {
        synchronized (LOCK)
        {
            Field field = OFFSET_FIELDS.get(Long.valueOf(offset));
            if (field == null)
            {
                throw new IllegalArgumentException("unknown offset: " + offset);
            }
            return field;
        }
    }

    public int arrayBaseOffset(Class<?> arrayClass)
    {
        return ARRAY_BASE;
    }

    public int arrayIndexScale(Class<?> arrayClass)
    {
        return scaleOf(arrayClass);
    }

    private static int scaleOf(Class<?> type)
    {
        if (type == long.class || type == double.class)
        {
            return 8;
        }
        if (type == int.class || type == float.class)
        {
            return 4;
        }
        if (type == short.class || type == char.class)
        {
            return 2;
        }
        if (type == byte.class || type == boolean.class)
        {
            return 1;
        }
        return 4;
    }

    private static int arrayIndex(Object array, long offset, Class<?> component)
    {
        return (int)((offset - ARRAY_BASE) / scaleOf(component));
    }

    private static Object lockFor(Object target)
    {
        return target == null ? LOCK : target;
    }

    public Object getObject(Object target, long offset)
    {
        if (target == null)
        {
            return readStatic(offset);
        }
        if (target.getClass().isArray())
        {
            return Array.get(target, arrayIndex(target, offset, target.getClass().getComponentType()));
        }
        try
        {
            return fieldFor(offset).get(target);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Object getObjectVolatile(Object target, long offset)
    {
        return getObject(target, offset);
    }

    public void putObject(Object target, long offset, Object value)
    {
        if (target == null)
        {
            writeStatic(offset, value);
            return;
        }
        if (target.getClass().isArray())
        {
            Array.set(target, arrayIndex(target, offset, target.getClass().getComponentType()), value);
            return;
        }
        try
        {
            fieldFor(offset).set(target, value);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void putObjectVolatile(Object target, long offset, Object value)
    {
        putObject(target, offset, value);
    }

    public void putOrderedObject(Object target, long offset, Object value)
    {
        putObject(target, offset, value);
    }

    public int getInt(Object target, long offset)
    {
        return ((Number)getObject(target, offset)).intValue();
    }

    public int getIntVolatile(Object target, long offset)
    {
        return getInt(target, offset);
    }

    public void putInt(Object target, long offset, int value)
    {
        putObject(target, offset, Integer.valueOf(value));
    }

    public void putIntVolatile(Object target, long offset, int value)
    {
        putInt(target, offset, value);
    }

    public void putOrderedInt(Object target, long offset, int value)
    {
        putInt(target, offset, value);
    }

    public long getLong(Object target, long offset)
    {
        return ((Number)getObject(target, offset)).longValue();
    }

    public long getLongVolatile(Object target, long offset)
    {
        return getLong(target, offset);
    }

    public void putLong(Object target, long offset, long value)
    {
        putObject(target, offset, Long.valueOf(value));
    }

    public void putLongVolatile(Object target, long offset, long value)
    {
        putLong(target, offset, value);
    }

    public void putOrderedLong(Object target, long offset, long value)
    {
        putLong(target, offset, value);
    }

    public boolean getBoolean(Object target, long offset)
    {
        return ((Boolean)getObject(target, offset)).booleanValue();
    }

    public void putBoolean(Object target, long offset, boolean value)
    {
        putObject(target, offset, Boolean.valueOf(value));
    }

    public byte getByte(Object target, long offset)
    {
        return ((Number)getObject(target, offset)).byteValue();
    }

    public void putByte(Object target, long offset, byte value)
    {
        putObject(target, offset, Byte.valueOf(value));
    }

    public short getShort(Object target, long offset)
    {
        return ((Number)getObject(target, offset)).shortValue();
    }

    public void putShort(Object target, long offset, short value)
    {
        putObject(target, offset, Short.valueOf(value));
    }

    public char getChar(Object target, long offset)
    {
        return ((Character)getObject(target, offset)).charValue();
    }

    public void putChar(Object target, long offset, char value)
    {
        putObject(target, offset, Character.valueOf(value));
    }

    public boolean compareAndSwapInt(Object target, long offset, int expected, int update)
    {
        synchronized (lockFor(target))
        {
            if (getInt(target, offset) != expected)
            {
                return false;
            }
            putInt(target, offset, update);
            return true;
        }
    }

    public boolean compareAndSwapLong(Object target, long offset, long expected, long update)
    {
        synchronized (lockFor(target))
        {
            if (getLong(target, offset) != expected)
            {
                return false;
            }
            putLong(target, offset, update);
            return true;
        }
    }

    public boolean compareAndSwapObject(Object target, long offset, Object expected, Object update)
    {
        synchronized (lockFor(target))
        {
            if (getObject(target, offset) != expected)
            {
                return false;
            }
            putObject(target, offset, update);
            return true;
        }
    }

    public int addressSize()
    {
        return 8;
    }

    public int pageSize()
    {
        return 4096;
    }

    private static Object readStatic(long offset)
    {
        try
        {
            return fieldFor(offset).get(null);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void writeStatic(long offset, Object value)
    {
        try
        {
            fieldFor(offset).set(null, value);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }
}
