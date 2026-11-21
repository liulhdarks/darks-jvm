package java.lang.reflect;

public final class Array
{
    private Array()
    {
    }

    private static native Object newArray(Class<?> componentType, int length);

    public static Object newInstance(Class<?> componentType, int length)
    {
        if (componentType == null)
        {
            throw new NullPointerException("componentType");
        }
        if (length < 0)
        {
            throw new NegativeArraySizeException("length " + length);
        }
        return newArray(componentType, length);
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
            throw new IllegalArgumentException("no dimensions");
        }
        for (int i = 0; i < dimensions.length; i++)
        {
            if (dimensions[i] < 0)
            {
                throw new NegativeArraySizeException("length " + dimensions[i]);
            }
        }
        if (dimensions.length == 1)
        {
            return newInstance(componentType, dimensions[0]);
        }
        int[] rest = new int[dimensions.length - 1];
        for (int i = 1; i < dimensions.length; i++)
        {
            rest[i - 1] = dimensions[i];
        }
        Object element = newInstance(componentType, rest);
        Object result = newArray(element.getClass(), dimensions[0]);
        int length = dimensions[0];
        for (int i = 0; i < length; i++)
        {
            set(result, i, newInstance(componentType, rest));
        }
        return result;
    }

    public static native int getLength(Object array);

    public static native Object get(Object array, int index);

    public static native boolean getBoolean(Object array, int index);

    public static native byte getByte(Object array, int index);

    public static native char getChar(Object array, int index);

    public static native short getShort(Object array, int index);

    public static native int getInt(Object array, int index);

    public static native long getLong(Object array, int index);

    public static native float getFloat(Object array, int index);

    public static native double getDouble(Object array, int index);

    public static native void set(Object array, int index, Object value);

    public static native void setBoolean(Object array, int index, boolean value);

    public static native void setByte(Object array, int index, byte value);

    public static native void setChar(Object array, int index, char value);

    public static native void setShort(Object array, int index, short value);

    public static native void setInt(Object array, int index, int value);

    public static native void setLong(Object array, int index, long value);

    public static native void setFloat(Object array, int index, float value);

    public static native void setDouble(Object array, int index, double value);
}
