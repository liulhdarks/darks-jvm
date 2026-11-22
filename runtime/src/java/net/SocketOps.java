package java.net;

/**
 * Thin, non-blocking POSIX socket layer. Every method maps to one syscall; the
 * Java wrappers implement blocking semantics by polling with Thread.sleep.
 */
public final class SocketOps
{
    private SocketOps()
    {
    }

    public static final int OPT_TCP_NODELAY = 1;
    public static final int OPT_SO_KEEPALIVE = 2;
    public static final int OPT_SO_REUSEADDR = 3;
    public static final int OPT_SO_RCVBUF = 4;
    public static final int OPT_SO_SNDBUF = 5;
    public static final int OPT_SO_LINGER = 6;
    public static final int OPT_SO_OOBINLINE = 7;

    public static native int socket0();

    public static native void close0(int fd);

    public static native int setOpt0(int fd, int option, int value);

    public static native int getOpt0(int fd, int option);

    public static native int bind0(int fd, byte[] address, int port);

    public static native int listen0(int fd, int backlog);

    public static native int accept0(int fd, byte[] address, int[] port);

    public static native int connect0(int fd, byte[] address, int port);

    public static native int finishConnect0(int fd, byte[] address, int[] port);

    public static native int read0(int fd, byte[] buffer, int offset, int length);

    public static native int write0(int fd, byte[] buffer, int offset, int length);

    public static native int localAddress0(int fd, byte[] address);

    public static native int peerAddress0(int fd, byte[] address);

    public static native void shutdown0(int fd, int how);

    public static native int poll0(int[] fds, int[] ops, int[] outOps);

    public static native int[] pipe0();

    public static native void drain0(int fd);
}
