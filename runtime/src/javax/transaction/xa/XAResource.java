package javax.transaction.xa;

public interface XAResource
{
    void commit(Xid xid, boolean onePhase) throws XAException;
    void end(Xid xid, int flags) throws XAException;
    void forget(Xid xid) throws XAException;
    int getTransactionTimeout() throws XAException;
    boolean isSameRM(XAResource other) throws XAException;
    int prepare(Xid xid) throws XAException;
    Xid[] recover(int flag) throws XAException;
    void rollback(Xid xid) throws XAException;
    boolean setTransactionTimeout(int seconds) throws XAException;
    void start(Xid xid, int flags) throws XAException;
}
