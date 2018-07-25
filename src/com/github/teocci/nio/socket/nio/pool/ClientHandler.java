package com.github.teocci.nio.socket.nio.pool;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * A simple runnable class that performs the basic work of this server.
 * It will read a line from the client, convert it to uppercase, and then
 * write it back to the client.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Jul-25
 */
public class ClientHandler implements Runnable
{
    /**
     * The socket connected to the client.
     */
    private final Socket clientSock;

    /**
     * Creates a new ClientHandler thread for the socket provided.
     *
     * @param clientSocket the socket to the client.
     */
    public ClientHandler(final Socket clientSocket)
    {
        this.clientSock = clientSocket;
    }

    /**
     * The run method is invoked by the ExecutorService (thread pool).
     */
    @Override
    public void run()
    {
        BufferedReader userInput = null;
        DataOutputStream userOutput = null;
        try {
            // Create the stream wrappers
            userInput = new BufferedReader(new InputStreamReader(this.clientSock.getInputStream()));
            userOutput = new DataOutputStream(this.clientSock.getOutputStream());

            while (true) {
                // Read a line from the client
                String line = userInput.readLine();
                if (line == null) {
                    break;
                }
                // Convert to uppercase
                String upperLine = line.toUpperCase() + "\n";
                // Write out as ASCII and flush
                userOutput.write(upperLine.getBytes("ASCII"));
                userOutput.flush();
            }
        } catch (IOException e) {
            // Close both streams, wrappers may not be closed by closing the socket
        }
        try {
            if (userInput != null) {
                userInput.close();
            }
            if (userOutput != null) {
                userOutput.close();
            }
            this.clientSock.close();
            System.err.println("Lost connection to " + this.clientSock.getRemoteSocketAddress());
        } catch (IOException ioe) {
            // Should be ignored
        }
    }
}
