package java.nio.channels;

import java.io.IOException;
import java.util.Set;
import java.nio.channels.spi.SelectorProvider;

public abstract class Selector implements java.io.Closeable
{
    protected Selector()
    {
    }

    public static Selector open() throws IOException
    {
        return new SelectorImpl();
    }

    public abstract boolean isOpen();

    public abstract void close() throws IOException;

    public abstract Set<SelectionKey> keys();

    public abstract Set<SelectionKey> selectedKeys();

    public abstract int select() throws IOException;

    public abstract int select(long timeout) throws IOException;

    public abstract int selectNow() throws IOException;

    public abstract Selector wakeup();

    public abstract SelectorProvider provider();
}
