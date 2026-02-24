package src;

import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

import src.connection.Connection;
import src.http.*;

public class Server {

    private final Selector selector;
    private final Config   config;

    private final Map<SocketChannel, Connection>  connections        = new HashMap<>();
    private final Map<SocketChannel, CgiProcess>  activeCgiProcesses = new HashMap<>();

    private long lastSessionCleanup = System.currentTimeMillis();
    private static final long SESSION_CLEANUP_INTERVAL = 5 * 60 * 1000;

    public Server(Config config) throws Exception {
        this.config   = config;
        this.selector = Selector.open();
        initServers();
    }

    private void initServers() throws Exception {
        for (int port : config.getPorts()) {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            String host = config.getHost();
            if (host == null || host.isEmpty()) host = "0.0.0.0";
            server.bind(new InetSocketAddress(host, port));
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Listening on " + port);
        }
    }

    public void start() {
        while (true) {
            try {
        
                selector.select(10);

                tickCgiProcesses(); 
                handleKeys();
                cleanupTimeouts();
                cleanupSessions();
            } catch (Exception e) {
                System.err.println("[ERROR] Event loop: " + e.getMessage());
            }
        }
    }

    private void tickCgiProcesses() {
        Iterator<Map.Entry<SocketChannel, CgiProcess>> it =
            activeCgiProcesses.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<SocketChannel, CgiProcess> entry = it.next();
            SocketChannel client = entry.getKey();
            CgiProcess    cgi    = entry.getValue();

            cgi.tick(); 

            if (cgi.isDone() || cgi.isError() || cgi.isTimeout()) {
                it.remove();

                Connection conn = connections.get(client);
                if (conn == null) { cgi.destroy(); continue; }

                HttpResponse res;
                if (cgi.isDone())        res = cgi.buildResponse();
                else if (cgi.isTimeout()) res = createErrorResponse(504, "CGI Timeout");
                else                      res = createErrorResponse(500, "CGI Error");

                conn.setResponse(res);

                SelectionKey key = client.keyFor(selector);
                if (key != null && key.isValid())
                    key.interestOps(SelectionKey.OP_WRITE);
            }
        }
    }

    private void handleKeys() throws Exception {
        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
        while (it.hasNext()) {
            SelectionKey key = it.next();
            it.remove();

            if (!key.isValid()) continue;

            if      (key.isAcceptable()) accept(key);
            else if (key.isReadable())   read(key);
            else if (key.isWritable())   write(key);
        }
    }

    private void accept(SelectionKey key) throws Exception {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        if (client == null) return;

        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        connections.put(client, new Connection(client));
    }

    private void read(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        Connection    conn   = connections.get(client);

        if (conn == null) { close(client); return; }

        try {
            conn.read();

            if (conn.isRequestComplete()) {

                if (conn.isContentLengthTooLarge()) {
                    // System.out.println("[413] Content-Length exceeds buffer limit");
                    sendErrorAndWrite(key, client, conn, 413, "Payload Too Large");
                    return;
                }

                HttpRequest req  = RequestParser.parse(conn.getBuffer());
                byte[]      body = req.getBody();

                if (body != null && body.length > config.getClientBodySizeLimit()) {
                    // System.out.println("[413] Request body too large: " + body.length + " bytes");
                    sendErrorAndWrite(key, client, conn, 413, "Payload Too Large");
                    return;
                }

                key.interestOps(0);

                if (isCgiRequest(req)) {
                    
                    try {
                        CgiProcess cgi = new CgiProcess(req, config);
                        activeCgiProcesses.put(client, cgi);
                        
                    } catch (Exception e) {
                        // System.err.println("[CGI start error] " + e.getMessage());
                        sendErrorAndWrite(key, client, conn, 500, "CGI Failed to Start");
                    }
                } else {
                    HttpResponse res = Router.route(req, config);
                    conn.setResponse(res);
                    key.interestOps(SelectionKey.OP_WRITE);
                }
            }
        } catch (Exception e) {
            // System.err.println("[ERROR] Failed to parse request: " + e.getMessage());
            sendErrorAndWrite(key, client, conn, 400, "Bad Request");
        }
    }

    private void write(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        Connection    conn   = connections.get(client);

        if (conn == null) { close(client); return; }

        try {
            conn.write();
            if (conn.isWriteComplete()) close(client);
        } catch (Exception e) {
            close(client);
        }
    }

    private boolean isCgiRequest(HttpRequest req) {
        String path = req.getPath().split("\\?")[0];
        
        for (Config.Route route : config.getRoutes()) {
            if (path.startsWith(route.getPath()) && route.isCgi()) {
                return true;
            }
        }
        return false;
    }

    private void sendErrorAndWrite(SelectionKey key, SocketChannel client,
                                   Connection conn, int code, String reason) {
        try {
            conn.setResponse(createErrorResponse(code, reason));
            key.interestOps(SelectionKey.OP_WRITE);
        } catch (Exception ex) {
            close(client);
        }
    }

    private void close(SocketChannel client) {
        try {
           
            CgiProcess cgi = activeCgiProcesses.remove(client);
            if (cgi != null) cgi.destroy();

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
            if (entry.getValue().isTimedOut(now)) {
                SocketChannel ch = entry.getKey();

                CgiProcess cgi = activeCgiProcesses.remove(ch);
                if (cgi != null) cgi.destroy();

                try { ch.close(); } catch (Exception ignored) {}
                it.remove();
            }
        }
    }

    private void cleanupSessions() {
        long now = System.currentTimeMillis();
        if (now - lastSessionCleanup > SESSION_CLEANUP_INTERVAL) {
            Session.cleanupExpiredSessions();
            lastSessionCleanup = now;
            System.out.println("[CLEANUP] Active sessions: " + Session.getSessionCount());
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
                    return response;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load error page: " + e.getMessage());
        }

        String html = "<!DOCTYPE html><html><head><title>" + statusCode + " " + reason +
                      "</title></head><body><h1>" + statusCode + " " + reason + "</h1></body></html>";
        HttpResponse response = new HttpResponse(statusCode, reason);
        response.addHeader("Content-Type", "text/html; charset=UTF-8");
        response.setBody(html);
        return response;
    }
}