package com.github.teocci.nio.socket.nio.pool;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The source code contained in this file is based on the "TCPServer" example
 * program provided by Kurose and Ross in <i>Computer Networking: A Top-Down
 * Approach</i>, Fifth Edition.
 * </p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Jul-25
 */
public class ThreadPoolEchoServer extends Thread
{
    /**
     * Prints a simple usage string to standard error that describes the
     * command-line arguments for this class.
     */
    private static void printUsage()
    {
        System.err.println("Echo server requires 1 argument: <Listen Port>");
    }

    /**
     * Pool of worker threads of unbounded size. A new thread will be created
     * for each concurrent connection, and old threads will be shut down if they
     * remain unused for about 1 minute.
     */
    private final ExecutorService workers = Executors.newCachedThreadPool();

    /**
     * Server socket on which to accept incoming client connections.
     */
    private ServerSocket listenSocket;

    /**
     * Flag to keep this server running.
     */
    private volatile boolean keepRunning = true;

    /**
     * Creates a new threaded echo server on the specified TCP port. Calls {@code System.exit(1)} if
     * it is unable to bind to the specified port.
     *
     * @param port the TCP port to accept incoming connections on.
     */
    public ThreadPoolEchoServer(final int port)
    {
        // Capture shutdown requests from the Virtual Machine.
        // This can occur when a user types Ctrl+C at the console
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                ThreadPoolEchoServer.this.shutdown();
            }
        });

        try {
            this.listenSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("An exception occurred while creating the listen socket: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * This is executed when ThreadPoolEchoServer.start() is invoked by another thread.
     * Will listen for incoming connections and hand them over to the ExecutorService (thread pool)
     * for the actual handling of client I/O.
     */
    @Override
    public void run()
    {
        // Set a timeout on the accept so we can catch shutdown requests
        try {
            this.listenSocket.setSoTimeout(1000);
        } catch (SocketException e1) {
            System.err
                    .println("Unable to set acceptor timeout value.  The server may not shutdown gracefully.");
        }

        System.out.println("Accepting incoming connections on port " + this.listenSocket.getLocalPort());

        // Accept an incoming connection, handle it, then close and repeat.
        while (this.keepRunning) {
            try {
                // Accept the next incoming connection
                final Socket clientSocket = this.listenSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                this.workers.execute(handler);

            } catch (SocketTimeoutException te) {
                // Ignored, timeouts will happen every 1 second
            } catch (IOException ioe) {
                System.err
                        .println("Exception occurred while handling client request: "
                                + ioe.getMessage());
                // Yield to other threads if an exception occurs (prevent CPU
                // spin)
                Thread.yield();
            }
        }
        try {
            // Make sure to release the port, otherwise it may remain bound for several minutes
            this.listenSocket.close();
        } catch (IOException ioe) {
            // Ignored
        }
        System.out.println("Stopped accepting incoming connections.");
    }

    /**
     * Shuts down this server.  Since the main server thread will time out every 1 second,
     * the shutdown process should complete in at most 1 second from the time this method is invoked.
     */
    public void shutdown()
    {
        System.out.println("Shutting down the server.");
        this.keepRunning = false;
        this.workers.shutdownNow();
        try {
            this.join();
        } catch (InterruptedException e) {
            // Ignored, we're exiting anyway
        }
    }


    /**
     * Parses the parameter (listen port) and accepts TCP connections on that
     * port. Each client is serviced by an independent thread, managed by a CachedThreadPool ExecutorService.
     * The thread will read a line from the client, convert it to uppercase, and then write the converted
     * line back to the client until the client disconnects or the server exits.
     *
     * @param args <listen port>
     */
    public static void main(String[] args)
    {
        // Make sure both arguments are present
        if (args.length < 1) {
            ThreadPoolEchoServer.printUsage();
            System.exit(1);
        }

        // Try to parse the port number
        int port = -1;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.err.println("Invalid listen port value: \"" + args[1] + "\".");
            ThreadPoolEchoServer.printUsage();
            System.exit(1);
        }

        // Make sure the port number is valid for TCP.
        if (port <= 0 || port > 65536) {
            System.err.println("Port value must be in (0, 65535].");
            System.exit(1);
        }

        final ThreadPoolEchoServer server = new ThreadPoolEchoServer(port);
        // Starts the server's independent thread
        server.start();

        try {
            // Wait for the server to shutdown
            server.join();
            System.out.println("Completed shutdown.");
        } catch (InterruptedException e) {
            // Exit with an error condition
            System.err.println("Interrupted before accept thread completed.");
            System.exit(1);
        }
    }
}
