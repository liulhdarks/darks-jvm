import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

@Retention(RetentionPolicy.RUNTIME)
@interface M2Inner
{
    String value() default "inner";
}

@Retention(RetentionPolicy.RUNTIME)
@interface M2Cfg
{
    String name() default "anon";
    int count() default 3;
    Class<?> type() default String.class;
    ElementType kind() default ElementType.TYPE;
    int[] nums() default {1, 2, 3};
    M2Inner inner() default @M2Inner;
}

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@interface M2Marker
{
    String value() default "m";
}

@Retention(RetentionPolicy.CLASS)
@interface M2ClassOnly
{
}

@M2Cfg
@M2Marker("base")
@M2ClassOnly
class M2AnnotatedParent
{
}

class M2AnnotatedChild extends M2AnnotatedParent
{
}

@M2Cfg(name = "explicit", count = 9)
class M2AnnotatedExplicit
{
}

public class M2AnnotationChecks
{
    private static int checks;
    private static int failures;

    private static void check(boolean condition, String message)
    {
        checks++;
        if (condition)
        {
            System.out.println("ok " + message);
        }
        else
        {
            failures++;
            System.out.println("FAIL " + message);
        }
    }

    private static boolean hasAnnotation(Annotation[] all, String name)
    {
        for (int i = 0; i < all.length; i++)
        {
            if (all[i].annotationType().getName().equals(name))
            {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception
    {
        check(M2Cfg.class.isAnnotation(), "Class.isAnnotation is true for annotations");
        check(M2Cfg.class.isInterface(), "annotations are interfaces");
        check(!M2AnnotationChecks.class.isAnnotation(),
            "Class.isAnnotation is false for ordinary classes");
        check(!M2Marker.class.isAnnotationPresent(M2Cfg.class),
            "annotation types do not carry unrelated annotations");

        Method name = M2Cfg.class.getDeclaredMethod("name", new Class[0]);
        Method count = M2Cfg.class.getDeclaredMethod("count", new Class[0]);
        Method type = M2Cfg.class.getDeclaredMethod("type", new Class[0]);
        Method kind = M2Cfg.class.getDeclaredMethod("kind", new Class[0]);
        Method nums = M2Cfg.class.getDeclaredMethod("nums", new Class[0]);
        Method inner = M2Cfg.class.getDeclaredMethod("inner", new Class[0]);

        check("anon".equals(name.getDefaultValue()), "String default value");
        check(Integer.valueOf(3).equals(count.getDefaultValue()), "int default value");
        check(type.getDefaultValue() == String.class, "Class default value");
        check(kind.getDefaultValue() == ElementType.TYPE, "enum default value");
        int[] numbers = (int[])nums.getDefaultValue();
        check(numbers != null && numbers.length == 3 && numbers[0] == 1 &&
            numbers[2] == 3, "array default value");
        Object innerDefault = inner.getDefaultValue();
        check(innerDefault instanceof M2Inner &&
            "inner".equals(((M2Inner)innerDefault).value()),
            "nested annotation default value");
        check(M2AnnotationChecks.class.getDeclaredMethod("main",
            new Class[] {String[].class}).getDefaultValue() == null,
            "ordinary methods have no annotation default");

        M2Cfg explicit = M2AnnotatedExplicit.class.getAnnotation(M2Cfg.class);
        check(explicit != null, "runtime annotation is visible");
        check("explicit".equals(explicit.name()), "explicit annotation element");
        check(explicit.count() == 9, "explicit int element");
        check(explicit.type() == String.class, "proxy fills the Class default");
        check(explicit.kind() == ElementType.TYPE, "proxy fills the enum default");
        check(explicit.nums().length == 3 && explicit.nums()[1] == 2,
            "proxy fills the array default");

        Annotation[] parentAnnotations = M2AnnotatedParent.class.getAnnotations();
        check(hasAnnotation(parentAnnotations, "M2Marker"),
            "runtime annotation present on the declaring class");
        check(!hasAnnotation(parentAnnotations, "M2ClassOnly"),
            "CLASS retention annotations are not visible at runtime");
        M2Marker inherited = M2AnnotatedChild.class.getAnnotation(M2Marker.class);
        check(inherited != null && "base".equals(inherited.value()),
            "@Inherited annotations are visible on subclasses");

        System.out.println("m2 annotation checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            throw new RuntimeException("m2 annotation checks failed");
        }
        System.out.println("m2 annotations: ok");
    }
}
