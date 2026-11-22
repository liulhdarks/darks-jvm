package java.net;

import java.io.IOException;
import java.io.OutputStream;

class SocketOutputStream extends OutputStream
{
    private final Socket socket;

    SocketOutputStream(Socket socket)
    {
        this.socket = socket;
    }

    public void write(int value) throws IOException
    {
        write(new byte[] { (byte)value }, 0, 1);
    }

    public void write(byte[] buffer, int offset, int length) throws IOException
    {
        if (buffer == null)
        {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || length > buffer.length - offset)
        {
            throw new IndexOutOfBoundsException();
        }
        int written = 0;
        while (written < length)
        {
            int fd = socket.nativeFd();
            if (fd < 0)
            {
                throw new SocketException("socket closed");
            }
            int n = SocketOps.write0(fd, buffer, offset + written, length - written);
            if (n > 0)
            {
                written += n;
                continue;
            }
            if (n == -2)
            {
                throw new SocketException("write failed");
            }
            Socket.sleepShort();
        }
    }

    public void flush() throws IOException
    {
    }

    public void close() throws IOException
    {
        socket.close();
    }
}
