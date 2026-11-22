package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;

/**
 * Blocking socket facade over the non-blocking native layer. Waits are done
 * with Thread.sleep so the coroutine scheduler stays in control.
 */
public class Socket
{
    private int fd = -1;
    private SocketChannel channel;
    private boolean connected;
    private boolean bound;
    private boolean closed;
    private boolean inputShutdown;
    private boolean outputShutdown;
    private InetSocketAddress remote;
    private int localPort = -1;
    private int soTimeout;
    private boolean tcpNoDelay;
    private boolean keepAlive;
    private boolean oobInline;
    private boolean reuseAddress;
    private int receiveBufferSize = -1;
    private int sendBufferSize = -1;
    private int soLinger = -1;
    private InputStream inputStream;
    private OutputStream outputStream;

    public Socket()
    {
    }

    public Socket(String host, int port) throws UnknownHostException, IOException
    {
        this(InetAddress.getByName(host), port);
    }

    public Socket(InetAddress address, int port) throws IOException
    {
        connect(new InetSocketAddress(address, port), 0);
    }

    public Socket(java.nio.channels.SocketChannel channel)
    {
        this.channel = channel;
        this.connected = true;
        this.bound = true;
        syncFromChannel();
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

    public void connect(SocketAddress endpoint) throws IOException
    {
        connect(endpoint, 0);
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException
    {
        if (closed)
        {
            throw new SocketException("socket closed");
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
        if (fd < 0)
        {
            fd = SocketOps.socket0();
            if (fd < 0)
            {
                throw new SocketException("cannot create socket");
            }
            applyOptions();
        }
        byte[] address = target.getAddress().getAddress();
        int[] portOut = new int[1];
        int result = SocketOps.connect0(fd, address, target.getPort());
        long deadline = timeout > 0 ? System.currentTimeMillis() + timeout : 0;
        while (result == -1)
        {
            if (timeout > 0 && System.currentTimeMillis() >= deadline)
            {
                closeQuietly();
                throw new SocketTimeoutException("connect timed out");
            }
            sleepShort();
            result = SocketOps.finishConnect0(fd, address, portOut);
        }
        if (result < 0)
        {
            closeQuietly();
            throw new ConnectException("Connection refused: " + target);
        }
        connected = true;
        bound = true;
        remote = new InetSocketAddress(InetAddress.getByAddress(address),
            target.getPort());
        refreshLocalPort();
    }

    private void refreshLocalPort()
    {
        byte[] address = new byte[4];
        int port = SocketOps.localAddress0(fd, address);
        if (port >= 0)
        {
            localPort = port;
        }
    }

    public InputStream getInputStream() throws IOException
    {
        if (inputStream == null)
        {
            inputStream = new SocketInputStream(this);
        }
        return inputStream;
    }

    public OutputStream getOutputStream() throws IOException
    {
        if (outputStream == null)
        {
            outputStream = new SocketOutputStream(this);
        }
        return outputStream;
    }

    public void setSoTimeout(int timeout) throws SocketException
    {
        this.soTimeout = timeout;
    }

    public int getSoTimeout() throws SocketException
    {
        return soTimeout;
    }

    public void setTcpNoDelay(boolean on) throws SocketException
    {
        tcpNoDelay = on;
        if (fd >= 0)
        {
            SocketOps.setOpt0(fd, SocketOps.OPT_TCP_NODELAY, on ? 1 : 0);
        }
    }

    public boolean getTcpNoDelay() throws SocketException
    {
        return tcpNoDelay;
    }

    public void setKeepAlive(boolean on) throws SocketException
    {
        keepAlive = on;
        if (fd >= 0)
        {
            SocketOps.setOpt0(fd, SocketOps.OPT_SO_KEEPALIVE, on ? 1 : 0);
        }
    }

    public boolean getKeepAlive() throws SocketException
    {
        return keepAlive;
    }

    public void setOOBInline(boolean on) throws SocketException
    {
        oobInline = on;
        if (fd >= 0)
        {
            SocketOps.setOpt0(fd, SocketOps.OPT_SO_OOBINLINE, on ? 1 : 0);
        }
    }

    public boolean getOOBInline() throws SocketException
    {
        return oobInline;
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
        return receiveBufferSize;
    }

    public void setSendBufferSize(int size) throws SocketException
    {
        sendBufferSize = size;
        if (fd >= 0)
        {
            SocketOps.setOpt0(fd, SocketOps.OPT_SO_SNDBUF, size);
        }
    }

    public int getSendBufferSize() throws SocketException
    {
        return sendBufferSize;
    }

    public void setSoLinger(boolean on, int linger) throws SocketException
    {
        soLinger = on ? linger : -1;
        if (fd >= 0)
        {
            SocketOps.setOpt0(fd, SocketOps.OPT_SO_LINGER, soLinger);
        }
    }

    public int getSoLinger() throws SocketException
    {
        return soLinger < 0 ? -1 : soLinger;
    }

    public void setPerformancePreferences(int connectionTime, int latency,
        int bandwidth)
    {
    }

    private void applyOptions()
    {
        if (fd < 0)
        {
            return;
        }
        SocketOps.setOpt0(fd, SocketOps.OPT_TCP_NODELAY, tcpNoDelay ? 1 : 0);
        SocketOps.setOpt0(fd, SocketOps.OPT_SO_KEEPALIVE, keepAlive ? 1 : 0);
        SocketOps.setOpt0(fd, SocketOps.OPT_SO_OOBINLINE, oobInline ? 1 : 0);
        if (receiveBufferSize > 0)
        {
            SocketOps.setOpt0(fd, SocketOps.OPT_SO_RCVBUF, receiveBufferSize);
        }
        if (sendBufferSize > 0)
        {
            SocketOps.setOpt0(fd, SocketOps.OPT_SO_SNDBUF, sendBufferSize);
        }
        if (soLinger >= 0)
        {
            SocketOps.setOpt0(fd, SocketOps.OPT_SO_LINGER, soLinger);
        }
    }

    public void shutdownInput() throws IOException
    {
        inputShutdown = true;
        if (fd >= 0)
        {
            SocketOps.shutdown0(fd, 0);
        }
    }

    public void shutdownOutput() throws IOException
    {
        outputShutdown = true;
        if (fd >= 0)
        {
            SocketOps.shutdown0(fd, 1);
        }
    }

    public boolean isInputShutdown()
    {
        return inputShutdown;
    }

    public boolean isOutputShutdown()
    {
        return outputShutdown;
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

    private void closeQuietly()
    {
        try
        {
            close();
        }
        catch (IOException ignored)
        {
        }
    }

    public boolean isClosed()
    {
        return closed;
    }

    public boolean isConnected()
    {
        return connected;
    }

    public boolean isBound()
    {
        return bound;
    }

    public InetAddress getInetAddress()
    {
        return remote == null ? null : remote.getAddress();
    }

    public InetAddress getLocalAddress()
    {
        if (fd < 0)
        {
            return null;
        }
        byte[] address = new byte[4];
        if (SocketOps.localAddress0(fd, address) < 0)
        {
            return null;
        }
        try
        {
            return InetAddress.getByAddress(address);
        }
        catch (UnknownHostException e)
        {
            return null;
        }
    }

    public int getPort()
    {
        return remote == null ? -1 : remote.getPort();
    }

    public int getLocalPort()
    {
        if (localPort < 0 && fd >= 0)
        {
            refreshLocalPort();
        }
        return localPort;
    }

    public SocketAddress getRemoteSocketAddress()
    {
        return remote;
    }

    public SocketAddress getLocalSocketAddress()
    {
        InetAddress address = getLocalAddress();
        if (address == null)
        {
            return null;
        }
        return new InetSocketAddress(address, getLocalPort());
    }

    public SocketChannel getChannel()
    {
        return channel;
    }

    int nativeFd()
    {
        syncFromChannel();
        return fd;
    }

    static Socket fromFd(int fd, InetSocketAddress remote)
    {
        Socket socket = new Socket();
        socket.fd = fd;
        socket.connected = true;
        socket.bound = true;
        socket.remote = remote;
        socket.refreshLocalPort();
        return socket;
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

    boolean readTimeout()
    {
        return soTimeout > 0;
    }

    int readTimeoutValue()
    {
        return soTimeout;
    }

    public String toString()
    {
        return "Socket[addr=" + getInetAddress() + ",port=" + getPort()
            + ",localport=" + getLocalPort() + "]";
    }
}
