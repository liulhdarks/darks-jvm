import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;

// Generic signature coverage: class/field/method/constructor type variables,
// parameterised super types and interfaces, wildcards and generic arrays.
public class GenericReflect
{
    public static class Box<T>
    {
        public T value;
        public List<T> items;

        public Box(T value)
        {
            this.value = value;
        }

        public T get()
        {
            return value;
        }

        public void set(T value)
        {
            this.value = value;
        }

        public <R> R identity(R value)
        {
            return value;
        }

        public List<? extends T> wild()
        {
            return null;
        }

        public void risky() throws java.io.IOException
        {
        }
    }

    public static class NumberBox<T extends Number>
    {
        public T number;
    }

    public interface Mapper<A, B>
    {
        B map(A value);
    }

    public static class StringMapper implements Mapper<String, Long>
    {
        public Long map(String value)
        {
            return Long.valueOf(value.length());
        }
    }

    public static class StringList extends ArrayList<String>
    {
    }

    public static class Comp implements Comparable<Comp>
    {
        public int compareTo(Comp other)
        {
            return 0;
        }
    }

    public static class ArrayBox<T>
    {
        public T[] values;
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
        Class<?> boxClass = Box.class;
        TypeVariable<?>[] parameters = boxClass.getTypeParameters();
        check("class-type-parameter-count", parameters.length == 1);
        check("class-type-parameter-name", "T".equals(parameters[0].getName()));
        check("class-type-parameter-owner",
            parameters[0].getGenericDeclaration() == boxClass);
        check("class-type-parameter-default-bound",
            parameters[0].getBounds().length == 1 &&
            parameters[0].getBounds()[0] == Object.class);

        TypeVariable<?>[] bounded = NumberBox.class.getTypeParameters();
        check("bounded-name", "T".equals(bounded[0].getName()));
        check("bounded-type", bounded[0].getBounds()[0] == Number.class);

        TypeVariable<?> valueType =
            (TypeVariable<?>)boxClass.getDeclaredField("value").getGenericType();
        check("field-type-variable", "T".equals(valueType.getName()));
        check("field-type-variable-identity", valueType.equals(parameters[0]));
        check("field-type-variable-owner",
            valueType.getGenericDeclaration() == boxClass);

        ParameterizedType items = (ParameterizedType)
            boxClass.getDeclaredField("items").getGenericType();
        check("field-parameterized-raw", items.getRawType() == List.class);
        check("field-parameterized-args", items.getActualTypeArguments().length == 1);
        check("field-parameterized-arg",
            parameters[0].equals(items.getActualTypeArguments()[0]));
        check("field-parameterized-owner", items.getOwnerType() == null);
        check("field-parameterized-toString",
            "java.util.List<T>".equals(items.toString()));
        check("field-parameterized-equals",
            items.equals(boxClass.getDeclaredField("items").getGenericType()));
        check("field-parameterized-hash",
            items.hashCode() ==
            boxClass.getDeclaredField("items").getGenericType().hashCode());

        Method getter = boxClass.getDeclaredMethod("get");
        check("generic-return-type", parameters[0].equals(getter.getGenericReturnType()));
        check("generic-return-name",
            "T".equals(((TypeVariable<?>)getter.getGenericReturnType()).getName()));
        Method setter = boxClass.getDeclaredMethod("set", Object.class);
        check("generic-parameter-type",
            parameters[0].equals(setter.getGenericParameterTypes()[0]));
        check("generic-parameter-count",
            setter.getGenericParameterTypes().length == 1);
        check("generic-method-type-parameters-empty",
            getter.getTypeParameters().length == 0);

        Method identity = boxClass.getDeclaredMethod("identity", Object.class);
        TypeVariable<?>[] methodParameters = identity.getTypeParameters();
        check("method-type-parameter-count", methodParameters.length == 1);
        check("method-type-parameter-name", "R".equals(methodParameters[0].getName()));
        check("method-type-parameter-owner",
            methodParameters[0].getGenericDeclaration() == identity);
        check("method-return-is-parameter",
            methodParameters[0].equals(identity.getGenericReturnType()));
        check("method-parameter-is-parameter",
            methodParameters[0].equals(identity.getGenericParameterTypes()[0]));
        check("method-parameter-default-bound",
            methodParameters[0].getBounds()[0] == Object.class);

        Method wild = boxClass.getDeclaredMethod("wild");
        ParameterizedType wildType = (ParameterizedType)wild.getGenericReturnType();
        check("wildcard-raw", wildType.getRawType() == List.class);
        check("wildcard-type",
            wildType.getActualTypeArguments()[0] instanceof WildcardType);
        WildcardType wildcard = (WildcardType)wildType.getActualTypeArguments()[0];
        check("wildcard-upper", wildcard.getUpperBounds().length == 1 &&
            parameters[0].equals(wildcard.getUpperBounds()[0]));
        check("wildcard-lower", wildcard.getLowerBounds().length == 0);
        check("wildcard-toString",
            "java.util.List<? extends T>".equals(wildType.toString()));

        TypeVariable<?>[] arrayParameters = ArrayBox.class.getTypeParameters();
        GenericArrayType arrayType = (GenericArrayType)
            ArrayBox.class.getDeclaredField("values").getGenericType();
        check("generic-array-component",
            arrayParameters[0].equals(arrayType.getGenericComponentType()));
        check("generic-array-toString", "T[]".equals(arrayType.toString()));

        Constructor<?> constructor =
            boxClass.getDeclaredConstructor(Object.class);
        check("generic-constructor-parameter",
            parameters[0].equals(constructor.getGenericParameterTypes()[0]));
        check("generic-constructor-type-parameters-empty",
            constructor.getTypeParameters().length == 0);

        Method risky = boxClass.getDeclaredMethod("risky");
        check("generic-exception-types",
            risky.getGenericExceptionTypes().length == 1 &&
            risky.getGenericExceptionTypes()[0] == java.io.IOException.class);

        Type[] mapperInterfaces = StringMapper.class.getGenericInterfaces();
        check("generic-interface-count", mapperInterfaces.length == 1);
        check("generic-interface-type",
            mapperInterfaces[0] instanceof ParameterizedType);
        ParameterizedType mapper = (ParameterizedType)mapperInterfaces[0];
        check("generic-interface-raw", mapper.getRawType() == Mapper.class);
        check("generic-interface-args", mapper.getActualTypeArguments().length == 2 &&
            mapper.getActualTypeArguments()[0] == String.class &&
            mapper.getActualTypeArguments()[1] == Long.class);

        Method map = StringMapper.class.getDeclaredMethod("map", String.class);
        check("erased-parameter", map.getGenericParameterTypes()[0] == String.class);
        check("erased-return", map.getGenericReturnType() == Long.class);

        Type stringListSuper = StringList.class.getGenericSuperclass();
        check("generic-superclass-type", stringListSuper instanceof ParameterizedType);
        ParameterizedType listSuper = (ParameterizedType)stringListSuper;
        check("generic-superclass-raw", listSuper.getRawType() == ArrayList.class);
        check("generic-superclass-arg",
            listSuper.getActualTypeArguments()[0] == String.class);

        Type[] compInterfaces = Comp.class.getGenericInterfaces();
        check("comparable-interface", compInterfaces.length == 1 &&
            compInterfaces[0] instanceof ParameterizedType);
        ParameterizedType comparable = (ParameterizedType)compInterfaces[0];
        check("comparable-raw", comparable.getRawType() == Comparable.class);
        check("comparable-arg", comparable.getActualTypeArguments()[0] == Comp.class);

        check("object-super", Object.class.getGenericSuperclass() == null);
        check("plain-super", String.class.getGenericSuperclass() == Object.class);
        check("interface-super", List.class.getGenericSuperclass() == null);
        check("no-interfaces", boxClass.getGenericInterfaces().length == 0);
        check("no-class-type-parameters",
            String.class.getTypeParameters().length == 0);
        check("class-is-type", ((Object)String.class) instanceof Type);
        check("wildcard-toString-short",
            new WildcardCapture().describe());

        System.out.println("generic-reflect checks=" + checks +
            " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
        System.out.println("generic reflection: ok");
    }

    private static class WildcardCapture
    {
        boolean describe()
        {
            try
            {
                Method wild = Box.class.getDeclaredMethod("wild");
                ParameterizedType type =
                    (ParameterizedType)wild.getGenericReturnType();
                return "? extends T".equals(
                    type.getActualTypeArguments()[0].toString());
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }
}
