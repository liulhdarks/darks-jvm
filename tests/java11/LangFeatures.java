import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

// Compiled with `javac --release 11`; exercises the language features that
// the Java 11 bytecode phase adds on top of the earlier SDK/JNI work.
interface Named
{
    String name();

    default String describe()
    {
        return "named:" + name() + suffix();
    }

    static String label()
    {
        return "static";
    }

    private String suffix()
    {
        return "!";
    }
}

interface Alpha
{
    default String tag()
    {
        return "alpha";
    }
}

interface Beta
{
    default String tag()
    {
        return "beta";
    }
}

interface Gamma extends Alpha, Beta
{
    default String tag()
    {
        return Alpha.super.tag() + "/" + Beta.super.tag();
    }
}

class GammaImpl implements Gamma
{
}

enum Color
{
    RED, GREEN, BLUE
}

interface Mapper<T>
{
    String map(T value);
}

class ErasedMapper implements Mapper<Integer>
{
    public String map(Integer value)
    {
        return "n" + value;
    }
}

public class LangFeatures
{
    static int staticCounter;
    static volatile int volatileValue;
    static final Object LOCK = new Object();
    final StringBuilder out = new StringBuilder();

    int instanceValue = 4;

    {
        instanceValue = instanceValue + 1;
    }

    static
    {
        staticCounter = 3;
    }

    static class Nested
    {
        int value;

        Nested(int value)
        {
            this.value = value;
        }

        int get()
        {
            return value;
        }
    }

    class Inner
    {
        int sum()
        {
            return instanceValue + 1;
        }
    }

    static final class FailingRes implements AutoCloseable
    {
        public void close()
        {
            throw new IllegalStateException("close-x");
        }
    }

    static final class Res implements AutoCloseable
    {
        final String name;

        Res(String name)
        {
            this.name = name;
        }

        public void close()
        {
            System.out.println("close:" + name);
        }
    }

    static int twice(int value)
    {
        return value * 2;
    }

    int plusOne(int value)
    {
        return value + 1;
    }

    static int varargs(int... values)
    {
        int sum = 0;
        for (int value : values)
        {
            sum += value;
        }
        return sum;
    }

    static int tryWithResources()
    {
        try (Res first = new Res("a"); Res second = new Res("b"))
        {
            return 5;
        }
    }

    static String multiCatch(int mode)
    {
        try
        {
            if (mode == 0)
            {
                throw new IllegalStateException("ise");
            }
            throw new NumberFormatException("nfe");
        }
        catch (IllegalStateException | NumberFormatException ex)
        {
            return "caught:" + ex.getMessage();
        }
    }

    static String suppressed()
    {
        Throwable primary = null;
        try (FailingRes resource = new FailingRes())
        {
            throw new RuntimeException("primary");
        }
        catch (RuntimeException ex)
        {
            primary = ex;
        }
        Throwable[] suppressed = primary.getSuppressed();
        return primary.getMessage() + ":" + suppressed.length + ":"
            + suppressed[0].getMessage();
    }

    static synchronized int increment()
    {
        return ++staticCounter;
    }

    static int synchronizedBlock()
    {
        int result;
        synchronized (LOCK)
        {
            result = staticCounter;
        }
        return result;
    }

    static String switchOnString(String value)
    {
        switch (value)
        {
        case "a":
            return "A";
        case "b":
            return "B";
        default:
            return "other";
        }
    }

    static String switchOnEnum(Color color)
    {
        switch (color)
        {
        case RED:
            return "r";
        case GREEN:
            return "g";
        case BLUE:
            return "b";
        default:
            return "other";
        }
    }

    void add(String text)
    {
        if (out.length() > 0)
        {
            out.append(',');
        }
        out.append(text);
    }

    public static void main(String[] args)
    {
        LangFeatures demo = new LangFeatures();

        Named named = () -> "n";
        demo.add("dflt:" + named.describe() + ":" + Named.label());

        Gamma gamma = new GammaImpl();
        demo.add("conflict:" + gamma.tag());

        int base = 2;
        Function<Integer, Integer> addBase = value -> value + base;
        Function<Integer, Integer> staticRef = LangFeatures::twice;
        IntUnaryOperator bound = demo::plusOne;
        Function<String, Integer> length = String::length;
        Function<String, Integer> parse = Integer::parseInt;
        Supplier<ArrayList<String>> factory = ArrayList::new;
        demo.add("lam:" + addBase.apply(3) + ":" + staticRef.apply(4) + ":"
            + bound.applyAsInt(5) + ":" + length.apply("abcd") + ":" + parse.apply("42")
            + ":" + factory.get().size());

        Runnable anon = new Runnable()
        {
            public void run()
            {
            }
        };
        anon.run();
        final int localBase = 5;
        class Local
        {
            int get()
            {
                return localBase + 1;
            }
        }
        demo.add("anonlocal:" + new Local().get());

        Nested nested = new Nested(9);
        LangFeatures holder = new LangFeatures();
        Inner inner = holder.new Inner();
        demo.add("nested:" + nested.get() + ":" + inner.sum());

        demo.add("var:" + varargs(1, 2, 3, 4));

        Integer boxed = 7;
        int unboxed = boxed + 1;
        List<Integer> boxedList = new ArrayList<Integer>();
        boxedList.add(4);
        int loopSum = 0;
        for (int value : boxedList)
        {
            loopSum += value;
        }
        demo.add("auto:" + unboxed + ":" + loopSum);

        Color[] colors = Color.values();
        Color green = Color.valueOf("GREEN");
        demo.add("enum:" + colors.length + ":" + green.ordinal() + ":" + green.name()
            + ":" + switchOnEnum(green) + ":" + switchOnString("a")
            + switchOnString("b") + switchOnString("z"));

        demo.add("twr:" + tryWithResources());
        demo.add("multi:" + multiCatch(0) + ":" + multiCatch(1));
        demo.add("sup:" + suppressed());
        demo.add("sync:" + increment() + ":" + increment() + ":" + synchronizedBlock());
        demo.add("init:" + new LangFeatures().instanceValue);
        volatileValue = 11;
        demo.add("vol:" + (volatileValue + 1));

        Mapper<Integer> mapper = new ErasedMapper();
        demo.add("bridge:" + mapper.map(5));

        System.out.println("summary=" + demo.out.toString());
        System.out.println("java11 language: ok");
    }
}
