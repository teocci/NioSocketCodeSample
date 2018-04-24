package com.github.teocci.nio.socket.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Apr-24
 */
public class NonBlockingWRServer implements Runnable
{
    public final static String HOSTNAME = "127.0.0.1";
    public final static int PORT = 9093;
    public final static long TIMEOUT = 10000;

    private ServerSocketChannel serverChannel;
    private Selector selector;
    /**
     * This HashMap is important. It keeps track of the data that will be written to the clients.
     * This is needed because we read/write asynchronously and we might be reading while the server
     * wants to write. In other words, we tell the Selector we are ready to write (SelectionKey.OP_WRITE)
     * and when we get a key for writing, we then write from the HashMap. The write() method explains this further.
     */
    private Map<SocketChannel, byte[]> dataTracking = new HashMap<>();

    public NonBlockingWRServer()
    {
        init();
    }

    private void init()
    {
        System.out.println("initializing server");
        // We do not want to call init() twice and recreate the selector or the serverChannel.
        if (selector != null) return;
        if (serverChannel != null) return;

        try {
            // This is how you open a Selector
            selector = Selector.open();
            // This is how you open a ServerSocketChannel
            serverChannel = ServerSocketChannel.open();
            // You MUST configure as non-blocking or else you cannot register the serverChannel to the Selector.
            serverChannel.configureBlocking(false);
            // bind to the address that you will use to Serve.
            serverChannel.socket().bind(new InetSocketAddress(HOSTNAME, PORT));

            /*
             * Here you are registering the serverSocketChannel to accept connection, thus the OP_ACCEPT.
             * This means that you just told your selector that this channel will be used to accept connections.
             * We can change this operation later to read/write, more on this later.
             */
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {
        System.out.println("Now accepting connections...");
        try {
            // A run the server as long as the thread is not interrupted.
            while (!Thread.currentThread().isInterrupted()) {
                /*
                 * selector.select(TIMEOUT) is waiting for an OPERATION to be ready and is a blocking call.
                 * For example, if a client connects right this second, then it will break from the select()
                 * call and run the code below it. The TIMEOUT is not needed, but its just so it doesn't
                 * block undefinable.
                 */
                selector.select(TIMEOUT);

                /*
                 * If we are here, it is because an operation happened (or the TIMEOUT expired).
                 * We need to get the SelectionKeys from the selector to see what operations are available.
                 * We use an iterator for this.
                 */
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    // remove the key so that we don't process this OPERATION again.
                    keys.remove();

                    // key could be invalid if for example, the client closed the connection.
                    if (!key.isValid()) {
                        continue;
                    }
                    /*
                     * In the server, we start by listening to the OP_ACCEPT when we register with the Selector.
                     * If the key from the keyset is Acceptable, then we must get ready to accept the client
                     * connection and do something with it. Go read the comments in the accept method.
                     */
                    if (key.isAcceptable()) {
                        System.out.println("Accepting connection");
                        accept(key);
                    }
                    /*
                     * If you already read the comments in the accept() method, then you know we changed
                     * the OPERATION to OP_WRITE. This means that one of these keys in the iterator will return
                     * a channel that is writable (key.isWritable()). The write() method will explain further.
                     */
                    if (key.isWritable()) {
                        System.out.println("Writing...");
                        write(key);
                    }
                    /*
                     * If you already read the comments in the write method then you understand that we registered
                     * the OPERATION OP_READ. That means that on the next Selector.select(), there is probably a key
                     * that is ready to read (key.isReadable()). The read() method will explain further.
                     */
                    if (key.isReadable()) {
                        System.out.println("Reading connection");
                        read(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    /**
     * We registered this channel in the Selector. This means that the SocketChannel we are receiving
     * back from the key.channel() is the same channel that was used to register the selector in the accept()
     * method. Again, I am just explaining as if things are synchronous to make things easy to understand.
     * This means that later, we might register to write from the read() method (for example).
     */
    private void write(SelectionKey key) throws IOException
    {
        SocketChannel channel = (SocketChannel) key.channel();
        /*
         * The HashMap contains the object SocketChannel along with the information in it to be written.
         * In this example, we send the "Hello from server" String and also an echo back to the client.
         * This is what the HashMap is for, to keep track of the messages to be written and their socketChannels.
         */
        byte[] data = dataTracking.get(channel);
        dataTracking.remove(channel);

        // Something to notice here is that reads and writes in NIO go directly to the channel and in form of
        // a buffer.
        channel.write(ByteBuffer.wrap(data));

        // Since we wrote, then we should register to read next, since that is the most logical thing
        // to happen next. YOU DO NOT HAVE TO DO THIS. But I am doing it for the purpose of the example
        // Usually if you register once for a read/write/connect/accept, you never have to register
        // again for that unless you register for none (0).
        // Like it said, I am doing it here for the purpose of the example. The same goes for all others.
        key.interestOps(SelectionKey.OP_READ);
    }

    // Nothing special, just closing our selector and socket.
    private void closeConnection()
    {
        System.out.println("Closing server down");
        if (selector != null) {
            try {
                selector.close();
                serverChannel.socket().close();
                serverChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Since we are accepting, we must instantiate a serverSocketChannel by calling key.channel().
     * We use this in order to get a socketChannel (which is like a socket in I/O) by calling
     * serverSocketChannel.accept() and we register that channel to the selector to listen
     * to a WRITE OPERATION. I do this because my server sends a hello message to each
     * client that connects to it. This doesn't mean that I will write right NOW. It means that I
     * told the selector that I am ready to write and that next time Selector.select() gets called
     * it should give me a key with isWritable(). More on this in the write() method.
     */
    private void accept(SelectionKey key) throws IOException
    {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        socketChannel.register(selector, SelectionKey.OP_WRITE);
        byte[] hello = "Hello from server".getBytes();
        dataTracking.put(socketChannel, hello);
    }

    /**
     * We read data from the channel. In this case, my server works as an echo, so it calls the echo() method.
     * The echo() method, sets the server in the WRITE OPERATION. When the while loop in run() happens again,
     * one of those keys from Selector.select() will be key.isWritable() and this is where the actual
     * write will happen by calling the write() method.
     */
    private void read(SelectionKey key) throws IOException
    {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        readBuffer.clear();

        int read;
        try {
            read = channel.read(readBuffer);
        } catch (IOException e) {
//            System.out.println("Reading problem, closing connection");
            e.printStackTrace();
            key.cancel();
            channel.close();
            return;
        }

        if (read == -1) {
            System.out.println("Nothing was there to be read, closing connection");
            channel.close();
            key.cancel();
            return;
        }
        // IMPORTANT - don't forget the flip() the buffer. It is like a reset without clearing it.
        readBuffer.flip();
        byte[] data = new byte[1000];
        readBuffer.get(data, 0, read);
        System.out.println("Received: " + new String(data));

        echo(key, data);
    }

    private void echo(SelectionKey key, byte[] data)
    {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        dataTracking.put(socketChannel, data);
        key.interestOps(SelectionKey.OP_WRITE);
    }

    public static void main(String[] args)
    {
        Thread server = new Thread(new NonBlockingWRServer());
        server.start();
    }
}
