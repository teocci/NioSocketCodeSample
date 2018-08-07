package com.github.teocci.nio.socket.io.udp.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Aug-07
 */
public class MulticastClient
{
    private DatagramSocket socket;
    private InetAddress group;
    private int expectedServerCount;
    private byte[] buf;

    public MulticastClient() throws Exception
    {
        this.socket = new DatagramSocket();
        this.group = InetAddress.getByName("230.0.0.0");
        this.expectedServerCount = 10;
    }

    public MulticastClient(int expectedServerCount) throws Exception
    {
        this.expectedServerCount = expectedServerCount;
        this.socket = new DatagramSocket();
        this.group = InetAddress.getByName("230.0.0.0");
    }

    public int discoverServers(String msg) throws IOException
    {
        copyMessageOnBuffer(msg);
        multicastPacket();

        return receivePackets();
    }

    private void copyMessageOnBuffer(String msg)
    {
        buf = msg.getBytes();
    }

    private void multicastPacket() throws IOException
    {
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
        socket.send(packet);
    }

    private int receivePackets() throws IOException
    {
        int serversDiscovered = 0;
        while (serversDiscovered != expectedServerCount) {
            receivePacket();
            serversDiscovered++;
            System.out.println("Count: " + serversDiscovered);
        }
        return serversDiscovered;
    }

    private void receivePacket() throws IOException
    {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
    }

    public void close()
    {
        socket.close();
    }

    public static void main(String[] args) throws IOException
    {
        try {
            MulticastClient client = new MulticastClient();
            client.discoverServers("AreYouThere");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
