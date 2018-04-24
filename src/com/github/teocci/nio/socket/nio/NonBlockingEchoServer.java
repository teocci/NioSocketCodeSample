package com.github.teocci.nio.socket.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Apr-24
 */
public class NonBlockingEchoServer
{
    private final static String HOSTNAME = "localhost";
    private final static int PORT = 9093;

    private Selector selector;

    private InetSocketAddress listenAddress;

    private Map<SocketChannel, byte[]> dataTracking = new HashMap<>();

    public static void main(String[] args) throws Exception
    {
        try {
            new NonBlockingEchoServer(HOSTNAME, PORT).startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NonBlockingEchoServer(String address, int port) throws IOException
    {
        listenAddress = new InetSocketAddress(address, port);
    }

    /**
     * Start the server
     *
     * @throws IOException
     */
    private void startServer() throws IOException
    {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        // Bind server socket channel to port
        serverChannel.socket().bind(listenAddress);
        serverChannel.configureBlocking(false);

        this.selector = Selector.open();
        serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server started on port >> " + PORT);

        while (true) {
            // Wait for events
            int readyCount = selector.select();
            if (readyCount == 0) {
                continue;
            }

            // Process selected keys...
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = (SelectionKey) iterator.next();

                // Remove key from set so we don't process it twice
                iterator.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    // Accept client connections
                    this.accept(key);
                } else if (key.isReadable()) {
                    // Read from client
                    this.read(key);
                } else if (key.isWritable()) {
                    // Write data to client...
                    this.write(key);
                }
            }
        }
    }


    private void accept(SelectionKey key) throws IOException
    {
        // Accept client connection
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        System.out.println("Connected to: " + remoteAddr);
        /*

         * Register channel with selector for further IO (record it for read/write
		 * operations, here we have used read operation)
		 */
        channel.register(this.selector, SelectionKey.OP_READ);

        byte[] hello = new String("Hello from server").getBytes();
        dataTracking.put(channel, hello);
    }

    // Read from the socket channel
    private void read(SelectionKey key) throws IOException
    {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numRead = channel.read(buffer);

        if (numRead == -1) {
            Socket socket = channel.socket();
            SocketAddress remoteAddress = socket.getRemoteSocketAddress();
            System.out.println("Connection closed by client: " + remoteAddress);
            channel.close();
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        System.out.println("Got: " + new String(data));
    }

    private void write(SelectionKey key) throws IOException
    {
        SocketChannel channel = (SocketChannel) key.channel();

        byte[] data;

        data = dataTracking.get(channel);
        dataTracking.remove(channel);
        channel.write(ByteBuffer.wrap(data));

        key.interestOps(SelectionKey.OP_READ);
    }
}
