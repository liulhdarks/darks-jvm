import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// Annotation semantics: element defaults, meta annotations, @Inherited,
// @Repeatable containers, nested annotations and annotation proxies.
public class AnnotationDemo
{
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD,
        ElementType.CONSTRUCTOR })
    public @interface Color
    {
        String value() default "red";

        int priority() default 3;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Repeatable(Tags.class)
    public @interface Tag
    {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface Tags
    {
        Tag[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Nested
    {
        Color color() default @Color("blue");
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Names
    {
        String[] value();

        int[] numbers() default { 1, 2, 3 };
    }

    public enum Flavor
    {
        SWEET, SOUR
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Kind
    {
        Flavor value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Ref
    {
        Class<?> value();
    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface InheritedMark
    {
        String value();
    }

    @Color(value = "green", priority = 7)
    @Nested
    @Names(value = { "a", "b" }, numbers = { 4, 5 })
    @Kind(Flavor.SOUR)
    @Ref(String.class)
    @Tag("one")
    @Tag("two")
    public static class Decorated
    {
        @Color
        public int field;

        @Color(priority = 9)
        public void method()
        {
        }

        @Color("ctor")
        public Decorated()
        {
        }
    }

    @Color
    public static class Defaults
    {
    }

    @InheritedMark("from-base")
    public static class Base
    {
    }

    public static class Derived extends Base
    {
    }

    @InheritedMark("on-derived")
    public static class Overriding extends Base
    {
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
        Color color = Decorated.class.getAnnotation(Color.class);
        check("class-annotation-present", color != null);
        check("class-annotation-value", "green".equals(color.value()));
        check("class-annotation-priority", color.priority() == 7);
        check("class-annotation-type", color.annotationType() == Color.class);
        check("annotation-present", Decorated.class.isAnnotationPresent(Color.class));
        check("annotation-absent",
            Decorated.class.getAnnotation(Kind.class) != null &&
            Decorated.class.getAnnotation(Tag.class) == null);

        Color defaults = Defaults.class.getAnnotation(Color.class);
        check("default-value", "red".equals(defaults.value()));
        check("default-priority", defaults.priority() == 3);

        Field field = Decorated.class.getDeclaredField("field");
        Color fieldColor = field.getAnnotation(Color.class);
        check("field-annotation", fieldColor != null &&
            "red".equals(fieldColor.value()));
        check("field-annotation-present",
            field.isAnnotationPresent(Color.class));

        Method method = Decorated.class.getDeclaredMethod("method");
        Color methodColor = method.getAnnotation(Color.class);
        check("method-annotation", methodColor != null &&
            "red".equals(methodColor.value()) && methodColor.priority() == 9);

        java.lang.reflect.Constructor<?> ctor =
            Decorated.class.getDeclaredConstructor();
        Color ctorColor = ctor.getAnnotation(Color.class);
        check("constructor-annotation", ctorColor != null &&
            "ctor".equals(ctorColor.value()));

        Nested nested = Decorated.class.getAnnotation(Nested.class);
        check("nested-annotation", nested != null);
        check("nested-annotation-value",
            nested != null && nested.color() != null &&
            "blue".equals(nested.color().value()));

        Names names = Decorated.class.getAnnotation(Names.class);
        check("array-annotation", names != null);
        check("array-annotation-values",
            names.value().length == 2 &&
            "a".equals(names.value()[0]) &&
            "b".equals(names.value()[1]));
        check("array-annotation-numbers",
            names.numbers().length == 2 &&
            names.numbers()[0] == 4 && names.numbers()[1] == 5);

        Kind kind = Decorated.class.getAnnotation(Kind.class);
        check("enum-annotation",
            kind != null && kind.value() == Flavor.SOUR);

        Ref ref = Decorated.class.getAnnotation(Ref.class);
        check("class-annotation-value-ref",
            ref != null && ref.value() == String.class);

        Tag[] tags = Decorated.class.getAnnotationsByType(Tag.class);
        check("repeatable-count", tags.length == 2);
        check("repeatable-order",
            "one".equals(tags[0].value()) && "two".equals(tags[1].value()));
        Tag[] declaredTags =
            Decorated.class.getDeclaredAnnotationsByType(Tag.class);
        check("repeatable-declared", declaredTags.length == 2);
        Tags container = Decorated.class.getAnnotation(Tags.class);
        check("repeatable-container", container != null &&
            container.value().length == 2 &&
            "one".equals(container.value()[0].value()));

        check("meta-retention",
            Color.class.getAnnotation(Retention.class) != null &&
            Color.class.getAnnotation(Retention.class).value() ==
            RetentionPolicy.RUNTIME);
        check("meta-target",
            Color.class.getAnnotation(Target.class) != null &&
            Color.class.getAnnotation(Target.class).value().length == 4);
        check("meta-absent", Color.class.getAnnotation(Inherited.class) == null);

        InheritedMark inherited = Derived.class.getAnnotation(InheritedMark.class);
        check("inherited-annotation", inherited != null &&
            "from-base".equals(inherited.value()));
        check("inherited-not-declared",
            Derived.class.getDeclaredAnnotation(InheritedMark.class) == null);
        check("inherited-in-annotations",
            findMark(Derived.class.getAnnotations()) != null);
        check("override-annotation",
            "on-derived".equals(
                Overriding.class.getAnnotation(InheritedMark.class).value()));

        Color first = Decorated.class.getAnnotation(Color.class);
        Color second = Decorated.class.getAnnotation(Color.class);
        check("annotation-equals", first.equals(second));
        check("annotation-hash", first.hashCode() == second.hashCode());
        check("annotation-not-equals-default", !first.equals(defaults));
        check("annotation-toString", first.toString().indexOf("Color") >= 0);
        check("annotation-to-string-at", first.toString().charAt(0) == '@');
        check("declared-count",
            Decorated.class.getDeclaredAnnotations().length == 6);

        System.out.println("annotation checks=" + checks +
            " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
        System.out.println("annotations: ok");
    }

    private static InheritedMark findMark(Annotation[] annotations)
    {
        for (int i = 0; i < annotations.length; i++)
        {
            if (annotations[i] instanceof InheritedMark)
            {
                return (InheritedMark)annotations[i];
            }
        }
        return null;
    }
}
