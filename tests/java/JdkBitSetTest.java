import java.util.BitSet;

public class JdkBitSetTest
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
        BitSet set = new BitSet();
        check("initially empty", set.isEmpty() && set.cardinality() == 0);
        set.set(3);
        set.set(64);
        set.set(130);
        check("get set bits", set.get(3) && set.get(64) && set.get(130));
        check("get clear bit", !set.get(4));
        check("cardinality", set.cardinality() == 3);
        check("length", set.length() == 131);
        check("size grows", set.size() >= 131);
        check("next set bit", set.nextSetBit(0) == 3);
        check("next set bit from 4", set.nextSetBit(4) == 64);
        check("next clear bit 3", set.nextClearBit(3) == 4);
        set.clear(64);
        check("clear", !set.get(64) && set.cardinality() == 2);
        set.flip(130);
        check("flip off", !set.get(130));

        BitSet range = new BitSet();
        range.set(10, 20);
        check("range cardinality", range.cardinality() == 10);
        check("range bounds", range.get(10) && range.get(19) && !range.get(9) && !range.get(20));
        range.clear(12, 15);
        check("range clear", range.cardinality() == 7 && !range.get(13));
        range.flip(0, 3);
        check("range flip", range.get(0) && range.get(2) && !range.get(3));

        BitSet other = new BitSet();
        other.set(1);
        other.set(15);
        check("intersects", range.intersects(other));
        check("previous set bit", range.previousSetBit(19) == 19);
        check("previous clear bit", range.previousClearBit(40) == 40);

        BitSet andSet = (BitSet)range.clone();
        andSet.and(other);
        check("and", andSet.cardinality() == 2 && andSet.get(1) && andSet.get(15));
        BitSet orSet = (BitSet)range.clone();
        orSet.or(other);
        check("or", orSet.get(0) && orSet.get(1) && orSet.get(15));
        BitSet xorSet = (BitSet)range.clone();
        xorSet.xor(other);
        check("xor", !xorSet.get(1) && xorSet.get(0));
        BitSet andNotSet = (BitSet)range.clone();
        andNotSet.andNot(other);
        check("andNot", !andNotSet.get(1) && andNotSet.get(0));

        BitSet copy = (BitSet)range.clone();
        check("clone equal", copy.equals(range) && copy.hashCode() == range.hashCode());
        copy.set(100);
        check("clone independent", !copy.equals(range) && !range.get(100));

        BitSet parsed = BitSet.valueOf(new long[] { 0xF0L });
        check("valueOf long", parsed.cardinality() == 4 && parsed.get(4) && parsed.get(7));
        long[] words = parsed.toLongArray();
        check("toLongArray", words.length == 1 && words[0] == 0xF0L);
        BitSet fromBytes = BitSet.valueOf(new byte[] { 0x01, 0x02 });
        check("valueOf bytes", fromBytes.get(0) && fromBytes.get(9) && fromBytes.cardinality() == 2);
        byte[] bytes = fromBytes.toByteArray();
        check("toByteArray", bytes.length == 2 && bytes[0] == 1 && bytes[1] == 2);

        BitSet bounded = new BitSet(1);
        bounded.set(100);
        check("grow on set", bounded.get(100) && bounded.size() > 100);

        BitSet afterClear = new BitSet();
        afterClear.set(5);
        afterClear.clear();
        check("clear all", afterClear.isEmpty() && afterClear.length() == 0);

        BitSet empty = new BitSet();
        check("empty toLongArray", empty.toLongArray().length == 0);
        check("empty toString", "{}".equals(empty.toString()));
        check("toString bits", "{1, 2}".equals(toStringOfBits()));

        System.out.println("bitset checks=" + checks + " failures=" + failures);
        if (failures == 0)
        {
            System.out.println("bitset: ok");
        }
    }

    private static String toStringOfBits()
    {
        BitSet set = new BitSet();
        set.set(1);
        set.set(2);
        return set.toString();
    }
}
