package java.nio.channels;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOps;
import java.net.SocketOption;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class SocketChannel extends SelectableChannel
    implements ByteChannel, GatheringByteChannel, ScatteringByteChannel, NetworkChannel
{
    private Socket socket;
    private boolean connected;
    private boolean connectionPending;
    private InetSocketAddress remote;

    protected SocketChannel()
    {
        fd = SocketOps.socket0();
    }

    public static SocketChannel open() throws IOException
    {
        return new SocketChannel();
    }

    public static SocketChannel open(SocketAddress remote) throws IOException
    {
        SocketChannel channel = new SocketChannel();
        channel.connect(remote);
        return channel;
    }

    static SocketChannel fromFd(int handle, InetSocketAddress remote)
    {
        SocketChannel channel = new SocketChannel();
        if (channel.fd >= 0)
        {
            SocketOps.close0(channel.fd);
        }
        channel.fd = handle;
        channel.connected = true;
        channel.remote = remote;
        return channel;
    }

    public int validOps()
    {
        return SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT;
    }

    public final Socket socket()
    {
        if (socket == null)
        {
            socket = new Socket(this);
        }
        return socket;
    }

    public boolean connect(SocketAddress endpoint) throws IOException
    {
        if (!(endpoint instanceof InetSocketAddress))
        {
            throw new IllegalArgumentException("unsupported address type");
        }
        InetSocketAddress target = (InetSocketAddress)endpoint;
        if (target.isUnresolved())
        {
            throw new UnresolvedAddressException();
        }
        if (fd < 0)
        {
            throw new ClosedChannelException();
        }
        remote = target;
        byte[] address = target.getAddress().getAddress();
        int[] portOut = new int[1];
        int result = SocketOps.connect0(fd, address, target.getPort());
        if (result == 0)
        {
            connected = true;
            return true;
        }
        if (result == -2)
        {
            throw new ConnectException("Connection refused: " + target);
        }
        connectionPending = true;
        if (!blocking)
        {
            return false;
        }
        while (true)
        {
            result = SocketOps.finishConnect0(fd, address, portOut);
            if (result == 0)
            {
                connected = true;
                connectionPending = false;
                remote = new InetSocketAddress(InetAddress.getByAddress(address),
                    portOut[0]);
                return true;
            }
            if (result == -2)
            {
                connectionPending = false;
                throw new ConnectException("Connection refused: " + target);
            }
            sleepShort();
        }
    }

    public boolean finishConnect() throws IOException
    {
        if (connected)
        {
            return true;
        }
        if (!connectionPending)
        {
            throw new NotYetConnectedException();
        }
        byte[] address = remote.getAddress().getAddress();
        int[] portOut = new int[1];
        int result = SocketOps.finishConnect0(fd, address, portOut);
        if (result == 0)
        {
            connected = true;
            connectionPending = false;
            remote = new InetSocketAddress(InetAddress.getByAddress(address),
                portOut[0]);
            return true;
        }
        return false;
    }

    public boolean isConnected()
    {
        return connected;
    }

    public boolean isConnectionPending()
    {
        return connectionPending;
    }

    public SocketAddress getRemoteAddress() throws IOException
    {
        return remote;
    }

    public SocketAddress getLocalAddress() throws IOException
    {
        if (fd < 0)
        {
            return null;
        }
        byte[] address = new byte[4];
        int port = SocketOps.localAddress0(fd, address);
        if (port < 0)
        {
            return null;
        }
        try
        {
            return new InetSocketAddress(InetAddress.getByAddress(address), port);
        }
        catch (UnknownHostException e)
        {
            return null;
        }
    }

    public SocketChannel bind(SocketAddress local) throws IOException
    {
        if (local == null)
        {
            return this;
        }
        if (!(local instanceof InetSocketAddress))
        {
            throw new IllegalArgumentException("unsupported address type");
        }
        InetSocketAddress target = (InetSocketAddress)local;
        if (SocketOps.bind0(fd, target.getAddress().getAddress(), target.getPort()) != 0)
        {
            throw new SocketException("bind failed");
        }
        return this;
    }

    public SocketChannel bind(SocketAddress local, int backlog) throws IOException
    {
        return bind(local);
    }

    public <T> SocketChannel setOption(SocketOption<T> name, T value)
        throws IOException
    {
        int option = optionId(name);
        if (option > 0 && fd >= 0)
        {
            int number = Boolean.TRUE.equals(value) ? 1
                : (value instanceof Integer ? ((Integer)value).intValue()
                    : (Boolean.FALSE.equals(value) ? 0 : 0));
            SocketOps.setOpt0(fd, option, number);
        }
        return this;
    }

    public <T> T getOption(SocketOption<T> name) throws IOException
    {
        int option = optionId(name);
        if (option <= 0 || fd < 0)
        {
            return null;
        }
        int value = SocketOps.getOpt0(fd, option);
        if (value < 0)
        {
            return null;
        }
        return (T)Integer.valueOf(value);
    }

    private static int optionId(SocketOption<?> name)
    {
        String text = name == null ? "" : name.name();
        if ("TCP_NODELAY".equals(text)) return SocketOps.OPT_TCP_NODELAY;
        if ("SO_KEEPALIVE".equals(text)) return SocketOps.OPT_SO_KEEPALIVE;
        if ("SO_REUSEADDR".equals(text)) return SocketOps.OPT_SO_REUSEADDR;
        if ("SO_RCVBUF".equals(text)) return SocketOps.OPT_SO_RCVBUF;
        if ("SO_SNDBUF".equals(text)) return SocketOps.OPT_SO_SNDBUF;
        if ("SO_LINGER".equals(text)) return SocketOps.OPT_SO_LINGER;
        return -1;
    }

    public Set<SocketOption<?>> supportedOptions()
    {
        return new HashSet<SocketOption<?>>();
    }

    public int read(ByteBuffer destination) throws IOException
    {
        if (!isOpen())
        {
            throw new ClosedChannelException();
        }
        int remaining = destination.remaining();
        if (remaining == 0)
        {
            return 0;
        }
        byte[] array = destination.array();
        int offset = destination.arrayOffset() + destination.position();
        while (true)
        {
            if (fd < 0)
            {
                throw new ClosedChannelException();
            }
            int n = SocketOps.read0(fd, array, offset, remaining);
            if (n > 0)
            {
                destination.position(destination.position() + n);
                return n;
            }
            if (n == 0)
            {
                return -1;
            }
            if (n == -2)
            {
                throw new IOException("read failed");
            }
            if (!blocking)
            {
                return 0;
            }
            sleepShort();
        }
    }

    public int write(ByteBuffer source) throws IOException
    {
        if (!isOpen())
        {
            throw new ClosedChannelException();
        }
        int remaining = source.remaining();
        if (remaining == 0)
        {
            return 0;
        }
        byte[] array = source.array();
        int offset = source.arrayOffset() + source.position();
        while (true)
        {
            if (fd < 0)
            {
                throw new ClosedChannelException();
            }
            int n = SocketOps.write0(fd, array, offset, remaining);
            if (n > 0)
            {
                source.position(source.position() + n);
                return n;
            }
            if (n == -2)
            {
                throw new IOException("write failed");
            }
            if (!blocking)
            {
                return 0;
            }
            sleepShort();
        }
    }

    public long read(ByteBuffer[] destinations) throws IOException
    {
        return read(destinations, 0, destinations.length);
    }

    public long read(ByteBuffer[] destinations, int offset, int length)
        throws IOException
    {
        long total = 0;
        for (int i = offset; i < offset + length; i++)
        {
            int n = read(destinations[i]);
            if (n < 0)
            {
                return total > 0 ? total : n;
            }
            total += n;
            if (n == 0 || !destinations[i].hasRemaining())
            {
                if (!blocking)
                {
                    break;
                }
            }
        }
        return total;
    }

    public long write(ByteBuffer[] sources) throws IOException
    {
        return write(sources, 0, sources.length);
    }

    public long write(ByteBuffer[] sources, int offset, int length)
        throws IOException
    {
        long total = 0;
        for (int i = offset; i < offset + length; i++)
        {
            while (sources[i].hasRemaining())
            {
                int n = write(sources[i]);
                if (n <= 0)
                {
                    return total;
                }
                total += n;
            }
        }
        return total;
    }

    public void close() throws IOException
    {
        if (!open)
        {
            return;
        }
        open = false;
        cancelAllKeys();
        if (fd >= 0)
        {
            SocketOps.close0(fd);
            fd = -1;
        }
        if (socket != null)
        {
            socket.close();
            socket = null;
        }
    }

    static void sleepShort()
    {
        try
        {
            Thread.sleep(1);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    public String toString()
    {
        return "SocketChannel[connected=" + connected + ", remote=" + remote + "]";
    }
}
