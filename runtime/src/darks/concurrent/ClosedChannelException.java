package darks.concurrent;

/**
 * Raised when a send targets a closed channel, or a sender blocked on a
 * channel that is closed while it waits.
 */
public class ClosedChannelException extends RuntimeException
{
    public ClosedChannelException()
    {
        super("channel closed");
    }
}
