package dev.darks.jni;

/*
 * Exercises a third-party native library compiled with the official JDK
 * <jni.h>. The library is loaded from an absolute path (passed as argv[0])
 * so the test does not depend on the library search path.
 */
public class NativeLib
{
    static native int isOnLoadCalled();

    static native int echoInt(int v);

    static native int over(int v);

    static native String over(String v);

    static native int registeredAdd(int a, int b);

    static native String callStatic(int v);

    static native int sumArray(int[] values);

    static native int[] makeArray(int n);

    static native String[] makeStrings(int n);

    static native String upper(String s);

    static native void raiseFromNative();

    static native int observeJavaException();

    static String callbackString(int v)
    {
        return "cb:" + Integer.toString(v * 2);
    }

    static void thrower()
    {
        throw new IllegalStateException("from java");
    }

    static int checks = 0;
    static int failures = 0;

    static void check(String name, boolean ok)
    {
        checks++;
        if (!ok)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args)
    {
        System.load(args[0]);

        check("onLoad", isOnLoadCalled() == 1);
        check("shortName", echoInt(7) == 22);
        check("longNameInt", over(5) == 105);
        check("longNameString", over("x").equals("over[x]"));
        check("registerNatives", registeredAdd(20, 22) == 42);
        check("callback", callStatic(21).equals("cb:42"));

        int[] values = { 1, 2, 3, 4 };
        check("sumArray", sumArray(values) == 10);
        int[] squares = makeArray(4);
        check("makeArrayLength", squares.length == 4);
        check("makeArrayContent", squares[3] == 9);
        String[] strings = makeStrings(3);
        check("makeStrings", strings.length == 3 && strings[2].equals("s2"));
        check("upper", upper("hello jni").equals("HELLO JNI"));

        try
        {
            raiseFromNative();
            check("nativeThrow", false);
        }
        catch (IllegalArgumentException e)
        {
            check("nativeThrow", "native boom".equals(e.getMessage()));
        }

        check("observeJavaException", observeJavaException() == 2);

        // Verify that the library is still usable after the exception paths.
        check("afterExceptions", echoInt(1) == 4);

        System.out.println("thirdparty jni checks: " + checks + ", failed: " + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
    }
}
