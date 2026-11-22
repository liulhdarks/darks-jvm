package java.security;

public class SecureRandom extends java.util.Random
{
    public SecureRandom()
    {
        super();
    }

    public SecureRandom(byte[] seed)
    {
        super();
    }

    public String getAlgorithm()
    {
        return "SHA1PRNG";
    }

    public static SecureRandom getInstance(String algorithm)
        throws NoSuchAlgorithmException
    {
        if (algorithm == null)
        {
            throw new NullPointerException("algorithm");
        }
        return new SecureRandom();
    }

    public static SecureRandom getInstance(String algorithm, String provider)
        throws NoSuchAlgorithmException
    {
        return getInstance(algorithm);
    }

    public void nextBytes(byte[] bytes)
    {
        if (bytes == null)
        {
            throw new NullPointerException("bytes");
        }
        int i = 0;
        while (i < bytes.length)
        {
            bytes[i] = (byte)nextInt();
            i = i + 1;
        }
    }

    public synchronized void setSeed(long seed)
    {
        super.setSeed(seed);
    }
}
