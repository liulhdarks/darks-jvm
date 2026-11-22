import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

// Covers the selector/channel/socket layer the NIO connector depends on:
// non-blocking accept, readiness reporting that survives interestOps(0),
// channel read/write and the classic java.net socket stream pair.
public class M3NioTest
{
    private static int checks;
    private static int failures;

    private static void check(String name, boolean condition)
    {
        checks++;
        if (!condition)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args) throws Exception
    {
        selectorEcho();
        blockingSocketEcho();
        System.out.println("m3 nio checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
        System.out.println("m3 nio: ok");
    }

    private static void selectorEcho() throws Exception
    {
        Selector selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.socket().bind(new InetSocketAddress("127.0.0.1", 0));
        int port = server.socket().getLocalPort();
        check("server bound", port > 0);
        server.register(selector, SelectionKey.OP_ACCEPT);

        final String[] clientResult = new String[1];
        Thread client = new Thread(new Runnable() {
            public void run()
            {
                try
                {
                    SocketChannel channel = SocketChannel.open(
                        new InetSocketAddress("127.0.0.1", port));
                    channel.write(ByteBuffer.wrap("ping".getBytes()));
                    ByteBuffer incoming = ByteBuffer.allocate(32);
                    ByteArrayOutputStream collected = new ByteArrayOutputStream();
                    long readDeadline = System.currentTimeMillis() + 5000L;
                    while (collected.size() < 9 && System.currentTimeMillis() < readDeadline)
                    {
                        int read = channel.read(incoming);
                        if (read < 0)
                        {
                            break;
                        }
                        if (read > 0)
                        {
                            incoming.flip();
                            byte[] chunk = new byte[read];
                            incoming.get(chunk);
                            collected.write(chunk, 0, chunk.length);
                            incoming.clear();
                        }
                    }
                    clientResult[0] = new String(collected.toByteArray());
                    channel.close();
                }
                catch (Exception error)
                {
                    clientResult[0] = "ERR:" + error;
                }
            }
        });
        client.start();

        boolean accepted = false;
        boolean echoed = false;
        SocketChannel connection = null;
        long deadline = System.currentTimeMillis() + 10000L;
        while (!echoed && System.currentTimeMillis() < deadline)
        {
            if (selector.select(1000L) <= 0)
            {
                continue;
            }
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext())
            {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isAcceptable())
                {
                    connection = server.accept();
                    check("accepted non-blocking", connection != null);
                    connection.configureBlocking(false);
                    connection.register(selector, SelectionKey.OP_READ);
                    accepted = true;
                }
                else if (key.isReadable())
                {
                    // Tomcat clears the interest op before dispatching; the
                    // readiness captured by select() must remain visible.
                    key.interestOps(0);
                    check("readiness survives interest clear", key.isReadable());
                    ByteArrayOutputStream request = new ByteArrayOutputStream();
                    long readDeadline = System.currentTimeMillis() + 5000L;
                    while (request.size() < 4 && System.currentTimeMillis() < readDeadline)
                    {
                        ByteBuffer incoming = ByteBuffer.allocate(16);
                        int read = connection.read(incoming);
                        if (read < 0)
                        {
                            break;
                        }
                        if (read > 0)
                        {
                            incoming.flip();
                            byte[] chunk = new byte[read];
                            incoming.get(chunk);
                            request.write(chunk, 0, chunk.length);
                        }
                    }
                    check("channel read", request.size() == 4);
                    String payload = request.toString();
                    check("channel payload", "ping".equals(payload));
                    int written = connection.write(ByteBuffer.wrap(("echo:" + payload).getBytes()));
                    check("channel write", written == 9);
                    echoed = true;
                    connection.close();
                }
            }
        }
        check("connection accepted", accepted);
        check("echo dispatched", echoed);
        client.join(10000L);
        check("client response", "echo:ping".equals(clientResult[0]));
        selector.close();
        server.close();
    }

    private static void blockingSocketEcho() throws Exception
    {
        final int[] portHolder = new int[1];
        final String[] received = new String[1];
        Thread server = new Thread(new Runnable() {
            public void run()
            {
                try
                {
                    ServerSocket listener = new ServerSocket(0, 5,
                        InetAddress.getByName("127.0.0.1"));
                    portHolder[0] = listener.getLocalPort();
                    Socket socket = listener.accept();
                    InputStream input = socket.getInputStream();
                    byte[] data = new byte[5];
                    int offset = 0;
                    while (offset < data.length)
                    {
                        int read = input.read(data, offset, data.length - offset);
                        if (read < 0)
                        {
                            break;
                        }
                        offset = offset + read;
                    }
                    received[0] = new String(data, 0, offset);
                    OutputStream output = socket.getOutputStream();
                    output.write(received[0].toUpperCase().getBytes());
                    output.flush();
                    socket.close();
                    listener.close();
                }
                catch (Exception error)
                {
                    received[0] = "ERR:" + error;
                }
            }
        });
        server.start();
        long deadline = System.currentTimeMillis() + 5000L;
        while (portHolder[0] == 0 && System.currentTimeMillis() < deadline)
        {
            Thread.sleep(5L);
        }
        check("socket server bound", portHolder[0] > 0);

        Socket client = new Socket("127.0.0.1", portHolder[0]);
        client.getOutputStream().write("hello".getBytes());
        client.getOutputStream().flush();
        InputStream input = client.getInputStream();
        ByteArrayOutputStream collected = new ByteArrayOutputStream();
        while (collected.size() < 5)
        {
            int value = input.read();
            if (value < 0)
            {
                break;
            }
            collected.write(value);
        }
        check("socket request delivered", "hello".equals(received[0]));
        check("socket response", "HELLO".equals(collected.toString()));
        client.close();
        server.join(5000L);
    }
}
