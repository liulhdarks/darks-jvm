package java.lang;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotationSupport;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.GenericSignatureParser;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;

public final class Class<T> implements GenericDeclaration, AnnotatedElement, Type
{
    private String name;
    private long vmClass;
    private TypeVariable<Class<T>>[] typeParametersCache;
    private Type genericSuperclassCache;
    private boolean genericSuperclassComputed;
    private Type[] genericInterfacesCache;

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

    public boolean isAnnotation()
    {
        return (getModifiers0() & 0x2000) != 0;
    }

    public boolean isEnum()
    {
        Class parent = getSuperclass();
        return parent != null && parent.getName().equals("java.lang.Enum");
    }

    public boolean isSynthetic()
    {
        return (getModifiers0() & 0x1000) != 0;
    }

    private String lastBinarySegment()
    {
        String n = name == null ? "" : name;
        int dollar = n.lastIndexOf('$');
        return dollar < 0 ? n : n.substring(dollar + 1);
    }

    public boolean isAnonymousClass()
    {
        if (isArray() || isPrimitive())
        {
            return false;
        }
        String segment = lastBinarySegment();
        return segment.length() > 0 &&
            segment.charAt(0) >= '0' && segment.charAt(0) <= '9';
    }

    public boolean isMemberClass()
    {
        if (isArray() || isPrimitive() || name == null || name.indexOf('$') < 0)
        {
            return false;
        }
        return !isAnonymousClass();
    }

    public boolean isLocalClass()
    {
        return false;
    }

    public String getTypeName()
    {
        if (!isArray())
        {
            return getName();
        }
        String n = getName();
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
            base = end > dims ? n.substring(dims + 1, end).replace('/', '.') : "";
        }
        else
        {
            base = getSimpleName();
        }
        String result = base;
        for (int i = 0; i < dims; i++)
        {
            result = result + "[]";
        }
        return result;
    }

    public String getCanonicalName()
    {
        String n = getName();
        return n == null ? null : n.replace('$', '.').replace('/', '.');
    }

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
        ClassLoader loader = getClassLoader0();
        return loader != null ? loader : ClassLoader.getSystemClassLoader();
    }

    private native ClassLoader getClassLoader0();

    public java.security.ProtectionDomain getProtectionDomain()
    {
        return null;
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
        ClassLoader loader = getClassLoader();
        return loader == null ? ClassLoader.getSystemResourceAsStream(absolute)
            : loader.getResourceAsStream(absolute);
    }

    private static native byte[] getResourceBytes0(String name);

    public native Class[] getInterfaces0();

    public Class[] getInterfaces()
    {
        return getInterfaces0();
    }

    public native Class[] getDeclaredClasses0();

    public Class[] getDeclaredClasses()
    {
        return getDeclaredClasses0();
    }

    public native Class getEnclosingClass0();

    public Class getEnclosingClass()
    {
        return getEnclosingClass0();
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
        if (!genericSuperclassComputed)
        {
            genericSuperclassCache = GenericSignatureParser.getGenericSuperclass(this);
            genericSuperclassComputed = true;
        }
        return genericSuperclassCache;
    }

    public Type[] getGenericInterfaces()
    {
        if (genericInterfacesCache == null)
        {
            genericInterfacesCache = GenericSignatureParser.getGenericInterfaces(this);
        }
        return genericInterfacesCache;
    }

    public TypeVariable<Class<T>>[] getTypeParameters()
    {
        if (typeParametersCache == null)
        {
            Type[] parsed = GenericSignatureParser.getClassTypeParameters(this);
            TypeVariable<Class<T>>[] typed =
                (TypeVariable<Class<T>>[])new TypeVariable[parsed.length];
            for (int i = 0; i < parsed.length; i++)
            {
                typed[i] = (TypeVariable<Class<T>>)parsed[i];
            }
            typeParametersCache = typed;
        }
        return typeParametersCache;
    }

    public native String getSignature0();

    public native Annotation[] getDeclaredAnnotations0();

    public Annotation[] getDeclaredAnnotations()
    {
        return getDeclaredAnnotations0();
    }

    public Annotation[] getAnnotations()
    {
        Annotation[] declared = getDeclaredAnnotations0();
        Class<?> parent = isInterface() ? null : getSuperclass();
        if (parent == null)
        {
            return declared;
        }
        Annotation[] inherited = parent.getAnnotations();
        if (inherited.length == 0)
        {
            return declared;
        }
        ArrayList<Annotation> merged = new ArrayList<Annotation>();
        for (int i = 0; i < declared.length; i++)
        {
            merged.add(declared[i]);
        }
        for (int i = 0; i < inherited.length; i++)
        {
            Class<?> type = inherited[i].annotationType();
            if (!AnnotationSupport.isInherited(type) || containsType(declared, type))
            {
                continue;
            }
            merged.add(inherited[i]);
        }
        Annotation[] result = new Annotation[merged.size()];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = merged.get(i);
        }
        return result;
    }

    private static boolean containsType(Annotation[] annotations, Class<?> type)
    {
        for (int i = 0; i < annotations.length; i++)
        {
            if (annotations[i].annotationType() == type)
            {
                return true;
            }
        }
        return false;
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
        T declared = getDeclaredAnnotation(annotationClass);
        if (declared != null)
        {
            return declared;
        }
        Class<?> parent = isInterface() ? null : getSuperclass();
        if (parent != null && AnnotationSupport.isInherited(annotationClass))
        {
            return parent.getAnnotation(annotationClass);
        }
        return null;
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
        T[] declared = getDeclaredAnnotationsByType(annotationClass);
        if (declared.length > 0)
        {
            return declared;
        }
        Class<?> parent = isInterface() ? null : getSuperclass();
        if (parent != null && AnnotationSupport.isInherited(annotationClass))
        {
            return parent.getAnnotationsByType(annotationClass);
        }
        return declared;
    }

    static native Class getPrimitiveClass(String name);

    public static Class<?> forName(String name) throws ClassNotFoundException
    {
        return forName(name, true, null);
    }

    public static Class<?> forName(String name, boolean initialize,
        ClassLoader loader) throws ClassNotFoundException
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        ClassLoader effective = loader != null ? loader : ClassLoader.getSystemClassLoader();
        Class<?> clazz = effective.loadClass(name);
        if (initialize)
        {
            initialize0(clazz);
        }
        return clazz;
    }

    private static native void initialize0(Class clazz);
}
