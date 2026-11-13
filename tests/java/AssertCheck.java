public class AssertCheck
{
    public static void main(String[] args)
    {
        boolean fired = false;
        String message = null;
        try
        {
            assert false : "assert-msg";
        }
        catch (AssertionError e)
        {
            fired = true;
            message = e.getMessage();
        }
        System.out.println("assert-fired=" + fired + " msg=" + message);
        System.out.println("assert-desired=" + AssertCheck.class.desiredAssertionStatus());
    }
}
