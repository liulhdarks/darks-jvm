package java.nio.channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOps;
import java.net.SocketOption;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public class ServerSocketChannel extends SelectableChannel implements NetworkChannel
{
    private ServerSocket socket;

    protected ServerSocketChannel()
    {
        fd = SocketOps.socket0();
    }

    public static ServerSocketChannel open() throws IOException
    {
        return new ServerSocketChannel();
    }

    public int validOps()
    {
        return SelectionKey.OP_ACCEPT;
    }

    public ServerSocket socket()
    {
        if (socket == null)
        {
            socket = new ServerSocket(this);
        }
        return socket;
    }

    public ServerSocketChannel bind(SocketAddress local, int backlog)
        throws IOException
    {
        socket().bind(local, backlog);
        return this;
    }

    public ServerSocketChannel bind(SocketAddress local) throws IOException
    {
        return bind(local, 50);
    }

    public <T> ServerSocketChannel setOption(SocketOption<T> name, T value)
        throws IOException
    {
        String text = name == null ? "" : name.name();
        int option = "SO_REUSEADDR".equals(text) ? SocketOps.OPT_SO_REUSEADDR
            : ("SO_RCVBUF".equals(text) ? SocketOps.OPT_SO_RCVBUF : -1);
        if (option > 0 && fd >= 0)
        {
            int number = Boolean.TRUE.equals(value) ? 1
                : (value instanceof Integer ? ((Integer)value).intValue() : 0);
            SocketOps.setOpt0(fd, option, number);
        }
        return this;
    }

    public <T> T getOption(SocketOption<T> name) throws IOException
    {
        return null;
    }

    public Set<SocketOption<?>> supportedOptions()
    {
        return new HashSet<SocketOption<?>>();
    }

    public SocketChannel accept() throws IOException
    {
        if (!isOpen())
        {
            throw new ClosedChannelException();
        }
        while (true)
        {
            if (fd < 0)
            {
                throw new ClosedChannelException();
            }
            byte[] peer = new byte[4];
            int[] port = new int[1];
            int handle = SocketOps.accept0(fd, peer, port);
            if (handle >= 0)
            {
                InetSocketAddress remote = null;
                try
                {
                    remote = new InetSocketAddress(InetAddress.getByAddress(peer),
                        port[0]);
                }
                catch (UnknownHostException ignored)
                {
                }
                return SocketChannel.fromFd(handle, remote);
            }
            if (handle == -2)
            {
                throw new SocketException("accept failed");
            }
            if (!blocking)
            {
                return null;
            }
            SocketChannel.sleepShort();
        }
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
}
