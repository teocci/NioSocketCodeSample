package com.github.teocci.nio.socket.io.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Aug-06
 */
public class KnockKnockClient
{
    private static final String MSG_HEARTBEAT_PING = "msg_hb_ping";
    private static final String MSG_HEARTBEAT_PONG = "msg_hb_pong";
    private static final long HB_TIMEOUT_INTERVAL = 2000;

    static int counter = 0;
    static Timer timer;

    static long lastRead = -1;
    static boolean active = true;

    static Socket kkSocket;

    public static void main(String[] args) throws IOException
    {
        String hostName;
        int portNumber;

        if (args.length != 2) {
//            System.err.println(
//                    "Usage: java EchoClient <host name> <port number>");
//            System.exit(1);
            hostName = "localhost";
            portNumber = 9060;
        } else {
            hostName = args[0];
            portNumber = Integer.parseInt(args[1]);
        }

        try {
            kkSocket = new Socket(hostName, portNumber);
            PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));

            kkSocket.setSoTimeout(0);
            kkSocket.setKeepAlive(true);
            kkSocket.setTcpNoDelay(true);

            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            String fromServer;
            String fromUser;

            //create thread to print counter value
            Thread t = new Thread(() -> {
                while (true) {
                    try {
                        System.out.println("Thread reading counter is: " + counter);
                        if (counter > 10) {
                            active = false;
                            System.out.println("isConnected() " + kkSocket.isConnected());
                            System.out.println("isClosed() " + kkSocket.isClosed());
                            System.out.println("read() " + in.read());
                            out.write(MSG_HEARTBEAT_PONG);
                            System.out.println("write() ");

                            System.out.println("Counter has reached 10 now will terminate");
                            timer.cancel();//end the timer
                            break;//end this loop
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException | IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });

            timer = new Timer("MyTimer");//create a new timer
            timer.scheduleAtFixedRate(new TimerTask()
            {
                @Override
                public void run()
                {
                    if ((HB_TIMEOUT_INTERVAL > 0) && ((System.currentTimeMillis() - lastRead) > HB_TIMEOUT_INTERVAL)) {
                        System.out.println("lastRead: " + (lastRead));
                        System.out.println("Diff: " + (System.currentTimeMillis() - lastRead));
                        counter++;
                        // no reply to heartbeat received.
                        // end the loop and perform a reconnect.
                    }
                }
            }, 30, 200);

            t.start();//start thread to display counter

            while (active) {
                try {
                    while ((fromServer = in.readLine()) != null) {
                        System.out.println("Server: " + fromServer);
                        if (fromServer.equals("Bye.")) break;

                        lastRead = System.currentTimeMillis();
                        if (MSG_HEARTBEAT_PING.equals(fromServer)) continue;

//                        fromUser = stdIn.readLine();
//                        if (fromUser != null) {
//                            System.out.println("Client: " + fromUser);
//                            out.println(fromUser);
//                        }
                    }
                    System.out.println("Line was null.");
                } catch (SocketTimeoutException ste) {
//                    ste.printStackTrace();
                    // in a typical situation the soTimeout should be about 200ms
                    // the heartbeat interval is usually a couple of seconds.
                    // and the heartbeat timeout interval a couple of seconds more.
                    if ((HB_TIMEOUT_INTERVAL > 0) && ((System.currentTimeMillis() - lastRead) > HB_TIMEOUT_INTERVAL)) {
                        System.err.println("lastRead: " + (lastRead));
                        System.err.println("Diff: " + (System.currentTimeMillis() - lastRead));
                        // no reply to heartbeat received.
                        // end the loop and perform a reconnect.
                        break;
                    }
                    // simple read timeout
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    hostName);
            System.exit(1);
        }
    }
}
