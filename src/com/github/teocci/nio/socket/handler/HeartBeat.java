package com.github.teocci.nio.socket.handler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Aug-06
 */
public class HeartBeat extends Thread
{
    // Sends a heartbeat message to the multicast group every 60 seconds
    public HeartBeat(MulticastSocket Csock,  InetAddress maddr, int port)
    {
        this.Csock = Csock;
        this.maddr = maddr;
        this.port = port;
    }

    public static MulticastSocket Csock;
    public static InetAddress maddr;
    public static int port;
    private DatagramPacket hbMsg;
    static private long TmHB = 60000;  //heartbeat frequency in milliseconds

    public void run()
    {
        // setup the hb datagram packet then run forever
        // setup the line to ignore the loopback we want to get it too
        String line = "5|";

        hbMsg = new DatagramPacket(line.getBytes(), line.length(), maddr, port);

        // continually loop and send this packet every TmHB seconds
        while (true) {
            try {
                Csock.send(hbMsg);
                sleep(TmHB);
            } catch (IOException e) {
                System.err.println("Server can't send heartbeat");
                System.exit(-1);
            } catch (InterruptedException e) {}
        }
    }// end run

    static public void doHbCheck()
    {// checks the HB status of all users and send leave if > 5
        int hb;
        // now keep removing all over 5
        // checkHB() returns the ip+port token or null if done
        String test = "";
        while (test != null) {
            // send a leave message for this client
            String line = "3|"; //leave flag plus user
            DatagramPacket lvMsg = new DatagramPacket(line.getBytes(), line.length(), maddr, port);
            try {
                Csock.send(lvMsg);
            } catch (IOException e) {}
        }
    }

}