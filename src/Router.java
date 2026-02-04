package src;

import src.http.HttpRequest;
import src.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Router {
    
    public static HttpResponse route(HttpRequest request, Config config) {
        String method = request.getMethod();
        String path = request.getPath();
        
        System.out.println("[REQUEST] " + method + " " + path);
        
        for (Config.Route route : config.getRoutes()) {
            if (matchesRoute(path, route.getPath())) {
                if (!route.getAllowedMethods().contains(method)) {
                    return new HttpResponse(405, "Method Not Allowed");
                }
                
                System.out.println("[MATCH] Route matched: " + route.getPath());
                return serveFile(request, route);
            }
        }
        
        System.out.println("[404] No route matched for: " + path);
        return new HttpResponse(404, "Not Found");
    }
    
    private static HttpResponse serveFile(HttpRequest request, Config.Route route) {
        try {
            String requestPath = request.getPath();
            
            if (requestPath.equals("/") || requestPath.equals(route.getPath())) {
                if (route.getDefaultFile() != null) {
                    requestPath = "/" + route.getDefaultFile();
                }
            }
            
            Path filePath = Paths.get("www", requestPath);
            
            System.out.println("[FILE] Attempting to serve: " + filePath);
            
            if (!Files.exists(filePath)) {
                System.out.println("[404] File not found: " + filePath);
                HttpResponse response = new HttpResponse(404, "Not Found");
                response.setBody("<h1>404 Not Found</h1>");
                response.addHeader("Content-Type", "text/html");
                return response;
            }
            
            byte[] content = Files.readAllBytes(filePath);
            
            String contentType = getContentType(filePath.toString());
            
            HttpResponse response = new HttpResponse(200, "OK");
            response.setBody(content);
            response.addHeader("Content-Type", contentType);
            
            System.out.println("[200] Served: " + filePath + " (" + content.length + " bytes)");
            return response;
            
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to serve file: " + e.getMessage());
            HttpResponse response = new HttpResponse(500, "Internal Server Error");
            response.setBody("<h1>500 Internal Server Error</h1>");
            response.addHeader("Content-Type", "text/html");
            return response;
        }
    }
    
    private static String getContentType(String filePath) {
        if (filePath.endsWith(".html")) return "text/html";
        if (filePath.endsWith(".css")) return "text/css";
        if (filePath.endsWith(".js")) return "application/javascript";
        if (filePath.endsWith(".json")) return "application/json";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
        if (filePath.endsWith(".gif")) return "image/gif";
        if (filePath.endsWith(".svg")) return "image/svg+xml";
        return "text/plain";
    }
    
    private static boolean matchesRoute(String requestPath, String routePath) {
        if (routePath.equals("/")) {
            return true;
        }
        return requestPath.equals(routePath) || requestPath.startsWith(routePath + "/");
    }
}