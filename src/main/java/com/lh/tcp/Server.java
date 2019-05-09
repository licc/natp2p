package com.lh.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * 外网端服务，穿透中继
 *
 * @author ln
 */
public class Server {

    public static List<ServerThread> connections = new ArrayList<ServerThread>();

    public static void main(String[] args) {
        try {
            // 1.创建一个服务器端Socket，即ServerSocket，指定绑定的端口，并监听此端口
            ServerSocket serverSocket = new ServerSocket(8888);
            Socket socket = null;
            // 记录客户端的数量
            int count = 0;
            System.out.println("***服务器即将启动，等待客户端的连接***");
            // 循环监听等待客户端的连接
            while (true) {
                // 调用accept()方法开始监听，等待客户端的连接
                socket = serverSocket.accept();
                // 创建一个新的线程
                ServerThread serverThread = new ServerThread(socket);
                // 启动线程
                serverThread.start();

                connections.add(serverThread);

                count++;// 统计客户端的数量
                System.out.println("客户端的数量：" + count);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ServerThread extends Thread {
        // 和本线程相关的Socket
        private Socket socket = null;
        private BufferedReader br = null;
        private PrintWriter pw = null;


        public ServerThread(Socket socket) throws IOException {
            this.socket = socket;
            this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.pw = new PrintWriter(socket.getOutputStream());
        }

        // 线程执行的操作，响应客户端的请求
        public void run() {

            InetAddress address = socket.getInetAddress();
            System.out.println("新连接，客户端的IP：" + address.getHostAddress() + " ,端口：" + socket.getPort());

            try {
                pw.write("已有客户端列表：" + Server.connections + "\n");

                // 获取输入流，并读取客户端信息
                String info = null;

                while ((info = br.readLine()) != null) {
                    // 循环读取客户端的信息
                    System.out.println("我是服务器，客户端[" + address.getHostAddress() + ":" + socket.getPort() + "]说：" + info);

                    if (info.startsWith("newConn_")) {
                        //接收到穿透消息，通知目标节点
                        String[] infos = info.split("_");
                        //目标节点的外网ip地址
                        String ip = infos[1];
                        //目标节点的外网端口
                        String port = infos[2];

                        System.out.println("打洞到 " + ip + ":" + port);

                        for (ServerThread server : Server.connections) {
                            if (server.socket.getInetAddress().getHostAddress().equals(ip)
                                    && server.socket.getPort() == Integer.parseInt(port)) {

                                //发送命令通知目标节点进行穿透连接
                                server.pw.write("autoConn_" + socket.getInetAddress().getHostAddress() + "_" + socket.getPort()
                                        + "\n");
                                server.pw.flush();

                                break;
                            }
                        }
                    } else {
                        // 获取输出流，响应客户端的请求
                        pw.write("欢迎您！" + info + "\n");
                        // 调用flush()方法将缓冲输出
                        pw.flush();
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.out.println("客户端关闭：" + address.getHostAddress() + " ,端口：" + socket.getPort());
                Server.connections.remove(this);
                // 关闭资源
                try {
                    if (pw != null) {
                        pw.close();
                    }
                    if (br != null) {
                        br.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public String toString() {
            return "ServerThread [socket=" + socket + "]";
        }
    }


}




