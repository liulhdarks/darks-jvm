package java.net;

public final class StandardSocketOptions
{
    private StandardSocketOptions()
    {
    }

    private static final class Option<T> implements SocketOption<T>
    {
        private final String name;
        private final Class<T> type;

        Option(String name, Class<T> type)
        {
            this.name = name;
            this.type = type;
        }

        public String name()
        {
            return name;
        }

        public Class<T> type()
        {
            return type;
        }

        public String toString()
        {
            return name;
        }
    }

    public static final SocketOption<Boolean> SO_BROADCAST =
        new Option<Boolean>("SO_BROADCAST", Boolean.class);
    public static final SocketOption<Boolean> SO_KEEPALIVE =
        new Option<Boolean>("SO_KEEPALIVE", Boolean.class);
    public static final SocketOption<Integer> SO_SNDBUF =
        new Option<Integer>("SO_SNDBUF", Integer.class);
    public static final SocketOption<Integer> SO_RCVBUF =
        new Option<Integer>("SO_RCVBUF", Integer.class);
    public static final SocketOption<Boolean> SO_REUSEADDR =
        new Option<Boolean>("SO_REUSEADDR", Boolean.class);
    public static final SocketOption<Boolean> SO_LINGER =
        new Option<Boolean>("SO_LINGER", Boolean.class);
    public static final SocketOption<Boolean> TCP_NODELAY =
        new Option<Boolean>("TCP_NODELAY", Boolean.class);
    public static final SocketOption<Integer> IP_TOS =
        new Option<Integer>("IP_TOS", Integer.class);
}
