package java.util;

/**
 * Bits packed into an array of longs.  This is the JDK 8 public surface used
 * by containers and code generators; the stream based methods are absent.
 */
public class BitSet implements Cloneable, java.io.Serializable
{
    private long[] words;
    private transient int wordsInUse;
    private transient boolean sizeIsSticky;

    public BitSet()
    {
        initWords(64);
        sizeIsSticky = false;
    }

    public BitSet(int nbits)
    {
        if (nbits < 0)
        {
            throw new NegativeArraySizeException("nbits < 0: " + nbits);
        }
        initWords(nbits);
        sizeIsSticky = true;
    }

    private void initWords(int nbits)
    {
        words = new long[wordIndex(nbits - 1) + 1];
    }

    private static int wordIndex(int bitIndex)
    {
        return bitIndex >> 6;
    }

    private void checkInvariants()
    {
        if (wordsInUse < 0 || wordsInUse > words.length)
        {
            throw new RuntimeException("wordsInUse out of range");
        }
    }

    private void recalculateWordsInUse()
    {
        int i;
        for (i = wordsInUse - 1; i >= 0; i--)
        {
            if (words[i] != 0)
            {
                break;
            }
        }
        wordsInUse = i + 1;
    }

    private void ensureCapacity(int wordsRequired)
    {
        if (words.length < wordsRequired)
        {
            int request = Math.max(2 * words.length, wordsRequired);
            long[] copy = new long[request];
            for (int i = 0; i < wordsInUse; i++)
            {
                copy[i] = words[i];
            }
            words = copy;
            sizeIsSticky = false;
        }
    }

    private void expandTo(int wordIndex)
    {
        int wordsRequired = wordIndex + 1;
        if (wordsInUse < wordsRequired)
        {
            ensureCapacity(wordsRequired);
            wordsInUse = wordsRequired;
        }
    }

    private static void checkRange(int fromIndex, int toIndex)
    {
        if (fromIndex < 0)
        {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        if (toIndex < 0)
        {
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        }
        if (fromIndex > toIndex)
        {
            throw new IndexOutOfBoundsException(
                "fromIndex: " + fromIndex + " > toIndex: " + toIndex);
        }
    }

    public void flip(int bitIndex)
    {
        if (bitIndex < 0)
        {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        int wordIndex = wordIndex(bitIndex);
        expandTo(wordIndex);
        words[wordIndex] ^= (1L << bitIndex);
        recalculateWordsInUse();
        checkInvariants();
    }

    public void flip(int fromIndex, int toIndex)
    {
        checkRange(fromIndex, toIndex);
        if (fromIndex == toIndex)
        {
            return;
        }
        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex = wordIndex(toIndex - 1);
        expandTo(endWordIndex);
        long firstWordMask = -1L << fromIndex;
        long lastWordMask = -1L >>> -toIndex;
        if (startWordIndex == endWordIndex)
        {
            words[startWordIndex] ^= (firstWordMask & lastWordMask);
        }
        else
        {
            words[startWordIndex] ^= firstWordMask;
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
            {
                words[i] ^= -1L;
            }
            words[endWordIndex] ^= lastWordMask;
        }
        recalculateWordsInUse();
        checkInvariants();
    }

    public void set(int bitIndex)
    {
        if (bitIndex < 0)
        {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        int wordIndex = wordIndex(bitIndex);
        expandTo(wordIndex);
        words[wordIndex] |= (1L << bitIndex);
        checkInvariants();
    }

    public void set(int bitIndex, boolean value)
    {
        if (value)
        {
            set(bitIndex);
        }
        else
        {
            clear(bitIndex);
        }
    }

    public void set(int fromIndex, int toIndex)
    {
        checkRange(fromIndex, toIndex);
        if (fromIndex == toIndex)
        {
            return;
        }
        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex = wordIndex(toIndex - 1);
        expandTo(endWordIndex);
        long firstWordMask = -1L << fromIndex;
        long lastWordMask = -1L >>> -toIndex;
        if (startWordIndex == endWordIndex)
        {
            words[startWordIndex] |= (firstWordMask & lastWordMask);
        }
        else
        {
            words[startWordIndex] |= firstWordMask;
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
            {
                words[i] = -1L;
            }
            words[endWordIndex] |= lastWordMask;
        }
        checkInvariants();
    }

    public void set(int fromIndex, int toIndex, boolean value)
    {
        if (value)
        {
            set(fromIndex, toIndex);
        }
        else
        {
            clear(fromIndex, toIndex);
        }
    }

    public void clear(int bitIndex)
    {
        if (bitIndex < 0)
        {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        int wordIndex = wordIndex(bitIndex);
        if (wordIndex >= wordsInUse)
        {
            return;
        }
        words[wordIndex] &= ~(1L << bitIndex);
        recalculateWordsInUse();
        checkInvariants();
    }

    public void clear(int fromIndex, int toIndex)
    {
        checkRange(fromIndex, toIndex);
        if (fromIndex == toIndex)
        {
            return;
        }
        int startWordIndex = wordIndex(fromIndex);
        if (startWordIndex >= wordsInUse)
        {
            return;
        }
        int endWordIndex = wordIndex(toIndex - 1);
        if (endWordIndex >= wordsInUse)
        {
            toIndex = length();
            endWordIndex = wordsInUse - 1;
        }
        long firstWordMask = -1L << fromIndex;
        long lastWordMask = -1L >>> -toIndex;
        if (startWordIndex == endWordIndex)
        {
            words[startWordIndex] &= ~(firstWordMask & lastWordMask);
        }
        else
        {
            words[startWordIndex] &= ~firstWordMask;
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
            {
                words[i] = 0;
            }
            words[endWordIndex] &= ~lastWordMask;
        }
        recalculateWordsInUse();
        checkInvariants();
    }

    public void clear()
    {
        while (wordsInUse > 0)
        {
            words[--wordsInUse] = 0;
        }
    }

    public boolean get(int bitIndex)
    {
        if (bitIndex < 0)
        {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        checkInvariants();
        int wordIndex = wordIndex(bitIndex);
        return (wordIndex < wordsInUse) &&
            ((words[wordIndex] & (1L << bitIndex)) != 0);
    }

    public BitSet get(int fromIndex, int toIndex)
    {
        checkRange(fromIndex, toIndex);
        checkInvariants();
        int len = length();
        if (len <= fromIndex || fromIndex == toIndex)
        {
            return new BitSet(0);
        }
        if (toIndex > len)
        {
            toIndex = len;
        }
        BitSet result = new BitSet(toIndex - fromIndex);
        int targetWords = wordIndex(toIndex - fromIndex - 1) + 1;
        int sourceIndex = wordIndex(fromIndex);
        boolean wordAligned = ((fromIndex & 63) == 0);
        for (int i = 0; i < targetWords - 1; i++, sourceIndex++)
        {
            result.words[i] = wordAligned ? words[sourceIndex]
                : (words[sourceIndex] >>> fromIndex) |
                  (words[sourceIndex + 1] << -fromIndex);
        }
        long lastWordMask = -1L >>> -toIndex;
        result.words[targetWords - 1] =
            ((toIndex - 1) & 63) < (fromIndex & 63)
                ? ((words[sourceIndex] >>> fromIndex) |
                   (words[sourceIndex + 1] & lastWordMask) << -fromIndex)
                : ((words[sourceIndex] & lastWordMask) >>> fromIndex);
        result.wordsInUse = targetWords;
        result.recalculateWordsInUse();
        result.checkInvariants();
        return result;
    }

    public int nextSetBit(int fromIndex)
    {
        if (fromIndex < 0)
        {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        checkInvariants();
        int u = wordIndex(fromIndex);
        if (u >= wordsInUse)
        {
            return -1;
        }
        long word = words[u] & (-1L << fromIndex);
        while (true)
        {
            if (word != 0)
            {
                return (u * 64) + Long.numberOfTrailingZeros(word);
            }
            if (++u == wordsInUse)
            {
                return -1;
            }
            word = words[u];
        }
    }

    public int nextClearBit(int fromIndex)
    {
        if (fromIndex < 0)
        {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        checkInvariants();
        int u = wordIndex(fromIndex);
        if (u >= wordsInUse)
        {
            return fromIndex;
        }
        long word = ~words[u] & (-1L << fromIndex);
        while (true)
        {
            if (word != 0)
            {
                return (u * 64) + Long.numberOfTrailingZeros(word);
            }
            if (++u == wordsInUse)
            {
                return wordsInUse * 64;
            }
            word = ~words[u];
        }
    }

    public int previousSetBit(int fromIndex)
    {
        if (fromIndex < 0)
        {
            if (fromIndex == -1)
            {
                return -1;
            }
            throw new IndexOutOfBoundsException("fromIndex < -1: " + fromIndex);
        }
        checkInvariants();
        int u = wordIndex(fromIndex);
        if (u >= wordsInUse)
        {
            return length() - 1;
        }
        long word = words[u] & (-1L >>> -(fromIndex + 1));
        while (true)
        {
            if (word != 0)
            {
                return (u + 1) * 64 - 1 - Long.numberOfLeadingZeros(word);
            }
            if (u-- == 0)
            {
                return -1;
            }
            word = words[u];
        }
    }

    public int previousClearBit(int fromIndex)
    {
        if (fromIndex < 0)
        {
            if (fromIndex == -1)
            {
                return -1;
            }
            throw new IndexOutOfBoundsException("fromIndex < -1: " + fromIndex);
        }
        checkInvariants();
        int u = wordIndex(fromIndex);
        if (u >= wordsInUse)
        {
            return fromIndex;
        }
        long word = ~words[u] & (-1L >>> -(fromIndex + 1));
        while (true)
        {
            if (word != 0)
            {
                return (u + 1) * 64 - 1 - Long.numberOfLeadingZeros(word);
            }
            if (u-- == 0)
            {
                return -1;
            }
            word = ~words[u];
        }
    }

    public int length()
    {
        if (wordsInUse == 0)
        {
            return 0;
        }
        return 64 * (wordsInUse - 1) +
            (64 - Long.numberOfLeadingZeros(words[wordsInUse - 1]));
    }

    public boolean isEmpty()
    {
        return wordsInUse == 0;
    }

    public boolean intersects(BitSet set)
    {
        for (int i = Math.min(wordsInUse, set.wordsInUse) - 1; i >= 0; i--)
        {
            if ((words[i] & set.words[i]) != 0)
            {
                return true;
            }
        }
        return false;
    }

    public int cardinality()
    {
        int sum = 0;
        for (int i = 0; i < wordsInUse; i++)
        {
            sum += Long.bitCount(words[i]);
        }
        return sum;
    }

    public void and(BitSet set)
    {
        if (this == set)
        {
            return;
        }
        while (wordsInUse > set.wordsInUse)
        {
            words[--wordsInUse] = 0;
        }
        for (int i = 0; i < wordsInUse; i++)
        {
            words[i] &= set.words[i];
        }
        recalculateWordsInUse();
        checkInvariants();
    }

    public void or(BitSet set)
    {
        if (this == set)
        {
            return;
        }
        int wordsInCommon = Math.min(wordsInUse, set.wordsInUse);
        if (wordsInUse < set.wordsInUse)
        {
            ensureCapacity(set.wordsInUse);
            wordsInUse = set.wordsInUse;
        }
        for (int i = 0; i < wordsInCommon; i++)
        {
            words[i] |= set.words[i];
        }
        if (wordsInCommon < set.wordsInUse)
        {
            for (int i = wordsInCommon; i < set.wordsInUse; i++)
            {
                words[i] = set.words[i];
            }
        }
        checkInvariants();
    }

    public void xor(BitSet set)
    {
        int wordsInCommon = Math.min(wordsInUse, set.wordsInUse);
        if (wordsInUse < set.wordsInUse)
        {
            ensureCapacity(set.wordsInUse);
            wordsInUse = set.wordsInUse;
        }
        for (int i = 0; i < wordsInCommon; i++)
        {
            words[i] ^= set.words[i];
        }
        if (wordsInCommon < set.wordsInUse)
        {
            for (int i = wordsInCommon; i < set.wordsInUse; i++)
            {
                words[i] = set.words[i];
            }
        }
        recalculateWordsInUse();
        checkInvariants();
    }

    public void andNot(BitSet set)
    {
        for (int i = Math.min(wordsInUse, set.wordsInUse) - 1; i >= 0; i--)
        {
            words[i] &= ~set.words[i];
        }
        recalculateWordsInUse();
        checkInvariants();
    }

    public int size()
    {
        return words.length * 64;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof BitSet))
        {
            return false;
        }
        if (this == obj)
        {
            return true;
        }
        BitSet set = (BitSet)obj;
        checkInvariants();
        set.checkInvariants();
        if (wordsInUse != set.wordsInUse)
        {
            return false;
        }
        for (int i = 0; i < wordsInUse; i++)
        {
            if (words[i] != set.words[i])
            {
                return false;
            }
        }
        return true;
    }

    public int hashCode()
    {
        long h = 1234;
        for (int i = wordsInUse; --i >= 0;)
        {
            h ^= words[i] * (i + 1);
        }
        return (int)((h >> 32) ^ h);
    }

    public Object clone()
    {
        BitSet result = new BitSet(64);
        result.words = new long[words.length];
        for (int i = 0; i < wordsInUse; i++)
        {
            result.words[i] = words[i];
        }
        result.wordsInUse = wordsInUse;
        result.sizeIsSticky = sizeIsSticky;
        return result;
    }

    public long[] toLongArray()
    {
        long[] result = new long[wordsInUse];
        for (int i = 0; i < wordsInUse; i++)
        {
            result[i] = words[i];
        }
        return result;
    }

    public static BitSet valueOf(long[] longs)
    {
        int n = longs.length;
        while (n > 0 && longs[n - 1] == 0)
        {
            n--;
        }
        BitSet result = new BitSet(Math.max(64, n * 64));
        for (int i = 0; i < n; i++)
        {
            result.words[i] = longs[i];
        }
        result.wordsInUse = n;
        return result;
    }

    public static BitSet valueOf(byte[] bytes)
    {
        int n = bytes.length;
        BitSet result = new BitSet(Math.max(64, n * 8));
        int word = 0;
        for (int i = 0; i < n; i++, word = (i >> 3))
        {
            result.words[word] |= (bytes[i] & 0xffL) << ((i & 7) * 8);
        }
        result.wordsInUse = (n + 7) / 8;
        result.recalculateWordsInUse();
        return result;
    }

    public byte[] toByteArray()
    {
        int n = wordsInUse;
        if (n == 0)
        {
            return new byte[0];
        }
        int len = 8 * (n - 1);
        for (long x = words[n - 1]; x != 0; x >>>= 8)
        {
            len++;
        }
        byte[] bytes = new byte[len];
        int i = 0;
        for (int w = 0; w < n; w++)
        {
            long x = words[w];
            for (int b = 0; b < 8 && i < len; b++, x >>>= 8)
            {
                bytes[i++] = (byte)x;
            }
        }
        return bytes;
    }

    public String toString()
    {
        checkInvariants();
        StringBuilder sb = new StringBuilder("{");
        int i = nextSetBit(0);
        if (i != -1)
        {
            sb.append(i);
            while (true)
            {
                i = nextSetBit(i + 1);
                if (i < 0)
                {
                    break;
                }
                sb.append(", ").append(i);
            }
        }
        return sb.append('}').toString();
    }
}
