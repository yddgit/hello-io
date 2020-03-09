package com.my.project.io.network.bio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        try(ServerSocket ss = new ServerSocket()) {
            ss.bind(new InetSocketAddress("127.0.0.1", 8888));
            while(true) {
                Socket s = ss.accept();
                new Thread(() -> handle(s)).start();
            }
        } catch (IOException e) {
            logger.error("network bio server error", e);
        }
    }

    private static void handle(Socket s) {
        try {
            byte[] buffer = new byte[1024];
            int len = s.getInputStream().read(buffer);
            logger.info("client says: " + new String(buffer, 0, len));
            s.getOutputStream().write("hello client".getBytes());
            s.getOutputStream().flush();
        } catch (IOException e) {
            logger.error("network bio server handle message error", e);
        }

    }
}
