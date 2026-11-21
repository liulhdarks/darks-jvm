import java.util.BitSet;

public class BitSetTest
{
    private static int failures;

    private static void check(String name, boolean value)
    {
        if (!value)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args)
    {
        BitSet bits = new BitSet();
        check("empty", bits.isEmpty());
        bits.set(3);
        bits.set(70);
        bits.set(130);
        check("get3", bits.get(3));
        check("get70", bits.get(70));
        check("get130", bits.get(130));
        check("get4", !bits.get(4));
        check("cardinality", bits.cardinality() == 3);
        check("length", bits.length() == 131);
        check("nextSetBit", bits.nextSetBit(0) == 3);
        check("nextSetBit70", bits.nextSetBit(4) == 70);
        check("nextClearBit", bits.nextClearBit(0) == 0);
        check("nextClearBit3", bits.nextClearBit(3) == 4);
        bits.clear(70);
        check("cleared", !bits.get(70));
        check("cardinality2", bits.cardinality() == 2);

        BitSet range = new BitSet();
        range.set(10, 20);
        check("range-size", range.cardinality() == 10);
        check("range-first", range.nextSetBit(0) == 10);
        check("range-last", range.previousSetBit(100) == 19);

        BitSet copy = (BitSet)range.clone();
        check("clone", copy.equals(range));
        copy.flip(10);
        check("flip", !copy.get(10) && range.get(10));

        BitSet other = new BitSet();
        other.set(15);
        other.set(30);
        BitSet intersect = (BitSet)range.clone();
        intersect.and(other);
        check("and", intersect.cardinality() == 1 && intersect.get(15));
        BitSet union = (BitSet)range.clone();
        union.or(other);
        check("or", union.cardinality() == 11 && union.get(30));
        union.andNot(other);
        check("andNot", union.cardinality() == 9 && !union.get(30) && !union.get(15));

        check("toString", range.toString().startsWith("{10, 11"));

        if (failures == 0)
        {
            System.out.println("bitset: ok");
        }
        else
        {
            System.out.println("bitset failures=" + failures);
            System.exit(1);
        }
    }
}
