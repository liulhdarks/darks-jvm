package java.nio.channels;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public final class Channels
{
    private Channels()
    {
    }

    public static InputStream newInputStream(final ReadableByteChannel channel)
    {
        return new InputStream()
        {
            private final ByteBuffer buffer = ByteBuffer.allocate(1);

            public int read() throws IOException
            {
                buffer.clear();
                int n = channel.read(buffer);
                if (n <= 0)
                {
                    return -1;
                }
                buffer.flip();
                return buffer.get() & 0xFF;
            }

            public int read(byte[] target, int offset, int length) throws IOException
            {
                if (length == 0)
                {
                    return 0;
                }
                ByteBuffer direct = ByteBuffer.wrap(target, offset, length);
                return channel.read(direct);
            }

            public void close() throws IOException
            {
                channel.close();
            }
        };
    }

    public static OutputStream newOutputStream(final WritableByteChannel channel)
    {
        return new OutputStream()
        {
            public void write(int value) throws IOException
            {
                write(new byte[] { (byte)value }, 0, 1);
            }

            public void write(byte[] source, int offset, int length) throws IOException
            {
                ByteBuffer buffer = ByteBuffer.wrap(source, offset, length);
                while (buffer.hasRemaining())
                {
                    channel.write(buffer);
                }
            }

            public void close() throws IOException
            {
                channel.close();
            }
        };
    }
}
