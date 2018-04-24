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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Apr-24
 */
public class NonBlockingEchoClient
{
    private final static String HOSTNAME = "localhost";
    private final static int PORT = 9093;

    public static void main(String[] args) throws Exception
    {
        Runnable client = () -> {
            try {
                new NonBlockingEchoClient().startClient();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        };
        new Thread(client, "client-A").start();
        new Thread(client, "client-B").start();
    }

    /**
     * Start the client
     *
     * @throws IOException
     */
    private void startClient() throws IOException, InterruptedException
    {
        InetSocketAddress hostAddress = new InetSocketAddress(HOSTNAME, PORT);
        SocketChannel client = SocketChannel.open(hostAddress);

        String threadName = Thread.currentThread().getName();

        // Send messages to server
        String[] messages = new String[]{threadName + ": msg1", threadName + ": msg2", threadName + ": msg3"};

        System.out.println(threadName + " started");

        for (int i = 0; i < messages.length; i++) {
            ByteBuffer buffer = ByteBuffer.allocate(74);
            buffer.put(messages[i].getBytes());
            buffer.flip();
            client.write(buffer);
            System.out.println(messages[i]);
            buffer.clear();
            Thread.sleep(5000);
        }
        client.close();
    }
}
