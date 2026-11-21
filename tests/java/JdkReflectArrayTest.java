import java.lang.reflect.Array;

public class JdkReflectArrayTest
{
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

    public static void main(String[] args)
    {
        int[] ints = (int[])Array.newInstance(int.class, 5);
        check("int length", Array.getLength(ints) == 5);
        Array.setInt(ints, 0, 42);
        check("int setInt/getInt", Array.getInt(ints, 0) == 42);
        Array.set(ints, 1, new Integer(7));
        check("int generic set/get", ((Integer)Array.get(ints, 1)).intValue() == 7);
        check("int array direct", ints[0] == 42 && ints[1] == 7);

        byte[] bytes = (byte[])Array.newInstance(byte.class, 2);
        Array.setByte(bytes, 0, (byte)12);
        check("byte", Array.getByte(bytes, 0) == 12);
        char[] chars = (char[])Array.newInstance(char.class, 2);
        Array.setChar(chars, 0, 'q');
        check("char", Array.getChar(chars, 0) == 'q');
        short[] shorts = (short[])Array.newInstance(short.class, 2);
        Array.setShort(shorts, 0, (short)300);
        check("short", Array.getShort(shorts, 0) == 300);
        long[] longs = (long[])Array.newInstance(long.class, 2);
        Array.setLong(longs, 0, 9000000000L);
        check("long", Array.getLong(longs, 0) == 9000000000L);
        float[] floats = (float[])Array.newInstance(float.class, 2);
        Array.setFloat(floats, 0, 1.5f);
        check("float", Array.getFloat(floats, 0) == 1.5f);
        double[] doubles = (double[])Array.newInstance(double.class, 2);
        Array.setDouble(doubles, 0, 2.25);
        check("double", Array.getDouble(doubles, 0) == 2.25);
        boolean[] booleans = (boolean[])Array.newInstance(boolean.class, 2);
        Array.setBoolean(booleans, 0, true);
        check("boolean", Array.getBoolean(booleans, 0));

        String[] strings = (String[])Array.newInstance(String.class, 3);
        Array.set(strings, 0, "hello");
        check("object set/get", "hello".equals(Array.get(strings, 0)));
        check("object length", Array.getLength(strings) == 3);

        String[][] grid = (String[][])Array.newInstance(String.class, 2, 3);
        check("grid outer length", grid.length == 2);
        check("grid inner length", grid[0].length == 3 && grid[1].length == 3);
        grid[1][2] = "cell";
        check("grid value", "cell".equals(Array.get(Array.get(grid, 1), 2)));
        check("grid type", Array.get(grid, 0) instanceof String[]);

        int failed = 0;
        try
        {
            Array.getLength(new Object());
        }
        catch (IllegalArgumentException expected)
        {
            failed++;
        }
        try
        {
            Array.getInt(ints, 99);
        }
        catch (ArrayIndexOutOfBoundsException expected)
        {
            failed++;
        }
        try
        {
            Array.newInstance(int.class, -1);
        }
        catch (NegativeArraySizeException expected)
        {
            failed++;
        }
        try
        {
            Array.set(strings, 0, new Integer(3));
        }
        catch (IllegalArgumentException expected)
        {
            failed++;
        }
        check("array errors", failed == 4);

        System.out.println("reflect-array checks=" + checks + " failures=" + failures);
        if (failures == 0)
        {
            System.out.println("reflect array: ok");
        }
    }
}
