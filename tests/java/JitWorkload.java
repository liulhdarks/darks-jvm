// Deterministic workload used to compare the interpreter with the lightweight
// JIT. The test harness runs this class twice (JIT off and JIT on) and requires
// identical output.
public class JitWorkload
{
    static int seed = 3;
    int field;

    static int mix(int n)
    {
        int acc = 0;
        for (int i = 0; i < n; i++)
        {
            acc = acc + i * 3 - (i & 7);
            if ((i & 1) == 0)
            {
                acc += 2;
            }
            else
            {
                acc -= 1;
            }
            acc ^= (i << 2);
        }
        return acc;
    }

    static int mixRef(int n)
    {
        int acc = 0;
        int i = 0;
        while (i < n)
        {
            int term = i * 3 - (i & 7);
            if ((i & 1) == 0)
            {
                term += 2;
            }
            else
            {
                term -= 1;
            }
            acc = (acc + term) ^ (i << 2);
            i++;
        }
        return acc;
    }

    int instance(int x)
    {
        field = field + x;
        return field ^ (field >>> 3);
    }

    static long longOps(int n)
    {
        long value = 1;
        for (int i = 1; i <= n; i++)
        {
            value = value * 3 + i;
        }
        return value;
    }

    static long longRef(int n)
    {
        long value = 1;
        int i = 1;
        while (i <= n)
        {
            value = value * 3 + i;
            i++;
        }
        return value;
    }

    static double doubleOps(int n)
    {
        double value = 1.5;
        for (int i = 0; i < n; i++)
        {
            value = value * 1.25 + 0.5;
        }
        return value;
    }

    static double doubleRef(int n)
    {
        double value = 1.5;
        int i = 0;
        while (i < n)
        {
            value = value * 1.25 + 0.5;
            i++;
        }
        return value;
    }

    static int arrayOps()
    {
        int[] values = new int[16];
        for (int i = 0; i < values.length; i++)
        {
            values[i] = i * i;
        }
        int sum = 0;
        for (int i = 0; i < values.length; i++)
        {
            sum += values[i];
        }
        return sum;
    }

    static int recursive(int n)
    {
        if (n <= 1)
        {
            return 1;
        }
        return n * recursive(n - 1);
    }

    static int divide(int a, int b)
    {
        try
        {
            return a / b;
        }
        catch (ArithmeticException expected)
        {
            return -1;
        }
    }

    static int choose(int value)
    {
        switch (value)
        {
        case 0:
            return 10;
        case 1:
            return 11;
        case 2:
            return 12;
        default:
            return 13;
        }
    }

    public static void main(String[] args)
    {
        int checks = 0;
        int failures = 0;

        int mixSum = 0;
        for (int i = 0; i < 200; i++)
        {
            mixSum += mix(40);
        }
        if (mix(40) == mixRef(40))
        {
            checks++;
        }
        else
        {
            failures++;
        }

        JitWorkload workload = new JitWorkload();
        int instanceSum = 0;
        for (int i = 0; i < 200; i++)
        {
            instanceSum += workload.instance(i);
        }
        if (workload.field == 19900)
        {
            checks++;
        }
        else
        {
            failures++;
        }

        long longValue = longOps(30);
        if (longValue == longRef(30))
        {
            checks++;
        }
        else
        {
            failures++;
        }

        double doubleValue = doubleOps(40);
        if (doubleValue == doubleRef(40))
        {
            checks++;
        }
        else
        {
            failures++;
        }

        int arrayValue = arrayOps();
        if (arrayValue == 1240)
        {
            checks++;
        }
        else
        {
            failures++;
        }

        int recursiveValue = recursive(10);
        if (recursiveValue == 3628800)
        {
            checks++;
        }
        else
        {
            failures++;
        }

        int divideValue = divide(7, 0);
        if (divideValue == -1)
        {
            checks++;
        }
        else
        {
            failures++;
        }

        int switchValue = choose(2);
        if (switchValue == 12)
        {
            checks++;
        }
        else
        {
            failures++;
        }

        long doubleScaled = (long)(doubleValue * 1000.0);
        System.out.println("jit mix=" + mixSum + " inst=" + instanceSum
            + " field=" + workload.field + " long=" + longValue
            + " double=" + doubleScaled + " arr=" + arrayValue
            + " rec=" + recursiveValue + " div=" + divideValue
            + " choose=" + switchValue);
        System.out.println("jit checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
        System.out.println("jit workload: ok");
    }
}
