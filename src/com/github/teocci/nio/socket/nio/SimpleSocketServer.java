package com.github.teocci.nio.socket.nio;

import com.github.teocci.nio.socket.handler.RequestHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-22
 */
public class SimpleSocketServer extends Thread
{
    private ServerSocket serverSocket;
    private int port;
    private boolean running = false;

    public SimpleSocketServer(int port)
    {
        this.port = port;
    }

    public void startServer()
    {
        try {
            serverSocket = new ServerSocket(port);
            this.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer()
    {
        running = false;
        this.interrupt();
    }

    @Override
    public void run()
    {
        running = true;
        while (running) {
            try {
                System.out.println("Listening for a connection");

                // Call accept() to receive the next connection
                Socket socket = serverSocket.accept();

                // Pass the socket to the RequestHandler thread for processing
                RequestHandler requestHandler = new RequestHandler(socket);
                requestHandler.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Server shutdown");
        }
    }

    public static void main(String[] args)
    {
        if (args.length == 0) {
            System.out.println("Usage: SimpleSocketServer <port>");
            System.exit(0);
        }
        int port = Integer.parseInt(args[0]);
        System.out.println("Start server on port: " + port);

        SimpleSocketServer server = new SimpleSocketServer(port);
        server.startServer();

        // Automatically shutdown in 1 minute
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Shutting down server");
        server.stopServer();
    }
}