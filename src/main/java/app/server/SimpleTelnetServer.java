package app.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class SimpleTelnetServer {
    private final int port;
    private final MessageCallback messageCallback;

    private final ServerSocketChannel ssc;
    private final Selector selector;
    private final ByteBuffer buf = ByteBuffer.allocate(256);

    public SimpleTelnetServer(int port, MessageCallback messageCallback) throws IOException {
        this.port = port;
        this.messageCallback = messageCallback;
        this.ssc = ServerSocketChannel.open();
        this.ssc.socket().bind(new InetSocketAddress(port));
        this.ssc.configureBlocking(false);
        this.selector = Selector.open();

        this.ssc.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void start() {
        new Thread(() -> {
            try {
                System.out.println("Server starting on port " + port);

                Iterator<SelectionKey> iter;
                SelectionKey key;
                while (ssc.isOpen()) {
                    selector.select();
                    iter = selector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        key = iter.next();
                        iter.remove();

                        if (key.isAcceptable()) handleAccept(key);
                        if (key.isReadable()) handleRead(key);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private final ByteBuffer welcomeBuf = ByteBuffer.wrap("Connected!\n".getBytes());

    private void handleAccept(SelectionKey key) throws IOException {
        var sc = ((ServerSocketChannel) key.channel()).accept();
        String address = sc.socket().getInetAddress().toString() + ":" + sc.socket().getPort();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ, address);
        sc.write(welcomeBuf);
        welcomeBuf.rewind();
        System.out.println("accepted connection from: " + address);
    }

    private void handleRead(SelectionKey key) throws IOException {
        var ch = (SocketChannel) key.channel();
        var sb = new StringBuilder();

        buf.clear();
        int read;
        while ((read = ch.read(buf)) > 0) {
            buf.flip();
            byte[] bytes = new byte[buf.limit()];
            buf.get(bytes);
            sb.append(new String(bytes));
            buf.clear();
        }

        if (read < 0) {
            ch.close();
        } else {
            String msg = sb.toString().trim();
            if (!msg.isEmpty())
                messageCallback.onMessage(msg);
        }
    }
}