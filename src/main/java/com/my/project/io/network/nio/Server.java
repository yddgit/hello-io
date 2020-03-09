package com.my.project.io.network.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        try(ServerSocketChannel ssc = ServerSocketChannel.open();
            Selector selector = Selector.open()) {
            ssc.socket().bind(new InetSocketAddress("127.0.0.1", 8888));
            ssc.configureBlocking(false);

            logger.info("server started, listening on :" + ssc.getLocalAddress());

            ssc.register(selector, SelectionKey.OP_ACCEPT);

            while(true) {
                int select = selector.select();
                if(select > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> it = keys.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        handle(key);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("network nio server error", e);
        }
    }

    private static void handle(SelectionKey key) throws IOException {
        if(key.isAcceptable()) {
            doAccept(key);
        } else if(key.isReadable()) {
            doRead(key);
        }
    }

    private static void doAccept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssc.accept();
        sc.configureBlocking(false);
        sc.register(key.selector(), SelectionKey.OP_READ);
    }

    private static void doRead(SelectionKey key) throws IOException {
        try (SocketChannel sc = (SocketChannel) key.channel()) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            int len = sc.read(buffer);
            while (len > 0) {
                buffer.flip();

                byte[] data = new byte[buffer.limit()];
                buffer.get(data);

                output.write(data);

                buffer.clear();

                len = sc.read(buffer);
            }
            output.close();
            logger.info("client says: " + new String(output.toByteArray()));

            doWrite(sc);
        }
    }

    private static void doWrite(SocketChannel sc) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap("hello client".getBytes());
        while(buffer.hasRemaining()) {
            sc.write(buffer);
        }
    }

}
