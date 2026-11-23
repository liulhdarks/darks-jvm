package java.lang.reflect;

import java.lang.annotation.Annotation;

public final class Field extends AccessibleObject implements AnnotatedElement, Member
{
    private Class declaringClass;
    private String name;
    private String descriptor;
    private int modifiers;
    private Type genericTypeCache;

    Field()
    {
    }

    public String getName()
    {
        return name;
    }

    public Class getDeclaringClass()
    {
        return declaringClass;
    }

    public int getModifiers()
    {
        return modifiers;
    }

    public boolean isSynthetic()
    {
        return (modifiers & 0x1000) != 0;
    }

    public boolean isEnumConstant()
    {
        return (modifiers & 0x4000) != 0;
    }

    public native Class getType();

    native String getSignature0();

    public Type getGenericType()
    {
        if (genericTypeCache == null)
        {
            genericTypeCache = GenericSignatureParser.getGenericFieldType(this);
        }
        return genericTypeCache;
    }

    public AnnotatedType getAnnotatedType()
    {
        return AnnotatedTypeImpl.of(getGenericType());
    }

    public native Object get0(Object obj);

    public Object get(Object obj) throws IllegalAccessException
    {
        return get0(obj);
    }

    public native void set0(Object obj, Object value);

    public void set(Object obj, Object value) throws IllegalAccessException
    {
        set0(obj, value);
    }

    public boolean getBoolean(Object obj) throws IllegalAccessException
    {
        return ((Boolean)get(obj)).booleanValue();
    }

    public byte getByte(Object obj) throws IllegalAccessException
    {
        return ((Number)get(obj)).byteValue();
    }

    public char getChar(Object obj) throws IllegalAccessException
    {
        return ((Character)get(obj)).charValue();
    }

    public short getShort(Object obj) throws IllegalAccessException
    {
        return ((Number)get(obj)).shortValue();
    }

    public int getInt(Object obj) throws IllegalAccessException
    {
        return ((Number)get(obj)).intValue();
    }

    public long getLong(Object obj) throws IllegalAccessException
    {
        return ((Number)get(obj)).longValue();
    }

    public float getFloat(Object obj) throws IllegalAccessException
    {
        return ((Number)get(obj)).floatValue();
    }

    public double getDouble(Object obj) throws IllegalAccessException
    {
        return ((Number)get(obj)).doubleValue();
    }

    public void setBoolean(Object obj, boolean value) throws IllegalAccessException
    {
        set(obj, Boolean.valueOf(value));
    }

    public void setByte(Object obj, byte value) throws IllegalAccessException
    {
        set(obj, Byte.valueOf(value));
    }

    public void setChar(Object obj, char value) throws IllegalAccessException
    {
        set(obj, Character.valueOf(value));
    }

    public void setShort(Object obj, short value) throws IllegalAccessException
    {
        set(obj, Short.valueOf(value));
    }

    public void setInt(Object obj, int value) throws IllegalAccessException
    {
        set(obj, Integer.valueOf(value));
    }

    public void setLong(Object obj, long value) throws IllegalAccessException
    {
        set(obj, Long.valueOf(value));
    }

    public void setFloat(Object obj, float value) throws IllegalAccessException
    {
        set(obj, Float.valueOf(value));
    }

    public void setDouble(Object obj, double value) throws IllegalAccessException
    {
        set(obj, Double.valueOf(value));
    }

    public native Annotation[] getDeclaredAnnotations0();

    public Annotation[] getDeclaredAnnotations()
    {
        return getDeclaredAnnotations0();
    }

    public Annotation[] getAnnotations()
    {
        return getDeclaredAnnotations0();
    }

    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass)
    {
        if (annotationClass == null)
        {
            throw new NullPointerException("annotationClass");
        }
        Annotation[] all = getDeclaredAnnotations0();
        for (int i = 0; i < all.length; i++)
        {
            if (all[i].annotationType() == annotationClass)
            {
                return (T)all[i];
            }
        }
        return null;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
    {
        return getDeclaredAnnotation(annotationClass);
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass)
    {
        return getAnnotation(annotationClass) != null;
    }

    public <T extends Annotation> T[] getDeclaredAnnotationsByType(
        Class<T> annotationClass)
    {
        if (annotationClass == null)
        {
            throw new NullPointerException("annotationClass");
        }
        return (T[])AnnotationSupport.expand(getDeclaredAnnotations0(),
            annotationClass);
    }

    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass)
    {
        return getDeclaredAnnotationsByType(annotationClass);
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof Field))
        {
            return false;
        }
        Field other = (Field)obj;
        return declaringClass == other.declaringClass &&
            (name == null ? other.name == null : name.equals(other.name)) &&
            (descriptor == null ? other.descriptor == null
                : descriptor.equals(other.descriptor));
    }

    public int hashCode()
    {
        int h = declaringClass == null ? 0 : declaringClass.getName().hashCode();
        return h ^ (name == null ? 0 : name.hashCode());
    }

    public String toString()
    {
        return declaringClass.getName() + "." + name;
    }
}
