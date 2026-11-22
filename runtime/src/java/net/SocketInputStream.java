package java.net;

import java.io.IOException;
import java.io.InputStream;

class SocketInputStream extends InputStream
{
    private final Socket socket;

    SocketInputStream(Socket socket)
    {
        this.socket = socket;
    }

    public int read() throws IOException
    {
        byte[] one = new byte[1];
        int n = read(one, 0, 1);
        return n < 0 ? -1 : one[0] & 0xFF;
    }

    public int read(byte[] buffer, int offset, int length) throws IOException
    {
        if (buffer == null)
        {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || length > buffer.length - offset)
        {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0)
        {
            return 0;
        }
        long deadline = socket.readTimeout()
            ? System.currentTimeMillis() + socket.readTimeoutValue() : 0;
        while (true)
        {
            int fd = socket.nativeFd();
            if (fd < 0)
            {
                throw new SocketException("socket closed");
            }
            int n = SocketOps.read0(fd, buffer, offset, length);
            if (n > 0)
            {
                return n;
            }
            if (n == 0)
            {
                return -1;
            }
            if (n == -2)
            {
                throw new SocketException("read failed");
            }
            if (socket.readTimeout() && System.currentTimeMillis() >= deadline)
            {
                throw new SocketTimeoutException("Read timed out");
            }
            Socket.sleepShort();
        }
    }

    public int available() throws IOException
    {
        return 0;
    }

    public void close() throws IOException
    {
        socket.close();
    }
}
