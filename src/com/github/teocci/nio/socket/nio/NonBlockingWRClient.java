package com.github.teocci.nio.socket.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Apr-24
 */
public class NonBlockingWRClient implements Runnable
{
    private final static String HOSTNAME = "localhost";
    private final static int PORT = 9093;

    private String message = "";
    private Selector selector;

    public static void main(String[] args)
    {
        String string1 = "Sending a test message";
        String string2 = "Second message";
        NonBlockingWRClient test1 = new NonBlockingWRClient(string1);
        NonBlockingWRClient test2 = new NonBlockingWRClient(string2);
        Thread thread = new Thread(test1);
        Thread thread2 = new Thread(test2);
        thread.start();
        //thread2.start();
    }


    public NonBlockingWRClient(String message)
    {
        this.message = message;
    }

    @Override
    public void run()
    {
        SocketChannel channel;
        try {
            channel = SocketChannel.open();
            channel.connect(new InetSocketAddress(HOSTNAME, PORT));
            channel.configureBlocking(false);

            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_CONNECT);

            while (!Thread.interrupted()) {
                selector.select(1000);

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) continue;

                    if (key.isConnectable()) {
                        System.out.println("I am connected to the server");
                        connect(key);
                    }
                    if (key.isWritable()) {
                        write(key);
                    }
                    if (key.isReadable()) {
                        read(key);
                    }
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            close();
        }
    }

    private void close()
    {
        try {
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key) throws IOException
    {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer readBuffer = ByteBuffer.allocate(1000);
        readBuffer.clear();
        int length;
        try {
            length = channel.read(readBuffer);
        } catch (IOException e) {
            System.out.println("Reading problem, closing connection");
            key.cancel();
            channel.close();
            return;
        }
        if (length == -1) {
            System.out.println("Nothing was read from server");
            channel.close();
            key.cancel();
            return;
        }
        readBuffer.flip();
        byte[] buff = new byte[1024];
        readBuffer.get(buff, 0, length);
        System.out.println("Server said: " + new String(buff));
    }

    private void write(SelectionKey key) throws IOException
    {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.write(ByteBuffer.wrap(message.getBytes()));

        // Lets get ready to read.
        key.interestOps(SelectionKey.OP_READ);
    }

    private void connect(SelectionKey key) throws IOException
    {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.isConnectionPending()) {
            channel.finishConnect();
        }
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_WRITE);
    }
}
