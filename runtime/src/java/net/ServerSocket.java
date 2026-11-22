package java.net;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;

public class ServerSocket
{
    private int fd = -1;
    private ServerSocketChannel channel;
    private boolean bound;
    private boolean closed;
    private int soTimeout;
    private boolean reuseAddress = true;
    private int receiveBufferSize = -1;
    private int localPort = -1;
    private int backlog = 50;
    private InetSocketAddress local;
    private InetAddress boundAddress;

    public ServerSocket()
    {
    }

    public ServerSocket(int port) throws IOException
    {
        this(port, 50, null);
    }

    public ServerSocket(int port, int backlog) throws IOException
    {
        this(port, backlog, null);
    }

    public ServerSocket(int port, int backlog, InetAddress bindAddress)
        throws IOException
    {
        this.backlog = backlog;
        this.boundAddress = bindAddress;
        bind(new InetSocketAddress(bindAddress, port), backlog);
    }

    public ServerSocket(java.nio.channels.ServerSocketChannel channel)
    {
        this.channel = channel;
        syncFromChannel();
        refreshLocal();
    }

    private void syncFromChannel()
    {
        if (channel != null)
        {
            int handle = channel.getNativeFd();
            if (handle >= 0)
            {
                fd = handle;
            }
        }
    }

    public void bind(SocketAddress endpoint) throws IOException
    {
        bind(endpoint, 50);
    }

    public void bind(SocketAddress endpoint, int backlog) throws IOException
    {
        if (closed)
        {
            throw new SocketException("socket closed");
        }
        if (bound)
        {
            throw new SocketException("already bound");
        }
        if (!(endpoint instanceof InetSocketAddress))
        {
            throw new IllegalArgumentException("unsupported address type");
        }
        InetSocketAddress target = (InetSocketAddress)endpoint;
        if (target.isUnresolved())
        {
            throw new UnknownHostException(target.getHostName());
        }
        this.backlog = backlog;
        syncFromChannel();
        if (fd < 0)
        {
            fd = SocketOps.socket0();
            if (fd < 0)
            {
                throw new SocketException("cannot create socket");
            }
        }
        if (reuseAddress)
        {
            SocketOps.setOpt0(fd, SocketOps.OPT_SO_REUSEADDR, 1);
        }
        if (receiveBufferSize > 0)
        {
            SocketOps.setOpt0(fd, SocketOps.OPT_SO_RCVBUF, receiveBufferSize);
        }
        if (SocketOps.bind0(fd, target.getAddress().getAddress(), target.getPort()) != 0)
        {
            throw new BindException("Address already in use: " + target);
        }
        if (SocketOps.listen0(fd, backlog) != 0)
        {
            throw new SocketException("listen failed");
        }
        bound = true;
        refreshLocal();
    }

    private void refreshLocal()
    {
        byte[] address = new byte[4];
        int port = SocketOps.localAddress0(fd, address);
        if (port >= 0)
        {
            localPort = port;
            try
            {
                local = new InetSocketAddress(InetAddress.getByAddress(address), port);
            }
            catch (UnknownHostException ignored)
            {
            }
        }
    }

    public Socket accept() throws IOException
    {
        if (!bound)
        {
            throw new SocketException("socket not bound");
        }
        long deadline = soTimeout > 0 ? System.currentTimeMillis() + soTimeout : 0;
        while (true)
        {
            if (closed)
            {
                throw new SocketException("socket closed");
            }
            byte[] peer = new byte[4];
            int[] peerPort = new int[1];
            int handle = SocketOps.accept0(fd, peer, peerPort);
            if (handle >= 0)
            {
                InetSocketAddress remote = null;
                try
                {
                    remote = new InetSocketAddress(InetAddress.getByAddress(peer),
                        peerPort[0]);
                }
                catch (UnknownHostException ignored)
                {
                }
                return Socket.fromFd(handle, remote);
            }
            if (handle == -2)
            {
                throw new SocketException("accept failed");
            }
            if (soTimeout > 0 && System.currentTimeMillis() >= deadline)
            {
                throw new SocketTimeoutException("Accept timed out");
            }
            Socket.sleepShort();
        }
    }

    public void close() throws IOException
    {
        if (closed)
        {
            return;
        }
        closed = true;
        if (fd >= 0)
        {
            SocketOps.close0(fd);
            fd = -1;
        }
    }

    public boolean isClosed()
    {
        return closed;
    }

    public boolean isBound()
    {
        return bound;
    }

    public InetAddress getInetAddress()
    {
        return local == null ? null : local.getAddress();
    }

    public int getLocalPort()
    {
        return localPort;
    }

    public SocketAddress getLocalSocketAddress()
    {
        return local;
    }

    public void setSoTimeout(int timeout) throws SocketException
    {
        this.soTimeout = timeout;
    }

    public int getSoTimeout() throws SocketException
    {
        return soTimeout;
    }

    public void setReuseAddress(boolean on) throws SocketException
    {
        reuseAddress = on;
        if (fd >= 0)
        {
            SocketOps.setOpt0(fd, SocketOps.OPT_SO_REUSEADDR, on ? 1 : 0);
        }
    }

    public boolean getReuseAddress() throws SocketException
    {
        return reuseAddress;
    }

    public void setReceiveBufferSize(int size) throws SocketException
    {
        receiveBufferSize = size;
        if (fd >= 0)
        {
            SocketOps.setOpt0(fd, SocketOps.OPT_SO_RCVBUF, size);
        }
    }

    public int getReceiveBufferSize() throws SocketException
    {
        return receiveBufferSize > 0 ? receiveBufferSize : 8192;
    }

    public void setPerformancePreferences(int connectionTime, int latency,
        int bandwidth)
    {
    }

    public ServerSocketChannel getChannel()
    {
        return channel;
    }

    int nativeFd()
    {
        syncFromChannel();
        return fd;
    }

    public String toString()
    {
        return "ServerSocket[addr=" + getInetAddress() + ",localport="
            + getLocalPort() + "]";
    }
}
