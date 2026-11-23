package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * Invocation handler behind annotation proxy instances.  The VM fills the
 * three fields directly when it materialises a RuntimeVisibleAnnotation, so
 * no constructor is required.
 */
final class AnnotationInvocationHandler implements InvocationHandler
{
    Class annotationType;
    String[] names;
    Object[] values;

    public Object invoke(Object proxy, Method method, Object[] args)
    {
        String name = method.getName();
        if ("equals".equals(name) && args != null && args.length == 1)
        {
            return Boolean.valueOf(equalsImpl(args[0]));
        }
        if ("hashCode".equals(name) && (args == null || args.length == 0))
        {
            return Integer.valueOf(hashCodeImpl());
        }
        if ("toString".equals(name) && (args == null || args.length == 0))
        {
            return toStringImpl();
        }
        if ("annotationType".equals(name) && (args == null || args.length == 0))
        {
            return annotationType;
        }
        int index = indexOf(name);
        if (index >= 0 && values[index] != null)
        {
            return values[index];
        }
        return defaultValue(name);
    }

    private Object defaultValue(String name)
    {
        if (annotationType == null)
        {
            return null;
        }
        try
        {
            Method method = annotationType.getMethod(name);
            return method.getDefaultValue();
        }
        catch (java.lang.NoSuchMethodException e)
        {
            return null;
        }
    }

    private int indexOf(String name)
    {
        for (int i = 0; names != null && i < names.length; i++)
        {
            if (names[i].equals(name))
            {
                return i;
            }
        }
        return -1;
    }

    private Object valueFor(String name)
    {
        int index = indexOf(name);
        return index < 0 ? null : values[index];
    }

    private static AnnotationInvocationHandler handlerOf(Object other)
    {
        try
        {
            InvocationHandler handler = Proxy.getInvocationHandler(other);
            if (handler instanceof AnnotationInvocationHandler)
            {
                return (AnnotationInvocationHandler)handler;
            }
        }
        catch (IllegalArgumentException ignored)
        {
        }
        return null;
    }

    private boolean equalsImpl(Object other)
    {
        if (!(other instanceof Annotation))
        {
            return false;
        }
        AnnotationInvocationHandler otherHandler = handlerOf(other);
        if (otherHandler == null || otherHandler.annotationType != annotationType)
        {
            return false;
        }
        for (int i = 0; names != null && i < names.length; i++)
        {
            if (!valueEquals(values[i], otherHandler.valueFor(names[i])))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean valueEquals(Object a, Object b)
    {
        if (a == b)
        {
            return true;
        }
        if (a == null || b == null)
        {
            return false;
        }
        return arrayEquals(a, b) || a.equals(b);
    }

    private static boolean arrayEquals(Object a, Object b)
    {
        if (a instanceof int[] && b instanceof int[])
        {
            return Arrays.equals((int[])a, (int[])b);
        }
        if (a instanceof long[] && b instanceof long[])
        {
            return Arrays.equals((long[])a, (long[])b);
        }
        if (a instanceof Object[] && b instanceof Object[])
        {
            return Arrays.equals((Object[])a, (Object[])b);
        }
        if (a instanceof byte[] && b instanceof byte[])
        {
            byte[] x = (byte[])a;
            byte[] y = (byte[])b;
            if (x.length != y.length) return false;
            for (int i = 0; i < x.length; i++) if (x[i] != y[i]) return false;
            return true;
        }
        if (a instanceof short[] && b instanceof short[])
        {
            short[] x = (short[])a;
            short[] y = (short[])b;
            if (x.length != y.length) return false;
            for (int i = 0; i < x.length; i++) if (x[i] != y[i]) return false;
            return true;
        }
        if (a instanceof char[] && b instanceof char[])
        {
            char[] x = (char[])a;
            char[] y = (char[])b;
            if (x.length != y.length) return false;
            for (int i = 0; i < x.length; i++) if (x[i] != y[i]) return false;
            return true;
        }
        if (a instanceof boolean[] && b instanceof boolean[])
        {
            boolean[] x = (boolean[])a;
            boolean[] y = (boolean[])b;
            if (x.length != y.length) return false;
            for (int i = 0; i < x.length; i++) if (x[i] != y[i]) return false;
            return true;
        }
        if (a instanceof float[] && b instanceof float[])
        {
            float[] x = (float[])a;
            float[] y = (float[])b;
            if (x.length != y.length) return false;
            for (int i = 0; i < x.length; i++) if (x[i] != y[i]) return false;
            return true;
        }
        if (a instanceof double[] && b instanceof double[])
        {
            double[] x = (double[])a;
            double[] y = (double[])b;
            if (x.length != y.length) return false;
            for (int i = 0; i < x.length; i++) if (x[i] != y[i]) return false;
            return true;
        }
        return false;
    }

    private int hashCodeImpl()
    {
        int result = 0;
        for (int i = 0; names != null && i < names.length; i++)
        {
            result += (127 * names[i].hashCode()) ^ valueHash(values[i]);
        }
        return result;
    }

    private static int valueHash(Object value)
    {
        if (value == null)
        {
            return 0;
        }
        if (value instanceof int[])
        {
            int[] a = (int[])value;
            int h = 1;
            for (int i = 0; i < a.length; i++) h = 31 * h + a[i];
            return h;
        }
        if (value instanceof long[])
        {
            long[] a = (long[])value;
            int h = 1;
            for (int i = 0; i < a.length; i++) h = 31 * h + (int)(a[i] ^ (a[i] >>> 32));
            return h;
        }
        if (value instanceof Object[])
        {
            Object[] a = (Object[])value;
            int h = 1;
            for (int i = 0; i < a.length; i++)
            {
                h = 31 * h + (a[i] == null ? 0 : a[i].hashCode());
            }
            return h;
        }
        if (value instanceof byte[])
        {
            byte[] a = (byte[])value;
            int h = 1;
            for (int i = 0; i < a.length; i++) h = 31 * h + a[i];
            return h;
        }
        if (value instanceof short[])
        {
            short[] a = (short[])value;
            int h = 1;
            for (int i = 0; i < a.length; i++) h = 31 * h + a[i];
            return h;
        }
        if (value instanceof char[])
        {
            char[] a = (char[])value;
            int h = 1;
            for (int i = 0; i < a.length; i++) h = 31 * h + a[i];
            return h;
        }
        if (value instanceof boolean[])
        {
            boolean[] a = (boolean[])value;
            int h = 1;
            for (int i = 0; i < a.length; i++) h = 31 * h + (a[i] ? 1231 : 1237);
            return h;
        }
        if (value instanceof float[])
        {
            float[] a = (float[])value;
            int h = 1;
            for (int i = 0; i < a.length; i++) h = 31 * h + Float.valueOf(a[i]).hashCode();
            return h;
        }
        if (value instanceof double[])
        {
            double[] a = (double[])value;
            int h = 1;
            for (int i = 0; i < a.length; i++) h = 31 * h + Double.hashCode(a[i]);
            return h;
        }
        return value.hashCode();
    }

    private String toStringImpl()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('@');
        sb.append(annotationType.getSimpleName());
        sb.append('(');
        for (int i = 0; names != null && i < names.length; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(names[i]);
            sb.append('=');
            sb.append(valueToString(values[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String valueToString(Object value)
    {
        if (value == null)
        {
            return "null";
        }
        if (value instanceof Object[])
        {
            return Arrays.toString((Object[])value);
        }
        if (value instanceof int[])
        {
            return Arrays.toString((int[])value);
        }
        if (value instanceof long[])
        {
            return Arrays.toString((long[])value);
        }
        if (value instanceof byte[])
        {
            return Arrays.toString((byte[])value);
        }
        if (value instanceof short[])
        {
            return Arrays.toString((short[])value);
        }
        if (value instanceof char[])
        {
            return Arrays.toString((char[])value);
        }
        if (value instanceof boolean[])
        {
            return Arrays.toString((boolean[])value);
        }
        if (value instanceof float[])
        {
            return Arrays.toString((float[])value);
        }
        if (value instanceof double[])
        {
            return Arrays.toString((double[])value);
        }
        return String.valueOf(value);
    }
}
