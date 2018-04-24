package com.github.teocci.nio.socket.handler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-22
 */
public class RequestHandler extends Thread
{
    private Socket socket;

    public RequestHandler(Socket socket)
    {
        this.socket = socket;
    }

    @Override
    public void run()
    {
        try {
            System.out.println("Received a connection");

            // Get input and output streams
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            // Write out our header to the client
            out.println("Echo Server 1.0");
            out.flush();

            // Echo lines back to the client until the client closes the connection or we receive an empty line
            String line = in.readLine();
            while (line != null && line.length() > 0) {
                System.out.println("Print line: " + line);
                out.println(line);
                out.flush();
                line = in.readLine();
            }

            // Close our connection
            in.close();
            out.close();
            socket.close();

            System.out.println("Connection closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}