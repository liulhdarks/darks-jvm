package javax.net.ssl;

public class SSLEngineResult
{
    public enum Status
    {
        BUFFER_UNDERFLOW,
        BUFFER_OVERFLOW,
        CLOSED,
        OK
    }

    public enum HandshakeStatus
    {
        NEED_TASK,
        NEED_UNWRAP,
        NEED_UNWRAP_AGAIN,
        NEED_WRAP,
        FINISHED,
        NOT_HANDSHAKING
    }

    private final Status status;
    private final HandshakeStatus handshakeStatus;
    private final int bytesConsumed;
    private final int bytesProduced;

    public SSLEngineResult(Status status, HandshakeStatus handshakeStatus,
        int bytesConsumed, int bytesProduced)
    {
        if (status == null || handshakeStatus == null || bytesConsumed < 0 || bytesProduced < 0)
        {
            throw new IllegalArgumentException("invalid SSLEngineResult");
        }
        this.status = status;
        this.handshakeStatus = handshakeStatus;
        this.bytesConsumed = bytesConsumed;
        this.bytesProduced = bytesProduced;
    }

    public final Status getStatus()
    {
        return status;
    }

    public final HandshakeStatus getHandshakeStatus()
    {
        return handshakeStatus;
    }

    public final int bytesConsumed()
    {
        return bytesConsumed;
    }

    public final int bytesProduced()
    {
        return bytesProduced;
    }

    public String toString()
    {
        return "SSLEngineResult{status=" + status + ", handshakeStatus=" + handshakeStatus
            + ", bytesConsumed=" + bytesConsumed + ", bytesProduced=" + bytesProduced + "}";
    }
}
