package src;

import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

import src.connection.Connection;
import src.http.*;

public class Server {

    private final Selector selector;
    private final Config config;
    private final Map<SocketChannel, Connection> connections = new HashMap<>();
    private long lastSessionCleanup = System.currentTimeMillis();
    private static final long SESSION_CLEANUP_INTERVAL = 5 * 60 * 1000; // 5 minutes

    public Server(Config config) throws Exception {
        this.config = config;
        this.selector = Selector.open();
        initServers();
    }

    private void initServers() throws Exception {
        for (int port : config.getPorts()) {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(config.getHost(), port));
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Listening on " + port);
        }
    }

    public void start() {
        while (true) {
            try {
                selector.select(1000);
                handleKeys();
                cleanupTimeouts();
                cleanupSessions(); 
            } catch (Exception e) {
                System.err.println("[ERROR] Event loop");
            }
        }
    }

    private void handleKeys() throws Exception {
        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
        while (it.hasNext()) {
            SelectionKey key = it.next();
            it.remove();

            if (!key.isValid()) continue;

            if (key.isAcceptable()) accept(key);
            else if (key.isReadable()) read(key);
            else if (key.isWritable()) write(key);
        }
    }

    private void accept(SelectionKey key) throws Exception {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();

        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);

        connections.put(client, new Connection(client));
    }

    private void read(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        Connection conn = connections.get(client);

        try {
            conn.read();

            if (conn.isRequestComplete()) {
                HttpRequest req = RequestParser.parse(conn.getBuffer());
                
                byte[] body = req.getBody();
                if (body != null) {
                    int bodySize = body.length;
                    if (bodySize > config.getClientBodySizeLimit()) {
                        System.out.println("[413] Request body too large: " + bodySize + " bytes");
                        HttpResponse res = createErrorResponse(413, "Payload Too Large");
                        conn.setResponse(res);
                        key.interestOps(SelectionKey.OP_WRITE);
                        return;
                    }
                }
                
                HttpResponse res = Router.route(req, config);
                conn.setResponse(res);
                key.interestOps(SelectionKey.OP_WRITE);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to parse request: " + e.getMessage());
            try {
                HttpResponse res = createErrorResponse(400, "Bad Request");
                conn.setResponse(res);
                key.interestOps(SelectionKey.OP_WRITE);
            } catch (Exception ex) {
                close(client);
            }
        }
    }

    private void write(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        Connection conn = connections.get(client);

        try {
            conn.write();
            if (conn.isWriteComplete()) close(client);
        } catch (Exception e) {
            close(client);
        }
    }

    private void close(SocketChannel client) {
        try {
            connections.remove(client);
            client.close();
        } catch (Exception ignored) {}
    }

    private void cleanupTimeouts() {
        long now = System.currentTimeMillis();
        connections.values().removeIf(c -> c.isTimedOut(now));
    }
    
    private void cleanupSessions() {
        long now = System.currentTimeMillis();
        if (now - lastSessionCleanup > SESSION_CLEANUP_INTERVAL) {
            Session.cleanupExpiredSessions();
            lastSessionCleanup = now;
            System.out.println("[CLEANUP] Session cleanup completed. Active sessions: " + Session.getSessionCount());
        }
    }
    
    private HttpResponse createErrorResponse(int statusCode, String reason) {
        HttpResponse response = new HttpResponse(statusCode, reason);
        response.setBody(reason);
        response.addHeader("Content-Type", "text/plain");
        return response;
    }
}