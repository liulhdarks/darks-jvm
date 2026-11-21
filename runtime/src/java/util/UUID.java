package java.util;

/**
 * 128-bit universally unique identifier.  randomUUID() combines the VM's
 * pseudo-random generator with the system clock; the runtime has no
 * cryptographic provider.
 */
public final class UUID implements Comparable<UUID>, java.io.Serializable
{
    private final long mostSigBits;
    private final long leastSigBits;

    public UUID(long mostSigBits, long leastSigBits)
    {
        this.mostSigBits = mostSigBits;
        this.leastSigBits = leastSigBits;
    }

    public static UUID randomUUID()
    {
        Random random = new Random();
        long most = random.nextLong() ^ System.nanoTime();
        long least = random.nextLong();
        most = (most & 0xffffffffffff0fffL) | 0x0000000000004000L;
        least = (least & 0x3fffffffffffffffL) | 0x8000000000000000L;
        return new UUID(most, least);
    }

    public static UUID fromString(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        String compact = name.replace("-", "");
        if (compact.length() != 32)
        {
            throw new IllegalArgumentException("Invalid UUID string: " + name);
        }
        long most = parseHex(compact, 0);
        long least = parseHex(compact, 16);
        return new UUID(most, least);
    }

    private static long parseHex(String text, int offset)
    {
        long value = 0L;
        for (int i = 0; i < 16; i++)
        {
            int digit = Character.digit(text.charAt(offset + i), 16);
            if (digit < 0)
            {
                throw new IllegalArgumentException("Invalid UUID string: " + text);
            }
            value = (value << 4) | digit;
        }
        return value;
    }

    private static String toHex(long value, int digits)
    {
        String text = Long.toHexString(value);
        while (text.length() < digits)
        {
            text = "0" + text;
        }
        return text.length() > digits ? text.substring(text.length() - digits) : text;
    }

    public long getMostSignificantBits()
    {
        return mostSigBits;
    }

    public long getLeastSignificantBits()
    {
        return leastSigBits;
    }

    public int version()
    {
        return (int)((mostSigBits >> 12) & 0x0fL);
    }

    public int variant()
    {
        return (int)((leastSigBits >>> 63) & 0x03L);
    }

    public long timestamp()
    {
        if (version() != 1)
        {
            throw new UnsupportedOperationException("Not a time-based UUID");
        }
        return ((mostSigBits & 0x0fffL) << 48) | ((mostSigBits & 0xffff0000L) << 16) |
            ((mostSigBits >> 32) & 0xffffL);
    }

    public int clockSequence()
    {
        if (version() != 1)
        {
            throw new UnsupportedOperationException("Not a time-based UUID");
        }
        return (int)((leastSigBits & 0x3fff000000000000L) >>> 48);
    }

    public long node()
    {
        if (version() != 1)
        {
            throw new UnsupportedOperationException("Not a time-based UUID");
        }
        return leastSigBits & 0x0000ffffffffffffL;
    }

    public int compareTo(UUID other)
    {
        if (mostSigBits != other.mostSigBits)
        {
            return mostSigBits < other.mostSigBits ? -1 : 1;
        }
        if (leastSigBits != other.leastSigBits)
        {
            return leastSigBits < other.leastSigBits ? -1 : 1;
        }
        return 0;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof UUID))
        {
            return false;
        }
        UUID o = (UUID)other;
        return mostSigBits == o.mostSigBits && leastSigBits == o.leastSigBits;
    }

    public int hashCode()
    {
        long hilo = mostSigBits ^ leastSigBits;
        return (int)(hilo ^ (hilo >>> 32));
    }

    public String toString()
    {
        String most = toHex(mostSigBits, 16);
        String least = toHex(leastSigBits, 16);
        return most.substring(0, 8) + "-" + most.substring(8, 12) + "-" +
            most.substring(12) + "-" + least.substring(0, 4) + "-" +
            least.substring(4);
    }
}
