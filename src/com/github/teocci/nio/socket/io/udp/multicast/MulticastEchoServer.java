package com.github.teocci.nio.socket.io.udp.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Aug-07
 */
public class MulticastEchoServer extends Thread
{
    protected MulticastSocket socket = null;
    private byte[] buf = new byte[256];
    private InetAddress group = null;

    public MulticastEchoServer() throws IOException
    {
        socket = new MulticastSocket(4446);
        socket.setReuseAddress(true);
        group = InetAddress.getByName("230.0.0.0");
        socket.joinGroup(group);
    }

    public void run()
    {
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Message: " + received + " from: " + address.getHostAddress() + ":" + port);
                if (received.equals("end")) {
                    break;
                }
                socket.send(packet);
            }
            socket.leaveGroup(group);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException
    {
        new MulticastEchoServer().start();
    }
}
