package com.my.project.io.network.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Client {

    public static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        try(Selector selector = Selector.open();
            SocketChannel sc = SocketChannel.open()) {
            sc.configureBlocking(false);
            sc.register(selector, SelectionKey.OP_READ);
            sc.connect(new InetSocketAddress("127.0.0.1", 8888));

            while(!sc.finishConnect()) {
                logger.info("check finish connection");
            }

            // write once
            doWrite(sc);

            while(selector.select(5 * 1000) <=0) {
                logger.info("wait 5 seconds to read from server");
            }

            // read once
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();
                if (key.isReadable()) {
                    doRead(key);
                }
            }
        } catch (IOException e) {
            logger.error("network nio client error", e);
        }
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
            logger.info("server says: " + new String(output.toByteArray()));
        }
    }

    private static void doWrite(SocketChannel sc) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap("hello server".getBytes());
        while(buffer.hasRemaining()) {
            sc.write(buffer);
        }
    }
}
