package java.lang.reflect;

public final class Array
{
    private Array()
    {
    }

    public static Object newInstance(Class<?> componentType, int length)
    {
        if (componentType == null)
        {
            throw new NullPointerException("componentType");
        }
        return newInstance0(componentType, length);
    }

    public static Object newInstance(Class<?> componentType, int... dimensions)
    {
        if (componentType == null)
        {
            throw new NullPointerException("componentType");
        }
        if (dimensions == null)
        {
            throw new NullPointerException("dimensions");
        }
        if (dimensions.length == 0)
        {
            throw new IllegalArgumentException("zero dimensions");
        }
        return newMultiInstance(componentType, dimensions, 0);
    }

    private static Object newMultiInstance(Class<?> componentType,
                                           int[] dimensions, int level)
    {
        Class<?> arrayType = componentType;
        for (int i = level + 1; i < dimensions.length; i++)
        {
            arrayType = newInstance0(arrayType, 0).getClass();
        }
        Object array = newInstance0(arrayType, dimensions[level]);
        if (level + 1 < dimensions.length)
        {
            Object[] elements = (Object[])array;
            for (int i = 0; i < elements.length; i++)
            {
                elements[i] = newMultiInstance(componentType, dimensions,
                                               level + 1);
            }
        }
        return array;
    }

    public static int getLength(Object array)
    {
        if (array == null || !array.getClass().isArray())
        {
            throw new IllegalArgumentException("argument is not an array");
        }
        return length0(array);
    }

    public static Object get(Object array, int index)
    {
        Class<?> type = array.getClass().getComponentType();
        if (!type.isPrimitive())
        {
            return ((Object[])array)[index];
        }
        if (type == boolean.class)
        {
            return Boolean.valueOf(((boolean[])array)[index]);
        }
        if (type == byte.class)
        {
            return Byte.valueOf(((byte[])array)[index]);
        }
        if (type == char.class)
        {
            return Character.valueOf(((char[])array)[index]);
        }
        if (type == short.class)
        {
            return Short.valueOf(((short[])array)[index]);
        }
        if (type == int.class)
        {
            return Integer.valueOf(((int[])array)[index]);
        }
        if (type == long.class)
        {
            return Long.valueOf(((long[])array)[index]);
        }
        if (type == float.class)
        {
            return Float.valueOf(((float[])array)[index]);
        }
        return Double.valueOf(((double[])array)[index]);
    }

    public static boolean getBoolean(Object array, int index)
    {
        return ((boolean[])array)[index];
    }

    public static byte getByte(Object array, int index)
    {
        return ((byte[])array)[index];
    }

    public static char getChar(Object array, int index)
    {
        return ((char[])array)[index];
    }

    public static short getShort(Object array, int index)
    {
        return ((short[])array)[index];
    }

    public static int getInt(Object array, int index)
    {
        return ((int[])array)[index];
    }

    public static long getLong(Object array, int index)
    {
        return ((long[])array)[index];
    }

    public static float getFloat(Object array, int index)
    {
        return ((float[])array)[index];
    }

    public static double getDouble(Object array, int index)
    {
        return ((double[])array)[index];
    }

    public static void set(Object array, int index, Object value)
    {
        Class<?> type = array.getClass().getComponentType();
        if (!type.isPrimitive())
        {
            try
            {
                ((Object[])array)[index] = value;
            }
            catch (ArrayStoreException error)
            {
                throw new IllegalArgumentException("argument type mismatch");
            }
            return;
        }
        if (type == boolean.class)
        {
            if (!(value instanceof Boolean))
            {
                throw new IllegalArgumentException("argument type mismatch");
            }
            ((boolean[])array)[index] = ((Boolean)value).booleanValue();
        }
        else if (type == byte.class)
        {
            if (!(value instanceof Number))
            {
                throw new IllegalArgumentException("argument type mismatch");
            }
            ((byte[])array)[index] = ((Number)value).byteValue();
        }
        else if (type == char.class)
        {
            if (!(value instanceof Character))
            {
                throw new IllegalArgumentException("argument type mismatch");
            }
            ((char[])array)[index] = ((Character)value).charValue();
        }
        else if (type == short.class)
        {
            if (!(value instanceof Number))
            {
                throw new IllegalArgumentException("argument type mismatch");
            }
            ((short[])array)[index] = ((Number)value).shortValue();
        }
        else if (type == int.class)
        {
            if (!(value instanceof Number))
            {
                throw new IllegalArgumentException("argument type mismatch");
            }
            ((int[])array)[index] = ((Number)value).intValue();
        }
        else if (type == long.class)
        {
            if (!(value instanceof Number))
            {
                throw new IllegalArgumentException("argument type mismatch");
            }
            ((long[])array)[index] = ((Number)value).longValue();
        }
        else if (type == float.class)
        {
            if (!(value instanceof Number))
            {
                throw new IllegalArgumentException("argument type mismatch");
            }
            ((float[])array)[index] = ((Number)value).floatValue();
        }
        else
        {
            if (!(value instanceof Number))
            {
                throw new IllegalArgumentException("argument type mismatch");
            }
            ((double[])array)[index] = ((Number)value).doubleValue();
        }
    }

    public static void setBoolean(Object array, int index, boolean value)
    {
        ((boolean[])array)[index] = value;
    }

    public static void setByte(Object array, int index, byte value)
    {
        ((byte[])array)[index] = value;
    }

    public static void setChar(Object array, int index, char value)
    {
        ((char[])array)[index] = value;
    }

    public static void setShort(Object array, int index, short value)
    {
        ((short[])array)[index] = value;
    }

    public static void setInt(Object array, int index, int value)
    {
        ((int[])array)[index] = value;
    }

    public static void setLong(Object array, int index, long value)
    {
        ((long[])array)[index] = value;
    }

    public static void setFloat(Object array, int index, float value)
    {
        ((float[])array)[index] = value;
    }

    public static void setDouble(Object array, int index, double value)
    {
        ((double[])array)[index] = value;
    }

    private static native Object newInstance0(Class<?> componentType, int length);

    private static native int length0(Object array);
}
