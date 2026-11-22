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

    private native String getSource0();

    public java.security.ProtectionDomain getProtectionDomain()
    {
        if (isPrimitive() || isArray())
        {
            return null;
        }
        java.net.URL url = null;
        String source = getSource0();
        if (source != null)
        {
            String location = source;
            int marker = source.indexOf("!/");
            if (marker >= 0)
            {
                location = source.substring(0, marker);
            }
            else
            {
                String suffix = name.replace('.', '/') + ".class";
                if (location.endsWith(suffix))
                {
                    location = location.substring(0, location.length() - suffix.length());
                    if (location.length() > 1 && location.endsWith("/"))
                    {
                        location = location.substring(0, location.length() - 1);
                    }
                }
            }
            try
            {
                url = new java.net.URL("file:" + location);
            }
            catch (java.net.MalformedURLException e)
            {
                url = null;
            }
        }
        java.security.cert.Certificate[] certificates =
            new java.security.cert.Certificate[0];
        java.security.CodeSource codeSource =
            new java.security.CodeSource(url, certificates);
        return new java.security.ProtectionDomain(codeSource,
            new java.security.Permission[0]);
    }

    public String toString()
    {
        return name;
    }

    public native boolean isInterface();

    public native boolean isArray();

    public native boolean isPrimitive();

    public native Class getComponentType();

    public native Class getSuperclass();

    public boolean isInstance(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        return isAssignableFrom(obj.getClass());
    }

    public native boolean isAssignableFrom(Class other);

    public native int getModifiers0();

    public int getModifiers()
    {
        return getModifiers0();
    }

    public boolean isEnum()
    {
        return (getModifiers0() & 0x4000) != 0;
    }

    public boolean isAnnotation()
    {
        return isInterface() && (getModifiers0() & 0x2000) != 0;
    }

    public boolean isSynthetic()
    {
        return (getModifiers0() & 0x1000) != 0;
    }

    private boolean nameHasNestedPart()
    {
        return name != null && name.indexOf('$') >= 0 && name.charAt(0) != '[';
    }

    private static boolean binaryTailIsDigits(String binaryName)
    {
        int dollar = binaryName.lastIndexOf('$');
        if (dollar < 0 || dollar + 1 >= binaryName.length())
        {
            return false;
        }
        for (int i = dollar + 1; i < binaryName.length(); i++)
        {
            char c = binaryName.charAt(i);
            if (c < '0' || c > '9')
            {
                return false;
            }
        }
        return true;
    }

    private static boolean binaryTailIsLocal(String binaryName)
    {
        int dollar = binaryName.lastIndexOf('$');
        if (dollar < 0 || dollar + 1 >= binaryName.length())
        {
            return false;
        }
        int i = dollar + 1;
        int digits = 0;
        while (i < binaryName.length() && binaryName.charAt(i) >= '0' && binaryName.charAt(i) <= '9')
        {
            i++;
            digits++;
        }
        return digits > 0 && i < binaryName.length();
    }

    public boolean isAnonymousClass()
    {
        return nameHasNestedPart() && binaryTailIsDigits(name);
    }

    public boolean isLocalClass()
    {
        return nameHasNestedPart() && binaryTailIsLocal(name);
    }

    public boolean isMemberClass()
    {
        return getDeclaringClass() != null;
    }

    private String enclosingBinaryName()
    {
        if (!nameHasNestedPart())
        {
            return null;
        }
        int dollar = name.lastIndexOf('$');
        return dollar > 0 ? name.substring(0, dollar) : null;
    }

    public Class<?> getDeclaringClass()
    {
        if (isAnonymousClass() || isLocalClass())
        {
            return null;
        }
        String enclosing = enclosingBinaryName();
        if (enclosing == null)
        {
            return null;
        }
        try
        {
            return Class.forName(enclosing, false, getClassLoader());
        }
        catch (ClassNotFoundException e)
        {
            return null;
        }
    }

    public Class<?> getEnclosingClass()
    {
        Class<?> declaring = getDeclaringClass();
        if (declaring != null)
        {
            return declaring;
        }
        String enclosing = enclosingBinaryName();
        if (enclosing == null)
        {
            return null;
        }
        try
        {
            return Class.forName(enclosing, false, getClassLoader());
        }
        catch (ClassNotFoundException e)
        {
            return null;
        }
    }

    public String getTypeName()
    {
        return getName();
    }

    public String toGenericString()
    {
        StringBuilder builder = new StringBuilder();
        int mod = getModifiers();
        if (mod != 0)
        {
            builder.append(java.lang.reflect.Modifier.toString(mod));
            builder.append(' ');
        }
        if (isAnnotation())
        {
            builder.append("@interface");
        }
        else if (isInterface())
        {
            builder.append("interface");
        }
        else if (isEnum())
        {
            builder.append("enum");
        }
        else
        {
            builder.append("class");
        }
        builder.append(' ');
        builder.append(getName());
        TypeVariable<Class<T>>[] variables = getTypeParameters();
        if (variables.length > 0)
        {
            builder.append('<');
            for (int i = 0; i < variables.length; i++)
            {
                if (i > 0)
                {
                    builder.append(", ");
                }
                builder.append(variables[i].getName());
            }
            builder.append('>');
        }
        Type superclass = getGenericSuperclass();
        if (superclass != null)
        {
            builder.append(" extends ");
            builder.append(superclass.toString());
        }
        Type[] interfaces = getGenericInterfaces();
        if (interfaces.length > 0)
        {
            builder.append(isInterface() ? " extends " : " implements ");
            for (int i = 0; i < interfaces.length; i++)
            {
                if (i > 0)
                {
                    builder.append(", ");
                }
                builder.append(interfaces[i].toString());
            }
        }
        return builder.toString();
    }

    public String getCanonicalName()
    {
        String binary = getName();
        if (binary == null || binary.length() == 0)
        {
            return binary;
        }
        if (binary.charAt(0) == '[')
        {
            return getSimpleName().replace('$', '.');
        }
        if (isAnonymousClass() || isLocalClass())
        {
            return null;
        }
        return binary.replace('$', '.');
    }

    public T cast(Object obj)
    {
        if (obj != null && !isInstance(obj))
        {
            throw new ClassCastException();
        }
        return (T)obj;
    }

    public <U> Class<? extends U> asSubclass(Class<U> clazz)
    {
        if (clazz.isAssignableFrom(this))
        {
            return (Class<? extends U>)this;
        }
        throw new ClassCastException(toString());
    }

    public T[] getEnumConstants()
    {
        if (!isEnum())
        {
            return null;
        }
        try
        {
            Field[] fields = getDeclaredFields();
            ArrayList<T> constants = new ArrayList<T>();
            for (int i = 0; i < fields.length; i++)
            {
                Field field = fields[i];
                if (field.getType() == this &&
                    (field.getModifiers() & 0x0008) != 0)
                {
                    constants.add((T)field.get(null));
                }
            }
            T[] result = (T[])java.lang.reflect.Array.newInstance(this, constants.size());
            for (int i = 0; i < constants.size(); i++)
            {
                result[i] = constants.get(i);
            }
            return result;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public ClassLoader getClassLoader()
    {
        ClassLoader loader = getClassLoader0();
        return loader != null ? loader : ClassLoader.getSystemClassLoader();
    }

    private String packageName()
    {
        if (isArray())
        {
            Class<?> component = getComponentType();
            return component == null ? null : component.packageName();
        }
        if (isPrimitive() || name == null)
        {
            return null;
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(0, dot);
    }

    public String getPackageName()
    {
        return packageName();
    }

    public Package getPackage()
    {
        if (isArray() || isPrimitive())
        {
            return null;
        }
        String packageName = packageName();
        if (packageName == null || packageName.length() == 0)
        {
            return null;
        }
        ClassLoader loader = getClassLoader0();
        if (loader == null)
        {
            loader = ClassLoader.getSystemClassLoader();
        }
        if (loader == null)
        {
            return null;
        }
        synchronized (loader)
        {
            Package pkg = loader.getPackage(packageName);
            if (pkg == null)
            {
                pkg = loader.definePackage(packageName, null, null, null,
                    null, null, null, null);
            }
            return pkg;
        }
    }

    private native ClassLoader getClassLoader0();

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

    public Class<?>[] getDeclaredClasses()
    {
        return getDeclaredClasses0();
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

    public T newInstance() throws InstantiationException, IllegalAccessException
    {
        try
        {
            return getDeclaredConstructor().newInstance();
        }
        catch (java.lang.reflect.InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException)cause;
            }
            if (cause instanceof Error)
            {
                throw (Error)cause;
            }
            throw new InstantiationException(cause == null ? e.toString() : cause.toString());
        }
        catch (NoSuchMethodException e)
        {
            throw new InstantiationException(e.toString());
        }
    }

    private static Constructor findConstructor(Constructor[] all,
        Class[] parameterTypes)
        throws NoSuchMethodException
    {        int expected = parameterTypes == null ? 0 : parameterTypes.length;
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
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        ClassLoader loader = null;
        Thread thread = Thread.currentThread();
        if (thread != null)
        {
            loader = thread.getContextClassLoader();
        }
        if (loader == null)
        {
            loader = ClassLoader.getSystemClassLoader();
        }
        return forName(name, true, loader);
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
