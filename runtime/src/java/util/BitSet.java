package java.util;

public class BitSet implements Cloneable, java.io.Serializable
{
    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 64;

    private long[] words;
    private int wordsInUse;
    private boolean sizeIsSticky;

    private static int wordIndex(int bitIndex)
    {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }

    private void checkRange(int fromIndex, int toIndex)
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
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex + " > toIndex: " + toIndex);
        }
    }

    private void ensureCapacity(int wordsRequired)
    {
        if (words.length < wordsRequired)
        {
            int request = Math.max(2 * words.length, wordsRequired);
            words = Arrays.copyOf(words, request);
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

    private void recalculateWordsInUse()
    {
        int i = wordsInUse - 1;
        while (i >= 0 && words[i] == 0)
        {
            i--;
        }
        wordsInUse = i + 1;
    }

    public BitSet()
    {
        words = new long[1];
        wordsInUse = 0;
        sizeIsSticky = false;
    }

    public BitSet(int nbits)
    {
        if (nbits < 0)
        {
            throw new NegativeArraySizeException("nbits < 0: " + nbits);
        }
        words = new long[(nbits + BITS_PER_WORD - 1) / BITS_PER_WORD];
        if (words.length == 0)
        {
            words = new long[1];
        }
        wordsInUse = 0;
        sizeIsSticky = true;
    }

    public static BitSet valueOf(long[] longs)
    {
        BitSet result = new BitSet(longs.length * BITS_PER_WORD);
        System.arraycopy(longs, 0, result.words, 0, longs.length);
        result.wordsInUse = longs.length;
        result.recalculateWordsInUse();
        return result;
    }

    public long[] toLongArray()
    {
        return Arrays.copyOf(words, wordsInUse);
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
        int wordIndex = wordIndex(bitIndex);
        return (wordIndex < wordsInUse) && ((words[wordIndex] & (1L << bitIndex)) != 0);
    }

    public BitSet get(int fromIndex, int toIndex)
    {
        checkRange(fromIndex, toIndex);
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
        boolean wordAligned = (fromIndex & (BITS_PER_WORD - 1)) == 0;
        for (int i = 0; i < targetWords - 1; i++, sourceIndex++)
        {
            result.words[i] = wordAligned ? words[sourceIndex]
                : (words[sourceIndex] >>> fromIndex) | (words[sourceIndex + 1] << -fromIndex);
        }
        long lastWordMask = -1L >>> -toIndex;
        result.words[targetWords - 1] =
            ((toIndex - 1) & (BITS_PER_WORD - 1)) < (fromIndex & (BITS_PER_WORD - 1))
            ? ((words[sourceIndex] >>> fromIndex) | (words[sourceIndex + 1] & lastWordMask) << -fromIndex)
            : ((words[sourceIndex] & lastWordMask) >>> fromIndex);
        result.wordsInUse = targetWords;
        result.recalculateWordsInUse();
        return result;
    }

    public int nextSetBit(int fromIndex)
    {
        if (fromIndex < 0)
        {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
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
                return (u * BITS_PER_WORD) + numberOfTrailingZeros(word);
            }
            if (++u == wordsInUse)
            {
                return -1;
            }
            word = words[u];
        }
    }

    private static int numberOfTrailingZeros(long value)
    {
        if (value == 0)
        {
            return 64;
        }
        int count = 0;
        while ((value & 1L) == 0)
        {
            value >>= 1;
            count++;
        }
        return count;
    }

    public int nextClearBit(int fromIndex)
    {
        if (fromIndex < 0)
        {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
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
                return (u * BITS_PER_WORD) + numberOfTrailingZeros(word);
            }
            if (++u == wordsInUse)
            {
                return wordsInUse * BITS_PER_WORD;
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
                return (u + 1) * BITS_PER_WORD - 1 - numberOfLeadingZeros(word);
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
                return (u + 1) * BITS_PER_WORD - 1 - numberOfLeadingZeros(word);
            }
            if (u-- == 0)
            {
                return -1;
            }
            word = ~words[u];
        }
    }

    private static int numberOfLeadingZeros(long value)
    {
        if (value == 0)
        {
            return 64;
        }
        int count = 0;
        long bit = 1L << 63;
        while ((value & bit) == 0)
        {
            value <<= 1;
            count++;
        }
        return count;
    }

    public int length()
    {
        if (wordsInUse == 0)
        {
            return 0;
        }
        return BITS_PER_WORD * (wordsInUse - 1)
            + (BITS_PER_WORD - numberOfLeadingZeros(words[wordsInUse - 1]));
    }

    public boolean isEmpty()
    {
        return wordsInUse == 0;
    }

    public boolean intersects(BitSet other)
    {
        for (int i = Math.min(wordsInUse, other.wordsInUse) - 1; i >= 0; i--)
        {
            if ((words[i] & other.words[i]) != 0)
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
            sum += bitCount(words[i]);
        }
        return sum;
    }

    private static int bitCount(long value)
    {
        int count = 0;
        while (value != 0)
        {
            value &= (value - 1);
            count++;
        }
        return count;
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
            System.arraycopy(set.words, wordsInCommon, words, wordsInCommon,
                             set.wordsInUse - wordsInCommon);
        }
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
            System.arraycopy(set.words, wordsInCommon, words, wordsInCommon,
                             set.wordsInUse - wordsInCommon);
        }
        recalculateWordsInUse();
    }

    public void andNot(BitSet set)
    {
        for (int i = Math.min(wordsInUse, set.wordsInUse) - 1; i >= 0; i--)
        {
            words[i] &= ~set.words[i];
        }
        recalculateWordsInUse();
    }

    public int hashCode()
    {
        long h = 1234;
        for (int i = wordsInUse; --i >= 0;)
        {
            h ^= words[i] * (i + 1);
        }
        return (int) ((h >> 32) ^ h);
    }

    public int size()
    {
        return words.length * BITS_PER_WORD;
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
        BitSet set = (BitSet) obj;
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

    public Object clone()
    {
        BitSet result = new BitSet(0);
        result.words = Arrays.copyOf(words, words.length);
        result.wordsInUse = wordsInUse;
        result.sizeIsSticky = sizeIsSticky;
        return result;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (int i = nextSetBit(0); i >= 0; i = nextSetBit(i + 1))
        {
            if (!first)
            {
                sb.append(", ");
            }
            first = false;
            sb.append(i);
        }
        sb.append('}');
        return sb.toString();
    }
}
