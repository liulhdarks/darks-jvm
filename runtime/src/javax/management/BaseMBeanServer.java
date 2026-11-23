package javax.management;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/** Tiny reflective MBean server used only when something explicitly creates one. */
class BaseMBeanServer implements MBeanServer
{
    private final String domain;
    private final HashMap<ObjectName, Object> beans = new HashMap<ObjectName, Object>();

    BaseMBeanServer(String domain)
    {
        this.domain = domain == null ? "DefaultDomain" : domain;
        // The JDK registers the standard delegate in every MBeanServer it
        // creates, so a fresh server reports one MBean and code that looks the
        // delegate up by name finds it.
        this.beans.put(MBeanServerDelegate.DELEGATE_NAME, new MBeanServerDelegate());
    }

    public String getDefaultDomain()
    {
        return domain;
    }

    public String[] getDomains()
    {
        return new String[] { domain };
    }

    public Integer getMBeanCount()
    {
        return Integer.valueOf(beans.size());
    }

    public ObjectInstance registerMBean(Object object, ObjectName name)
        throws InstanceAlreadyExistsException, MBeanRegistrationException,
        NotCompliantMBeanException
    {
        if (name == null)
        {
            throw new MBeanRegistrationException(new IllegalArgumentException("null name"));
        }
        if (beans.containsKey(name))
        {
            throw new InstanceAlreadyExistsException(name.toString());
        }
        ObjectName registered = name;
        if (object instanceof MBeanRegistration)
        {
            try
            {
                registered = ((MBeanRegistration)object).preRegister(this, name);
            }
            catch (Exception e)
            {
                throw new MBeanRegistrationException(e);
            }
            if (registered == null)
            {
                return null;
            }
        }
        beans.put(registered, object);
        if (object instanceof MBeanRegistration)
        {
            ((MBeanRegistration)object).postRegister(Boolean.TRUE);
        }
        return new ObjectInstance(registered, object.getClass().getName());
    }

    public void unregisterMBean(ObjectName name)
        throws InstanceNotFoundException, MBeanRegistrationException
    {
        Object bean = find(name);
        if (bean instanceof MBeanRegistration)
        {
            try
            {
                ((MBeanRegistration)bean).preDeregister();
            }
            catch (Exception e)
            {
                throw new MBeanRegistrationException(e);
            }
        }
        beans.remove(name);
        if (bean instanceof MBeanRegistration)
        {
            ((MBeanRegistration)bean).postDeregister();
        }
    }

    public boolean isRegistered(ObjectName name)
    {
        return beans.containsKey(name);
    }

    public ObjectInstance getObjectInstance(ObjectName name)
        throws InstanceNotFoundException
    {
        return new ObjectInstance(name, find(name).getClass().getName());
    }

    private Object find(ObjectName name) throws InstanceNotFoundException
    {
        Object bean = beans.get(name);
        if (bean == null)
        {
            throw new InstanceNotFoundException(String.valueOf(name));
        }
        return bean;
    }

    public Object getAttribute(ObjectName name, String attribute)
        throws MBeanException, AttributeNotFoundException,
        InstanceNotFoundException, ReflectionException
    {
        Object bean = find(name);
        try
        {
            return invokeAccessible(
                bean.getClass().getMethod("get" + attribute), bean, null);
        }
        catch (Exception e)
        {
            throw new MBeanException(e);
        }
    }

    public void setAttribute(ObjectName name, Attribute attribute)
        throws InstanceNotFoundException, AttributeNotFoundException,
        InvalidAttributeValueException, MBeanException, ReflectionException
    {
        Object bean = find(name);
        try
        {
            Class[] types = new Class[] { attribute.getValue() == null
                ? Object.class : attribute.getValue().getClass() };
            invokeAccessible(
                bean.getClass().getMethod("set" + attribute.getName(), types),
                bean, new Object[] { attribute.getValue() });
        }
        catch (Exception e)
        {
            throw new MBeanException(e);
        }
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes)
        throws InstanceNotFoundException, ReflectionException
    {
        AttributeList result = new AttributeList();
        for (int i = 0; i < attributes.length; i++)
        {
            try
            {
                result.add(new Attribute(attributes[i], getAttribute(name, attributes[i])));
            }
            catch (Exception ignored)
            {
            }
        }
        return result;
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes)
        throws InstanceNotFoundException, ReflectionException
    {
        AttributeList result = new AttributeList();
        for (int i = 0; i < attributes.size(); i++)
        {
            Attribute attribute = (Attribute)attributes.get(i);
            try
            {
                setAttribute(name, attribute);
                result.add(attribute);
            }
            catch (Exception ignored)
            {
            }
        }
        return result;
    }

    public Object invoke(ObjectName name, String operationName, Object[] params,
        String[] signature)
        throws InstanceNotFoundException, MBeanException, ReflectionException
    {
        Object bean = find(name);
        try
        {
            Class[] types = new Class[signature.length];
            for (int i = 0; i < signature.length; i++)
            {
                types[i] = classOf(signature[i]);
            }
            return invokeAccessible(
                bean.getClass().getMethod(operationName, types), bean, params);
        }
        catch (Exception e)
        {
            throw new MBeanException(e);
        }
    }

    // MBeans are reachable through their management interface regardless of
    // the package their implementation class lives in.  The JDK server invokes
    // through sun.reflect.misc.MethodUtil, which makes the member accessible
    // first; do the same here so caller-sensitive access checks accept it.
    private static Object invokeAccessible(java.lang.reflect.Method method,
        Object bean, Object[] args) throws Exception
    {
        if (!method.isAccessible())
        {
            method.setAccessible(true);
        }
        return method.invoke(bean, args);
    }

    private static Class classOf(String name) throws ClassNotFoundException
    {
        if ("boolean".equals(name)) return Boolean.TYPE;
        if ("int".equals(name)) return Integer.TYPE;
        if ("long".equals(name)) return Long.TYPE;
        if ("short".equals(name)) return Short.TYPE;
        if ("byte".equals(name)) return Byte.TYPE;
        if ("char".equals(name)) return Character.TYPE;
        if ("float".equals(name)) return Float.TYPE;
        if ("double".equals(name)) return Double.TYPE;
        if ("void".equals(name)) return Void.TYPE;
        return Class.forName(name);
    }

    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException
    {
        Object bean = find(name);
        return new MBeanInfo(bean.getClass().getName(), "", null, null, null, null);
    }

    public java.util.Set<ObjectName> queryNames(ObjectName name, QueryExp query)
    {
        java.util.HashSet<ObjectName> result = new java.util.HashSet<ObjectName>();
        java.util.Iterator<ObjectName> keys = beans.keySet().iterator();
        while (keys.hasNext())
        {
            ObjectName key = keys.next();
            if (name == null || name.equals(key))
            {
                result.add(key);
            }
        }
        return result;
    }

    public java.util.Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query)
    {
        java.util.HashSet<ObjectInstance> result = new java.util.HashSet<ObjectInstance>();
        java.util.Iterator<ObjectName> keys = queryNames(name, query).iterator();
        while (keys.hasNext())
        {
            ObjectName key = keys.next();
            result.add(new ObjectInstance(key, beans.get(key).getClass().getName()));
        }
        return result;
    }

    public boolean isInstanceOf(ObjectName name, String className)
        throws InstanceNotFoundException
    {
        try
        {
            Class target = classOf(className);
            return target.isInstance(find(name));
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }

    public void addNotificationListener(ObjectName name, NotificationListener listener,
        NotificationFilter filter, Object handback)
        throws InstanceNotFoundException
    {
        find(name);
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener,
        NotificationFilter filter, Object handback)
        throws InstanceNotFoundException, ListenerNotFoundException
    {
        find(name);
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener)
        throws InstanceNotFoundException, ListenerNotFoundException
    {
        find(name);
    }
}
