package javax.transaction.xa;

public interface Xid
{
    int MAXGTRIDSIZE = 64;
    int MAXBQUALSIZE = 64;

    byte[] getBranchQualifier();
    int getFormatId();
    byte[] getGlobalTransactionId();
}
