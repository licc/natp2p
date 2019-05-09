package com.lh.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * 内网客户端，要进行穿透的内网服务
 *
 * @author lihuan
 * @Date: 2019-04-28 16:27
 * @Description:
 */
public class Client {

    //输入scanner
    private Scanner scanner = new Scanner(System.in);
    //是否等待输入
    private boolean isWaitInput = true;
    //首次与外网主机通信的连接
    private Socket socket;
    //首次与外网主机通信的本地端口
    private int localPort;
    private String localIp;

    private PrintWriter pw;
    private BufferedReader br;

    private Boolean connSucc = false;

    public static void main(String[] args) {
        new Client().start();
    }

    public void start() {
        try {
            // 新建一个socket通道
            socket = new Socket();
            // 设置reuseAddress为true
            socket.setReuseAddress(true);

            //TODO 在此输入外网地址和端口
            String ip = "139.*.*.*";
            int port = 8888;
            socket.connect(new InetSocketAddress(ip, port));

            //首次与外网服务器通信的端口
            //这就意味着我们内网服务要与其他内网主机通信，就可以利用这个通道
            localPort = socket.getLocalPort();

            localIp = socket.getLocalAddress().getHostAddress();

            System.out.println("本地端口：" + localPort + "==" + localIp);
            System.out.println("请输入命令 notwait等待穿透，或者输入conn进行穿透");

            pw = new PrintWriter(socket.getOutputStream());
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            try {
                while (true) {
                    if (process()) {
                        break;
                    }
                }
            } finally {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * 处理与服务器连接的交互，返回是否退出
     */
    private boolean process() throws IOException {

        String in = null;

        if (isWaitInput) {
            //把输入的命令发往服务端
            in = scanner.next();
            pw.write(in + "\n");

            //调用flush()方法将缓冲输出
            pw.flush();

            if ("notwait".equals(in.trim())) {
                isWaitInput = false;
            }
        }


        //获取服务器的响应信息
        String info = br.readLine();
        if (info != null) {
            System.out.println("我是客户端，服务器说：" + info);
        }
        //处理本地命令
        processLocalCommand(in);

        //处理服务器命令
        processRemoteCommand(info);

        return "exit".equals(in);
    }

    private void processRemoteCommand(String info) throws IOException {
        if (info != null && info.startsWith("autoConn_")) {

            System.out.println("服务器端返回的打洞命令，自动连接目标");

            String[] infos = info.split("_");
            //目标外网地址
            String ip = infos[1];
            //目标外网端口
            String port = infos[2];

            doPenetration(ip, Integer.parseInt(port));
        }
    }

    private void processLocalCommand(String in) throws IOException {
        if ("conn".equals(in)) {
            System.out.println("请输入要连接的目标外网ip:");
            String ip = scanner.next();
            System.out.println("请输入要连接的目标外网端口:");
            int port = scanner.nextInt();

            pw.write("newConn_" + ip + "_" + port + "\n");
            pw.flush();

            doPenetration(ip, port);

            isWaitInput = false;
        }
    }

    /*
     * 对目标服务器进行穿透
     */
    private void doPenetration(final String ip, final int port) {
        try {
            //异步对目标发起连接
            new Thread() {
                public void run() {


                    //打洞需要次多次尝试
                    for (int i = 1; i < 100; i++) {
                        try {
                            System.out.println("第" + i + "次尝试连接");
                            Socket newsocket = new Socket();

                            newsocket.setReuseAddress(true);
                            InetSocketAddress inetSocketAddress = new InetSocketAddress(localIp, localPort);

                            newsocket.bind(inetSocketAddress);

                            System.out.println("本地监听 " + inetSocketAddress);


                            InetSocketAddress remote = new InetSocketAddress(ip, port);
                            System.out.println("connect to " + remote);


                            newsocket.connect(remote, 5000);
                            System.out.println("connect success");
                            connSucc = true;

                            BufferedReader b = new BufferedReader(
                                    new InputStreamReader(newsocket.getInputStream()));
                            PrintWriter p = new PrintWriter(newsocket.getOutputStream());

                            while (true) {

                                if (newsocket.isClosed()) {
                                    System.out.println("对方已经下线");
                                    break;
                                }
                                String msg = "hello " + System.currentTimeMillis() + "\n";

                                System.out.println("我说: " + msg);

                                String sendMsg = "[" + localIp + "]说: " + msg;
                                p.write(sendMsg);
                                p.flush();

                                String rMsg = b.readLine();
                                System.out.println(rMsg);
                                if ("exit".equals(rMsg)) {
                                    break;
                                }

                                Thread.sleep(1000l);
                            }


                            b.close();
                            p.close();
                            newsocket.close();

                            if (newsocket.isClosed()) {
                                isWaitInput = true;
                                System.out.println("对方已经下线,我也要关闭下线");
                                break;
                            }

                        } catch (Exception e) {
                            //成功后的异常需要退出重连
                            if (connSucc) {
                                System.out.println("断线了，请重新连接");
                                connSucc = false;
                                break;
                            }
                            System.out.println("第" + i + "次尝试连接失败" + e.getMessage());

                        }
                        try {
                            Thread.sleep(500l);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }


                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("监听端口 " + socket.getLocalPort() + " 出错");
        }
    }
}
