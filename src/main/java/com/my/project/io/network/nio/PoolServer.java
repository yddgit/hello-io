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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PoolServer {

    private static final Logger logger = LoggerFactory.getLogger(PoolServer.class);

    ExecutorService pool = Executors.newFixedThreadPool(50);
    private Selector selector;

    public static void main(String[] args) {
        PoolServer server = new PoolServer();
        try {
            server.initServer("127.0.0.1", 8888);
            server.listen();
        } catch (IOException e) {
            logger.error("network nio pool server error", e);
        }
    }

    public void initServer(String hostname, int port) throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(hostname, port));
        server.configureBlocking(false);
        this.selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        logger.info("server started, listening on :" + server.getLocalAddress());
    }

    public void listen() throws IOException {
        while(true) {
            selector.select();
            Iterator<SelectionKey> it = this.selector.selectedKeys().iterator() ;
            while(it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();
                if(key.isAcceptable()) {
                    doAccept(key);
                } else if(key.isReadable()) {
                    key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
                    pool.execute(new ChannelHandlerThread(key));
                }
            }
        }
    }

    private void doAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel channel = server.accept();
        channel.configureBlocking(false);
        channel.register(this.selector, SelectionKey.OP_READ);
    }

    private class ChannelHandlerThread implements Runnable {

        private SelectionKey key;

        ChannelHandlerThread(SelectionKey key) {
            this.key = key;
        }

        @Override
        public void run() {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                int len = 0;
                while((len = channel.read(buffer)) > 0) {
                    buffer.flip();
                    output.write(buffer.array(), 0, len);
                    buffer.clear();
                }
                output.close();

                byte[] content = output.toByteArray();
                if(content.length > 0) {
                    logger.info("client says: " + new String(content, 0, content.length));
                }

                byte[] echo = "[echo] ".getBytes();
                ByteBuffer writeBuffer = ByteBuffer.allocate(echo.length+ content.length);
                writeBuffer.put(echo);
                writeBuffer.put(content);
                writeBuffer.flip();
                channel.write(writeBuffer);
                if(len == -1) {
                    channel.close();
                } else {
                    key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                    key.selector().wakeup();
                }
            } catch (IOException e) {
                logger.error("network nio pool server channel handler thread error", e);
            }
        }
    }
}
