package src;

import java.nio.channels.*;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.*;

public class Server {
    private Selector selector;
    private Config config;
    private Map<SelectionKey, Connection> connections;
    
    public Server(Config config) throws IOException {
        this.config = config;
        this.selector = Selector.open();
        this.connections = new HashMap<>();
        
        for (int port : config.getPorts()) {
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(config.getHost(), port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            System.out.println("Listening on " + config.getHost() + ":" + port);
        }
    }
    
    public void start() {
        while (true) {
            try {
                selector.select(1000);
                
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    
                    if (!key.isValid()) continue;
                    
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                }
                
                cleanupTimeouts();
                
            } catch (IOException e) {
                System.err.println("Error in event loop: " + e.getMessage());
            }
        }
    }
    
    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
            
            Connection conn = new Connection(clientChannel, config);
            connections.put(clientKey, conn);
        }
    }
    
    private void handleRead(SelectionKey key) {
        Connection conn = connections.get(key);
        try {
            conn.read();
            
            if (conn.isRequestComplete()) {
                HttpRequest request = RequestParser.parse(conn.getBuffer());
                HttpResponse response = Router.route(request, config);
                conn.setResponse(response);
                
                key.interestOps(SelectionKey.OP_WRITE);
            }
        } catch (IOException e) {
            closeConnection(key);
        }
    }
    
    private void handleWrite(SelectionKey key) {
        Connection conn = connections.get(key);
        try {
            conn.write();
            
            if (conn.isWriteComplete()) {
                closeConnection(key);
            }
        } catch (IOException e) {
            closeConnection(key);
        }
    }
    
    private void closeConnection(SelectionKey key) {
        connections.remove(key);
        try {
            key.channel().close();
        } catch (IOException e) {
        }
        key.cancel();
    }
    
    private void cleanupTimeouts() {
        long now = System.currentTimeMillis();
        connections.entrySet().removeIf(entry -> {
            if (entry.getValue().isTimedOut(now)) {
                try {
                    entry.getKey().channel().close();
                    entry.getKey().cancel();
                } catch (IOException e) {
                }
                return true;
            }
            return false;
        });
    }
}