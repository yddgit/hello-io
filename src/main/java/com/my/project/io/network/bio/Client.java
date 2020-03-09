package com.my.project.io.network.bio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.awt.windows.WBufferStrategy;

import java.io.IOException;
import java.net.Socket;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        try(Socket s = new Socket("127.0.0.1", 8888)) {
            s.getOutputStream().write("hello server".getBytes());
            s.getOutputStream().flush();
            byte[] buffer = new byte[1024];
            int len = s.getInputStream().read(buffer);
            logger.info("server says: " + new String(buffer, 0, len));
        } catch (IOException e) {
            logger.error("network bio client error", e);
        }
    }
}
