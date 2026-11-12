import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// Exercises the reflection surface that is not covered by ReflectDemo:
// access checks, static/private members, overloads, virtual dispatch,
// exception wrapping, constructors, descriptor-derived types and the
// documented UnsupportedOperationException for generic signatures.
public class ReflectionAdvanced
{
    public interface Marker
    {
    }

    public static class Base
    {
        public String who()
        {
            return "base";
        }

        public int over(int value)
        {
            return value + 1;
        }
    }

    public static class Derived extends Base implements Marker
    {
        public String who()
        {
            return "derived";
        }

        public int over(int value)
        {
            return value * 2;
        }

        public int over(String text)
        {
            return text.length() + 100;
        }
    }

    public static class Subject
    {
        public int pub = 7;
        private int secret = 41;
        public static long counter = 1000;
        private static String hidden = "h";

        public Subject()
        {
        }

        private Subject(int value)
        {
            secret = value;
        }

        public int add(int a, int b)
        {
            return a + b;
        }

        public static int twice(int value)
        {
            return value * 2;
        }

        private String peek()
        {
            return hidden;
        }

        public void boom()
        {
            throw new IllegalStateException("boom");
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

    public static void main(String[] args) throws Exception
    {
        Class<?> subjectClass = Class.forName("ReflectionAdvanced$Subject");
        Class<?> baseClass = Class.forName("ReflectionAdvanced$Base");
        Class<?> derivedClass = Class.forName("ReflectionAdvanced$Derived");

        // Class hierarchy and assignability.
        check("superclass", subjectClass.getSuperclass() == Object.class);
        check("derived-superclass", derivedClass.getSuperclass() == baseClass);
        check("assignable", baseClass.isAssignableFrom(derivedClass));
        check("not-assignable", !derivedClass.isAssignableFrom(baseClass));
        check("interfaces", derivedClass.getInterfaces().length == 1);
        check("interface-flag", derivedClass.getInterfaces()[0].isInterface());
        Object derived = derivedClass.getDeclaredConstructor().newInstance();
        check("is-instance", baseClass.isInstance(derived));
        check("not-instance", !subjectClass.isInstance(derived));

        // Public / private instance and static fields.
        Field pub = subjectClass.getDeclaredField("pub");
        Field secret = subjectClass.getDeclaredField("secret");
        Field counter = subjectClass.getDeclaredField("counter");
        Field hidden = subjectClass.getDeclaredField("hidden");
        Object subject = subjectClass.getDeclaredConstructor().newInstance();
        check("field-public", ((Integer)pub.get(subject)).intValue() == 7);
        check("field-type-int", secret.getType() == int.class);
        check("field-type-long", counter.getType() == long.class);

        boolean denied = false;
        try
        {
            secret.get(subject);
        }
        catch (IllegalAccessException expected)
        {
            denied = true;
        }
        check("private-denied", denied);
        check("accessible-default", !secret.isAccessible());
        secret.setAccessible(true);
        check("accessible-set", secret.isAccessible());
        check("private-get", ((Integer)secret.get(subject)).intValue() == 41);
        secret.set(subject, Integer.valueOf(55));
        check("private-set", ((Integer)secret.get(subject)).intValue() == 55);

        check("static-get", ((Long)counter.get(null)).longValue() == 1000L);
        counter.set(null, Long.valueOf(2000L));
        check("static-set", ((Long)counter.get(null)).longValue() == 2000L);
        hidden.setAccessible(true);
        check("static-ref", "h".equals(hidden.get(null)));

        boolean typeError = false;
        try
        {
            secret.set(subject, "not-an-int");
        }
        catch (IllegalArgumentException expected)
        {
            typeError = true;
        }
        check("field-type-mismatch", typeError);

        // Methods: overload selection, virtual dispatch, static calls.
        Method add = subjectClass.getDeclaredMethod("add", int.class, int.class);
        check("param-count", add.getParameterTypes().length == 2);
        check("param-type", add.getParameterTypes()[0] == int.class);
        check("return-type", add.getReturnType() == int.class);
        check("invoke", ((Integer)add.invoke(subject, Integer.valueOf(2),
            Integer.valueOf(3))).intValue() == 5);

        Method overInt = baseClass.getMethod("over", int.class);
        check("virtual-int", ((Integer)overInt.invoke(derived,
            Integer.valueOf(10))).intValue() == 20);
        Method overString = derivedClass.getDeclaredMethod("over", String.class);
        check("overload-string", ((Integer)overString.invoke(derived, "abcd"))
            .intValue() == 104);

        Method who = baseClass.getMethod("who");
        check("virtual-dispatch", "derived".equals(who.invoke(derived)));

        Method twice = subjectClass.getDeclaredMethod("twice", int.class);
        check("static-invoke", ((Integer)twice.invoke(null, Integer.valueOf(21)))
            .intValue() == 42);

        boolean privateDenied = false;
        Method peek = subjectClass.getDeclaredMethod("peek");
        try
        {
            peek.invoke(subject);
        }
        catch (IllegalAccessException expected)
        {
            privateDenied = true;
        }
        check("private-method-denied", privateDenied);
        peek.setAccessible(true);
        check("private-method", "h".equals(peek.invoke(subject)));

        Method boom = subjectClass.getDeclaredMethod("boom");
        boolean wrapped = false;
        try
        {
            boom.invoke(subject);
        }
        catch (InvocationTargetException expected)
        {
            Throwable target = expected.getTargetException();
            wrapped = target instanceof IllegalStateException &&
                "boom".equals(target.getMessage());
        }
        check("invocation-target", wrapped);

        // Constructors, including a private one.
        Constructor<?>[] constructors = subjectClass.getDeclaredConstructors();
        check("constructor-count", constructors.length == 2);
        Constructor<?> privateCtor = subjectClass.getDeclaredConstructor(int.class);
        boolean ctorDenied = false;
        try
        {
            privateCtor.newInstance(Integer.valueOf(99));
        }
        catch (IllegalAccessException expected)
        {
            ctorDenied = true;
        }
        check("private-ctor-denied", ctorDenied);
        privateCtor.setAccessible(true);
        Object built = privateCtor.newInstance(Integer.valueOf(99));
        check("private-ctor", ((Integer)secret.get(built)).intValue() == 99);
        Constructor<?> publicCtor = subjectClass.getDeclaredConstructor();
        check("public-ctor", subjectClass.isInstance(publicCtor.newInstance()));
        check("ctor-params", privateCtor.getParameterTypes().length == 1 &&
            privateCtor.getParameterTypes()[0] == int.class);

        // Array and primitive Class representations.
        Class<?> intArray = Class.forName("[I");
        check("array-flag", intArray.isArray());
        check("array-name", "[I".equals(intArray.getName()));
        check("array-component", intArray.getComponentType() == int.class);
        check("primitive-flag", int.class.isPrimitive());
        check("primitive-type", Integer.TYPE == int.class);
        Class<?> stringArray = Class.forName("[Ljava/lang/String;");
        check("ref-array", stringArray.getComponentType() == String.class);

        // Missing members surface as the documented exceptions.
        boolean noField = false;
        try
        {
            subjectClass.getDeclaredField("nope");
        }
        catch (NoSuchFieldException expected)
        {
            noField = true;
        }
        check("missing-field", noField);
        boolean noMethod = false;
        try
        {
            subjectClass.getDeclaredMethod("nope");
        }
        catch (NoSuchMethodException expected)
        {
            noMethod = true;
        }
        check("missing-method", noMethod);
        boolean noClass = false;
        try
        {
            Class.forName("no.such.Class");
        }
        catch (ClassNotFoundException expected)
        {
            noClass = true;
        }
        check("missing-class", noClass);

        // Generic signatures are explicitly unsupported.
        check("generic-super", unsupported(subjectClass));
        check("generic-type", unsupportedField(secret));
        check("generic-return", unsupportedMethod(add));
        check("generic-ctor", unsupportedConstructor(privateCtor));

        System.out.println("reflect-adv checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
        System.out.println("reflection advanced: ok");
    }

    private static boolean unsupported(Class<?> clazz) throws Exception
    {
        try
        {
            clazz.getGenericSuperclass();
        }
        catch (UnsupportedOperationException expected)
        {
            return true;
        }
        return false;
    }

    private static boolean unsupportedField(Field field) throws Exception
    {
        try
        {
            field.getGenericType();
        }
        catch (UnsupportedOperationException expected)
        {
            return true;
        }
        return false;
    }

    private static boolean unsupportedMethod(Method method) throws Exception
    {
        try
        {
            method.getGenericReturnType();
        }
        catch (UnsupportedOperationException expected)
        {
            return true;
        }
        return false;
    }

    private static boolean unsupportedConstructor(Constructor<?> ctor) throws Exception
    {
        try
        {
            ctor.getGenericParameterTypes();
        }
        catch (UnsupportedOperationException expected)
        {
            return true;
        }
        return false;
    }
}
