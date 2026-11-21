package java.beans;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Reflection based bean introspection.  Only the descriptor information a
 * container needs to discover properties (getter/setter pairs) and public
 * methods is produced.
 */
public final class Introspector
{
    public static final int USE_ALL_BEANINFO = 1;
    public static final int IGNORE_IMMEDIATE_BEANINFO = 2;
    public static final int IGNORE_ALL_BEANINFO = 3;

    private Introspector()
    {
    }

    public static BeanInfo getBeanInfo(Class<?> beanClass)
        throws IntrospectionException
    {
        return getBeanInfo(beanClass, null);
    }

    public static BeanInfo getBeanInfo(Class<?> beanClass, int flags)
        throws IntrospectionException
    {
        // BeanInfo classes shipped by a library are not consulted: the
        // reflection-derived descriptors are always used, matching the
        // IGNORE_ALL_BEANINFO behaviour.
        return getBeanInfo(beanClass, (Class<?>) null);
    }

    public static BeanInfo getBeanInfo(Class<?> beanClass, Class<?> stopClass)
        throws IntrospectionException
    {
        if (beanClass == null)
        {
            throw new NullPointerException("beanClass");
        }
        Method[] methods = beanClass.getMethods();
        ArrayList<Method> visible = new ArrayList<Method>();
        for (int i = 0; i < methods.length; i++)
        {
            Class<?> declaring = methods[i].getDeclaringClass();
            if (declaring == Object.class || inStopHierarchy(declaring, stopClass))
            {
                continue;
            }
            visible.add(methods[i]);
        }

        MethodDescriptor[] methodDescriptors =
            new MethodDescriptor[visible.size()];
        for (int i = 0; i < visible.size(); i++)
        {
            methodDescriptors[i] = new MethodDescriptor(visible.get(i));
        }

        ArrayList<String> names = new ArrayList<String>();
        ArrayList<Method> readers = new ArrayList<Method>();
        ArrayList<Method> writers = new ArrayList<Method>();
        ArrayList<Class> types = new ArrayList<Class>();
        for (int i = 0; i < visible.size(); i++)
        {
            Method method = visible.get(i);
            String methodName = method.getName();
            Class[] parameters = method.getParameterTypes();
            if (parameters.length == 0 && methodName.startsWith("get") &&
                methodName.length() > 3 && method.getReturnType() != void.class)
            {
                addReader(names, readers, writers, types,
                    decapitalize(methodName.substring(3)), method);
            }
            else if (parameters.length == 0 && methodName.startsWith("is") &&
                methodName.length() > 2 && method.getReturnType() == boolean.class)
            {
                addReader(names, readers, writers, types,
                    decapitalize(methodName.substring(2)), method);
            }
            else if (parameters.length == 1 && methodName.startsWith("set") &&
                methodName.length() > 3)
            {
                addWriter(names, readers, writers, types,
                    decapitalize(methodName.substring(3)), method);
            }
        }

        ArrayList<PropertyDescriptor> properties =
            new ArrayList<PropertyDescriptor>();
        for (int i = 0; i < names.size(); i++)
        {
            properties.add(new PropertyDescriptor(names.get(i), readers.get(i),
                writers.get(i)));
        }
        Collections.sort(properties, new Comparator<PropertyDescriptor>()
        {
            public int compare(PropertyDescriptor left, PropertyDescriptor right)
            {
                return left.getName().compareTo(right.getName());
            }
        });
        PropertyDescriptor[] propertyDescriptors =
            new PropertyDescriptor[properties.size()];
        for (int i = 0; i < propertyDescriptors.length; i++)
        {
            propertyDescriptors[i] = properties.get(i);
        }
        return new BeanInfoImpl(new BeanDescriptor(beanClass), propertyDescriptors,
            methodDescriptors);
    }

    public static void flushCaches()
    {
    }

    public static void flushFromCaches(Class<?> clazz)
    {
    }

    public static String decapitalize(String name)
    {
        if (name == null || name.length() == 0)
        {
            return name;
        }
        if (name.length() > 1 && isUpper(name.charAt(0)) && isUpper(name.charAt(1)))
        {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static boolean isUpper(char c)
    {
        return c >= 'A' && c <= 'Z';
    }

    private static boolean inStopHierarchy(Class<?> declaring, Class<?> stopClass)
    {
        if (stopClass == null)
        {
            return false;
        }
        for (Class<?> c = stopClass; c != null; c = c.getSuperclass())
        {
            if (c == declaring)
            {
                return true;
            }
        }
        return false;
    }

    private static int indexOf(ArrayList<String> names, String name)
    {
        for (int i = 0; i < names.size(); i++)
        {
            if (names.get(i).equals(name))
            {
                return i;
            }
        }
        return -1;
    }

    private static void addReader(ArrayList<String> names,
        ArrayList<Method> readers, ArrayList<Method> writers,
        ArrayList<Class> types, String property, Method method)
    {
        int index = indexOf(names, property);
        if (index < 0)
        {
            names.add(property);
            readers.add(method);
            writers.add(null);
            types.add(method.getReturnType());
        }
        else
        {
            readers.set(index, method);
            types.set(index, method.getReturnType());
        }
    }

    private static void addWriter(ArrayList<String> names,
        ArrayList<Method> readers, ArrayList<Method> writers,
        ArrayList<Class> types, String property, Method method)
    {
        int index = indexOf(names, property);
        Class<?> type = method.getParameterTypes()[0];
        if (index < 0)
        {
            names.add(property);
            readers.add(null);
            writers.add(method);
            types.add(type);
        }
        else
        {
            writers.set(index, method);
            if (readers.get(index) == null)
            {
                types.set(index, type);
            }
        }
    }

    private static final class BeanInfoImpl extends SimpleBeanInfo
    {
        private final BeanDescriptor beanDescriptor;
        private final PropertyDescriptor[] properties;
        private final MethodDescriptor[] methods;

        BeanInfoImpl(BeanDescriptor beanDescriptor,
            PropertyDescriptor[] properties, MethodDescriptor[] methods)
        {
            this.beanDescriptor = beanDescriptor;
            this.properties = properties;
            this.methods = methods;
        }

        public BeanDescriptor getBeanDescriptor()
        {
            return beanDescriptor;
        }

        public PropertyDescriptor[] getPropertyDescriptors()
        {
            return properties;
        }

        public MethodDescriptor[] getMethodDescriptors()
        {
            return methods;
        }
    }
}
