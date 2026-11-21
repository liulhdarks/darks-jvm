import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

// Method/Field/Constructor equality and hashing back reflection-keyed caches
// (Spring resolves @AliasFor through a Method-keyed map), so these have to
// follow the JDK contract: equal members are interchangeable, distinct
// overloads are not.
public class JdkReflectionEqualityTest
{
    public static class Sample
    {
        public int field;
        public String other;

        public Sample() {}
        public Sample(int field) { this.field = field; }
        public Sample(String other) { this.other = other; }

        public void noArgs() {}
        public int first() { return 1; }
        public String second() { return "s"; }
        public int overloaded() { return 0; }
        public int overloaded(int x) { return x; }
    }

    private static int checks;
    private static int failures;

    private static void check(String name, boolean condition)
    {
        checks++;
        if (!condition)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args) throws Exception
    {
        Class<?> sample = Sample.class;
        Class<?>[] none = new Class<?>[0];

        Method firstA = sample.getDeclaredMethod("first", none);
        Method firstB = sample.getDeclaredMethod("first", none);
        check("method self", firstA.equals(firstA));
        check("method same declaration", firstA.equals(firstB));
        check("method hash stable", firstA.hashCode() == firstB.hashCode());

        Method second = sample.getDeclaredMethod("second", none);
        check("method different name", !firstA.equals(second));

        Method overloadedA = sample.getDeclaredMethod("overloaded", none);
        Method overloadedB = sample.getDeclaredMethod("overloaded", new Class[] { int.class });
        check("method overload not equal", !overloadedA.equals(overloadedB));
        check("method noarg equals itself", overloadedA.equals(sample.getDeclaredMethod("overloaded", none)));
        check("method arity differs", !overloadedA.equals(sample.getDeclaredMethod("overloaded", new Class[] { int.class })));

        check("method null", !firstA.equals(null));
        check("method other type", !firstA.equals("first"));
        check("method toString names", firstA.toString().indexOf("first") >= 0);

        HashMap<Method, String> cache = new HashMap<Method, String>();
        cache.put(firstA, "first");
        cache.put(second, "second");
        cache.put(overloadedA, "noarg");
        cache.put(overloadedB, "onearg");
        check("method cache size", cache.size() == 4);
        check("method cache lookup", "first".equals(cache.get(firstB)));
        check("method cache overloads", "noarg".equals(cache.get(sample.getDeclaredMethod("overloaded", none))) &&
            "onearg".equals(cache.get(sample.getDeclaredMethod("overloaded", new Class[] { int.class }))));

        Field fieldA = sample.getDeclaredField("field");
        Field fieldB = sample.getDeclaredField("field");
        Field other = sample.getDeclaredField("other");
        check("field same declaration", fieldA.equals(fieldB));
        check("field hash stable", fieldA.hashCode() == fieldB.hashCode());
        check("field different name", !fieldA.equals(other));
        check("field null", !fieldA.equals(null));
        check("field other type", !fieldA.equals("field"));
        check("field self", fieldA.equals(fieldA));

        Map<Field, String> fields = new HashMap<Field, String>();
        fields.put(fieldA, "field");
        fields.put(other, "other");
        check("field cache size", fields.size() == 2);
        check("field cache lookup", "field".equals(fields.get(fieldB)));

        Constructor<?> ctorA = sample.getDeclaredConstructor(none);
        Constructor<?> ctorB = sample.getDeclaredConstructor(none);
        Constructor<?> ctorInt = sample.getDeclaredConstructor(new Class[] { int.class });
        Constructor<?> ctorString = sample.getDeclaredConstructor(new Class[] { String.class });
        check("constructor same declaration", ctorA.equals(ctorB));
        check("constructor hash stable", ctorA.hashCode() == ctorB.hashCode());
        check("constructor overload differs", !ctorInt.equals(ctorString));
        check("constructor null", !ctorA.equals(null));
        check("constructor other type", !ctorA.equals("ctor"));

        Map<Constructor<?>, String> ctors = new HashMap<Constructor<?>, String>();
        ctors.put(ctorA, "default");
        ctors.put(ctorInt, "int");
        ctors.put(ctorString, "string");
        check("constructor cache size", ctors.size() == 3);
        check("constructor cache lookup",
            "default".equals(ctors.get(sample.getDeclaredConstructor(none))));

        Method runA = Runnable.class.getMethod("run", none);
        Method runB = Runnable.class.getMethod("run", none);
        check("interface method equality", runA.equals(runB) && runA.hashCode() == runB.hashCode());

        System.out.println("reflection-equality checks=" + checks + " failures=" + failures);
        if (failures == 0)
        {
            System.out.println("reflection equality: ok");
        }
    }
}
