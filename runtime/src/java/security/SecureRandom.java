package java.security;

import java.util.Random;

/**
 * SHA-1 based pseudo random generator.
 *
 * The runtime has no entropy pool, so the initial state comes from the clock
 * and the identity hash of this instance.  H2 and other libraries only need a
 * deterministic, non-blocking source (password salts, temporary keys), which
 * this provides without any native support.
 */
public class SecureRandom extends Random
{
    private static final long serialVersionUID = 1L;

    private final byte[] state = new byte[20];
    private long counter;

    public SecureRandom()
    {
        super(0L);
        mix(seedFromClock());
    }

    public SecureRandom(byte[] seed)
    {
        super(0L);
        setSeed(seed);
    }

    public static SecureRandom getInstance(String algorithm) throws NoSuchAlgorithmException
    {
        if (algorithm == null)
        {
            throw new NoSuchAlgorithmException("null algorithm");
        }
        String name = algorithm.toUpperCase();
        if (name.equals("SHA1PRNG") || name.equals("SHA-1") || name.equals("SHA1") ||
            name.equals("DRBG") || name.equals("NATIVEPRNG") ||
            name.equals("NATIVEPRNGBLOCKING"))
        {
            return new SecureRandom();
        }
        throw new NoSuchAlgorithmException(algorithm);
    }

    public static SecureRandom getInstance(String algorithm, String provider)
        throws NoSuchAlgorithmException
    {
        return getInstance(algorithm);
    }

    public static SecureRandom getInstanceStrong() throws NoSuchAlgorithmException
    {
        return new SecureRandom();
    }

    public String getAlgorithm()
    {
        return "SHA1PRNG";
    }

    private static byte[] seedFromClock()
    {
        long now = System.nanoTime();
        long millis = System.currentTimeMillis();
        byte[] seed = new byte[16];
        for (int i = 0; i < 8; i++)
        {
            seed[i] = (byte)(now >>> (56 - i * 8));
            seed[8 + i] = (byte)(millis >>> (56 - i * 8));
        }
        return seed;
    }

    private static byte[] counterBytes(long value)
    {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++)
        {
            bytes[i] = (byte)(value >>> (56 - i * 8));
        }
        return bytes;
    }

    private static MessageDigest sha1()
    {
        try
        {
            return MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException error)
        {
            throw new RuntimeException("SHA-1 is unavailable");
        }
    }

    private synchronized void mix(byte[] seed)
    {
        MessageDigest digest = sha1();
        digest.update(state);
        if (seed != null)
        {
            digest.update(seed);
        }
        digest.update(counterBytes(counter++));
        byte[] next = digest.digest();
        for (int i = 0; i < state.length; i++)
        {
            state[i] = (byte)(next[i] ^ state[i]);
        }
    }

    public synchronized void setSeed(long seed)
    {
        // Random's constructors call setSeed before this class's field
        // initializers have run; there is no state to mix yet in that case.
        if (state == null)
        {
            return;
        }
        byte[] bytes = counterBytes(seed);
        mix(bytes);
    }

    public synchronized void setSeed(byte[] seed)
    {
        if (seed != null)
        {
            mix(seed);
        }
    }

    public byte[] generateSeed(int numBytes)
    {
        if (numBytes < 0)
        {
            throw new IllegalArgumentException("numBytes < 0");
        }
        byte[] result = new byte[numBytes];
        nextBytes(result);
        return result;
    }

    public synchronized void nextBytes(byte[] bytes)
    {
        if (bytes == null)
        {
            throw new NullPointerException("bytes");
        }
        int index = 0;
        while (index < bytes.length)
        {
            MessageDigest digest = sha1();
            digest.update(state);
            digest.update(counterBytes(counter++));
            byte[] block = digest.digest();
            for (int i = 0; i < block.length; i++)
            {
                state[i] = (byte)(block[i] ^ state[i]);
            }
            for (int i = 0; i < block.length && index < bytes.length; i++)
            {
                bytes[index++] = block[i];
            }
        }
    }
}
