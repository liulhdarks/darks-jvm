package java.util;

public final class UUID implements Comparable<UUID>
{
    private static final char[] HEX = "0123456789abcdef".toCharArray();

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
        long most = (random.nextLong() & 0xffffffffffff0fffL) | 0x0000000000004000L;
        long least = (random.nextLong() & 0x3fffffffffffffffL) | 0x8000000000000000L;
        return new UUID(most, least);
    }

    public static UUID fromString(String name)
    {
        String value = name.replace("-", "");
        if (value.length() != 32)
        {
            throw new IllegalArgumentException("Invalid UUID string: " + name);
        }
        long most = 0;
        long least = 0;
        for (int i = 0; i < 16; i++)
        {
            int high = hexValue(value.charAt(i * 2));
            int low = hexValue(value.charAt(i * 2 + 1));
            int octet = (high << 4) | low;
            if (i < 8)
            {
                most = (most << 8) | octet;
            }
            else
            {
                least = (least << 8) | octet;
            }
        }
        return new UUID(most, least);
    }

    private static int hexValue(char c)
    {
        if (c >= '0' && c <= '9')
        {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f')
        {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F')
        {
            return c - 'A' + 10;
        }
        throw new IllegalArgumentException("Invalid hex digit: " + c);
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
        return (int)((mostSigBits >> 12) & 0xf);
    }

    public int variant()
    {
        return (int)((leastSigBits >>> 62) & 0x3);
    }

    public String toString()
    {
        char[] buffer = new char[36];
        int position = 0;
        for (int i = 0; i < 16; i++)
        {
            if ((position == 8 || position == 13 || position == 18 || position == 23))
            {
                buffer[position++] = '-';
            }
            long bits = i < 8 ? mostSigBits : leastSigBits;
            int octet = (int)((bits >>> (56 - (i % 8) * 8)) & 0xff);
            buffer[position++] = HEX[(octet >>> 4) & 0xf];
            buffer[position++] = HEX[octet & 0xf];
        }
        return new String(buffer);
    }

    public int hashCode()
    {
        long hash = mostSigBits ^ leastSigBits;
        return (int)(hash ^ (hash >>> 32));
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof UUID))
        {
            return false;
        }
        UUID uuid = (UUID)other;
        return mostSigBits == uuid.mostSigBits && leastSigBits == uuid.leastSigBits;
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
}
