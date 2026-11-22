import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

// Integration test: a selector based HTTP endpoint built from the same NIO
// primitives the Tomcat connector uses, driven by a real blocking socket
// client that sends "GET /probe HTTP/1.1" and reads the response.
public class M3HttpLoopbackTest
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
        String body = "M3-NIO-OK";
        String response = "HTTP/1.1 200 OK\r\n"
            + "Content-Type: text/plain\r\n"
            + "Content-Length: " + body.length() + "\r\n"
            + "Connection: close\r\n"
            + "\r\n"
            + body;

        Selector selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.socket().bind(new InetSocketAddress("127.0.0.1", 0));
        final int port = server.socket().getLocalPort();
        server.register(selector, SelectionKey.OP_ACCEPT);
        check("endpoint bound", port > 0);

        final String[] clientResponse = new String[1];
        final String[] clientError = new String[1];
        Thread client = new Thread(new Runnable() {
            public void run()
            {
                try
                {
                    Socket socket = new Socket("127.0.0.1", port);
                    socket.setSoTimeout(8000);
                    OutputStream output = socket.getOutputStream();
                    output.write(("GET /probe HTTP/1.1\r\n"
                        + "Host: localhost\r\n"
                        + "Connection: close\r\n"
                        + "\r\n").getBytes());
                    output.flush();
                    InputStream input = socket.getInputStream();
                    ByteArrayOutputStream collected = new ByteArrayOutputStream();
                    int value = input.read();
                    while (value >= 0)
                    {
                        collected.write(value);
                        value = input.read();
                    }
                    clientResponse[0] = collected.toString();
                    socket.close();
                }
                catch (Exception error)
                {
                    clientError[0] = String.valueOf(error);
                }
            }
        });
        client.start();

        ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();
        SocketChannel connection = null;
        boolean responded = false;
        long deadline = System.currentTimeMillis() + 10000L;
        while (!responded && System.currentTimeMillis() < deadline)
        {
            if (selector.select(500L) <= 0)
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
                    check("accepted HTTP client", connection != null);
                    connection.configureBlocking(false);
                    connection.register(selector, SelectionKey.OP_READ);
                }
                else if (key.isReadable())
                {
                    while (true)
                    {
                        ByteBuffer buffer = ByteBuffer.allocate(256);
                        int read = connection.read(buffer);
                        if (read <= 0)
                        {
                            break;
                        }
                        buffer.flip();
                        byte[] chunk = new byte[read];
                        buffer.get(chunk);
                        requestBytes.write(chunk, 0, chunk.length);
                    }
                    String request = requestBytes.toString();
                    if (request.indexOf("\r\n\r\n") >= 0)
                    {
                        ByteBuffer output = ByteBuffer.wrap(response.getBytes());
                        while (output.remaining() > 0)
                        {
                            connection.write(output);
                        }
                        responded = true;
                        connection.close();
                    }
                }
            }
        }

        client.join(10000L);
        check("request received", requestBytes.toString().indexOf("GET /probe HTTP/1.1") >= 0);
        check("response sent", responded);
        check("client completed", clientError[0] == null);
        check("status line", clientResponse[0] != null
            && clientResponse[0].indexOf("HTTP/1.1 200 OK") >= 0);
        check("body", clientResponse[0] != null && clientResponse[0].endsWith(body));
        selector.close();
        server.close();

        System.out.println("m3 http loopback checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
        System.out.println("m3 http loopback: ok");
    }
}
