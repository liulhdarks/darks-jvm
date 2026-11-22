package java.nio.channels;

import java.io.IOException;

public interface Channel extends java.io.Closeable
{
    boolean isOpen();

    void close() throws IOException;
}
