package java.lang;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public final class Class<T>
{
    private String name;
    private long vmClass;

    public String getName()
    {
        return name;
    }

    public String getSimpleName()
    {
        String n = name;
        if (n == null)
        {
            return "";
        }
        if (n.length() > 0 && n.charAt(0) == '[')
        {
            int dims = 0;
            while (dims < n.length() && n.charAt(dims) == '[')
            {
                dims++;
            }
            char kind = n.charAt(dims);
            String base;
            if (kind == 'L')
            {
                int end = n.lastIndexOf(';');
                String internal = end > dims ? n.substring(dims + 1, end) : "";
                base = simpleNameOf(internal);
            }
            else if (kind == 'Z')
            {
                base = "boolean";
            }
            else if (kind == 'B')
            {
                base = "byte";
            }
            else if (kind == 'C')
            {
                base = "char";
            }
            else if (kind == 'S')
            {
                base = "short";
            }
            else if (kind == 'I')
            {
                base = "int";
            }
            else if (kind == 'J')
            {
                base = "long";
            }
            else if (kind == 'F')
            {
                base = "float";
            }
            else if (kind == 'D')
            {
                base = "double";
            }
            else
            {
                base = "";
            }
            for (int i = 0; i < dims; i++)
            {
                base = base + "[]";
            }
            return base;
        }
        return simpleNameOf(n);
    }

    private static String simpleNameOf(String n)
    {
        int dot = n.lastIndexOf('.');
        if (dot < 0)
        {
            dot = n.lastIndexOf('/');
        }
        String local = dot < 0 ? n : n.substring(dot + 1);
        int dollar = local.lastIndexOf('$');
        if (dollar >= 0)
        {
            local = local.substring(dollar + 1);
        }
        // Anonymous/local classes have a numeric prefix before the real name.
        int start = 0;
        while (start < local.length() && local.charAt(start) >= '0' && local.charAt(start) <= '9')
        {
            start++;
        }
        if (start > 0)
        {
            local = local.substring(start);
        }
        return local;
    }

    public native boolean desiredAssertionStatus();

    public String toString()
    {
        return name;
    }

    public native boolean isInterface();

    public native boolean isArray();

    public native boolean isPrimitive();

    public native Class getComponentType();

    public native Class getSuperclass();

    public native boolean isInstance(Object obj);

    public native boolean isAssignableFrom(Class other);

    public native int getModifiers0();

    public int getModifiers()
    {
        return getModifiers0();
    }

    public ClassLoader getClassLoader()
    {
        return ClassLoader.getSystemClassLoader();
    }

    public java.io.InputStream getResourceAsStream(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        String absolute;
        if (name.startsWith("/"))
        {
            absolute = name.substring(1);
        }
        else
        {
            String own = getName();
            int dot = own.lastIndexOf('.');
            String pkg = dot < 0 ? "" : own.substring(0, dot + 1).replace('.', '/');
            absolute = pkg + name;
        }
        byte[] data = getResourceBytes0(absolute);
        return data == null ? null : new java.io.ByteArrayInputStream(data);
    }

    private static native byte[] getResourceBytes0(String name);

    public native Class[] getInterfaces0();

    public Class[] getInterfaces()
    {
        return getInterfaces0();
    }

    public native Method[] getDeclaredMethods0();

    public Method[] getDeclaredMethods()
    {
        return getDeclaredMethods0();
    }

    public native Method[] getMethods0();

    public Method[] getMethods()
    {
        return getMethods0();
    }

    public native Method getDeclaredMethod0(String name, Class[] parameterTypes);

    public Method getDeclaredMethod(String name, Class... parameterTypes)
        throws NoSuchMethodException
    {
        return getDeclaredMethod0(name, parameterTypes);
    }

    public native Method getMethod0(String name, Class[] parameterTypes);

    public Method getMethod(String name, Class... parameterTypes)
        throws NoSuchMethodException
    {
        return getMethod0(name, parameterTypes);
    }

    public native Field[] getDeclaredFields0();

    public Field[] getDeclaredFields()
    {
        return getDeclaredFields0();
    }

    public native Field[] getFields0();

    public Field[] getFields()
    {
        return getFields0();
    }

    public native Field getDeclaredField0(String name);

    public Field getDeclaredField(String name)
        throws NoSuchFieldException
    {
        return getDeclaredField0(name);
    }

    public native Field getField0(String name);

    public Field getField(String name)
        throws NoSuchFieldException
    {
        return getField0(name);
    }

    public native Constructor[] getDeclaredConstructors0();

    public Constructor[] getDeclaredConstructors()
    {
        return getDeclaredConstructors0();
    }

    public native Constructor[] getConstructors0();

    public Constructor[] getConstructors()
    {
        return getConstructors0();
    }

    public Constructor<T> getDeclaredConstructor(Class... parameterTypes)
        throws NoSuchMethodException
    {
        return findConstructor(getDeclaredConstructors(), parameterTypes);
    }

    public Constructor<T> getConstructor(Class... parameterTypes)
        throws NoSuchMethodException
    {
        return findConstructor(getConstructors(), parameterTypes);
    }

    private static Constructor findConstructor(Constructor[] all,
        Class[] parameterTypes)
        throws NoSuchMethodException
    {
        int expected = parameterTypes == null ? 0 : parameterTypes.length;
        for (int i = 0; i < all.length; i++)
        {
            Class[] actual = all[i].getParameterTypes();
            if (actual.length != expected)
            {
                continue;
            }
            boolean match = true;
            for (int j = 0; j < expected; j++)
            {
                if (actual[j] != parameterTypes[j])
                {
                    match = false;
                    break;
                }
            }
            if (match)
            {
                return all[i];
            }
        }
        throw new NoSuchMethodException("constructor not found");
    }

    public Type getGenericSuperclass()
    {
        throw new UnsupportedOperationException(
            "generic signatures are not supported");
    }

    public Type[] getGenericInterfaces()
    {
        throw new UnsupportedOperationException(
            "generic signatures are not supported");
    }

    public TypeVariable<Class<T>>[] getTypeParameters()
    {
        throw new UnsupportedOperationException(
            "generic signatures are not supported");
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

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
    {
        Annotation[] all = getDeclaredAnnotations0();
        for (int i = 0; i < all.length; i++)
        {
            if (annotationClass.isInstance(all[i]))
            {
                return (T)all[i];
            }
        }
        return null;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass)
    {
        return getAnnotation(annotationClass) != null;
    }

    static native Class getPrimitiveClass(String name);

    public static native Class forName(String name) throws ClassNotFoundException;
}
