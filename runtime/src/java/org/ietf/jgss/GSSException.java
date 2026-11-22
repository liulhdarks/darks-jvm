package org.ietf.jgss;

public class GSSException extends Exception
{
    public static final int BAD_MECH = 1;
    public static final int BAD_NAME = 2;
    public static final int BAD_NAMETYPE = 3;
    public static final int BAD_BINDINGS = 4;
    public static final int DEFECTIVE_TOKEN = 5;
    public static final int DEFECTIVE_CREDENTIAL = 6;
    public static final int NO_CRED = 8;
    public static final int NO_CONTEXT = 9;
    public static final int FAILURE = 13;

    private final int major;

    public GSSException(int majorCode)
    {
        this(majorCode, 0, null);
    }

    public GSSException(int majorCode, int minorCode, String message)
    {
        super(message);
        this.major = majorCode;
    }

    public int getMajor()
    {
        return major;
    }

    public int getMinor()
    {
        return 0;
    }
}
