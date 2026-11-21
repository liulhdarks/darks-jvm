import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.Transient;
import java.beans.SimpleBeanInfo;
import java.lang.reflect.Method;

// java.beans: reflection based property/method discovery and descriptor
// metadata used by containers to bind beans.
public class BeansDemo
{
    public static class Person
    {
        private String name = "nobody";
        private int age;
        private boolean active;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public int getAge()
        {
            return age;
        }

        public void setAge(int age)
        {
            this.age = age;
        }

        public boolean isActive()
        {
            return active;
        }

        public void setActive(boolean active)
        {
            this.active = active;
        }

        public String describe()
        {
            return name + ":" + age;
        }

        public static int helper()
        {
            return 1;
        }
    }

    public static class Employee extends Person
    {
        private long salary;

        public long getSalary()
        {
            return salary;
        }

        public void setSalary(long salary)
        {
            this.salary = salary;
        }

        public void promote()
        {
        }
    }

    public static class Empty
    {
    }

    public static class Secret
    {
        @Transient
        public String getHidden()
        {
            return "hidden";
        }

        @Transient(false)
        public String getVisible()
        {
            return "visible";
        }
    }

    private static int checks;
    private static int failures;

    private static void check(String name, boolean ok)
    {
        checks++;
        if (!ok)
        {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    private static PropertyDescriptor property(BeanInfo info, String name)
    {
        PropertyDescriptor[] all = info.getPropertyDescriptors();
        for (int i = 0; i < all.length; i++)
        {
            if (all[i].getName().equals(name))
            {
                return all[i];
            }
        }
        return null;
    }

    private static boolean hasMethod(BeanInfo info, String name)
    {
        MethodDescriptor[] all = info.getMethodDescriptors();
        for (int i = 0; i < all.length; i++)
        {
            if (all[i].getName().equals(name))
            {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception
    {
        Package stringPackage = String.class.getPackage();
        check("package-name",
            stringPackage != null && "java.lang".equals(stringPackage.getName()));
        check("package-cached",
            stringPackage != null && stringPackage == Object.class.getPackage());
        check("package-default-null", BeansDemo.class.getPackage() == null);
        check("package-array-null", String[].class.getPackage() == null);
        check("package-primitive-null", int.class.getPackage() == null);

        check("decapitalize-name", "name".equals(Introspector.decapitalize("Name")));
        check("decapitalize-url", "URL".equals(Introspector.decapitalize("URL")));
        check("decapitalize-lower", "x".equals(Introspector.decapitalize("x")));

        BeanInfo info = Introspector.getBeanInfo(Person.class);
        check("bean-descriptor", info.getBeanDescriptor() != null &&
            info.getBeanDescriptor().getBeanClass() == Person.class);
        check("bean-descriptor-name",
            "BeansDemo$Person".equals(info.getBeanDescriptor().getName()));

        PropertyDescriptor[] properties = info.getPropertyDescriptors();
        check("property-count", properties.length == 3);
        check("property-sorted",
            "active".equals(properties[0].getName()) &&
            "age".equals(properties[1].getName()) &&
            "name".equals(properties[2].getName()));

        PropertyDescriptor name = property(info, "name");
        PropertyDescriptor age = property(info, "age");
        PropertyDescriptor active = property(info, "active");
        check("property-name-type", name != null && name.getPropertyType() == String.class);
        check("property-name-reader", name != null && name.getReadMethod() != null &&
            "getName".equals(name.getReadMethod().getName()));
        check("property-name-writer", name != null && name.getWriteMethod() != null &&
            "setName".equals(name.getWriteMethod().getName()));
        check("property-age-type", age != null && age.getPropertyType() == int.class);
        check("property-active-type",
            active != null && active.getPropertyType() == boolean.class);
        check("property-active-reader",
            active != null && active.getReadMethod() != null &&
            "isActive".equals(active.getReadMethod().getName()));

        Person person = new Person();
        name.getWriteMethod().invoke(person, new Object[] { "ada" });
        check("property-write", "ada".equals(person.getName()));
        check("property-read", "ada".equals(name.getReadMethod().invoke(person)));

        check("method-descriptor-count",
            info.getMethodDescriptors().length >= 7);
        check("method-descriptor-get", hasMethod(info, "getName"));
        check("method-descriptor-describe", hasMethod(info, "describe"));
        check("method-descriptor-static", hasMethod(info, "helper"));
        check("method-descriptor-method",
            info.getMethodDescriptors()[0].getMethod() != null);
        check("default-property-index", info.getDefaultPropertyIndex() == -1);

        BeanInfo stopped = Introspector.getBeanInfo(Employee.class, Person.class);
        check("stop-property-count",
            stopped.getPropertyDescriptors().length == 1);
        check("stop-property-salary",
            property(stopped, "salary") != null &&
            property(stopped, "salary").getPropertyType() == long.class);
        check("stop-excludes-inherited", property(stopped, "name") == null);
        check("stop-method-promote", hasMethod(stopped, "promote"));
        check("stop-method-excludes", !hasMethod(stopped, "getAge"));

        PropertyDescriptor direct =
            new PropertyDescriptor("name", Person.class);
        check("direct-constructor-type", direct.getPropertyType() == String.class);
        check("direct-constructor-reader",
            direct.getReadMethod() != null &&
            "getName".equals(direct.getReadMethod().getName()));

        Method method = Person.class.getMethod("describe");
        MethodDescriptor methodDescriptor = new MethodDescriptor(method);
        check("method-descriptor-name", "describe".equals(methodDescriptor.getName()));
        check("method-descriptor-getMethod",
            methodDescriptor.getMethod() == method);
        methodDescriptor.setShortDescription("describes the person");
        check("method-descriptor-description",
            "describes the person".equals(methodDescriptor.getShortDescription()));

        BeanInfo empty = Introspector.getBeanInfo(Empty.class);
        check("empty-properties", empty.getPropertyDescriptors().length == 0);

        check("transient-default",
            Secret.class.getMethod("getHidden").isAnnotationPresent(Transient.class) &&
            Secret.class.getMethod("getHidden").getAnnotation(Transient.class).value());
        check("transient-false",
            !Secret.class.getMethod("getVisible").getAnnotation(Transient.class).value());

        SimpleBeanInfo simple = new SimpleBeanInfo();
        check("simple-bean-descriptor", simple.getBeanDescriptor() == null);
        check("simple-properties", simple.getPropertyDescriptors().length == 0);
        check("simple-methods", simple.getMethodDescriptors().length == 0);

        boolean failed = false;
        try
        {
            new PropertyDescriptor("missing", Person.class);
        }
        catch (IntrospectionException expected)
        {
            failed = true;
        }
        check("missing-property", failed);

        BeanDescriptor descriptor = new BeanDescriptor(Person.class);
        check("bean-descriptor-direct",
            descriptor.getBeanClass() == Person.class &&
            descriptor.getCustomizerClass() == null);

        System.out.println("beans checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
        System.out.println("java beans: ok");
    }
}
