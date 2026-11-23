package java.sql;

public class BatchUpdateException extends SQLException
{
    private int[] updateCounts;

    public BatchUpdateException()
    {
        super();
    }

    public BatchUpdateException(String reason, String SQLState, int vendorCode, int[] updateCounts)
    {
        super(reason, SQLState, vendorCode);
        this.updateCounts = updateCounts;
    }

    public BatchUpdateException(String reason, String SQLState, int vendorCode,
                                long[] updateCounts, Throwable cause)
    {
        super(reason, SQLState, vendorCode, cause);
        this.updateCounts = toInts(updateCounts);
    }

    public int[] getUpdateCounts()
    {
        return updateCounts;
    }

    private static int[] toInts(long[] values)
    {
        if (values == null)
        {
            return null;
        }
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++)
        {
            result[i] = (int)values[i];
        }
        return result;
    }
}
