// GC stress / correctness test for the mark-sweep collector.
//
// Run with a small heap threshold (see tests/CMakeLists.txt) so that the
// allocation loop triggers many collections while the local variables and the
// static field below must survive. Every object held by a local variable, the
// operand stack or a static field is a GC root.
public class GcStress
{
    static int[] saved;

    public static void main(String[] args)
    {
        int[] keep = new int[4];
        keep[0] = 1234;

        saved = new int[8];
        saved[0] = 5678;

        int[] chain = new int[2];
        chain[0] = 11;
        chain[1] = 22;

        int i = 0;
        while (i < 20000)
        {
            int[] junk = new int[32];
            junk[0] = i;
            i = i + 1;
        }

        if (keep[0] == 1234)
        {
            System.out.println("gc: local array survived");
        }
        else
        {
            System.out.println("gc: local array corrupted");
        }

        if (saved[0] == 5678)
        {
            System.out.println("gc: static array survived");
        }
        else
        {
            System.out.println("gc: static array corrupted");
        }

        if (chain[0] == 11 && chain[1] == 22)
        {
            System.out.println("gc: local chain survived");
        }
        else
        {
            System.out.println("gc: local chain corrupted");
        }
    }
}
