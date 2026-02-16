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
            String host = config.getHost();
            if (host == null || host.isEmpty()) {
            host = "0.0.0.0";
        }
            server.bind(new InetSocketAddress(host, port));
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
        
        if (conn == null) {
        close(client);
        return;
    }

        try {
            conn.read();

            if (conn.isRequestComplete()) {

                            if (conn.isContentLengthTooLarge()) {
                System.out.println("[413] Content-Length exceeds buffer limit");
                HttpResponse res = createErrorResponse(413, "Payload Too Large");
                conn.setResponse(res);
                key.interestOps(SelectionKey.OP_WRITE);
                return;  
            }

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
        
        if (conn == null) {
        close(client);
        return;
    }
    
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

    Iterator<Map.Entry<SocketChannel, Connection>> it =
            connections.entrySet().iterator();

    while (it.hasNext()) {
        Map.Entry<SocketChannel, Connection> entry = it.next();
        Connection conn = entry.getValue();

        if (conn.isTimedOut(now)) {
            SocketChannel client = entry.getKey();
            try {
                client.close();
            } catch (Exception ignored) {}

            it.remove();
        }
    }
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
    try {
        String errorPagePath = config.getErrorPages().get(statusCode);
        if (errorPagePath != null) {
            java.io.File errorFile = new java.io.File(errorPagePath);
            if (errorFile.exists()) {
                byte[] content = java.nio.file.Files.readAllBytes(errorFile.toPath());
                HttpResponse response = new HttpResponse(statusCode, reason);
                response.addHeader("Content-Type", "text/html; charset=UTF-8");
                response.setBody(content);
                System.out.println("[DEBUG] Loaded custom error page: " + errorPagePath);
                return response;
            } else {
                System.err.println("[ERROR] Error page file not found: " + errorPagePath);
            }
        } else {
            System.err.println("[ERROR] No error page configured for status: " + statusCode);
        }
    } catch (Exception e) {
        System.err.println("[ERROR] Failed to load error page: " + e.getMessage());
        e.printStackTrace();
    }
    
    String html = "<!DOCTYPE html>" +
                 "<html>" +
                 "<head><title>" + statusCode + " " + reason + "</title></head>" +
                 "<body>" +
                 "<h1>" + statusCode + " " + reason + "</h1>" +
                 "</body>" +
                 "</html>";
    
    HttpResponse response = new HttpResponse(statusCode, reason);
    response.addHeader("Content-Type", "text/html; charset=UTF-8");
    response.setBody(html);
    return response;
}
}