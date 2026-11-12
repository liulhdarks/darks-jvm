import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// Compiled with `javac --release 11`; covers runtime-retained annotations and
// the core java.lang.reflect entry points.
public class ReflectDemo
{
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
    public @interface Info
    {
        String name();

        int count() default 7;
    }

    @Info(name = "cls")
    public static class Subject implements Runnable
    {
        @Info(name = "field", count = 3)
        public int value = 5;

        @Info(name = "method", count = 4)
        public int add(int delta)
        {
            return value + delta;
        }

        public Subject()
        {
        }

        public void run()
        {
        }
    }

    public static void main(String[] args) throws Exception
    {
        Class<?> clazz = Class.forName("ReflectDemo$Subject");
        Info classInfo = clazz.getAnnotation(Info.class);

        Field field = clazz.getDeclaredField("value");
        Info fieldInfo = field.getAnnotation(Info.class);

        Method method = clazz.getDeclaredMethod("add", int.class);
        Info methodInfo = method.getAnnotation(Info.class);

        Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
        Object instance = constructor.newInstance();

        field.set(instance, Integer.valueOf(40));
        Object invoked = method.invoke(instance, Integer.valueOf(2));

        Method run = clazz.getMethod("run");
        run.invoke(instance);

        Annotation[] annotations = clazz.getDeclaredAnnotations();

        System.out.println("refl=" + clazz.getSuperclass().getName() + ":"
            + classInfo.count() + ":" + fieldInfo.count() + ":" + methodInfo.count()
            + ":" + invoked + ":" + field.get(instance)
            + ":" + (constructor.getDeclaringClass() == clazz ? 1 : 0)
            + ":" + (clazz.isInstance(instance) ? 1 : 0)
            + ":" + clazz.getInterfaces().length
            + ":" + annotations.length
            + ":" + (Modifier.isPublic(ReflectDemo.class.getModifiers()) ? 1 : 0));
        System.out.println("java11 reflect: ok");
    }
}
