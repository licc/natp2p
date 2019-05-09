package com.lh.udp;

import java.io.IOException;
import java.net.*;

public class UDPClientB {

    public static void main(String[] args) throws IOException {

        System.out.println("客户端B已经提起");
        /*************客户端B给服务器发送请求*************************/
        String url = "139.*.*.*";

        InetAddress ip = InetAddress.getByName(url);
        System.out.println("ip=" + ip);
        SocketAddress target = new InetSocketAddress(ip, 8888);// 42982
        DatagramSocket client = new DatagramSocket();
        String message = "clientB";
        byte[] sendBuf = message.getBytes();

        DatagramPacket pack = new DatagramPacket(sendBuf, sendBuf.length, target);
        client.send(pack);

        //	receive(client);
        /********************客户端接收服务器发来的客户端A的IP信息**********************************/
        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        client.receive(packet);
        String clientA_IP = new String(packet.getData(), 0, packet.getLength());
        System.out.println(clientA_IP);

        String[] IP_port = clientA_IP.split(":");
        String clientA_address = IP_port[0];
        int clientA_port = Integer.parseInt(IP_port[1]);
        String requestClientA = "clientA你好，我是clientB";

        /*********************客户端B在路由器打洞并请求客户端A*************************/
        sendBuf = requestClientA.getBytes();

        SocketAddress clientA_Address = new InetSocketAddress(clientA_address, clientA_port);
        DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, clientA_Address);

        client.send(sendPacket);
        System.out.println("com.lh.udp.UDPClientB 发送给UDPClientA 消息成功");


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /***************************接收客户端A发来的信息*********************************/
        client.receive(packet);
        String clientA_Message = new String(packet.getData(), 0, packet.getLength());
        System.out.println("收到消息了：" + clientA_Message);

    }

}