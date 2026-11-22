package java.nio.channels.spi;

import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SelectorProvider
{
    private static final SelectorProvider INSTANCE = new SelectorProvider();

    protected SelectorProvider()
    {
    }

    public static SelectorProvider provider()
    {
        return INSTANCE;
    }

    public Selector openSelector() throws java.io.IOException
    {
        return Selector.open();
    }

    public SocketChannel openSocketChannel() throws java.io.IOException
    {
        return SocketChannel.open();
    }

    public ServerSocketChannel openServerSocketChannel() throws java.io.IOException
    {
        return ServerSocketChannel.open();
    }
}
